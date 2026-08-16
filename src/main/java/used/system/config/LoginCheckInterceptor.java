package used.system.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Set;
import org.springframework.web.servlet.HandlerInterceptor;
import used.system.controller.member.SessionConst;

/**
 * 로그인이 필요한 경로를 컨트롤러 진입 전에 막는다.
 *
 * <p>세 개의 훅 중 preHandle만 구현한다. 나머지는 HandlerInterceptor의 default 구현을 그대로 쓴다. 인증은 컨트롤러가 실행되기 전에 끊어야
 * 의미가 있고, 흐름을 끊을 수 있는 건 preHandle뿐이다(나머지는 반환 타입이 void).
 *
 * <p>어느 경로에 적용할지, 그 경로에서 어느 HTTP 메서드를 검사할지는 이 클래스가 아니라 등록하는 쪽(WebMvcConfigurer)이 정한다.
 */
public class LoginCheckInterceptor implements HandlerInterceptor {

  /**
   * 검사할 HTTP 메서드. 비어 있으면 메서드를 가리지 않고 전부 검사한다.
   *
   * <p>인터셉터의 경로 매칭은 GET·POST를 구분하지 못한다. 같은 경로에 조회와 등록이 함께 걸려 있어 한쪽만 막아야 할 때 이 필드로 좁힌다.
   */
  private final Set<String> guardedMethods;

  public LoginCheckInterceptor(String... guardedMethods) {
    this.guardedMethods = Set.of(guardedMethods);
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {

    // 검사 대상 메서드를 지정해 등록했고 이 요청이 거기 없으면, 로그인을 보지 않고 통과시킨다.
    if (!guardedMethods.isEmpty() && !guardedMethods.contains(request.getMethod())) {
      return true;
    }

    // false여야 한다. 인자 없는 getSession()은 세션이 없으면 새로 만들어버려서,
    // 비로그인 방문자마다 빈 세션이 쌓인다. 여기선 확인만 한다.
    HttpSession session = request.getSession(false);

    // session이 null이면 오른쪽은 평가되지 않는다(단축 평가).
    // 왼쪽 검사를 빼면 null.getAttribute(...)로 NPE가 난다.
    if (session == null || session.getAttribute(SessionConst.LOGIN_MEMBER) == null) {
      // 컨트롤러가 아니라 뷰 이름("redirect:/login")을 반환할 수 없다. 직접 302를 쓴다.
      response.sendRedirect("/login");
      return false; // 요청을 여기서 끝낸다. 컨트롤러도 뷰 렌더링도 일어나지 않는다.
    }

    return true; // 통과. 컨트롤러로 넘어간다.
  }
}
