<#
.SYNOPSIS
    백업과 복원이 함께 쓰는 부분이다.

.DESCRIPTION
    접속 정보 읽기, 컨테이너 확인, 비밀번호 전달을 한곳에 둔다. 두 스크립트가 각자
    같은 일을 하면 한쪽만 고쳐 두고 다른 쪽은 그대로 남는다.
#>

Set-StrictMode -Version Latest

function Fail([string]$Reason) {
    Write-Error $Reason
    exit 1
}

function Get-DatabaseContainerName {
    return 'maplemetric-postgres'
}

<#
    .env에서 이름과 값을 읽는다.

    값에 = 이 들어 있을 수 있으므로 첫 = 만 구분자로 본다.
#>
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

function Assert-ContainerRunning([string]$Container) {
    $running = docker ps --filter "name=$Container" --format '{{.Names}}'

    if ($running -ne $Container) {
        Fail "데이터베이스 컨테이너가 떠 있지 않다: $Container"
    }
}

<#
    비밀번호를 이 프로세스의 환경변수에 둔다.

    docker에 -e PGPASSWORD=값 으로 넘기면 그 값이 docker 명령의 인자가 되어, 명령
    목록을 볼 수 있는 다른 프로세스에 그대로 보인다. 이름만 넘기고 값은 여기서
    물려준다.
#>
function Set-DatabasePassword([string]$Password) {
    $env:PGPASSWORD = $Password
}

function Clear-DatabasePassword {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}

<#
    이름이 데이터베이스 이름으로 쓸 수 있는 모양인지 본다.

    이 값은 SQL 문장에 그대로 들어간다. 큰따옴표로 감싸도 이름 안에 큰따옴표가 있으면
    감싼 것이 끊긴다.
#>
function Assert-DatabaseName([string]$Name) {
    if ($Name -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
        Fail "데이터베이스 이름으로 쓸 수 없다: $Name"
    }
}
