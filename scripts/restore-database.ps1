<#
.SYNOPSIS
    받아 둔 백업 파일로 데이터베이스를 되돌린다.

.DESCRIPTION
    복원을 해 보지 않은 백업은 백업이 아니다. 뜨는 것만 만들어 두면 정작 필요할 때
    되돌아가지 않는다는 것을 그때 알게 된다.

    되돌리기가 성공한 뒤에만 대상을 바꾼다. 대상을 먼저 비우면, 백업이 깨져 있거나
    되돌리다 멈췄을 때 원래 있던 것도 없고 새로 넣은 것도 온전하지 않은 상태가 된다.

.PARAMETER BackupFile
    되돌릴 백업 파일이다.

.PARAMETER TargetDatabase
    되돌릴 대상 데이터베이스다. 적지 않으면 .env의 이름을 쓴다.

.PARAMETER EnvFile
    접속 정보를 읽을 파일이다.

.PARAMETER Force
    확인을 묻지 않는다. 사람이 없는 곳에서 돌릴 때만 쓴다.

.EXAMPLE
    .\scripts\restore-database.ps1 -BackupFile backup\maplemetric-20260829-105948.dump `
        -TargetDatabase maplemetric_restore_test

.EXAMPLE
    .\scripts\restore-database.ps1 -BackupFile backup\maplemetric-20260829-105948.dump
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$BackupFile,

    [string]$TargetDatabase = '',

    [string]$EnvFile = '.env',

    [switch]$Force
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\database-common.ps1"

function Invoke-Psql([string]$Container, [string]$User, [string]$Sql) {
    docker exec -e PGPASSWORD $Container `
        psql -U $User -d postgres -v ON_ERROR_STOP=1 -c $Sql | Out-Null

    return $LASTEXITCODE
}

if (-not (Test-Path $BackupFile)) {
    Fail "백업 파일이 없다: $BackupFile"
}

$settings = Read-EnvFile $EnvFile

$container = Get-DatabaseContainerName
$user = Require-Value $settings 'DB_USERNAME'
$password = Require-Value $settings 'DB_PASSWORD'

if ($TargetDatabase -eq '') {
    $TargetDatabase = Require-Value $settings 'DB_NAME'
}

Assert-DatabaseName $TargetDatabase
Assert-ContainerRunning $container

if (-not $Force) {
    Write-Output ''
    Write-Output "되돌릴 대상: $TargetDatabase"
    Write-Output "쓸 백업    : $BackupFile"
    Write-Output ''
    Write-Output '되돌리기가 성공한 뒤에 대상이 바뀐다. 지금 내용은 그때 사라진다.'

    $answer = Read-Host '되돌리려면 대상 이름을 그대로 입력한다'

    if ($answer -ne $TargetDatabase) {
        Fail '이름이 다르다. 아무것도 하지 않았다.'
    }
}

$runId = [guid]::NewGuid().ToString('N').Substring(0, 8)

$staging = "${TargetDatabase}_restoring_$runId"
$retired = "${TargetDatabase}_replaced_$runId"

Assert-DatabaseName $staging
Assert-DatabaseName $retired

$inContainer = "/tmp/maplemetric-restore-$runId.dump"

Set-DatabasePassword $password

# 임시 자리가 아직 남아 있는지다. 제자리로 옮기고 나면 더 지울 것이 없다.
$stagingActive = $false

# 원래 대상을 옆으로 치워 두었는지다.
$retiredActive = $false

try {
    Write-Output '백업 파일을 컨테이너로 옮긴다.'
    docker cp $BackupFile "${container}:${inContainer}"

    if ($LASTEXITCODE -ne 0) {
        Fail "백업 파일을 옮기지 못했다. 종료 코드 $LASTEXITCODE"
    }

    Write-Output "먼저 임시 자리에 되돌린다: $staging"

    if ((Invoke-Psql $container $user "CREATE DATABASE `"$staging`"") -ne 0) {
        Fail '임시 자리를 만들지 못했다.'
    }

    $stagingActive = $true

    # 되돌리다 실패하면 여기서 멈춘다. 대상은 아직 그대로다.
    docker exec -e PGPASSWORD $container `
        pg_restore -U $user -d $staging --no-owner --exit-on-error $inContainer

    if ($LASTEXITCODE -ne 0) {
        Fail "되돌리지 못했다. 대상은 그대로다. 종료 코드 $LASTEXITCODE"
    }

    Write-Output '되돌리기가 끝났다. 대상을 바꾼다.'

    # 붙어 있는 연결이 하나라도 있으면 이름을 바꾸지 못한다. 뒤의 강제 삭제와 달리
    # 이름 바꾸기는 연결을 알아서 끊지 않는다. 앱을 켜 둔 채로 되돌리면 여기서
    # 멈추므로, 남은 연결을 먼저 끊는다.
    #
    # 앱을 미리 멈추는 편이 낫다. 이것은 안전장치이지 대신하는 방법이 아니다.
    Invoke-Psql $container $user (
        "SELECT pg_terminate_backend(pid) FROM pg_stat_activity " +
        "WHERE datname = '$TargetDatabase' AND pid <> pg_backend_pid()"
    ) | Out-Null

    # 이름만 바꿔 자리를 옮긴다. 원래 대상은 곧바로 지우지 않고 옆으로 치워 둔다.
    # 바꾸는 도중에 무슨 일이 생겨도 되돌아갈 곳이 남아야 한다.
    $targetExists = docker exec -e PGPASSWORD $container `
        psql -U $user -d postgres -At `
        -c "SELECT 1 FROM pg_database WHERE datname = '$TargetDatabase'"

    if ($targetExists -eq '1') {
        if ((Invoke-Psql $container $user `
                    "ALTER DATABASE `"$TargetDatabase`" RENAME TO `"$retired`"") -ne 0) {
            Fail '대상을 옆으로 치우지 못했다. 아무것도 바뀌지 않았다.'
        }

        $retiredActive = $true
    }

    if ((Invoke-Psql $container $user `
                "ALTER DATABASE `"$staging`" RENAME TO `"$TargetDatabase`"") -ne 0) {
        # 여기서 그냥 멈추면 대상 이름을 가진 데이터베이스가 없어진다. 원래 것은
        # 치워 둔 이름에만 있고, 앱은 뜨지 못한다. 실패한 복원이 장애가 된다.
        # 치워 둔 것을 제자리로 되돌려 놓고 멈춘다.
        Write-Output '제자리에 놓지 못했다. 치워 둔 것을 되돌린다.'

        if ((Invoke-Psql $container $user `
                    "ALTER DATABASE `"$retired`" RENAME TO `"$TargetDatabase`"") -eq 0) {
            $retiredActive = $false

            Fail '되돌린 것을 제자리에 놓지 못했다. 원래 것을 되살렸다.'
        }

        # 둘 다 남겨 둔다. 사람이 골라 제자리에 놓아야 하는데, 여기서 되돌린 것을
        # 지우면 고를 것이 하나만 남는다.
        $stagingActive = $false

        Fail ("되돌린 것을 제자리에 놓지 못했고 원래 것도 되살리지 못했다. " +
            "원래 것은 $retired 에, 되돌린 것은 $staging 에 있다. " +
            "둘 중 하나를 $TargetDatabase 로 직접 바꿔야 한다.")
    }

    $stagingActive = $false

    if ($retiredActive) {
        Write-Output "치워 둔 이전 데이터베이스를 지운다: $retired"

        # 지우지 못했는데 성공으로 알리면, 되돌릴 때마다 통째로 남은 데이터베이스가
        # 하나씩 쌓인다. 그것 하나가 원본만 한 크기다.
        if ((Invoke-Psql $container $user `
                    "DROP DATABASE IF EXISTS `"$retired`" WITH (FORCE)") -ne 0) {
            Fail ("되돌리기는 끝냈지만 치워 둔 것을 지우지 못했다. " +
                "$retired 이(가) 남아 있다. 확인한 뒤 직접 지운다.")
        }

        $retiredActive = $false
    }
}
finally {
    # 되돌리다 멈췄으면 임시 자리를 치운다. 남겨 두면 다음 사람이 무엇인지 모른다.
    if ($stagingActive) {
        Write-Output '임시 자리를 치운다.'

        Invoke-Psql $container $user `
            "DROP DATABASE IF EXISTS `"$staging`" WITH (FORCE)" | Out-Null
    }

    # 백업 파일에는 데이터 전체가 들어 있다. 컨테이너 안에 남겨 두지 않는다.
    docker exec $container rm -f $inContainer 2>$null | Out-Null

    Clear-DatabasePassword
}

Set-DatabasePassword $password

try {
    Write-Output ''
    Write-Output '되돌렸다. 행 수를 확인한다.'

    docker exec -e PGPASSWORD $container `
        psql -U $user -d $TargetDatabase -At -F'|' -v ON_ERROR_STOP=1 -c @'
SELECT 'p_overall_ranking_collection', COUNT(*)
FROM p_overall_ranking_collection
UNION ALL
SELECT 'p_overall_ranking_snapshot', COUNT(*)
FROM p_overall_ranking_snapshot
UNION ALL
SELECT 'flyway_schema_history', COUNT(*)
FROM flyway_schema_history
'@

    # 확인이 실패했는데 성공으로 끝내면, 사람이 없는 곳에서 돌린 되돌리기가 무엇을
    # 되돌렸는지 아무도 보지 않은 채 성공으로 기록된다.
    if ($LASTEXITCODE -ne 0) {
        Fail "되돌린 것을 확인하지 못했다. 종료 코드 $LASTEXITCODE"
    }
}
finally {
    Clear-DatabasePassword
}
