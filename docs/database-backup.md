# 데이터베이스 백업과 복원

## 왜 필요한가

Docker의 named volume은 컨테이너를 지워도 데이터가 남는다는 뜻이지 백업이 아니다.
volume을 잘못 지우거나 디스크가 죽으면 그대로 잃는다.

잃는 것의 크기가 문제다. Nexon Open API는 랭킹 이력을 2년만 제공한다. 그 기간을 넘긴
기준일은 다시 받을 수 없고, 시간이 지날수록 복구할 수 없는 몫이 늘어난다.

수집을 자동화하고 지표를 붙여도 이건 해결되지 않는다. 잃은 뒤에 알아채는 것과 잃지
않는 것은 다르다.

## 백업

```powershell
.\scripts\backup-database.ps1
```

`backup\maplemetric-<날짜>-<시각>.dump`가 만들어진다. 기본으로 최근 7개만 남기고
오래된 것부터 지운다. 무한히 쌓이면 디스크를 채우고, 그러면 백업 자체가 실패한다.

위치와 개수를 바꾸려면:

```powershell
.\scripts\backup-database.ps1 -OutputDirectory D:\backup -KeepCount 14
```

접속 정보는 `.env`에서 읽는다. 비밀번호를 명령 인자로 넘기지 않는다. 인자로 주면
프로세스 목록에 그대로 보인다.

## 복원

되돌리기가 성공한 뒤에만 대상을 바꾼다.

```text
임시 자리를 만든다
  ↓
거기에 되돌린다        ← 실패하면 여기서 멈춘다. 대상은 그대로다
  ↓
원래 대상을 옆으로 치운다
  ↓
되돌린 것을 제자리에 놓는다
  ↓
치워 둔 것을 지운다
```

대상을 먼저 비우면, 백업이 깨져 있거나 되돌리다 멈췄을 때 원래 있던 것도 없고 새로
넣은 것도 온전하지 않은 상태가 된다. 백업이 깨졌다는 것은 대개 되돌려 보고 나서야
알게 되므로, 그 순간에 원본이 남아 있어야 한다.

먼저 시험용 이름으로 되돌려 확인하는 것을 권한다.

```powershell
.\scripts\restore-database.ps1 `
    -BackupFile backup\maplemetric-20260829-105948.dump `
    -TargetDatabase maplemetric_restore_test
```

실제로 되돌릴 때는 대상을 적지 않으면 `.env`의 이름을 쓴다.

```powershell
.\scripts\restore-database.ps1 -BackupFile backup\maplemetric-20260829-105948.dump
```

대상 이름을 그대로 입력해야 진행된다. 사람이 없는 곳에서 돌려야 하면 `-Force`를
쓴다.

복원이 끝나면 주요 표의 행 수를 찍는다. 백업 시점의 수와 맞는지 본다.

## 복원 시험

복원을 해 보지 않은 백업은 백업이 아니다. 뜨는 것만 만들어 두면 정작 필요할 때
되돌아가지 않는다는 것을 그때 알게 된다.

시험은 이렇게 한다.

1. 백업을 뜬다.
2. 원본의 행 수를 적어 둔다.
3. 시험용 이름으로 되돌린다.
4. 행 수가 같은지 본다.
5. 시험용 데이터베이스를 지운다.

```powershell
docker exec -e PGPASSWORD=<비밀번호> maplemetric-postgres `
    psql -U postgres -d postgres `
    -c "DROP DATABASE IF EXISTS maplemetric_restore_test WITH (FORCE)"
```

### 마지막 시험 기록

```text
시점        2026-08-29
백업 크기   81.6 MB
```

새 데이터베이스로 되돌리기

| 표 | 원본 | 복원본 |
| --- | --- | --- |
| `p_overall_ranking_collection` | 739 | 739 |
| `p_overall_ranking_snapshot` | 1,478,000 | 1,478,000 |
| `flyway_schema_history` | 21 | 21 |

이미 있는 데이터베이스에 덮어쓰기 — 같은 행 수로 바뀌었고 치워 둔 이전 것도 지워졌다.

깨진 백업으로 되돌리기 — 앞부분만 잘라 낸 파일로 시도했다. 임시 자리에서 멈췄고
대상은 739행 그대로 남았다. 임시 자리도 남지 않았다.

## 주의

백업 파일 하나에 데이터 전체가 들어 있다. `.gitignore`가 `backup/`과 `*.dump`를
막지만, 다른 곳으로 옮길 때도 그 파일이 무엇인지 잊지 않는다.

`pg_dump`는 실행 중인 데이터베이스에서 한 시점의 일관된 상태를 받는다. 앱을 멈추지
않아도 된다.

## 아직 하지 않은 것

정해진 시각에 자동으로 백업하는 것은 배포 환경 구성의 몫이라 여기서 다루지 않는다.
호스팅이 정해지면 그 환경의 예약 실행에 이 스크립트를 걸면 된다.
