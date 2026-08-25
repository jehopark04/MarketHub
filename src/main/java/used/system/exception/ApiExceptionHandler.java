package used.system.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * API 요청의 예외 → JSON 응답.
 *
 * <p>GlobalExceptionHandler는 뷰 이름을 반환한다. API 요청이 거기로 흘러가면 클라이언트가 JSON 대신 에러 HTML을 받는다. 그래서 API용을 따로
 * 둔다.
 *
 * <p>두 가지 장치로 갈라낸다. annotations = RestController.class로 @RestController에서 터진 예외만 잡고, @Order로
 * GlobalExceptionHandler(@Order 없음 = 최하위)보다 먼저 검사되게 한다. 필터에 걸리지 않는 SSR 컨트롤러의 예외는 이 핸들러를 건너뛰어 기존대로
 * GlobalExceptionHandler가 처리한다 — 그쪽은 한 줄도 고치지 않았다.
 *
 * <p>응답 형식은 스프링 내장 ProblemDetail(RFC 9457)이다. 에러 응답 DTO를 직접 만드는 것보다 낫다 — 표준이라 클라이언트가 형식을 예측할 수 있고,
 * 반환값의 status가 응답 상태 코드로 그대로 적용된다.
 *
 * <p>⚠️ GlobalExceptionHandler에 예외를 추가할 때 이 클래스에도 추가해야 한다. 빠뜨리면 API가 조용히 HTML을 받는다.
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

  @ExceptionHandler(ProductNotFoundException.class)
  public ProblemDetail handleProductNotFound(ProductNotFoundException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(MemberNotFoundException.class)
  public ProblemDetail handleMemberNotFound(MemberNotFoundException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(ForbiddenException.class)
  public ProblemDetail handleForbidden(ForbiddenException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
  }
}
