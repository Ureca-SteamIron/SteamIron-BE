# 가격 알림(카프카) 수동 테스트 가이드

배치 서버 → 카프카 → 알림 서버로 이어지는 가격 알림 파이프라인을, 6시간 스케줄을 기다리거나
실제 스팀 할인을 기다리지 않고 수동으로 트리거해서 확인하는 방법이다.

## 사전 준비

1. Docker Desktop 실행
2. 저장소 루트(`SteamIron-BE`)에서 로컬 인프라 실행

   ```bash
   docker compose up -d
   ```

   Kafka, Kafka UI(`http://localhost:8090`), Mongo가 뜬다.
3. Postgres — 별도 덤프 복원된 로컬 컨테이너(`steamiron-pg`, `mydb` DB)가 필요하다. 없으면 팀 채널에 문의.
4. `app-batch-server`, `app-notification` 각각 로컬 `application.yml`이 필요하다 (`application-example.yml` 참고,
   git에는 안 올라가는 파일이라 각자 로컬에 직접 만들어야 함). Postgres/Mongo/Kafka 주소를 전부 `localhost`로 맞출 것.
5. `app-notification`을 먼저 띄워서 대기시킨다.

   ```bash
   ./gradlew :app-notification:bootRun
   ```

## 1. 알림 로직만 빠르게 테스트 — `NotificationTestTrigger`

실제 스팀 크롤링 없이, 특정 게임 가격을 직접 지정해서 `PriceAlertNotificationService`부터 즉시 테스트한다.

```bash
./gradlew :app-batch-server:bootRun --args="--spring.profiles.active=notification-trigger --gameId=2561650 --finalPrice=15000 --originalPrice=20000 --discountPercent=25 --discountStarted=false"
```

| 파라미터 | 필수 | 기본값 | 설명 |
|---|---|---|---|
| `--gameId` | ✅ | - | 테스트할 게임의 `game_id`. Postgres에 실제 존재해야 하고, 그 게임에 `PriceAlert`가 걸려 있어야 알림이 나간다 |
| `--finalPrice` | ✅ | - | 시뮬레이션할 판매가 |
| `--originalPrice` | | `finalPrice`와 동일 | 정가 |
| `--discountPercent` | | `0` | 할인율 |
| `--discountStarted` | | `false` | `true`면 "할인 시작" 조건으로, `false`면 "목표가 도달" 조건으로 판정 |

실행하면 몇 초 안에:
1. `app-batch-server` 콘솔에 `가격 알림 이벤트 발행` 로그
2. `app-notification` 콘솔에 `웹 알림 저장 완료` + `Discord DM 발송 처리 완료` 로그
3. 실제 디스코드로 메시지 도착 (그 유저가 Discord 알림을 켜놓은 경우에만)

## 2. 진짜 배치 전체를 수동 실행 — `BatchTestTrigger`

6시간 스케줄을 기다리지 않고, 실제 Steam API로 전체 할인 목록을 긁어와서 diff 판정까지 그대로 실행한다.

```bash
./gradlew :app-batch-server:bootRun --args="--spring.profiles.active=batch-trigger"
```

⚠️ 실제 Steam 할인 목록을 페이지당 1.5초 간격으로 끝까지 긁어오기 때문에, **할인 중인 게임 수에 따라 몇 분씩 걸릴 수 있다.**
이 방식은 지금 실제로 할인 중이면서 우리 DB에 `PriceAlert`가 걸려 있는 게임이 있어야 눈에 보이는 결과가 나온다.

## 주의사항

- 두 트리거 모두 `--spring.profiles.active=...`를 명시적으로 켜야만 동작한다. 그냥 `bootRun`만 실행하면(평소 배포와 동일)
  아무 트리거도 안 돌고, 기존 `steamTop100Job`(Top100 수집)만 시작 시 자동 실행된다.
- 로컬 DB에서 테스트하면 실제 유저 데이터(가격, `lastNotifiedPrice` 등)가 그대로 바뀐다. 테스트 후 값이 이상해지면
  직접 원복하거나 팀에 공유할 것.
