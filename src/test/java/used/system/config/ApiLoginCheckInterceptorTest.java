package used.system.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import used.system.controller.member.SessionConst;
import used.system.member.Member;

/**
 * ApiLoginCheckInterceptor 단위 테스트 - 스프링 컨텍스트 없이 preHandle을 직접 호출한다.
 *
 * <p>화면용 LoginCheckInterceptor와 판단은 같으므로, 여기서 볼 것은 "거절하는 방법"이다. 302 + 로그인 HTML이 아니라 401 + JSON이어야
 * 한다.
 */
class ApiLoginCheckInterceptorTest {

  private final ApiLoginCheckInterceptor interceptor = new ApiLoginCheckInterceptor();
  private final Member loginMember = new Member("userA", "에이", "password1");
  private final Object handler = new Object();

  private MockHttpServletRequest apiRequest() {
    return new MockHttpServletRequest("PUT", "/api/me/likes/1");
  }

  @Test
  @DisplayName("세션이 없으면 401로 끊고 리다이렉트하지 않는다")
  void 세션_없음() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean passed = interceptor.preHandle(apiRequest(), response, handler);

    assertThat(passed).isFalse();
    assertThat(response.getStatus()).isEqualTo(401);
    // 여기가 "/login"이 되면 클라이언트는 실패를 성공한 HTML 응답으로 받는다.
    assertThat(response.getRedirectedUrl()).isNull();
  }

  @Test
  @DisplayName("세션은 있어도 로그인 정보가 없으면 401")
  void 세션은_있고_로그인_정보만_없음() throws Exception {
    MockHttpServletRequest request = apiRequest();
    request.setSession(new MockHttpSession()); // 빈 세션
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThat(interceptor.preHandle(request, response, handler)).isFalse();
    assertThat(response.getStatus()).isEqualTo(401);
  }

  @Test
  @DisplayName("401 본문은 ApiExceptionHandler와 같은 Problem Details 형식이다")
  void 거절_본문_형식() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    interceptor.preHandle(apiRequest(), response, handler);

    // 인터셉터 거절과 예외 응답의 형식이 갈라지면 클라이언트가 실패 처리를 두 벌 만들어야 한다.
    assertThat(response.getContentType()).startsWith("application/problem+json");
    assertThat(response.getContentAsString()).contains("\"status\":401").contains("\"detail\"");
  }

  @Test
  @DisplayName("로그인 상태면 통과시키고 응답에 손대지 않는다")
  void 로그인_상태() throws Exception {
    MockHttpServletRequest request = apiRequest();
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(SessionConst.LOGIN_MEMBER, loginMember);
    request.setSession(session);
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThat(interceptor.preHandle(request, response, handler)).isTrue();
    assertThat(response.getStatus()).isEqualTo(200); // 아직 아무도 손대지 않은 기본값
    assertThat(response.getContentAsString()).isEmpty();
  }
}
