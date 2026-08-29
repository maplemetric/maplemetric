<#
.SYNOPSIS
    PostgreSQL 데이터를 파일 하나로 받아 둔다.

.DESCRIPTION
    Docker의 named volume은 컨테이너를 지워도 데이터가 남는다는 뜻이지 백업이 아니다.
    volume을 잘못 지우거나 디스크가 죽으면 그대로 잃는다.

    잃는 것의 크기가 문제다. 랭킹 저장본 가운데 외부가 이력을 주는 기간을 넘긴 기준일은
    다시 받을 수 없다. 시간이 지날수록 복구할 수 없는 몫이 늘어난다.

    접속 정보는 .env에서 읽는다. 비밀번호를 명령 인자로 넘기면 프로세스 목록에 남는다.

.PARAMETER OutputDirectory
    백업 파일을 둘 위치다. 없으면 만든다.

.PARAMETER KeepCount
    남겨 둘 백업 개수다. 이보다 오래된 것부터 지운다.

.PARAMETER EnvFile
    접속 정보를 읽을 파일이다.

.EXAMPLE
    .\scripts\backup-database.ps1

.EXAMPLE
    .\scripts\backup-database.ps1 -OutputDirectory D:\backup -KeepCount 14
#>

param(
    [string]$OutputDirectory = 'backup',

    [ValidateRange(1, 365)]
    [int]$KeepCount = 7,

    [string]$EnvFile = '.env'
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
            $name = $trimmed.Substring(0, $separator).Trim()
            $value = $trimmed.Substring($separator + 1).Trim()

            $values[$name] = $value
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

$settings = Read-EnvFile $EnvFile

$container = 'maplemetric-postgres'
$database = Require-Value $settings 'DB_NAME'
$user = Require-Value $settings 'DB_USERNAME'
$password = Require-Value $settings 'DB_PASSWORD'

$running = docker ps --filter "name=$container" --format '{{.Names}}'

if ($running -ne $container) {
    Fail "데이터베이스 컨테이너가 떠 있지 않다: $container"
}

if (-not (Test-Path $OutputDirectory)) {
    New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
}

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$target = Join-Path $OutputDirectory "maplemetric-$stamp.dump"

Write-Output "백업을 시작한다: $target"

$inContainer = '/tmp/maplemetric-backup.dump'

try {
    # custom 형식으로 받는다. 평문 SQL보다 작고, 복원할 때 표를 골라 되돌릴 수 있다.
    #
    # 컨테이너 안에 쓰고 꺼낸다. 표준 출력으로 받아 넘기면 셸이 그 바이트를 글자로
    # 다루면서 내용이 어긋난다. 그렇게 만든 파일은 복원할 때에야 깨진 것을 알게 된다.
    #
    # 비밀번호는 인자가 아니라 환경변수로 넘긴다. 인자로 주면 컨테이너 안팎의 프로세스
    # 목록에 그대로 보인다.
    docker exec -e PGPASSWORD=$password $container `
        pg_dump -U $user -d $database -Fc -f $inContainer

    if ($LASTEXITCODE -ne 0) {
        Fail "백업에 실패했다. 종료 코드 $LASTEXITCODE"
    }

    docker cp "${container}:${inContainer}" $target

    if ($LASTEXITCODE -ne 0) {
        Fail "백업 파일을 꺼내지 못했다. 종료 코드 $LASTEXITCODE"
    }
}
finally {
    # 파일 하나에 데이터 전체가 들어 있다. 컨테이너 안에 남겨 두지 않는다.
    docker exec $container rm -f $inContainer | Out-Null
}

$size = (Get-Item $target).Length

if ($size -eq 0) {
    Remove-Item $target -Force
    Fail '백업 파일이 비었다. 지우고 멈춘다.'
}

Write-Output ("완료: {0} ({1:N1} MB)" -f $target, ($size / 1MB))

# 오래된 것부터 지운다. 무한히 쌓이면 디스크를 채우고, 그러면 백업 자체가 실패한다.
$stale = Get-ChildItem -Path $OutputDirectory -Filter 'maplemetric-*.dump' |
    Sort-Object LastWriteTime -Descending |
    Select-Object -Skip $KeepCount

foreach ($file in $stale) {
    Write-Output "오래된 백업을 지운다: $($file.Name)"
    Remove-Item $file.FullName -Force
}
