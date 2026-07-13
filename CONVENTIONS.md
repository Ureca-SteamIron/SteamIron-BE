# 스팀다리미 — Git 컨벤션

> 조직: [Ureca-SteamIron](https://github.com/Ureca-SteamIron) — **프론트엔드(React) / 백엔드(Spring Boot)** 레포 2개 분리 구조 기준
> 팀 5명 · 개발 기간 2주

---

## 1. 브랜치 전략

프론트/백엔드 레포가 나뉘어 있으므로, **각 레포는 독립적으로 동일한 브랜치 전략**을 따른다.

```
main         배포/시연 가능한 안정 버전 (직접 push 금지)
 └ develop   개발 통합 브랜치
     ├ feat/*      기능 개발
     ├ fix/*       버그 수정
     ├ refactor/*  리팩터링
     └ chore/*     설정·환경
```

- **main**: 항상 실행 가능한 상태만 유지. `develop`에서 검증된 것만 머지.
- **develop**: 평소 작업이 모이는 통합 브랜치.
- **작업 브랜치**: 기능 하나 = 브랜치 하나. `develop`에서 분기해서 `develop`으로 PR.

### 브랜치 이름 규칙

```
feat/게임-목록-조회
feat/login-page
fix/price-null-error
refactor/game-service
chore/docker-setup
```

- 형식: `타입/작업-내용`
- 소문자, 공백 대신 하이픈(-)
- 이슈 번호를 쓰면: `feat/12-login-page`

| 타입 | 용도 |
|------|------|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변화 없는 코드 개선 |
| `chore` | 빌드·설정·패키지 등 |
| `docs` | 문서 |

---

## 2. 커밋 메시지 컨벤션

**Conventional Commits** 형식을 따른다.

```
<타입>: <제목>
```

### 예시

```
feat: 회원가입 API 구현
feat: 게임 상세 페이지 UI 구현
fix: 가격이 null일 때 발생하는 오류 수정
refactor: GameService 조회 로직 분리
docs: API 명세서 업데이트
chore: PostgreSQL 도커 설정 추가
```

### 백엔드 — scope 사용 권장

백엔드는 도메인이 뚜렷하므로 scope를 붙이면 명확하다. (선택)

```
feat(auth): JWT 로그인 구현
feat(game): 게임 검색 API 구현
feat(batch): Steam top100 스크래핑 배치 추가
fix(alert): target_price null 체크 누락 수정
```

도메인: `auth`, `user`, `game`, `wishlist`, `alert`, `notification`, `comment`, `batch`, `crawler`, `discord`

### 타입 종류

| 타입 | 용도 |
|------|------|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변화 없는 코드 개선 |
| `style` | 포맷팅 등 (동작 변화 없음) |
| `docs` | 문서 |
| `test` | 테스트 코드 |
| `chore` | 빌드·설정·패키지 등 잡일 |

### 규칙

- 제목은 50자 이내, 명령형으로 작성 ("구현했음" ❌ → "구현" ⭕)
- 제목 끝에 마침표(.) 금지
- 한글로 통일 (팀 합의)
- 상세 설명이 필요하면 본문에 한 줄 띄우고 추가

---

## 3. Pull Request 규칙

- 작업 브랜치 → `develop`으로 **PR 필수**. `develop`, `main` 직접 push 금지.
- **최소 1명 리뷰 후 머지.**
- **PR은 작게.** 기능 하나 끝나면 바로 올린다. 며칠 치를 몰아서 올리지 않는다.
- 머지 방식은 **Squash and merge** 권장 (히스토리 정리).
- 충돌 시 **본인 브랜치에서 `develop`을 먼저 당겨와(pull) 해결한 뒤** PR 갱신.

---

## 4. 팀장 초기 세팅 체크리스트

각 레포(FE / BE)에 대해:

- [ ] `main`, `develop` 브랜치 보호 설정 (main 직접 push 금지, PR 리뷰 1명 필수)
- [ ] `.gitignore` 설정 — **비밀 정보 파일 커밋 금지**
  - 백엔드: `application.yml`(또는 `application-secret.yml`), JWT secret, DB 비번, Steam API 키, Discord webhook URL
  - 프론트(React): `.env`, `.env.local` — Vite 기준 `VITE_` 접두사 변수도 빌드에 노출되므로 비밀 값 금지
  - 예시용 `application-example.yml` / `.env.example`만 커밋
- [ ] PR 템플릿(`.github/pull_request_template.md`) 추가
- [ ] 이슈 템플릿(`.github/ISSUE_TEMPLATE/feature.md`, `bug.md`) 추가
- [ ] 첫날 다 같이 `clone → 브랜치 생성 → PR` 리허설 1회

> 이 폴더의 `pull_request_template.md`, `ISSUE_TEMPLATE/`을 각 레포의 `.github/` 아래에 그대로 복사하면 됨.

---

## 5. 자주 나는 사고 & 예방

| 사고 | 예방 |
|------|------|
| 비밀 키(.env, application.yml) 커밋 | `.gitignore` 먼저 설정, 유출 시 키 즉시 재발급 |
| main에서 바로 작업 | 브랜치 보호로 원천 차단 |
| 거대한 PR | "기능 하나 = PR 하나" 규칙 |
| 머지 충돌 지옥 | 매일 아침 `develop`을 본인 브랜치로 pull |

---

## 핵심 요약

1. 브랜치는 `feat/작업-내용`, `develop`엔 **PR로만** 머지
2. 커밋은 `feat: 내용` (백엔드는 `feat(도메인): 내용`)
3. 비밀 키 파일은 **절대 커밋 금지**
