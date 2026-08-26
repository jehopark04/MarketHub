package used.system.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 예외 → JSON 응답.
 *
 * <p>예외를 상태 코드로 옮기는 일을 여기 한곳에 모은다. 컨트롤러가 try-catch로 흩뿌리면 같은 예외가 곳에 따라 다른 코드로 나갈 수 있다.
 *
 * <p>ApiLoginCheckInterceptor의 401도 같은 형식을 쓴다. 인터셉터에는 @ExceptionHandler가 닿지 않아 그쪽은 본문을 직접 쓰지만, 모양이
 * 갈라지면 클라이언트가 실패 처리를 두 벌 만들어야 한다.
 *
 * <p>응답 형식은 스프링 내장 ProblemDetail(RFC 9457)이다. 에러 응답 DTO를 직접 만드는 것보다 낫다 — 표준이라 클라이언트가 형식을 예측할 수 있고,
 * 반환값의 status가 응답 상태 코드로 그대로 적용된다.
 *
 * <p>⚠️ 새 커스텀 예외를 만들면 여기에 항목을 더해야 한다. 빠뜨리면 500이 나간다.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(ProductNotFoundException.class)
  public ProblemDetail handleProductNotFound(ProductNotFoundException e) {
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
   * <p>400이 아닌 이유: 요청 자체는 형식도 값도 올바르다. 지금 서버 상태와 부딪힐 뿐이다. 400과 나눠두면 클라이언트가 "입력을 고쳐라"와 "그 아이디는 임자가
   * 있다"를 구분해 안내할 수 있다.
   *
   * <p>요청을 보낸 쪽이 아이디를 바꿔 다시 시도하면 되는 상황이라, 서버 결함을 뜻하는 5xx가 아니다.
   */
  @ExceptionHandler(DuplicateLoginIdException.class)
  public ProblemDetail handleDuplicateLoginId(DuplicateLoginIdException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
  }

  /**
   * 요청 본문 검증 실패 → 400.
   *
   * <p>어느 필드가 왜 틀렸는지를 응답에 담지 않으면 클라이언트가 입력칸에 에러를 표시할 수 없다. 스프링 기본 처리는 400을 주되 메시지를 통째로 버린다.
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
