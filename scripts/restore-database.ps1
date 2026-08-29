<#
.SYNOPSIS
    받아 둔 백업 파일로 데이터베이스를 되돌린다.

.DESCRIPTION
    복원을 해 보지 않은 백업은 백업이 아니다. 뜨는 것만 만들어 두면 정작 필요할 때
    되돌아가지 않는다는 것을 그때 알게 된다.

    되돌리는 것은 지우는 일이기도 하다. 대상 데이터베이스의 지금 내용은 사라진다.
    그래서 이름을 다시 한 번 확인받는다.

.PARAMETER BackupFile
    되돌릴 백업 파일이다.

.PARAMETER TargetDatabase
    되돌릴 대상 데이터베이스다. 적지 않으면 .env의 이름을 쓴다.

.PARAMETER EnvFile
    접속 정보를 읽을 파일이다.

.PARAMETER Force
    확인을 묻지 않는다. 사람이 없는 곳에서 돌릴 때만 쓴다.

.EXAMPLE
    .\scripts\restore-database.ps1 -BackupFile backup\maplemetric-20260829-090000.dump `
        -TargetDatabase maplemetric_restore_test

.EXAMPLE
    .\scripts\restore-database.ps1 -BackupFile backup\maplemetric-20260829-090000.dump
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

function Fail([string]$Reason) {
    Write-Error $Reason
    exit 1
}

function Read-EnvFile([string]$Path) {
    if (-not (Test-Path $Path)) {
        Fail "접속 정보를 읽을 파일이 없다: $Path"
    }

    $values = @{}

    foreach ($line in Get-Content $Path -Encoding UTF8) {
        $trimmed = $line.Trim()

        if ($trimmed -eq '' -or $trimmed.StartsWith('#')) {
            continue
        }

        $separator = $trimmed.IndexOf('=')

        if ($separator -gt 0) {
            $values[$trimmed.Substring(0, $separator).Trim()] =
                $trimmed.Substring($separator + 1).Trim()
        }
    }

    return $values
}

function Require-Value($Values, [string]$Name) {
    if (-not $Values.ContainsKey($Name) -or $Values[$Name] -eq '') {
        Fail "접속 정보에 $Name 이(가) 없다."
    }

    return $Values[$Name]
}

if (-not (Test-Path $BackupFile)) {
    Fail "백업 파일이 없다: $BackupFile"
}

$settings = Read-EnvFile $EnvFile

$container = 'maplemetric-postgres'
$user = Require-Value $settings 'DB_USERNAME'
$password = Require-Value $settings 'DB_PASSWORD'

if ($TargetDatabase -eq '') {
    $TargetDatabase = Require-Value $settings 'DB_NAME'
}

$running = docker ps --filter "name=$container" --format '{{.Names}}'

if ($running -ne $container) {
    Fail "데이터베이스 컨테이너가 떠 있지 않다: $container"
}

if (-not $Force) {
    Write-Output ''
    Write-Output "되돌릴 대상: $TargetDatabase"
    Write-Output "쓸 백업    : $BackupFile"
    Write-Output ''
    Write-Output '대상의 지금 내용은 사라진다.'

    $answer = Read-Host "되돌리려면 대상 이름을 그대로 입력한다"

    if ($answer -ne $TargetDatabase) {
        Fail '이름이 다르다. 아무것도 하지 않았다.'
    }
}

$inContainer = "/tmp/maplemetric-restore.dump"

Write-Output '백업 파일을 컨테이너로 옮긴다.'
docker cp $BackupFile "${container}:${inContainer}"

if ($LASTEXITCODE -ne 0) {
    Fail "백업 파일을 옮기지 못했다. 종료 코드 $LASTEXITCODE"
}

try {
    Write-Output "대상을 비우고 다시 만든다: $TargetDatabase"

    # 이어 붙이지 않고 비운 자리에 넣는다. 남아 있던 행 위에 덮으면 백업에 없던 행이
    # 살아남아, 되돌렸다고 적어 두고 실제로는 뒤섞인 상태가 된다.
    docker exec -e PGPASSWORD=$password $container `
        psql -U $user -d postgres `
        -c "DROP DATABASE IF EXISTS `"$TargetDatabase`" WITH (FORCE)"

    if ($LASTEXITCODE -ne 0) {
        Fail "대상을 비우지 못했다. 종료 코드 $LASTEXITCODE"
    }

    docker exec -e PGPASSWORD=$password $container `
        psql -U $user -d postgres `
        -c "CREATE DATABASE `"$TargetDatabase`""

    if ($LASTEXITCODE -ne 0) {
        Fail "대상을 만들지 못했다. 종료 코드 $LASTEXITCODE"
    }

    Write-Output '되돌리는 중이다.'

    docker exec -e PGPASSWORD=$password $container `
        pg_restore -U $user -d $TargetDatabase --no-owner $inContainer

    if ($LASTEXITCODE -ne 0) {
        Fail "되돌리지 못했다. 종료 코드 $LASTEXITCODE"
    }
}
finally {
    # 백업 파일에는 데이터 전체가 들어 있다. 컨테이너 안에 남겨 두지 않는다.
    docker exec $container rm -f $inContainer | Out-Null
}

Write-Output ''
Write-Output '되돌렸다. 행 수를 확인한다.'

docker exec -e PGPASSWORD=$password $container `
    psql -U $user -d $TargetDatabase -At -F'|' -c @'
SELECT 'p_overall_ranking_collection', COUNT(*)
FROM p_overall_ranking_collection
UNION ALL
SELECT 'p_overall_ranking_snapshot', COUNT(*)
FROM p_overall_ranking_snapshot
UNION ALL
SELECT 'flyway_schema_history', COUNT(*)
FROM flyway_schema_history
'@
