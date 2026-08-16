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
 * LoginCheckInterceptor 단위 테스트 - 스프링 컨텍스트 없이 preHandle을 직접 호출한다.
 *
 * <p>컨트롤러에 있던 비로그인 차단 책임이 이 클래스로 옮겨왔으므로, "세션이 없으면 컨트롤러에 닿기 전에 끊는가"를 여기서 검증한다.
 *
 * <p>어느 경로에 걸리는지는 WebConfig의 몫이라 여기서 다루지 않는다. 이 테스트가 보는 건 preHandle이 호출된 다음의 판단뿐이다.
 */
class LoginCheckInterceptorTest {

  private final Member loginMember = new Member("userA", "에이", "password1");
  private final Object handler = new Object();

  private MockHttpServletRequest requestWithLogin(String method) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, "/products");
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(SessionConst.LOGIN_MEMBER, loginMember);
    request.setSession(session);
    return request;
  }

  // ---------- 메서드를 지정하지 않은 등록 (전부 검사) ----------

  @Test
  @DisplayName("세션 자체가 없으면 로그인 페이지로 돌려보내고 요청을 끊는다")
  void 세션_없음() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/my-page");
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean result = new LoginCheckInterceptor().preHandle(request, response, handler);

    assertThat(result).isFalse();
    assertThat(response.getRedirectedUrl()).isEqualTo("/login");
  }

  @Test
  @DisplayName("세션은 있어도 로그인 회원이 담겨 있지 않으면 요청을 끊는다")
  void 세션은_있지만_비로그인() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/my-page");
    request.setSession(new MockHttpSession()); // 비어 있는 세션
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean result = new LoginCheckInterceptor().preHandle(request, response, handler);

    assertThat(result).isFalse();
    assertThat(response.getRedirectedUrl()).isEqualTo("/login");
  }

  @Test
  @DisplayName("로그인 회원이 세션에 있으면 통과시킨다")
  void 로그인_상태() throws Exception {
    MockHttpServletRequest request = requestWithLogin("GET");
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean result = new LoginCheckInterceptor().preHandle(request, response, handler);

    assertThat(result).isTrue();
    assertThat(response.getRedirectedUrl()).isNull();
  }

  // ---------- 메서드를 지정한 등록 (POST만 검사) ----------

  @Test
  @DisplayName("검사 대상 메서드가 아니면 비로그인이라도 통과시킨다")
  void 대상_메서드가_아니면_통과() throws Exception {
    // /products는 목록 조회(GET)와 상품 등록(POST)이 같은 경로다. 목록은 비로그인도 봐야 한다.
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/products");
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean result = new LoginCheckInterceptor("POST").preHandle(request, response, handler);

    assertThat(result).isTrue();
    assertThat(response.getRedirectedUrl()).isNull();
  }

  @Test
  @DisplayName("검사 대상 메서드면 비로그인일 때 요청을 끊는다")
  void 대상_메서드면_비로그인_차단() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/products");
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean result = new LoginCheckInterceptor("POST").preHandle(request, response, handler);

    assertThat(result).isFalse();
    assertThat(response.getRedirectedUrl()).isEqualTo("/login");
  }

  @Test
  @DisplayName("검사 대상 메서드라도 로그인 상태면 통과시킨다")
  void 대상_메서드_로그인_상태() throws Exception {
    MockHttpServletRequest request = requestWithLogin("POST");
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean result = new LoginCheckInterceptor("POST").preHandle(request, response, handler);

    assertThat(result).isTrue();
    assertThat(response.getRedirectedUrl()).isNull();
  }
}
