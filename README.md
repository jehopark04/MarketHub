# MarketHub — 미니 중고거래 플랫폼

중고나라/당근마켓의 축소판을 직접 만들며 **Spring MVC 웹 애플리케이션의 구조를 체화**하기 위한 학습 프로젝트입니다.

단순 CRUD 게시판이 아니라 회원/세션 로그인, 상품, 권한 검증, 검증(Validation), 예외 처리, 검색·필터, 찜, (예정) 문의·REST API까지 —
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

### Level 2 — 비즈니스 규칙 (진행 중)

- [x] 로그인 체크 인터셉터 (`LoginCheckInterceptor` + `WebConfig` — 컨트롤러마다 반복되던 세션 체크를 하나로 통합)
- [x] 검색 / 필터 (`/products?keyword=&minPrice=&maxPrice=&grade=` — 조건별 독립 적용, 빈 조건은 전체 조회)
- [x] 찜 기능 (하트 토글, 내가 찜한 상품 목록 · 중복 찜 방지 · 삭제된 상품 자동 제외)
- [ ] 상품 문의 / 판매자 답변 (작성자·판매자 권한 체크)

### Level 3 — REST API + 예외 응답 (예정)

- [ ] 찜하기 / 찜 취소 API (`POST·DELETE /api/products/{id}/likes`)
- [ ] 상품 검색 API (`GET /api/products/search`)
- [ ] `@RestControllerAdvice` 기반 공통 JSON 예외 응답 (`{ "code": ..., "message": ... }`)
- [ ] API 전용 DTO

### 부가 작업 (레벨과 무관 — 아무 때나 끼워 넣어도 되는 항목)

- [ ] 백엔드 로깅 도입 (요청 흐름이 아니라 로그 레벨·구조화 로깅 관점의 일반 로깅 정비)

### 이후 로드맵

- [ ] Memory Repository → JPA 전환
- [ ] Spring Security 도입 (2차 리팩토링, JPA 전환 이후)

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
| 상품 목록 / 검색 | `/products?keyword=&minPrice=&maxPrice=&grade=` | GET |
| 상품 상세 | `/products/{id}` | GET |
| 상품 등록 | `/products/new` → `/products` | GET → POST |
| 상품 수정 | `/products/{id}/edit` | GET → POST |
| 상품 삭제 | `/products/{id}/delete` | POST |
| 찜하기 / 찜 취소 | `/products/{id}/likes` / `/products/{id}/likes/delete` | POST / POST |
| 마이페이지 | `/my-page` | GET |
| 내가 등록한 상품 | `/my-page/products` | GET |
| 내가 찜한 상품 | `/my-page/likes` | GET |

찜을 토글 하나로 두지 않고 추가·취소로 나눈 이유는 아래 설계 원칙 7번 참고.

### API (예정)

| 기능 | URL | 메서드 |
|---|---|---|
| 찜하기 / 찜 취소 | `/api/products/{id}/likes` | POST / DELETE |
| 상품 검색 | `/api/products/search?keyword=&grade=` | GET |

---

## 패키지 구조

```
used.system
 ├── SystemApplication
 ├── config
 │    ├── WebConfig               # 인터셉터 등록 (경로별 로그인 요구 여부 배선)
 │    └── LoginCheckInterceptor   # 세션 미인증 요청을 컨트롤러 진입 전에 차단
 ├── controller
 │    ├── home        # 웰컴 페이지
 │    ├── like        # 찜하기 / 찜 취소
 │    ├── member      # 회원가입, 로그인/로그아웃, SessionConst, Form 객체
 │    ├── myPage      # 마이페이지, 내가 등록한 상품, 내가 찜한 상품
 │    └── product     # 상품 등록/목록/상세/수정, Form 객체, 검색 조건
 ├── like             # Like 도메인, Service, Repository(Memory)
 ├── member           # Member 도메인, Service, Repository(Memory)
 ├── product          # Product 도메인, ProductGrade, Service, Repository(Memory), UpdateDto, SearchCond
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
- **인증**(로그인 여부)은 `LoginCheckInterceptor`가 컨트롤러 진입 전에 일괄 차단한다.
  - 인터셉터의 경로 매칭은 HTTP 메서드를 구분하지 못한다 — `/products`처럼 GET(조회)은
    공개, POST(등록)만 보호해야 하는 경로는 `guardedMethods`로 메서드를 좁혀 별도 등록한다.
  - "전부 막고 열 곳만 뚫는다" 방식(`excludePathPatterns`)을 쓴다. 반대로 막을 곳만
    나열하면, 새 페이지를 추가하며 등록을 잊었을 때 그 페이지가 조용히 무방비로 열린다.
- **인가**(소유권 검증, "본인 상품만 수정·삭제 가능")는 인터셉터가 아니라 **서비스 계층**에
  둔다. 인터셉터는 "누구냐"만 알고 "이 자원이 이 사람 것이냐"는 데이터를 봐야 판단
  가능한 비즈니스 규칙이라, 어떤 호출 경로로 들어와도 우회 불가능한 서비스가 최종 방어선이다.
- 삭제 확인 창(`confirm`)은 실수 방지용 UX일 뿐 보안 장치가 아니다 — JS를 끄거나 직접 요청을 보내면 무력화된다.

### 6. 도메인 응집도

- 무분별한 setter를 열어두지 않는다 — 상태 변경은 의미 있는 메서드(`product.update(...)`)로만.
- 수정 시각(`updatedAt`) 갱신처럼 상태 변경에 따라오는 규칙은 도메인 메서드 안에서 자동 처리.

### 7. 같은 요청이 두 번 와도 결과가 같아야 한다 (멱등성)

- 하트 연타, 새로고침, 네트워크 재시도로 **같은 요청이 중복 도착하는 것은 정상**이다.
- 그래서 찜을 **토글 하나로 만들지 않는다.** 토글은 두 번 보내면 결과가 뒤집힌다.
  추가(`/likes`)와 취소(`/likes/delete`)로 나누고, 어느 폼을 그릴지는 화면이 현재 상태를 보고 정한다.
  사용자에게는 여전히 하트 하나를 누르는 것으로 보인다.
- 이미 찜한 상품을 다시 찜하거나, 찜하지 않은 것을 취소해도 **예외 대신 통과**시킨다.
  이미 원하는 상태인데 에러 화면을 띄울 이유가 없다. 중복 저장이 막히는 것은 그대로다.

### 8. 목록 화면에서 항목마다 조회하지 않는다

- "이 상품을 내가 찜했나"를 상품마다 물으면 목록 길이만큼 질의가 늘어난다(N+1).
- 대신 **내 찜 id 전체를 `Set`으로 한 번 받아**, 화면은 `contains`만 확인한다.
- 비로그인에는 `null`이 아니라 **빈 `Set`**을 넘긴다 — `null`이면 템플릿의 `contains` 호출이 터진다.
  "없음"은 언제나 빈 컬렉션으로 표현한다(리포지토리 반환값도 동일).

### 9. 클라이언트가 보낸 값으로 이동하지 않는다

- 찜 후 원래 화면으로 되돌릴 때 `Referer` 헤더를 쓰되, **경로와 쿼리만 떼어 쓰고 호스트는 버린다.**
- 값을 그대로 `redirect:`에 넣으면 외부 주소로 사용자를 튕겨 보낼 수 있다(**오픈 리다이렉트**).
  헤더는 조작 가능한 입력이므로 신뢰하지 않는다 — 원칙 5의 "클라이언트를 믿지 않는다"와 같은 이유.

---

## 테스트

**단위 테스트만 작성한다.** `@SpringBootTest`, `MockMvc` 등 스프링 컨텍스트를 띄우는 테스트는
쓰지 않는다 — 느리고, 이 프로젝트의 학습 목표(계층 간 책임 분리를 코드로 확인하는 것)에는
컨트롤러/서비스 메서드를 직접 호출하는 것으로 충분하다.

- 컨트롤러: `BindingResult`는 `BeanPropertyBindingResult`, 세션은 `MockHttpServletRequest`로
  직접 만들어 넘긴다. 둘 다 컨텍스트 없이 동작하는 단순 구현체다.
- 폼 검증: `Validation.buildDefaultValidatorFactory()`로 Validator를 직접 만든다.
- 인터셉터 배선(`WebConfigTest`): `InterceptorRegistry`에서 등록 결과를 직접 꺼내
  `DispatcherServlet`이 하는 매칭·`preHandle` 순회를 그대로 흉내 낸다. 어느 URL이 보호돼야
  하는지는 정책 판단이라 테스트 대상이 아니고, `WebConfig`에 적어둔 의도대로 실제 매칭이
  일어나는지(배선)만 검증한다.
- 검증 신뢰: 새 테스트가 실제로 회귀를 잡는지는 뮤테이션(정상 동작하던 코드를 일부러
  깨뜨려보고 해당 테스트만 실패하는지 확인)으로 확인한다. 초록 불이 켜져도 "왜 초록인지"를
  설명 못 하면 허수 테스트일 수 있다.
