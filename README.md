# MarketHub — 미니 중고거래 플랫폼

중고나라/당근마켓의 축소판을 직접 만들며 **Spring MVC 웹 애플리케이션의 구조를 체화**하기 위한 학습 프로젝트입니다.

단순 CRUD 게시판이 아니라 회원/세션 로그인, 상품, 권한 검증, 검증(Validation), 예외 처리, (예정) 찜·검색·REST API까지 —
스프링 기본편 · MVC · HTTP 강의에서 배운 내용을 실제 구조로 엮는 것이 목표입니다.

> 완성도 높은 서비스가 아니라 **계층형 아키텍처와 HTTP 요청/응답 설계를 몸으로 이해하는 것**이 이 프로젝트의 존재 이유입니다.

---

## 기술 스택

| 구분 | 내용 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.x (Spring MVC) |
| View | Thymeleaf |
| Validation | Bean Validation (spring-boot-starter-validation) |
| Build | Gradle |
| 저장소 | In-Memory `HashMap` 기반 (→ 구조 완성 후 JPA 전환 예정) |
| 인증 | 세션/쿠키 직접 구현 (Spring Security 미사용 — 의도적 선택) |

**Spring Security를 쓰지 않는 이유**: 세션·쿠키·인터셉터의 동작 원리를 직접 구현하며 이해하기 위함.
Security 도입은 프로젝트 완성 후 2차 리팩토링으로 진행할 계획.

**Memory Repository로 시작하는 이유**: 지금의 학습 목표는 DB가 아니라 웹 요청 흐름 · MVC 구조 · 계층 분리.
`ProductRepository` 인터페이스를 기준으로 구현체만 갈아끼우는 방식(`Memory → JPA`)으로 전환 예정.

---

## 실행 방법

```bash
./gradlew bootRun
# http://localhost:8080 접속
```

---

## 구현 현황

### Level 1 — 기본 MVC ✅ 완료

- [x] 회원가입 (폼 검증, 비밀번호 확인 검증, 로그인 아이디 중복 검증)
- [x] 세션 기반 로그인 / 로그아웃 (`HttpSession`, `SessionConst`)
- [x] 웰컴 페이지 (로그인 시 이름 인사말)
- [x] 마이페이지 진입 (비로그인 시 로그인 페이지로 리다이렉트)
- [x] 상품 등록 (로그인 가드, 세션에서 판매자 자동 연결, 등급 선택)
- [x] 상품 목록 / 상세 조회
- [x] 내가 등록한 상품 목록
- [x] 상품 수정 (기존 값 채운 폼 + **서비스 계층 소유권 검증**)
- [x] 상품 삭제 (확인 창 + 소유권 검증)
- [x] 전역 예외 처리 (`@ControllerAdvice` + 커스텀 에러 페이지)

### Level 2 — 비즈니스 규칙 (예정)

- [ ] 로그인 체크 인터셉터 (`LoginCheckInterceptor` — 컨트롤러마다 반복되는 세션 체크 통합)
- [ ] 검색 / 필터 (`/products?keyword=&minPrice=&maxPrice=`)
- [ ] 찜 기능 (중복 찜 방지 · 본인 상품 찜 불가)
- [ ] 상품 문의 / 판매자 답변 (작성자·판매자 권한 체크)

### Level 3 — REST API + 예외 응답 (예정)

- [ ] 찜하기 / 찜 취소 API (`POST·DELETE /api/products/{id}/likes`)
- [ ] 상품 검색 API (`GET /api/products/search`)
- [ ] `@RestControllerAdvice` 기반 공통 JSON 예외 응답 (`{ "code": ..., "message": ... }`)
- [ ] API 전용 DTO

### 이후 로드맵

- [ ] Memory Repository → JPA 전환
- [ ] Spring Security 도입 (2차 리팩토링)

---

## URL 설계

HTML Form은 GET/POST만 지원하므로 삭제·수정도 POST로 처리하고, JSON API는 별도의 `/api` 경로에서 REST 원칙대로 설계한다.

> **데이터를 변경하는 요청에 GET을 쓰지 않는다.** GET은 "서버 상태를 바꾸지 않는다(safe)"는 약속이라,
> 크롤러·브라우저 prefetch·링크 미리보기 봇이 URL을 따라가는 것만으로 데이터가 삭제될 수 있다.
> 또한 변경 요청의 응답은 항상 redirect로 끝낸다(PRG 패턴) — 새로고침 시 요청이 중복 전송되는 것을 막기 위함.

### 화면 (HTML)

| 기능 | URL | 메서드 |
|---|---|---|
| 웰컴 페이지 | `/` | GET |
| 회원가입 | `/members/new` → `/members` | GET → POST |
| 로그인 / 로그아웃 | `/login` / `/logout` | GET·POST / POST |
| 상품 목록 | `/products` | GET |
| 상품 상세 | `/products/{id}` | GET |
| 상품 등록 | `/products/new` → `/products` | GET → POST |
| 상품 수정 | `/products/{id}/edit` | GET → POST |
| 상품 삭제 | `/products/{id}/delete` | POST |
| 마이페이지 | `/my-page` | GET |
| 내가 등록한 상품 | `/my-page/products` | GET |
| 내가 찜한 상품 (예정) | `/my-page/likes` | GET |

### API (예정)

| 기능 | URL | 메서드 |
|---|---|---|
| 찜하기 / 찜 취소 | `/api/products/{id}/likes` | POST / DELETE |
| 상품 검색 | `/api/products/search?keyword=&status=` | GET |

---

## 패키지 구조

```
used.system
 ├── SystemApplication
 ├── controller
 │    ├── home        # 웰컴 페이지
 │    ├── member      # 회원가입, 로그인/로그아웃, SessionConst, Form 객체
 │    ├── myPage      # 마이페이지, 내가 등록한 상품
 │    └── product     # 상품 등록/목록/상세/수정, Form 객체
 ├── member           # Member 도메인, Service, Repository(Memory)
 ├── product          # Product 도메인, ProductGrade, Service, Repository(Memory), UpdateDto
 └── exception        # 커스텀 예외 + GlobalExceptionHandler(@ControllerAdvice)
```

---

## 설계 원칙

이 프로젝트에서 강제하는 규칙들. 어기면 학습 효과가 반토막 나므로 리팩토링 대상이 된다.

### 1. 계층 의존은 한 방향으로만

```
controller → service → repository
   (Form)      (Dto)      (Domain)
```

- 컨트롤러는 요청/응답 흐름만, 비즈니스 로직은 서비스에, 저장은 리포지토리에.
- 서비스는 웹 계층 객체(Form)를 모른다 → `Form.toDto()`로 변환해 전달 (`ProductUpdateDto`).
- 도메인은 DTO조차 모른다 → 서비스가 DTO를 풀어 값으로 전달 (`product.update(...)`).

### 2. Form과 Domain의 분리

- 폼 객체(`MemberForm`, `ProductForm`, …)는 화면 입력 + 형식 검증 전용.
- 사용자가 입력해선 안 되는 값(`sellerId` 등)은 폼에 두지 않는다 → 과잉 바인딩(mass assignment) 방지.
- 판매자 식별은 입력값이 아닌 **세션**에서 가져온다.

### 3. 검증의 이원화

| 종류 | 예 | 처리 위치 |
|---|---|---|
| 형식 검증 | 빈 값, 길이, 최소 가격 | 폼 객체 어노테이션 (`@NotBlank`, `@Size`, `@Min`) + `BindingResult` |
| 필드 간 비교 | 비밀번호 확인 일치 | 컨트롤러에서 `rejectValue` |
| 비즈니스 검증 | 아이디 중복, 소유권 | **서비스 계층** (저장소를 봐야 판단 가능한 규칙) |

### 4. 예외 처리의 이원화 — "사용자가 고칠 수 있는가?"

| 상황 | 처리 | 이유 |
|---|---|---|
| 중복 아이디 등 재시도로 해결 가능 | 컨트롤러 try-catch → `rejectValue` → **폼 재표시** | 입력값 유지 + 필드 에러 표시에 필요한 재료(폼, BindingResult)는 컨트롤러에만 있다 |
| 없는 상품(404), 남의 상품 접근(403) 등 | 그대로 전파 → `@ControllerAdvice` → **에러 페이지** | 사용자가 고칠 수 없는 상황은 안내가 최선 |

현재 예외: `ProductNotFoundException`(404) · `MemberNotFoundException`(404) · `ForbiddenException`(403) · `DuplicateLoginIdException`(폼 에러 변환)

### 5. 보안 검증은 서버가, 화면 동선은 안내일 뿐

- 버튼 숨기기/페이지 동선 차단은 보안이 아니다 — URL 직접 접근은 언제나 가능하다.
- 로그인 체크: GET(폼 진입)은 UX용, **POST(데이터 변경)는 방어용** — 둘 다 한다.
- 소유권 검증("본인 상품만 수정·삭제 가능")은 어떤 경로로 호출돼도 우회 불가능하도록 **서비스 계층**에 둔다.
- 삭제 확인 창(`confirm`)은 실수 방지용 UX일 뿐 보안 장치가 아니다 — JS를 끄거나 직접 요청을 보내면 무력화된다.

### 6. 도메인 응집도

- 무분별한 setter를 열어두지 않는다 — 상태 변경은 의미 있는 메서드(`product.update(...)`)로만.
- 수정 시각(`updatedAt`) 갱신처럼 상태 변경에 따라오는 규칙은 도메인 메서드 안에서 자동 처리.
