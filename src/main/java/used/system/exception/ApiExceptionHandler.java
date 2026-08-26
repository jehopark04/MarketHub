package used.system.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

  /**
   * 로그인 실패 → 401.
   *
   * <p>403이 아니다. 403은 "누구인지는 알지만 권한이 없다"이고, 여기는 아직 누구인지 확인되지 않은 상태다.
   */
  @ExceptionHandler(LoginFailedException.class)
  public ProblemDetail handleLoginFailed(LoginFailedException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
  }

  /**
   * 중복 아이디 → 409 Conflict.
   *
   * <p>400이 아닌 이유: 요청 자체는 형식도 값도 올바르다. 지금 서버 상태와 부딪힐 뿐이다. 400과 나눠두면 클라이언트가 "입력을 고쳐라"와 "그 아이디는
   * 임자가 있다"를 구분해 안내할 수 있다.
   *
   * <p>화면 쪽은 이 예외를 컨트롤러에서 잡아 폼 에러로 바꾼다 - 그래서 GlobalExceptionHandler에는 이 항목이 없다.
   */
  @ExceptionHandler(DuplicateLoginIdException.class)
  public ProblemDetail handleDuplicateLoginId(DuplicateLoginIdException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
  }

  /**
   * 요청 본문 검증 실패 → 400.
   *
   * <p>화면은 BindingResult를 들고 폼으로 되돌아가지만 API는 되돌아갈 화면이 없다. 어느 필드가 왜 틀렸는지를 응답에 담지 않으면 클라이언트가 폼에 에러를
   * 표시할 수 없다 - 스프링 기본 처리는 400을 주되 메시지를 통째로 버린다.
   *
   * <p>한 필드에 검증이 여러 개 걸릴 수 있어 먼저 담긴 메시지를 남긴다. Collectors.toMap은 키가 겹치면 예외를 던져 400이 500이 된다.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
    Map<String, String> errors = new LinkedHashMap<>();
    for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
      errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
    }

    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다.");
    problem.setProperty("errors", errors);
    return problem;
  }

  /**
   * 본문을 읽지 못함 → 400. JSON이 깨졌거나 "grade":"Z"처럼 변환할 수 없는 값일 때다.
   *
   * <p>예외 메시지를 그대로 싣지 않는다. Jackson의 메시지에는 내부 클래스명과 경로가 들어 있어 클라이언트에게 줄 것이 아니다.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail handleNotReadable(HttpMessageNotReadableException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다.");
  }
}
