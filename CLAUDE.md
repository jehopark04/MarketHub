# system

중고거래 웹 애플리케이션. Spring Boot + Thymeleaf 서버사이드 렌더링(SSR). 학습용 프로젝트.

## 스택
Spring Boot 4 / Java 21 / Gradle / Thymeleaf + Spring WebMVC / Lombok

- 테스트 `./gradlew test` · 포맷 `./gradlew spotlessApply`
  (Spotless: googleJavaFormat + removeUnusedImports)
- ⚠️ Boot **4**라 starter 이름이 3.x와 다르다 — `spring-boot-starter-webmvc`,
  `-webmvc-test`, `-thymeleaf-test`. `spring-boot-starter-web` / `spring-boot-starter-test`가 아니다.

## 구조 규칙
`used.system` 아래에 **계층이 아니라 도메인으로 먼저 나눈다.** 도메인 패키지 안에
Service·ServiceImpl·Repository·MemoryRepository가 함께 산다.

- **Controller → Service(인터페이스) → Repository(인터페이스)** 단방향. 역방향 금지.
- 생성자 주입(`@RequiredArgsConstructor` + `private final`). 필드/세터 주입 금지.
- Service·Repository는 **인터페이스 + 구현 쌍**으로 둔다. **구현이 하나뿐이어도 인터페이스를
  제거하지 않는다** — 구현 교체 지점이고, 테스트가 인터페이스에 의존해야 갈아끼울 수 있다.
  YAGNI로 판단하지 않는다.
- 비즈니스 규칙(소유권·존재 검증)은 **Service**. Controller는 세션 확인·바인딩·뷰 반환만.

## 도메인 객체
- 상태 변경은 의미 있는 메서드로(`product.update(...)`). setter 남발 금지.
- `setId`만 예외 — 리포지토리가 `save()` 시점에 채번해 부여하는 통로. 의도된 설계다.
- 생성 시 `createAt`, 변경 시 `updatedAt` 갱신.

## 예외 · 뷰
- 도메인별 커스텀 예외(`RuntimeException` 상속).
- 예외 → HTTP 상태·에러 뷰 매핑은 `GlobalExceptionHandler`(`@ControllerAdvice`)에 **집중**한다.
  컨트롤러에서 try-catch로 흩뿌리지 않는다.
- 에러 뷰는 `error/` 하위 템플릿, `model`에 `message` 전달.

## 인증 · 폼
- 세션 키는 `SessionConst.LOGIN_MEMBER` 상수. 문자열 하드코딩 금지.
- `@SessionAttribute(required = false)`로 꺼내고 `null`이면 `redirect:/login`.
- 폼 객체에 Bean Validation, 컨트롤러에서 `@Validated` + `BindingResult`.
  `hasErrors()`면 해당 폼 뷰로 되돌린다.

## 테스트
- 위치: `src/test/java/used/`, 소스와 동일 패키지 구조.
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
| SSR(Thymeleaf) | 의도된 선택. REST API 전환 제안 금지 |
| `setId` | 리포지토리 채번 통로. "setter 하나뿐이라 일관성 없다"·"캡슐화 위반" 금지 |
| 동시성·스레드 안전성 | 단일 사용자 학습 전제로 프로젝트 범위 밖. `HashMap`·`++sequence`를 스레드 안전성 문제로 올리지 않는다 |

## 작업 산출물
자동화 도구의 중간 산출물은 `_workspace/`에 둔다 (`.gitignore` 처리됨).
