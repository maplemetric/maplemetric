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

    [switch]$Force,

    <#
        되돌린 것에 이만큼은 들어 있어야 한다.

        표만 있고 비어 있는 백업은 되돌리기가 성공한다. 그대로 자리를 바꾸면 쓰던
        것을 빈 것으로 갈아치운다. 시험용으로 만든 백업이나 수집 전에 뜬 백업이
        그렇다.
    #>
    [ValidateRange(0, [int]::MaxValue)]
    [int]$MinimumCollectionCount = 1
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# 확인 질의의 종료 코드다. 결과 줄과 섞이지 않게 따로 둔다.
$script:verifyExitCode = 0

. "$PSScriptRoot\database-common.ps1"

<#
    되돌린 것이 쓸 만한지 본다.

    pg_restore가 성공했다는 것은 파일을 읽었다는 뜻이지 그것이 맞는 백업이라는 뜻이
    아니다. 다른 데이터베이스를 받아 둔 파일이거나 표가 빠져 있어도 되돌리기 자체는
    끝난다. 그래서 자리를 바꾸기 전에 여기서 본다.
#>
function Invoke-Verify([string]$Container, [string]$User, [string]$Database) {
    # 결과 줄과 종료 코드를 함께 돌려주지 않는다. 함수 안에서 출력한 줄도 반환값에
    # 섞이므로, 그대로 0과 견주면 줄이 하나라도 있는 한 다르다고 나온다.
    $rows = docker exec -e PGPASSWORD $Container `
        psql -U $User -d $Database -At -F'|' -v ON_ERROR_STOP=1 -c @'
SELECT 'p_overall_ranking_collection', COUNT(*)
FROM p_overall_ranking_collection
UNION ALL
SELECT 'p_overall_ranking_snapshot', COUNT(*)
FROM p_overall_ranking_snapshot
UNION ALL
SELECT 'flyway_schema_history', COUNT(*)
FROM flyway_schema_history
'@

    $script:verifyExitCode = $LASTEXITCODE

    foreach ($row in $rows) {
        Write-Output $row
    }
}

<#
    관리용 데이터베이스에 붙어 이름 바꾸기와 삭제를 한다.

    지금 붙어 있는 데이터베이스는 이름을 바꿀 수 없다. 대상이 postgres면 거기에
    붙은 채로 그것을 바꾸려다 거부당한다. 그때는 다른 곳에 붙는다.
#>
function Invoke-Psql([string]$Container, [string]$User, [string]$Sql) {
    docker exec -e PGPASSWORD $Container `
        psql -U $User -d $script:maintenanceDatabase -v ON_ERROR_STOP=1 -c $Sql |
        Out-Null

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

# 대상과 같은 곳에 붙으면 그 이름을 바꿀 수 없다.
$script:maintenanceDatabase = if ($TargetDatabase -eq 'postgres') {
    'template1'
} else {
    'postgres'
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

    Write-Output ''
    Write-Output '되돌린 것을 확인한다.'

    # 확인이 끝나기 전에는 대상을 건드리지 않는다. 여기서 실패하면 임시 자리만
    # 지우고 멈추므로, 잘못된 백업을 들고 와도 쓰던 것을 잃지 않는다.
    Invoke-Verify $container $user $staging

    if ($script:verifyExitCode -ne 0) {
        Fail '되돌린 것이 쓸 만하지 않다. 대상은 그대로다.'
    }

    $collectionCount = docker exec -e PGPASSWORD $container `
        psql -U $user -d $staging -At -v ON_ERROR_STOP=1 `
        -c 'SELECT COUNT(*) FROM p_overall_ranking_collection'

    if ([int]$collectionCount -lt $MinimumCollectionCount) {
        Fail ("되돌린 것에 수집 기준일이 $collectionCount 개뿐이다. " +
            "$MinimumCollectionCount 개 이상이어야 한다. 대상은 그대로다.")
    }

    if (-not $Force) {
        Write-Output ''
        Write-Output '위 행 수가 되돌리려던 것과 맞는지 본다.'
        Write-Output "이 뒤로는 $TargetDatabase 의 지금 내용이 사라진다."

        $answer = Read-Host '이대로 바꾸려면 yes 를 입력한다'

        if ($answer -ne 'yes') {
            Fail '바꾸지 않았다. 대상은 그대로다.'
        }
    }

    Write-Output ''
    Write-Output '대상을 바꾼다.'

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
        psql -U $user -d $script:maintenanceDatabase -At `
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
        # 치워 둔 것이 없으면 되살릴 것도 없다. 처음부터 없던 이름으로 되돌리는
        # 경우다. 임시 자리는 그대로 두어 뒷정리가 지우게 한다.
        if (-not $retiredActive) {
            Fail '되돌린 것을 제자리에 놓지 못했다. 원래 없던 이름이라 바뀐 것은 없다.'
        }

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
    # 자리를 바꾸는 도중에 끊겼을 수 있다. 원래 것을 옆으로 치워 둔 채로 멈추면
    # 대상 이름을 가진 데이터베이스가 없어져 앱이 뜨지 못한다. 실패한 되돌리기가
    # 장애가 되지 않도록, 그 이름이 비어 있으면 치워 둔 것을 제자리로 돌려놓는다.
    if ($retiredActive) {
        $targetNow = docker exec -e PGPASSWORD $container `
            psql -U $user -d $script:maintenanceDatabase -At `
            -c "SELECT 1 FROM pg_database WHERE datname = '$TargetDatabase'"

        if ($targetNow -ne '1') {
            Write-Output "대상 이름이 비어 있다. 치워 둔 것을 되돌린다: $retired"

            Invoke-Psql $container $user `
                "ALTER DATABASE `"$retired`" RENAME TO `"$TargetDatabase`"" |
                Out-Null
        }
    }

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
    Write-Output '제자리에서 다시 확인한다.'

    # 자리를 바꾼 뒤에도 한 번 더 본다. 바꾸는 도중에 어긋났다면 여기서 드러난다.
    Invoke-Verify $container $user $TargetDatabase

    if ($script:verifyExitCode -ne 0) {
        Fail '제자리에 놓인 것을 확인하지 못했다.'
    }
}
finally {
    Clear-DatabasePassword
}
