# 스팀다리미 — Backend

> Steam 게임 가격 추적 & 할인 알림 서비스 (백엔드)

## 기술 스택

- Spring Boot, Spring Security, JWT/OAuth2
- JPA + MyBatis 혼용 (단순 CRUD는 JPA, 복잡한 조회는 MyBatis)
- PostgreSQL
- 외부 연동: Steam API, Discord Webhook, Gemini

## 시작하기

```bash
# 1. 클론 후 예시 설정 복사
cp src/main/resources/application-example.yml src/main/resources/application.yml
# 2. application.yml에 DB 접속 정보 등 채우기 (이 파일은 커밋되지 않음)
# 3. 실행
./gradlew bootRun
```

## 브랜치 전략

```
main         배포/시연 가능한 안정 버전 (직접 push 금지)
 └ develop   개발 통합 브랜치 ← feat/* 브랜치는 여기서 분기, 여기로 PR
```

자세한 규칙은 [CONVENTIONS.md](CONVENTIONS.md) 참고.

## 커밋 컨벤션

```
feat(auth): JWT 로그인 구현
fix(alert): target_price null 체크 누락 수정
```

도메인 scope: `auth` `user` `game` `wishlist` `alert` `notification` `comment` `batch` `crawler` `discord`
