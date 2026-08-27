# system

중고거래 웹 애플리케이션. Spring Boot 기반 **JSON REST API**. 학습용 프로젝트.

원래 Thymeleaf SSR이었고 같은 기능을 API로 옮긴 뒤 화면 계층을 걷어냈다.
지금은 `src/main/resources/static/`에 바닐라 JS 프론트를 단계적으로 얹는 중이다.
같은 서버가 화면과 API를 모두 주므로 동일 오리진이고, CORS 설정도 토큰 인증도 필요 없다.

## 스택
Spring Boot 4 / Java 21 / Gradle / Spring WebMVC / Lombok

- 테스트 `./gradlew test` · 포맷 `./gradlew spotlessApply`
  (Spotless: googleJavaFormat + removeUnusedImports)
- ⚠️ Boot **4**라 starter 이름이 3.x와 다르다 — `spring-boot-starter-webmvc`,
  `-webmvc-test`. `spring-boot-starter-web` / `spring-boot-starter-test`가 아니다.

## 구조 규칙
`used.system` 아래 배치는 다음과 같다. **새 파일을 만들 때 이 자리를 지킨다.**

| 대상 | 위치 | 예 |
|---|---|---|
| 도메인 객체·Service·ServiceImpl·Repository·MemoryRepository | `used.system.<도메인>` — 한 패키지에 함께 산다 | `used.system.product.ProductServiceImpl` |
| Controller·요청/응답 DTO·세션 상수 | `used.system.controller.<도메인>` | `used.system.controller.product.ProductCreateRequest` |
| 커스텀 예외·`ApiExceptionHandler` | `used.system.exception` | `used.system.exception.ProductNotFoundException` |
| 화면(HTML·CSS·JS) | `src/main/resources/static/` — 아래 규칙 참조 | `static/js/api.js` |

즉 **서비스·리포지토리 계층은 도메인으로 나누고, 웹 계층과 예외는 계층으로 모은다.**
컨트롤러를 도메인 패키지(`used.system.product`)에 두지 않는다.

- **Controller → Service(인터페이스) → Repository(인터페이스)** 단방향. 역방향 금지.
- 생성자 주입(`@RequiredArgsConstructor` + `private final`). 필드/세터 주입 금지.
- Service·Repository는 **인터페이스 + 구현 쌍**으로 둔다. **구현이 하나뿐이어도 인터페이스를
  제거하지 않는다** — 구현 교체 지점이고, 테스트가 인터페이스에 의존해야 갈아끼울 수 있다.
  YAGNI로 판단하지 않는다.
- 비즈니스 규칙(소유권·존재 검증)은 **Service**. Controller는 세션 확인·바인딩·응답 변환만.
- 요청/응답 DTO는 `record`. 이름은 `~Request` / `~Response`로 역할을 드러낸다.
  도메인 객체를 그대로 응답에 싣지 않는다 — `Member`를 반환하면 비밀번호가 나간다.

## 도메인 객체
- 상태 변경은 의미 있는 메서드로(`product.update(...)`). setter 남발 금지.
- `setId`만 예외 — 리포지토리가 `save()` 시점에 채번해 부여하는 통로. 의도된 설계다.
- 생성 시 `createAt`, 변경 시 `updatedAt` 갱신.

## 예외 · 응답
- 도메인별 커스텀 예외(`RuntimeException` 상속).
- 예외 → HTTP 상태 매핑은 `ApiExceptionHandler`(`@RestControllerAdvice`)에 **집중**한다.
  컨트롤러에서 try-catch로 흩뿌리지 않는다. **새 커스텀 예외를 만들면 여기 항목을 더한다.**
- 에러 응답 본문은 스프링 내장 `ProblemDetail`(RFC 9457). 에러 DTO를 직접 만들지 않는다.
- 성공 응답: 상태 코드가 늘 같으면 DTO를 그대로 반환하고, 상태 코드나 헤더를
  메서드가 정해야 할 때만 `ResponseEntity`로 감싼다.

## 인증 · 검증
- 인증은 **세션**이다. 세션 키는 `used.system.controller.member.SessionConst`의
  `LOGIN_MEMBER` 상수. 문자열 하드코딩 금지.
- **로그인 검사는 컨트롤러에 두지 않는다.** `ApiLoginCheckInterceptor`가 컨트롤러에 닿기 전에
  401로 끊는다. 어느 경로를 열지는 `WebConfig`가 정한다 — 전부 막고 열 곳만 뚫는다.
  경로 패턴은 HTTP 메서드를 구분하지 못하므로, 조회만 연 경로는 쓰기를 메서드로 다시 막는다.
- 세션 회원은 `@SessionAttribute(name = SessionConst.LOGIN_MEMBER)`로 받는다.
  인터셉터가 이미 막았으니 `required = false`는 **비로그인도 허용하는 경로에서만** 쓴다.
- 신원(판매자·소유자)은 **요청 본문이 아니라 세션에서** 정한다.
  클라이언트가 보낸 값을 쓰면 남의 이름으로 등록된다.
- 로그인 상태로 만드는 일은 `MemberApiController.startSession` 하나를 쓴다.
  **기존 세션을 버리고 새로 발급한다** — 남이 심어둔 세션 id가 인증되면 심은 쪽이 함께
  들어온다(세션 고정). 인자 없는 `getSession()`은 기존 세션을 승격시키므로 쓰지 않는다.
- **회원가입도 세션을 발급한다.** 가입만 시키고 로그인을 따로 요청하게 하면 방금 정한 평문
  비밀번호가 두 번 오가고, 가입은 됐는데 로그인만 실패하는 되돌릴 수 없는 상태가 생긴다.
- 요청 DTO에 Bean Validation, 컨트롤러에서 `@Validated` + `@RequestBody`.
  **`BindingResult`를 받지 않는다** — 받으면 검증 실패가 예외로 안 올라가 400이 나가지 않는다.
  실패는 `MethodArgumentNotValidException` → `ApiExceptionHandler`가 필드별 메시지와 함께 400.
- 예외: 검색 조건(`ProductSearchCond`)만 `BindingResult`를 받아 **일부러 삼킨다**.
  조회는 잘못된 조건에 실패로 답하지 않는다 — 그 필드만 빠진 채 나머지로 검색된다.

## 프론트 (`src/main/resources/static/`)
- 빌드 도구·프레임워크·CDN을 쓰지 않는다. **바닐라 JS(ES 모듈) + CSS**만 쓴다.
  브라우저가 그대로 실행하는 코드여야 학습 대상이 흐려지지 않는다.
- 배치: 페이지는 `static/*.html`, 스타일은 `static/css/`, 스크립트는 `static/js/`.
  id가 필요한 화면은 `product.html?id=1`처럼 쿼리 파라미터로 받는다.
- **모든 서버 호출은 `js/api.js`를 지난다.** 화면이 `fetch`를 직접 부르지 않는다 —
  실패 처리가 화면 수만큼 복사된다. 경로 문자열도 여기서만 안다.
- 색은 `css/tokens.css`의 변수로만 쓴다. 직접 적으면 다크 모드에서 한쪽만 바뀐다.
- 사용자 입력을 `innerHTML`에 넣을 때는 이스케이프한다(`escapeHtml`).
- 목록을 다시 그리는 요청은 **앞선 요청을 취소**한다(`AbortController`).
  취소하지 않으면 먼저 보낸 응답이 늦게 도착해 최신 결과를 덮어쓴다.
- `fetch` 주의: `res.ok`를 직접 확인한다(4xx·5xx는 예외를 던지지 않는다).
  `204`는 본문이 없어 `res.json()`을 부르면 터진다.

## 테스트
- 위치: `src/test/java/used/`, **소스와 동일 패키지 구조**. 컨트롤러 테스트도 마찬가지로
  `used.system.controller.<도메인>`에 둔다.
- **단위테스트만 작성한다.** 인메모리 리포지토리라 Service 테스트에 실제 구현을 그대로 쓸 수 있다.
- **`@SpringBootTest`를 쓰지 않는다** — 느리고 통합 성격이라 단위테스트 범위를 벗어난다.
  기존 `SystemApplicationTests`는 Spring Initializr 기본 생성물이라 예외(지적·수정 대상 아님).
- 사용 가능: JUnit Jupiter · AssertJ(`assertThat`) · Mockito. 의존성 추가 없이 쓸 수 있다.
- 메서드명 한글 서술형 허용 (`상품_소유자가_아니면_수정시_예외`).
- **소스를 정리하다가 테스트를 고쳐 통과시키지 않는다.** 실패하는 테스트는 소스나 테스트 자체에
  문제가 있다는 신호다. 신호를 끄지 말고 원인을 보고한다.

## 커밋 메시지
**한글 서술형** ("상품 삭제 기능 구현"). Conventional Commits 아님.

## 지적 대상이 아닌 것
아래는 의도된 선택이다. **지적하거나 대안을 제안하지 않는다.**
이 목록이 없으면 의도된 설계가 매번 문제로 보고된다.

| 항목 | 판단 |
|---|---|
| 인메모리 리포지토리 | 학습 단계의 의도. "DB를 써라" 제안 금지 |
| `@SpringBootTest` 미사용 | 위 테스트 규약대로 의도적 |
| 프론트가 단계적으로만 있음 | 화면을 한 번에 다 만들지 않고 단계로 나눠 얹는다. 아직 없는 화면을 결함으로 올리지 않는다 |
| 프론트에 빌드 도구·프레임워크 없음 | 위 프론트 규약대로 의도적. "React를 써라"·"번들러를 붙여라" 금지 |
| `setId` | 리포지토리 채번 통로. "setter 하나뿐이라 일관성 없다"·"캡슐화 위반" 금지 |
| 동시성·스레드 안전성 | 단일 사용자 학습 전제로 프로젝트 범위 밖. `HashMap`·`++sequence`를 스레드 안전성 문제로 올리지 않는다 |

## 작업 산출물
자동화 도구의 중간 산출물은 `_workspace/`에 둔다 (`.gitignore` 처리됨).
