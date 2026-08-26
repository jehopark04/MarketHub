package used.system.exception;

/**
 * 아이디나 비밀번호가 맞지 않음.
 *
 * <p>메시지가 둘을 구분하지 않는 것은 의도다. "없는 아이디입니다"라고 답하면 공격자가 어떤 아이디가 존재하는지 하나씩 확인할 수 있다.
 *
 * <p>MemberService.login은 실패를 null로 알린다. 컨트롤러가 그 null을 이 예외로 옮겨, 실패 응답을 만드는 일이 ApiExceptionHandler
 * 한곳에 모인다.
 */
public class LoginFailedException extends RuntimeException {

  public LoginFailedException(String message) {
    super(message);
  }
}
