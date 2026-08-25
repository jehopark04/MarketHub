package used.system.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import used.system.controller.member.SessionConst;

/**
 * API 요청의 로그인 검사.
 *
 * <p>LoginCheckInterceptor와 판단은 같고 거절하는 방법만 다르다. 화면은 로그인 페이지로 보내면 되지만, API에 302를 주면 클라이언트가 로그인
 * HTML을 응답 본문으로 받는다 — 요청이 실패했다는 사실이 전달되지 않는다. 그래서 401로 끊는다.
 *
 * <p>본문 형식은 RFC 9457(Problem Details)을 따른다. ApiExceptionHandler가 내보내는 형식과 같아야 클라이언트가 실패를 한 가지 방법으로만
 * 처리할 수 있다. 여기는 인터셉터라 @ExceptionHandler가 닿지 않으므로 같은 모양을 직접 쓴다.
 */
public class ApiLoginCheckInterceptor implements HandlerInterceptor {

  private static final String UNAUTHORIZED_BODY =
      "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,"
          + "\"detail\":\"로그인이 필요합니다.\"}";

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {

    // false여야 한다. 인자 없는 getSession()은 세션이 없으면 새로 만들어버린다.
    HttpSession session = request.getSession(false);

    if (session == null || session.getAttribute(SessionConst.LOGIN_MEMBER) == null) {
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
      response.getWriter().write(UNAUTHORIZED_BODY);
      return false; // 요청을 여기서 끝낸다. 컨트롤러는 실행되지 않는다.
    }

    return true;
  }
}
