# Emergent 백엔드 기능 비교 및 구현 계획

## 1. Executive Summary

이 문서는 Emergent FastAPI 프로토타입의 기능 계약과 사용자 경험을 현재 MapleMetric Spring Boot 백엔드에 단계적으로 반영하기 위한 기준 문서다. 목표는 FastAPI 코드를 Java로 옮기는 것이 아니라, 실제 Nexon Open API와 검증 가능한 서버 계산을 기반으로 현재 공개 API를 보존하면서 필요한 기능을 추가하는 것이다.

핵심 결론은 다음과 같다.

- 현재 Spring Boot 백엔드의 기준 공개 API는 `GET /api/v1/characters/search?characterName=`이며, `basic`, `stat`, `ranking`, `union`, `symbols`, `skills`, `hexa`, `equipment`을 이미 제공한다.
- 현재 Spring 구현은 Emergent보다 랭킹 대상 캐릭터 선택, 직업 랭킹 필터, 장비 프리셋, V매트릭스·HEXA·링크 스킬, 심볼 아이콘, 추가옵션 계산과 오류 분류에서 더 엄격하다. 이 구현을 유지한다.
- Emergent의 공지·이벤트와 랭킹 목록은 Nexon API 기반이므로 실제 기능 후보지만, 대시보드 수치, 직업·월드 통계, 직업 트렌드, 변화율, 신뢰도와 일부 캐릭터 분석은 샘플 또는 임시 계산이다. 이를 실데이터처럼 복제하지 않는다.
- 캐릭터 응답 확장은 Nexon에서 직접 조회 가능한 필드부터 진행한다. 과거 비교가 필요한 변화율·장비 변경·랭킹 변화·신뢰도는 Snapshot 구축 이후에만 제공한다.
- 통계는 전체 모집단으로 표현하지 않는다. 랭킹 API에서 수집한 표본이라면 `scope`, `sampleSize`, `asOf`, `source`, `limitations`를 반드시 함께 제공한다.
- OpenAI는 검증된 Domain Fact를 자연어로 설명하는 역할만 맡는다. 계산, 방향 판정, 변화율, 점수, 신뢰도는 서버의 결정적 로직이 담당한다.
- Redis, PostgreSQL, Flyway와 Spring Modulith는 이미 의존성에 포함되어 있다. 이번 계획을 위해 MongoDB, FastAPI 구조, 새 HTTP 클라이언트나 AI 프레임워크를 도입할 필요가 없다.
- 첫 구현 Queue는 기존 단일 API의 하위 호환성을 유지하는 **캐릭터 종합 조회 응답 확장**이다. `setEffect`는 Nexon 조회 가능 여부와 별개로 현재 제품 범위에서 제외된 상태이므로 요구사항 변경 전에는 추가하지 않는다.

분석 기준 시점은 2026-07-22이며, 데이터 계약 우선순위는 실제 Nexon 응답, 현재 Spring 공개 API와 도메인 규칙, 실제 Next.js 소비 계약, Emergent 화면 요구, Emergent 샘플 데이터 순이다.

## 2. 현재 두 백엔드 구조 비교

### 2.1 Emergent FastAPI 프로토타입

| 영역 | 구현 | 판정 |
|---|---|---|
| Web | FastAPI 단일 `server.py`, `/api` prefix | 화면 검증용 구조다. Spring 계층으로 직역하지 않는다. |
| 외부 API | 공유 `httpx.AsyncClient`, Semaphore 5, 429/5xx 최대 3회 재시도 | 동시성 제한 개념은 참고하되 오류를 `None`으로 삼키는 정책은 사용하지 않는다. |
| 저장소 | MongoDB 클라이언트를 애플리케이션 시작 시 생성 | 실제 API 로직에서는 저장 기능이 거의 없고, 현재 PostgreSQL 전략과 맞지 않는다. |
| 분석 | `AnalysisEngine` 정적 메서드 | 공식 전투력 조회 외 일부 점수·신뢰도 계산은 임시 규칙이다. |
| AI | `AIService`, Structured Output | Fact와 설명 분리 원칙은 유효하지만 키 누락 시 부팅 실패, 고정 모델, fallback 부재를 개선해야 한다. |
| 캐릭터 조회 | OCID 조회 후 15개 캐릭터 API와 5개 랭킹 API 호출 | 넓은 화면 계약은 참고한다. 모든 예외를 `None`으로 바꾸는 부분 실패 방식은 데이터 출처를 숨긴다. |
| Dashboard | 서버 내 하드코딩 지표와 OpenAI 브리핑 | 실제 데이터가 아니므로 복제 금지다. |
| 통계 | 하드코딩된 직업·월드 목록 | 실제 모집단 통계가 아니므로 복제 금지다. |
| 공지·이벤트 | Nexon 공지 API 실시간 조회 | 실제 기능 후보다. 캐시, 날짜대, 외부 오류 정책을 추가해야 한다. |
| Health | 모든 서비스 상태를 항상 `connected/active`로 반환 | 실제 상태를 검사하지 않아 운영 Health로 사용할 수 없다. |

주요 근거 파일은 `backend/server.py`, `backend/nexon_client.py`, `backend/models.py`, `backend/analysis_engine.py`, `backend/ai_service.py`, `backend/tests/backend_test.py`, `memory/PRD.md`다.

### 2.2 현재 Spring Boot 백엔드

| 영역 | 현재 상태 | 유지 또는 보완 방향 |
|---|---|---|
| Web | Spring MVC, 공통 `ApiResponse`, 도메인 오류 코드 | 공개 응답 envelope와 `/api/v1` 경로를 유지한다. |
| 캐릭터 | Controller → Query Service → Client → Nexon DTO → Result → Response | 현재 계층과 `from/of` 변환을 확장한다. |
| 외부 API | Spring `RestClient`, 타임아웃, 오류 본문 파싱, OCID 마스킹, 제한 오류 재시도 | API별 오류 정책과 Secret 비노출을 유지한다. |
| 데이터 | PostgreSQL/JPA/Flyway 준비, 도메인 테이블은 아직 없음 | Snapshot Queue에서 append-only 중심 스키마를 추가한다. |
| 캐시 | Redis/Spring Cache 의존성 및 설정만 존재 | 데이터별 TTL·키·무효화 정책을 정한 뒤 활성화한다. |
| 모듈 | Spring Modulith 구조 테스트 | 신규 `ranking`, `notice`, `snapshot`, `statistics`, `insight`, `dashboard` 경계를 명확히 한다. |
| 캐릭터 계약 | 8개 영역과 장비 프리셋 1~3 제공 | 기존 필드를 삭제·변경하지 않고 additive 확장한다. |
| 계산 | 직업별 추가옵션 계산 정책 V1 | Emergent의 잠재등급+스타포스 합산 계산으로 교체하지 않는다. |
| 테스트 | Client, Result, Service, Controller, Modulith, 계산 테스트 | 외부 API 실호출 없이 계약·부분 실패·DB 통합 테스트를 보강한다. |
| 운영 상태 | Actuator health/info/metrics/prometheus 노출 설정 | 별도 가짜 `/api/health`보다 Actuator와 실제 contributor를 사용한다. |

현재 `CharacterQueryService`는 OCID를 한 번 조회해 동일 OCID로 각 Nexon API를 순차 호출한다. 응답 범위가 커질수록 지연이 증가하므로 bounded concurrency를 검토하되, 현재의 오류 의미를 보존하는 것이 선행 조건이다.

### 2.3 실제 Next.js 프론트 계약

- 실제 프론트는 `lib/api/character.ts`에서 Spring의 단일 캐릭터 검색 API만 호출한다.
- `lib/schemas/character.ts`의 Zod schema가 현재 백엔드 응답 8개 영역을 검증한다.
- 캐릭터 화면은 프로필, 전체·월드·직업·월드직업 랭킹, 무릉 층, 스탯, 유니온, 심볼, V매트릭스, 링크 프리셋, HEXA, 장비 프리셋과 추가옵션 평가를 실제로 소비한다.
- 홈 화면은 백엔드 API를 호출하지 않고 `lib/sample-data/home.ts`의 데이터를 사용하며 화면에 `SAMPLE DATA` 경고를 노출한다.
- 따라서 Dashboard·통계 API가 준비되기 전까지 홈 샘플 표시는 유지하고, 실제 데이터로 오인될 수 있는 문구만 제거해서는 안 된다.

## 3. API Parity Matrix

| Emergent 경로 | Spring 권장 경로 | 요청 파라미터 | 응답 필드 | 실데이터/Sample 여부 | 데이터 출처 | 현재 Spring 구현 여부 | 추가 구현 필요 여부 | Snapshot 필요 여부 | OpenAI 필요 여부 | 난이도 | 선행 작업 | 주요 위험 | 테스트 방법 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `GET /api/dashboard` | `GET /api/v1/dashboard` | 없음 | `metrics`, `briefing`, `recentChanges`, `jobDistribution`, `worldActivity`, `latestNotices`, `ongoingEvents`, `dataUpdatedAt`, 표본 메타 | Emergent는 대부분 Sample | Snapshot 집계 + 공지·이벤트 + 검증된 Fact | 없음 | 필요 | 필수 | 브리핑에 선택 | 높음 | Ranking/Notice Snapshot, Statistics, Insight 기반 | 가짜 전체 통계, 서로 다른 기준일 혼합, AI가 수치 생성 | 집계 단위 테스트, Testcontainers, 고정 Clock, Controller 계약 테스트 |
| `GET /api/character/search` | 기존 `GET /api/v1/characters/search` 유지 | `characterName` | 기존 8개 영역 + 승인된 확장 필드 + 선택적 `meta` | Nexon 실데이터, Emergent `analysis` 일부 Sample | Nexon Character/Ranking API, 현재 추가옵션 정책 | 구현됨 | 확장 필요 | 변화 분석에만 필요 | `aiInsight`에 선택 | 중간 | 필드별 공식 응답 확인, 부분 실패 계약 | 호출 수·지연·rate limit, null 계약, 기존 Zod 호환 | Client mock, Service orchestration, MockMvc, JSON fixture |
| `GET /api/ranking/overall` | `GET /api/v1/rankings/overall` | `date?`, `worldName?`, `class?`, `page` | `ranking`, `page`, `hasNext`, `asOf`, `source` | Nexon 실데이터 | Nexon Overall Ranking API | 캐릭터 내부 순위만 구현 | 목록 API 필요 | 변화량에는 필요 | 불필요 | 중간 | 기준일·필터 정책 재검증 | 제공 시각 경계, 직업 필터 형식, 페이지 상한 | 쿼리 파라미터 mock, 빈 200/4xx 구분, 경계 Clock 테스트 |
| `GET /api/ranking/union` | `GET /api/v1/rankings/union` | `date?`, `worldName?`, `page` | `ranking`, `page`, `hasNext`, `asOf`, `source` | Nexon 실데이터 | Nexon Union Ranking API | 없음 | 필요 | 변화량에는 필요 | 불필요 | 중간 | Ranking 공통 모델 | 응답 필드 차이, 기준일, rate limit | Client 역직렬화, pagination, Controller 계약 테스트 |
| `GET /api/ranking/dojang` | `GET /api/v1/rankings/dojang` | `date?`, `worldName?`, `difficulty`, `page` | `ranking`, `page`, `hasNext`, `asOf`, `source` | Nexon 실데이터 | Nexon Dojang Ranking API | 캐릭터 최고 층만 구현 | 목록 API 필요 | 변화량에는 필요 | 불필요 | 중간 | Ranking 공통 모델 | `difficulty` 의미, 기록 없는 캐릭터, 기준일 | 파라미터 조합, 빈 200, 일반 오류, pagination 테스트 |
| `GET /api/statistics/jobs` | `GET /api/v1/statistics/jobs` | `asOf?`, `scope?` | `scope`, `sampleSize`, `asOf`, `source`, `limitations`, `jobs[]` | Emergent는 Sample | 저장된 ranking snapshot 표본 집계 | 없음 | 필요 | 필수 | 불필요 | 높음 | Snapshot 수집, 표본 정의 | 표본을 전체 모집단으로 오인, 직업명 분류, 중복 캐릭터 | SQL/QueryDSL 집계, Testcontainers, 합계·비율 불변식 테스트 |
| `GET /api/statistics/worlds` | `GET /api/v1/statistics/worlds` | `asOf?`, `scope?` | 표본 메타, `worlds[]`, `totalSampleCount` | Emergent는 Sample | 저장된 ranking snapshot 표본 집계 | 없음 | 필요 | 필수 | 불필요 | 높음 | Snapshot 수집, 표본 정의 | `population` 명칭 오해, 월드 타입 혼합, 평균 계산 | Testcontainers 집계, 월드 필터, 빈 표본 테스트 |
| `GET /api/analysis/job-trend` | `GET /api/v1/insights/jobs/{jobName}` | `from?`, `to?`, `scope?` | 검증된 변화 Fact, `insight`, 표본 메타 | Emergent는 고정 수치 Sample | 최소 2개 이상의 통계 Snapshot + InsightGenerator | 없음 | 필요 | 필수 | 설명에 선택 | 높음 | Statistics, OpenAI 기반 | 임의 변화율, 인과관계 단정, 작은 표본 | 변화 계산 단위 테스트, fallback, schema validation, 고정 데이터 통합 테스트 |
| `GET /api/notices` | `GET /api/v1/notices` | `category?`, `limit`, `cursor?` | `notices`, pagination, `dataUpdatedAt` | Nexon 실데이터 | Nexon notice/update/cashshop API | 없음 | 필요 | 변경 감지에 선택 | 불필요 | 낮음 | Notice client와 DTO | 카테고리별 필드 차이, 외부 장애, 중복 | Client mock, category, cursor, cache, 오류 테스트 |
| `GET /api/events` | `GET /api/v1/events` | `status?`, `cursor?` | `events`, pagination, `dataUpdatedAt` | Nexon 실데이터 | Nexon event notice API | 없음 | 필요 | 이력·변경 감지에 선택 | 불필요 | 낮음 | Event client와 날짜 정책 | UTC/KST 상태 경계, 종료일 포함 여부 | 고정 Clock 경계, 필터, 역직렬화, Controller 테스트 |
| `GET /api/health` | 운영은 `GET /actuator/health` 유지 | 필요 시 상세 권한 | 실제 component 상태 | Emergent 응답은 Sample | Spring Actuator, DB/Redis 실제 indicator | 구현됨 | 별도 공개 API 불필요 | 불필요 | 불필요 | 낮음 | 운영 노출 정책 | 상세 정보 노출, 외부 API를 매번 호출하는 health | Actuator context, 권한·노출 설정 테스트 |

Spring 공개 API는 공통 `ApiResponse` envelope를 유지한다. 목록 API의 페이지 크기와 Nexon `page` 상한은 구현 Issue에서 공식 문서와 실제 응답으로 확정하며, 현재 표의 필드는 계약 초안이다.

## 4. Character Response Field Matrix

| 필드/영역 | Emergent 형태 | 현재 Spring 형태 | 분류 | 구현 판단 |
|---|---|---|---|---|
| 기본 정보 | `character` | `basic` | 이름만 다름 | Spring `basic`을 유지한다. 기존 필드가 더 상세하다. |
| 스탯 | `stats.stat_power`, `final_stat` | `stat.combatPower`, `finalStat` | 이름·구조만 다름 | Spring 구조를 유지한다. `finalStat` 항목을 임의 삭제하지 않는다. |
| `popularity` | 단일 숫자 | 없음 | 현재 Spring에 없음 / Nexon API로 추가 가능 | Queue 1 후보. 응답은 별도 `popularity` 영역 또는 명확한 필드로 추가한다. |
| `hyperStat` | Nexon 원본 객체 | 없음 | 현재 Spring에 없음 / Nexon API로 추가 가능 | Queue 1 후보. `stat.finalStat`으로 대체하지 않는다. 프리셋 구조를 DTO로 명시한다. |
| `ability` | Nexon 원본 객체 | 없음 | 현재 Spring에 없음 / Nexon API로 추가 가능 | Queue 1 후보. 현재 장착 정보와 프리셋 범위를 공식 응답에 맞춰 명시한다. |
| `setEffect` | Nexon 원본 목록 | 없음 | Nexon API로 추가 가능하지만 현재 범위 제외 | 기존 제품 결정을 유지한다. 별도 요구사항 승인 전 Queue 1에 포함하지 않는다. |
| 무릉 상세 | `best_floor`, `best_time`, `date_dojang_record` | `ranking.dojangFloor` | 구조만 다름 / 일부 없음 | 기존 `dojangFloor`를 유지하고, 시간·기록일이 필요하면 additive `dojang` 상세 영역을 검토한다. |
| 장비 현재 목록 | `equipment` | `equipment.itemEquipment` | 구조만 다름 | Spring 계약을 유지한다. |
| 장비 프리셋 | `equipment_presets.1~3` | `itemEquipmentPreset1~3` | 이미 동일하게 제공됨 | 현재 세 프리셋과 각 아이템의 추가옵션 평가를 유지한다. |
| 활성 프리셋 | `active_preset_no` | `equipment.presetNo` | 이름만 다름 | 이미 제공됨. `presetNo` 유지. |
| 칭호 정보 | `title_info` | 없음 | 현재 Spring에 없음 / Nexon API로 추가 가능 | 화면 요구가 확인되면 Queue 1 후보로 DTO를 명시한다. |
| 용 장비 | `dragon_equipment` | 없음 | 현재 Spring에 없음 / Nexon API로 추가 가능 | 에반 등 해당 직업에서만 존재하는 선택 데이터로 설계한다. |
| 메카닉 장비 | `mechanic_equipment` | 없음 | 현재 Spring에 없음 / Nexon API로 추가 가능 | 메카닉에서만 존재하는 선택 데이터로 설계한다. |
| 랭킹 | overall/world/class/union/dojang | overall/world/class/worldClass/dojang | 구조만 다름 / Spring 일부 우위 | 기존 4개 랭킹과 무릉 층을 유지한다. 캐릭터 유니온 랭킹 추가는 별도 요구가 있을 때 검토한다. |
| 심볼 | 원본 목록 | Arcane/Authentic 분류 + 이름·레벨·아이콘 | Spring이 더 구체적 | 현재 구조를 유지한다. |
| V매트릭스·링크 | 원본 중심 | 코어 스킬 매핑 + 링크 프리셋 1~3 | Spring이 더 구체적 | 현재 구조를 유지한다. `currentSkills`는 전수받아 장착한 링크 스킬 의미를 유지한다. |
| HEXA | 원본 객체 | Core/Stat 정규화 | 구조만 다름 | 현재 검증·필터링 로직을 유지한다. |
| 추가옵션 평가 | 전체 캐릭터 합산 임시 점수 | 아이템별 `additionalOptionEvaluation` | Emergent는 제공하면 안 되는 잘못된 데이터 | Spring Policy V1만 사용한다. 두 점수 체계를 병합하지 않는다. |
| `analysis` | 변화율 0, 장비 변경 false, 신뢰도 0.85 등 고정값 포함 | 없음 | 계산 또는 Snapshot 필요 / 현재 값은 제공 금지 | Snapshot 전에는 제공하지 않는다. 전투력은 이미 `stat.combatPower`로 제공된다. |
| `weakestPart` | 스타포스+잠재등급 단순 비교 | 없음 | 제공하면 안 되는 잘못된 데이터 | 검증된 장비 평가 모델이 생기기 전에는 추가하지 않는다. |
| `aiInsight` | Structured Output | 없음 | 현재 Spring에 없음 / Fact와 OpenAI 필요 | Snapshot 기반 Fact 또는 현재 검증된 Fact만 입력하고 fallback과 함께 제공한다. |
| `dataUpdatedAt` | 요청 처리 시각 | 없음 | 현재 Spring에 없음 | Queue 1에서 의미를 구분한다. 서버 응답 생성 시각과 Nexon 기준 `date`를 혼동하지 않는다. |

### 4.1 Queue 1 권장 응답 확장 원칙

- 기존 8개 최상위 영역과 내부 필드명은 유지한다.
- `popularity`, `hyperStat`, `ability`는 Nexon 공식 응답 DTO → Application Result → Controller Response 계층을 모두 둔다.
- 장비의 `title`, `dragonEquipment`, `mechanicEquipment`는 `equipment` 내부의 선택 필드로 확장하는 방안을 우선한다.
- 무릉 기록일과 시간은 기존 `ranking.dojangFloor`를 삭제하지 않고 별도 상세 응답으로 추가한다.
- `dataUpdatedAt`은 서버 생성 시각이라면 UTC ISO-8601 instant로, Nexon 데이터 기준일은 각 영역의 `date/asOf`로 분리한다.
- 부분 실패 메타데이터를 도입한다면 기존 영역 객체를 없애지 않고 `meta.partial`, `meta.failedSections` 같은 additive 필드로 설계한다. 실제 프론트 Zod 계약 반영 전까지는 현재 fail-fast 의미를 유지한다.

## 5. 실제 데이터와 Sample Data 구분

### 5.1 Nexon 실데이터 기반

- 캐릭터 OCID, 기본 정보, 스탯, 장비, 유니온, 인기도, 하이퍼 스탯, 어빌리티, 링크 스킬, 심볼, 무릉, V매트릭스, HEXA, 스킬
- 종합·월드·직업·월드직업 랭킹과 별도 종합·유니온·무릉 랭킹 목록
- 일반·업데이트·캐시샵 공지와 이벤트 공지
- 현재 Spring의 추가옵션 평가는 Nexon `item_add_option`을 입력으로 사용하는 서버 계산 결과

실데이터도 그대로 전체 모집단 통계가 되는 것은 아니다. 랭킹 API의 특정 페이지를 집계한 결과는 랭킹 표본이다.

### 5.2 Emergent Sample 또는 검증되지 않은 계산

- Dashboard의 총 캐릭터, 활성 유저, 평균 레벨, 신규 캐릭터, 주요 이벤트 숫자
- Dashboard의 직업 점유율, 월드 활성도, 실시간 변화와 원인·신뢰도
- `/statistics/jobs`의 직업별 count·percentage·changeRate·trend7d 전체 목록
- `/statistics/worlds`의 population·activeRate·averageLevel·changeRate 전체 목록
- `/analysis/job-trend`의 고정 점유율·변화율·7일 트렌드
- 캐릭터 `stat_power_change_rate=0`, `equipment_changed=false`, `trend=STABLE`, `confidence=0.85`
- 잠재능력 등급과 스타포스를 합산한 Emergent 추가옵션 점수·등급
- 스타포스와 잠재 등급만으로 선택한 `weakestPart`
- 실제 연결을 확인하지 않고 모든 서비스를 정상으로 표시하는 `/api/health`

### 5.3 현재 Next.js Sample

실제 Next.js 홈의 요약 지표, 실시간 변화, 시그널, 직업 점유율, 월드 활성도, 랭킹 변화, 패치 영향, 공지, 이벤트는 모두 `lib/sample-data/home.ts`에 있다. 프론트는 이를 명시적으로 Sample로 표시한다. 실제 Dashboard API가 다음 조건을 만족한 뒤에만 교체한다.

1. 데이터 출처와 기준시각이 응답에 포함된다.
2. 표본이면 범위와 한계가 표시된다.
3. 변화 지표는 비교 가능한 Snapshot으로 계산된다.
4. 공지·이벤트는 Nexon 실데이터다.
5. AI 실패 시에도 계산 Fact와 화면이 유지된다.

## 6. 권장 Spring 패키지 구조

기존 도메인별 계층과 Spring Modulith 경계를 유지한다.

```text
com.maplemetric
├─ character
│  ├─ application
│  │  ├─ calculator
│  │  ├─ result
│  │  └─ service
│  ├─ domain
│  ├─ infrastructure.client
│  └─ presentation
├─ ranking
│  ├─ application.result
│  ├─ application.service
│  ├─ domain
│  ├─ infrastructure.client
│  └─ presentation
├─ notice
│  ├─ application.result
│  ├─ application.service
│  ├─ domain
│  ├─ infrastructure.client
│  └─ presentation
├─ snapshot
│  ├─ application.service
│  ├─ domain
│  └─ infrastructure.persistence
├─ statistics
│  ├─ application.result
│  ├─ application.service
│  ├─ domain
│  ├─ infrastructure.persistence
│  └─ presentation
├─ insight
│  ├─ application
│  ├─ domain
│  └─ infrastructure.openai
├─ dashboard
│  ├─ application.result
│  ├─ application.service
│  └─ presentation
└─ global
   ├─ config
   └─ exception
```

설계 규칙:

- `character`는 기존 단일 검색 API와 캐릭터 직접 조회·계산을 소유한다.
- `ranking`은 목록 조회와 Nexon ranking DTO를 소유하며, `character`가 필요한 단일 캐릭터 랭킹 계약과 중복되지 않도록 공통 client 추출 여부를 구현 시 검토한다.
- `snapshot`은 수집 이력과 원본/정규화 데이터 보존을 담당하며 웹 응답 DTO를 참조하지 않는다.
- `statistics`는 Snapshot query 결과로 Fact를 계산한다. Nexon client를 직접 호출하지 않는다.
- `insight`는 Domain Fact를 입력받는 `InsightGenerator` 포트를 제공한다. `dashboard`나 `character`가 OpenAI 구현체를 직접 참조하지 않는다.
- `dashboard`는 통계·공지·이벤트·인사이트 결과를 조합하고 계산을 중복하지 않는다.
- 모듈 간 의존은 공개 application API 또는 이벤트로 제한하고, Entity/Repository 직접 참조를 피한다.

## 7. DB 및 Flyway 변경 계획

현재 migration은 `V1__initialize_schema.sql`과 `V2__create_event_publication.sql`까지 존재한다. 기존 migration은 수정하지 않으며, 실제 Queue 시작 시 최신 `dev`의 버전을 다시 확인한다.

### 7.1 Snapshot 후보 스키마

| 테이블 | 핵심 컬럼 | 제약·인덱스 | 목적 |
|---|---|---|---|
| `ranking_snapshot_batch` | `id`, `ranking_type`, `reference_date`, `world_name`, `class_filter`, `difficulty`, `page`, `collected_at`, `source` | 요청 차원의 unique key, `reference_date` index | 동일한 범위의 수집 단위를 식별한다. |
| `ranking_snapshot_entry` | `batch_id`, `ranking`, `character_name`, `world_name`, `class_name`, `sub_class_name`, 유형별 수치 | `(batch_id, ranking)` unique, 캐릭터·월드·직업 index | 랭킹 변화와 표본 통계의 원천이다. |
| `character_snapshot` | `id`, 안정적 캐릭터 키, `observed_at`, `source_date`, `combat_power`, `equipment_hash`, `schema_version` | 캐릭터 키+관측시각 index | 전투력·장비 변경 Fact를 계산한다. 필요한 필드만 저장한다. |
| `notice_snapshot` | `notice_id`, `category`, `title`, `url`, `published_at`, `content_hash`, `revision`, `observed_at` | `(notice_id, revision)` unique | 공지 변경과 최신 목록을 관리한다. |
| `event_snapshot` | `event_id`, `title`, `url`, `starts_at`, `ends_at`, `content_hash`, `revision`, `observed_at` | `(event_id, revision)` unique, 기간 index | 이벤트 상태와 변경 이력을 관리한다. |
| `statistic_snapshot` | `id`, `statistic_type`, `scope`, `as_of`, `sample_size`, `source`, `limitations`, `payload`, `calculated_at` | type+scope+asOf unique | 재현 가능한 집계 결과와 메타데이터를 보존한다. |

### 7.2 Migration 분할

- 후보 `V3`: ranking/character snapshot 원천 테이블과 인덱스
- 후보 `V4`: notice/event snapshot과 revision 제약
- 후보 `V5`: statistic snapshot 또는 집계 조회 최적화 구조

번호는 계획용이며 각 Queue가 최신 `dev`에서 시작될 때 다시 배정한다. 한 migration에 서로 독립적인 도메인을 과도하게 묶지 않는다.

### 7.3 저장 정책

- Snapshot은 append-only를 기본으로 하고 동일 요청 재실행은 unique key로 멱등 처리한다.
- Nexon 원본 전체 JSON을 무기한 저장하지 않는다. 재계산에 필요한 정규화 필드 또는 버전이 있는 JSONB만 저장한다.
- OCID 원문 저장 필요성을 별도 검토한다. 필요하면 로그에는 계속 마스킹하고 DB 접근·보존 기간·삭제 정책을 명시한다.
- 모든 수집시각은 UTC instant로 저장하고, Nexon 기준일은 `DATE`로 분리한다.
- 통계 응답은 사용한 batch/snapshot을 추적할 수 있어야 한다.
- 대량 랭킹 수집 전 예상 row 수, pagination, retention, vacuum 비용을 산정한다.
- Rollback은 기존 migration 삭제가 아니라 후속 migration으로 객체를 변경한다. 운영 데이터 삭제가 필요한 rollback은 자동화하지 않는다.

## 8. OpenAI 연동 계획

### 8.1 책임 분리

```text
Snapshot/현재 조회 데이터
→ 결정적 Domain Fact 계산
→ InsightGenerator
→ OpenAI Structured Output 또는 Template fallback
→ Response
```

권장 계약:

```java
public interface InsightGenerator {
    InsightResult generate(InsightFacts facts);
}
```

구현체:

- `OpenAiInsightGenerator`: 검증된 Fact만 최소 JSON으로 전송하고 strict schema를 검증한다.
- `TemplateInsightGenerator`: 키 누락, 비활성화, 타임아웃, 제한, 잘못된 응답 시 결정적인 한국어 문구를 반환한다.

### 8.2 설정과 운영

- `OPENAI_API_KEY`가 없어도 애플리케이션이 부팅되어야 한다.
- 모델은 `OPENAI_MODEL` 설정으로만 선택하고 Emergent의 고정 모델 문자열을 복제하지 않는다.
- 기존 `RestClient`와 Jackson으로 구현 가능한지 우선 검토한다. 새 SDK나 AI 프레임워크는 필요성과 운영 비용 승인 전 추가하지 않는다.
- timeout, 출력 토큰 상한, strict schema, 오류 분류, 관측 지표를 설정한다.
- Fact canonical JSON hash를 캐시 키로 사용해 동일 입력의 중복 비용을 막는다.
- 원본 Nexon 응답, API Key, OCID, 불필요한 캐릭터 개인정보를 prompt나 로그에 포함하지 않는다.
- AI 응답의 `sentiment`, `headline`, `summary`, `evidence`는 설명 필드다. 수치·방향·신뢰도는 Fact에서 그대로 가져온다.
- AI 결과가 없어도 핵심 API 성공 여부와 계산 Fact는 영향을 받지 않는다.

### 8.3 도입 순서

1. OpenAI 호출 없는 interface, schema validator, template fallback을 먼저 구현한다.
2. Fake generator로 character/dashboard use case를 테스트한다.
3. 개발 환경에서만 실제 provider를 opt-in 한다.
4. Snapshot 기반 Fact가 준비된 기능부터 사용자 응답에 연결한다.
5. 비용·지연·실패율 지표를 확인한 뒤 TTL과 호출 범위를 조정한다.

## 9. 캐시 및 외부 API 호출 전략

### 9.1 캐시 후보

| 데이터 | 권장 TTL 시작값 | 키 구성 | 주의점 |
|---|---|---|---|
| characterName → OCID | 6~24시간 | 정규화 캐릭터명 | 캐릭터 없음 응답은 짧은 negative TTL만 검토한다. |
| 캐릭터 종합 조회 | 1~5분 | OCID + response schema version | 프리셋·스탯 갱신 지연을 사용자에게 숨기지 않는다. Queue 1과 별도 변경으로 검토한다. |
| 기준일 랭킹 페이지 | 기준일 제공 경계까지 또는 5~30분 | type+date+filters+page | 현재 Spring 09:30 정책과 Emergent 08:40 정책이 다르므로 공식 문서·실제 호출 재검증 후 확정한다. |
| 공지·이벤트 | 5~15분 | category/status/cursor | 종료 상태 계산은 원본 캐시와 분리하거나 현재 시각 기준 재계산한다. |
| 통계 Snapshot | 다음 성공 수집까지 | scope+asOf | 응답에 `asOf`와 `sampleSize`를 항상 포함한다. |
| AI insight | Fact가 바뀔 때까지, 최대 24시간 | factHash+schemaVersion+model | fallback 결과와 provider 결과를 구분한다. |

TTL은 초기 운영값이며 rate limit, 데이터 갱신 주기, 프론트 요구를 측정한 뒤 조정한다.

### 9.2 오류와 재시도

- `200 OK + 빈 목록`만 정상적인 빈 결과로 처리한다.
- 일반 4xx, 5xx, timeout을 빈 데이터로 바꾸지 않는다.
- 현재 Client의 Nexon 오류 코드 파싱, `OPENAPI00007` 제한 재시도, timeout/invalid response/server error 구분을 유지한다.
- 재시도는 제한 오류와 일시적 서버 오류 중 명시적으로 허용한 경우만 사용한다. 지수 backoff, jitter, `Retry-After`를 구현 Issue에서 검토한다.
- API Key는 request URL, cache key, exception, metric tag, 로그에 절대 포함하지 않는다. OCID 로그는 기존 마스킹 helper를 통과한다.
- 동일 키 cache stampede를 막기 위해 `@Cacheable(sync = true)` 또는 동등한 단일 비행 방식을 검토하되, 긴 외부 호출로 cache lock이 고갈되지 않게 한다.

### 9.3 수집과 사용자 조회 분리

- 대량 ranking/statistics 수집은 사용자 HTTP 요청에서 실행하지 않는다.
- 수집기는 정해진 기준일과 pagination으로 Snapshot을 저장하고 성공한 완전 batch만 통계에 노출한다.
- 수집 실패 시 마지막 성공 Snapshot을 `stale=true`와 기준시각으로 제공하거나 명시적 unavailable 응답을 반환한다. 새 값처럼 위장하지 않는다.

## 10. 동시 호출 및 부분 실패 전략

### 10.1 캐릭터 조회 의존 그래프

```text
characterName 검증
→ OCID 조회
→ basic 조회
→ basic 비의존 조회: stat, union, symbols, skills, HEXA, equipment, dojang
→ basic 의존 조회: world ranking
→ overall ranking 결과 의존: class filter
→ class filter 의존: class/world-class ranking
→ Result 조합
```

OCID와 basic은 선행 필수다. 직업 필터는 전체 랭킹에서 대상 캐릭터와 일치하는 행으로 생성하며, 필터가 없으면 직업·월드직업 랭킹 요청을 생략하는 현재 정책을 유지한다.

### 10.2 동시성 도입 조건

- JDK/Spring 기본 기능의 bounded executor를 사용하고 별도 라이브러리를 추가하지 않는다.
- Nexon 동시 호출 상한을 설정하고 전체 요청 timeout보다 각 외부 호출 timeout을 짧게 둔다.
- `CompletableFuture`를 사용하더라도 취소, MDC/log context, executor 포화, 예외 unwrap 정책을 테스트한다.
- 초기 상한은 보수적으로 3~4개를 검토하고, rate limit과 p95 latency 측정 후 조정한다.
- class ranking처럼 선행 응답이 필요한 호출을 억지로 병렬화하지 않는다.
- OpenAI 호출은 캐릭터 핵심 조회 완료 후 실행하고, 핵심 응답의 성공 조건으로 두지 않는다.

### 10.3 부분 실패 정책

Client 계층은 외부 오류를 빈 DTO로 바꾸지 않는다. Service 계층에서만 영역의 필수/선택 여부를 판단한다.

| 영역 | 초기 정책 | 이유 |
|---|---|---|
| OCID, basic | 실패 시 전체 실패 | 캐릭터 식별과 정규화된 기본 정보가 없으면 응답을 구성할 수 없다. |
| stat, equipment | 현재는 전체 실패 유지 | 실제 프론트의 종합 화면 핵심 계약이며 기존 fail-fast 의미를 보존한다. |
| ranking, union, symbols, skills, hexa | 부분 실패 후보 | 화면의 독립 영역이지만 프론트가 빈 값과 장애를 구분할 metadata가 먼저 필요하다. |
| popularity, hyperStat, ability, 특수 장비 | 선택 영역 후보 | 직업/레벨에 따라 데이터가 없을 수 있다. 정상 무데이터와 장애를 구분해야 한다. |
| AI insight | fallback 또는 null | 도메인 Fact와 핵심 조회를 실패시키지 않는다. |

부분 실패 도입 순서:

1. Nexon의 정상 무데이터 응답 형태와 특정 오류 코드를 endpoint별로 실제 검증한다.
2. `meta.partial`, `failedSections`, `dataUpdatedAt` 계약을 백엔드와 프론트에서 합의한다.
3. 외부 장애는 failed section으로 기록하고, 정상 무데이터만 빈 목록/null로 매핑한다.
4. Zod schema와 화면의 unavailable 상태가 준비된 뒤 Service orchestration을 변경한다.
5. 그 전에는 현재 전체 실패 정책을 유지한다.

## 11. 테스트 전략

### 11.1 단위 테스트

- Nexon DTO → Result → Response의 null, 빈 목록, 필드명, 프리셋 매핑
- ranking 대상 캐릭터 일치 선택과 class filter 생성/생략
- event 상태의 KST/UTC 경계와 종료일 포함 정책
- Snapshot 비교에 따른 rank change, new entry, equipment changed, combat power change
- 통계의 count, percentage, average, 표본 메타데이터와 합계 불변식
- OpenAI Fact 생성과 template fallback, strict output validation
- 기존 AdditionalOptionCalculator 경계값과 unsupported 직업 정책 회귀

### 11.2 Client 테스트

`MockRestServiceServer`로 다음을 검증한다.

- 정확한 path, query parameter, header와 역직렬화
- `200 + 빈 목록`, 빈 body, malformed JSON
- endpoint별 404/일반 4xx/5xx/timeout/통신 오류
- 제한 오류 재시도와 소진
- 로그에 API Key 원문·header 이름·OCID 원문이 없는지
- 테스트가 실제 Nexon 서버의 파라미터 허용 검증으로 오인되지 않도록 명시

### 11.3 Service/Controller 계약 테스트

- 동일 OCID 재사용과 호출 순서/횟수
- 기준일을 사용하는 모든 랭킹 호출의 동일 date
- 선택 호출 생략과 오류 전파/부분 실패 정책
- 공통 `ApiResponse`의 success/code/message/data
- 기존 캐릭터 JSON 경로와 신규 additive 필드
- 목록 API pagination, filter, 빈 결과

### 11.4 Persistence 통합 테스트

- Testcontainers PostgreSQL로 Flyway migration 성공과 JPA validation 확인
- unique constraint 기반 수집 멱등성
- incomplete batch가 통계에 포함되지 않는지
- 기준일별 이전/현재 Snapshot 조회
- QueryDSL/Repository 집계 결과와 인덱스 사용 계획 확인

### 11.5 Cross-repository 계약

- Queue 8에서 Spring Controller fixture를 JSON 파일로 고정하고 실제 Next.js Zod schema와 호환성을 확인한다.
- 백엔드 저장소에서 프론트 파일을 수정하지 않는다. 계약 변경은 Queue 9 문서와 별도 프론트 Issue로 전달한다.
- 홈 Sample 교체 전 Dashboard 응답 fixture와 로딩·오류·stale·partial 상태를 프론트에서 검증한다.

### 11.6 검증 명령

각 Queue에서 최소 다음을 실행한다.

```powershell
.\gradlew.bat test
.\gradlew.bat build
git diff --check
```

외부 API 자동 테스트와 OpenAI 실제 호출은 금지한다. 실제 키가 준비된 수동 검증은 endpoint별 요청 결과를 확인하되 키와 OCID 원문을 출력하지 않는다.

## 12. 마이그레이션 순서

1. **Queue 1 캐릭터 응답 확장**: 현재 단일 API에 Nexon 직접 조회 가능 필드를 additive하게 추가하고 데이터 시각 의미를 확정한다.
2. **Queue 2 랭킹 목록 API**: 종합·유니온·무릉 목록 계약, pagination, 기준일 정책을 구현한다.
3. **Queue 3 공지·이벤트 API**: 실제 Nexon 공지 데이터와 시간대 기반 이벤트 상태를 제공한다.
4. **Queue 4 OpenAI 기반**: provider와 무관한 `InsightGenerator`, strict schema, template fallback, 설정을 만든다. 아직 가짜 Fact를 연결하지 않는다.
5. **Queue 5 Snapshot 저장**: ranking/character/notice/event 수집 원천과 멱등 저장, retention을 구현한다.
6. **Queue 6 직업·월드 통계**: 완전한 Snapshot batch의 표본만 집계하고 범위·한계 메타데이터를 제공한다.
7. **Queue 7 Dashboard**: 통계·변화 Fact·공지·이벤트를 조합하고 선택적으로 AI 설명을 추가한다.
8. **Queue 8 계약·통합 테스트**: 전체 API fixture, Flyway, 실패 정책과 하위 호환성을 강화한다.
9. **Queue 9 프론트 연동 명세**: 실제 response example, 오류 코드, stale/partial, Sample 교체 조건을 확정한다.

Snapshot 이전에도 랭킹·공지 목록은 실시간 조회로 제공할 수 있지만, 변화량·통계·Dashboard 수치에는 사용하지 않는다.

## 13. 예상 이슈 및 위험

| 위험 | 영향 | 대응 |
|---|---|---|
| Nexon ranking 제공 시각 정책 불일치 | 날짜 경계에서 4xx 또는 빈 데이터 | 현재 Spring 09:30 정책을 함부로 변경하지 않고 공식 문서와 실제 호출을 Queue 2에서 재검증한다. |
| 직업 필터 문자열 불일치 | 직업 랭킹 누락 또는 4xx | 전체 랭킹의 `className/subClassName`으로 필터를 만들고 실제 허용값을 fixture로 고정한다. |
| 캐릭터 종합 호출 증가 | latency와 rate limit 증가 | 캐시, bounded concurrency, 선택 필드, endpoint별 timeout을 함께 설계한다. |
| 부분 실패가 정상 무데이터로 보임 | 사용자에게 잘못된 0/null 제공 | Client 오류를 유지하고 응답 metadata와 unavailable UI가 준비된 뒤 부분 실패를 도입한다. |
| 표본 통계를 전체 통계로 표현 | 제품 신뢰도 훼손 | scope/sampleSize/source/limitations를 필수화하고 `population` 같은 명칭을 제한한다. |
| Snapshot 중복·부분 수집 | 잘못된 변화율 | batch 완전성 상태, unique key, transaction, 멱등 수집을 적용한다. |
| 공지·이벤트 날짜대 오류 | 진행/종료 상태 오판 | 원본 offset 보존, KST 정책과 경계 Clock 테스트를 둔다. |
| OpenAI 지연·비용·장애 | API 지연 또는 부팅 실패 | opt-in provider, fallback, timeout, 토큰 상한, factHash cache를 사용한다. |
| AI 환각 또는 수치 생성 | 잘못된 분석 | Fact를 서버에서 확정하고 schema와 prompt에 없는 숫자를 검증/거부한다. |
| Character schema 확대 | Next.js Zod parsing 실패 | additive 변경도 fixture로 검증하고 nullable/empty 계약을 합의한다. |
| JSONB schema drift | Snapshot 재계산 불가 | `schema_version`, 최소 정규화 필드, migration/reader 호환 정책을 둔다. |
| OCID·Secret 노출 | 보안 사고 | 기존 마스킹을 모든 로그 경로에 유지하고 API Key를 저장·태깅하지 않는다. |
| Actuator 상세 노출 | 내부 구성 유출 | production `show-details: never`와 endpoint 노출 정책을 유지한다. |

## 14. 구현 Issue 분할안

각 Queue는 독립 Issue와 PR로 진행한다. 한 Queue가 merge되고 `dev`를 최신화한 후 다음 Queue를 시작한다.

| Queue | 범위 | 제외 범위 | 완료 기준 |
|---|---|---|---|
| 1 | popularity, hyperStat, ability, 승인된 무릉/특수장비/갱신시각 확장 | setEffect, Snapshot, AI, 통계, Dashboard | 기존 캐릭터 계약 회귀 없음, Nexon DTO와 Controller 테스트 통과 |
| 2 | 종합·유니온·무릉 랭킹 목록 | 변화량·통계 저장 | 필터·페이지·기준일·오류 정책 확정 |
| 3 | 일반/업데이트/캐시샵 공지, 이벤트 | 변경 이력과 AI 요약 | Nexon 실데이터 계약, 시간대 경계, 캐시 정책 확정 |
| 4 | InsightGenerator, OpenAI provider, fallback | 사용자용 가짜 분석 연결 | 키 없이 부팅, 실제 호출 없는 테스트, strict schema |
| 5 | Snapshot Entity/Repository/Flyway/수집 use case | 통계 화면과 AI 설명 | 멱등·완전 batch·retention·Testcontainers 검증 |
| 6 | 직업·월드 표본 통계 | 전체 모집단 표현, 인과 분석 | scope/sampleSize/asOf/source/limitations 포함 |
| 7 | 홈 Dashboard 조합과 검증된 변화 Fact | 하드코딩 수치 | stale/partial/fallback 포함 실제 응답 제공 |
| 8 | API 계약·통합 테스트 | 신규 사용자 기능 | 주요 fixture와 실패 경로 자동 검증 |
| 9 | 프론트 연동 명세 | 프론트 코드 수정 | 실제 response/error examples와 Sample 교체 조건 명시 |

## 15. 각 Issue 제목, Branch, Commit 단위

Issue 번호는 생성 후 확정하며 아래 `{issueNumber}`를 실제 번호로 교체한다. PR 제목은 Issue 제목과 문자 단위로 같게 하고 base는 `dev`다.

| Queue | Issue/PR 제목 | Branch | 권장 작업 Commit 단위 | Squash Commit |
|---|---|---|---|---|
| 1 | `[FEAT] 캐릭터 종합 조회 응답 확장` | `feat/{issueNumber}-character-summary-extension` | `feat: 캐릭터 확장 정보 API 클라이언트 추가` / `feat: 캐릭터 종합 응답 확장` / `test: 캐릭터 확장 응답 테스트 추가` | `feat: 캐릭터 종합 조회 응답 확장` |
| 2 | `[FEAT] 랭킹 조회 API 추가` | `feat/{issueNumber}-ranking-apis` | `feat: 랭킹 조회 클라이언트 추가` / `feat: 랭킹 목록 조회 API 추가` / `test: 랭킹 조회 계약 테스트 추가` | `feat: 랭킹 조회 API 추가` |
| 3 | `[FEAT] 공지 및 이벤트 조회 API 추가` | `feat/{issueNumber}-notices-events` | `feat: 공지 및 이벤트 클라이언트 추가` / `feat: 공지 및 이벤트 조회 API 추가` / `test: 공지 및 이벤트 테스트 추가` | `feat: 공지 및 이벤트 조회 API 추가` |
| 4 | `[FEAT] OpenAI 인사이트 생성 기반 구축` | `feat/{issueNumber}-openai-insight-foundation` | `feat: 인사이트 생성 추상화 및 fallback 추가` / `feat: OpenAI Structured Output 연동 추가` / `test: 인사이트 생성 테스트 추가` | `feat: OpenAI 인사이트 생성 기반 구축` |
| 5 | `[FEAT] 데이터 스냅샷 저장 구조 추가` | `feat/{issueNumber}-data-snapshots` | `feat: 데이터 스냅샷 Flyway 스키마 추가` / `feat: 스냅샷 저장 및 조회 구현` / `test: 스냅샷 통합 테스트 추가` | `feat: 데이터 스냅샷 저장 구조 추가` |
| 6 | `[FEAT] 직업 및 월드 통계 API 추가` | `feat/{issueNumber}-job-world-statistics` | `feat: 표본 통계 집계 로직 추가` / `feat: 직업 및 월드 통계 API 추가` / `test: 통계 집계 및 계약 테스트 추가` | `feat: 직업 및 월드 통계 API 추가` |
| 7 | `[FEAT] 홈 대시보드 인사이트 API 추가` | `feat/{issueNumber}-dashboard-insights` | `feat: 대시보드 Fact 조합 로직 추가` / `feat: 대시보드 인사이트 API 추가` / `test: 대시보드 응답 테스트 추가` | `feat: 홈 대시보드 인사이트 API 추가` |
| 8 | `[TEST] 백엔드 API 계약 및 통합 테스트 보완` | `test/{issueNumber}-backend-contract-tests` | `test: 캐릭터 및 목록 API 계약 테스트 보완` / `test: Snapshot 및 실패 정책 통합 테스트 보완` | `test: 백엔드 API 계약 및 통합 테스트 보완` |
| 9 | `[DOCS] 프론트 연동용 백엔드 API 명세 정리` | `docs/{issueNumber}-frontend-api-contract` | `docs: 프론트 연동 API 요청 및 응답 명세 추가` / `docs: 오류 및 Sample 전환 기준 추가` | `docs: 프론트 연동용 백엔드 API 명세 정리` |

Commit 수는 실제 변경 결합도에 따라 줄일 수 있지만 기능·테스트·migration의 목적이 다른 변경을 하나의 중간 Commit에 무리하게 섞지 않는다. 최종 Squash 제목은 표의 값을 사용한다.

## 16. 프론트 연동 전 완료 조건

### 16.1 공통

- 모든 공개 API는 `/api/v1`과 공통 `ApiResponse`를 사용한다.
- request parameter, null/빈 배열, 날짜·시간대, pagination, 오류 코드가 문서와 fixture로 고정되어 있다.
- 기존 캐릭터 API의 8개 영역과 현재 필드가 삭제·이름 변경되지 않았다.
- 실제 Next.js Zod schema가 성공 응답 fixture를 통과한다.
- 200 무데이터와 외부 4xx/5xx/timeout이 구분된다.
- API Key와 OCID 원문이 로그·응답·테스트 산출물에 없다.
- `./gradlew test`, `./gradlew build`, `git diff --check`가 통과한다.

### 16.2 캐릭터 화면

- popularity/hyperStat/ability/특수 장비의 직업·레벨별 정상 무데이터 형태가 확정되어 있다.
- `dataUpdatedAt`, 각 Nexon `date`, ranking `asOf`의 의미가 구분된다.
- 부분 실패를 사용한다면 프론트에 section unavailable 상태와 `meta.failedSections` 처리가 먼저 구현되어 있다.
- 장비 프리셋 1~3, 추가옵션 평가, 랭킹, 심볼, 스킬, HEXA 회귀가 없다.

### 16.3 랭킹·공지·이벤트 화면

- 기준일 경계와 필터 값은 공식 문서 및 실제 수동 호출로 검증되어 있다.
- pagination과 마지막 페이지 판단이 정의되어 있다.
- 공지 category와 event status의 시간대·포함 경계가 확정되어 있다.
- 외부 장애 시 stale cache를 제공한다면 `stale`과 원본 기준시각을 표시한다.

### 16.4 통계·Dashboard·AI

- 화면의 모든 숫자는 Snapshot batch와 계산식으로 재현할 수 있다.
- 표본 통계에 `scope`, `sampleSize`, `asOf`, `source`, `limitations`가 있다.
- 변화율은 비교 가능한 두 Snapshot의 동일 scope로 계산된다.
- 원인·영향을 데이터 없이 단정하지 않는다.
- OpenAI가 비활성화되거나 실패해도 계산 Fact와 화면이 정상 동작한다.
- 홈의 `SAMPLE DATA` 경고는 대응하는 모든 섹션이 실제 API로 전환된 뒤에만 제거한다. 일부 섹션만 전환되면 Sample과 실데이터를 영역별로 계속 명시한다.

이 조건을 만족하기 전에는 Emergent 화면과 모양이 같다는 이유만으로 백엔드 parity 완료로 판단하지 않는다.
