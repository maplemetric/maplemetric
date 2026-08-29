<#
.SYNOPSIS
    PostgreSQL 데이터를 파일 하나로 받아 둔다.

.DESCRIPTION
    Docker의 named volume은 컨테이너를 지워도 데이터가 남는다는 뜻이지 백업이 아니다.
    volume을 잘못 지우거나 디스크가 죽으면 그대로 잃는다.

    잃는 것의 크기가 문제다. 랭킹 저장본 가운데 외부가 이력을 주는 기간을 넘긴 기준일은
    다시 받을 수 없다. 시간이 지날수록 복구할 수 없는 몫이 늘어난다.

    접속 정보는 .env에서 읽는다. 비밀번호는 명령 인자에 싣지 않는다.

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

. "$PSScriptRoot\database-common.ps1"

$settings = Read-EnvFile $EnvFile

$container = Get-DatabaseContainerName
$database = Require-Value $settings 'DB_NAME'
$user = Require-Value $settings 'DB_USERNAME'
$password = Require-Value $settings 'DB_PASSWORD'

Assert-ContainerRunning $container

if (-not (Test-Path $OutputDirectory)) {
    New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
}

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$runId = [guid]::NewGuid().ToString('N').Substring(0, 8)

$target = Join-Path $OutputDirectory "maplemetric-$stamp.dump"

# 받는 도중의 파일에는 다른 이름을 준다. 중간에 멈추면 온전한 백업과 같은 이름의
# 반쪽짜리가 남고, 나중에 그것을 꺼내 쓰려다 안 된다는 것을 그때 알게 된다.
$partial = "$target.partial"

# 실행마다 다른 이름을 쓴다. 예약 실행과 사람이 누른 실행이 겹치면 같은 파일에
# 두 pg_dump가 쓰고, 한쪽이 쓰는 중에 다른 쪽이 지운다.
$inContainer = "/tmp/maplemetric-backup-$runId.dump"

Write-Output "백업을 시작한다: $target"

Set-DatabasePassword $password

try {
    # custom 형식으로 받는다. 평문 SQL보다 작고, 복원할 때 표를 골라 되돌릴 수 있다.
    #
    # 컨테이너 안에 쓰고 꺼낸다. 표준 출력으로 받아 넘기면 셸이 그 바이트를 글자로
    # 다루면서 내용이 어긋난다. 그렇게 만든 파일은 복원할 때에야 깨진 것을 알게 된다.
    docker exec -e PGPASSWORD $container `
        pg_dump -U $user -d $database -Fc -f $inContainer

    if ($LASTEXITCODE -ne 0) {
        Fail "백업에 실패했다. 종료 코드 $LASTEXITCODE"
    }

    docker cp "${container}:${inContainer}" $partial

    if ($LASTEXITCODE -ne 0) {
        Fail "백업 파일을 꺼내지 못했다. 종료 코드 $LASTEXITCODE"
    }

    if (-not (Test-Path $partial)) {
        Fail '백업 파일이 만들어지지 않았다.'
    }

    $size = (Get-Item $partial).Length

    if ($size -eq 0) {
        Fail '백업 파일이 비었다.'
    }

    # 온전히 받은 뒤에만 제 이름을 준다.
    Move-Item -Path $partial -Destination $target -Force

    Write-Output ("완료: {0} ({1:N1} MB)" -f $target, ($size / 1MB))
}
finally {
    Clear-DatabasePassword

    # 파일 하나에 데이터 전체가 들어 있다. 컨테이너 안에 남겨 두지 않는다.
    docker exec $container rm -f $inContainer 2>$null | Out-Null

    if (Test-Path $partial) {
        Write-Output '받다 만 파일을 지운다.'
        Remove-Item $partial -Force
    }
}

# 오래된 것부터 지운다. 무한히 쌓이면 디스크를 채우고, 그러면 백업 자체가 실패한다.
$stale = Get-ChildItem -Path $OutputDirectory -Filter 'maplemetric-*.dump' |
    Where-Object { -not $_.Name.EndsWith('.partial') } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -Skip $KeepCount

foreach ($file in $stale) {
    Write-Output "오래된 백업을 지운다: $($file.Name)"
    Remove-Item $file.FullName -Force
}
