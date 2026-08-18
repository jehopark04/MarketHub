package used.system.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;
import used.system.controller.member.SessionConst;
import used.system.member.Member;

/**
 * WebConfig의 인터셉터 등록이 의도한 요청에 걸리는지 검증한다. 스프링 컨텍스트는 띄우지 않는다.
 *
 * <p>"이 URL이 공개여도 되는가"는 정책 판단이라 테스트할 수 없다. 여기서 보는 건 배선이다 — WebConfig에 적어둔 의도대로 실제 매칭이 일어나는가.
 *
 * <p>패턴 문자열을 테스트에 다시 적지 않고 WebConfig 인스턴스에서 등록 결과를 꺼내 쓴다. 다시 적으면 복사본을 검증하게 되어, WebConfig만 고치고 테스트를
 * 안 고쳐도 통과한다 — 정작 막으려던 사고를 못 막는다.
 */
class WebConfigTest {

  private final Member loginMember = new Member("userA", "에이", "password1");
  private final Object handler = new Object();

  /** 로그인이 필요한 요청. "메서드 경로" 형태. */
  private static final String[] 보호_대상 = {
    "GET /products/new",
    "POST /products",
    "GET /products/5/edit",
    "POST /products/5/edit",
    "POST /products/5/delete",
    "GET /my-page",
    "GET /my-page/products",
  };

  /** 로그인 없이 열려야 하는 요청. */
  private static final String[] 공개_대상 = {
    "GET /",
    "GET /login",
    "POST /login",
    "POST /logout",
    "GET /members/new",
    "POST /members",
    "GET /products",
    "GET /products/5",
  };

  /** InterceptorRegistry.getInterceptors()가 protected라 서브클래스로만 꺼낼 수 있다. */
  private static class ExposedRegistry extends InterceptorRegistry {
    List<Object> registered() {
      return getInterceptors();
    }
  }

  private List<Object> interceptors() {
    ExposedRegistry registry = new ExposedRegistry();
    new WebConfig().addInterceptors(registry);
    return registry.registered();
  }

  /**
   * DispatcherServlet이 하는 일을 축약한다 — 경로가 맞는 인터셉터만 골라 preHandle을 순서대로 돌리고, 하나라도 false를 반환하면 요청이 끊긴
   * 것이다.
   *
   * @return 컨트롤러까지 도달하면 true
   */
  private boolean reachesController(String method, String path, MockHttpSession session)
      throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest(method, path);
    if (session != null) {
      request.setSession(session);
    }
    // MappedInterceptor.matches가 캐시된 RequestPath를 읽는다. 평소엔 DispatcherServlet이 채운다.
    ServletRequestPathUtils.parseAndCache(request);
    MockHttpServletResponse response = new MockHttpServletResponse();

    for (Object each : interceptors()) {
      MappedInterceptor mapped = (MappedInterceptor) each;
      if (!mapped.matches(request)) {
        continue;
      }
      HandlerInterceptor interceptor = mapped.getInterceptor();
      if (!interceptor.preHandle(request, response, handler)) {
        // 끊었다면 로그인 페이지로 보냈어야 한다. 엉뚱한 곳으로 갔다면 그것도 결함이다.
        assertThat(response.getRedirectedUrl())
            .as("%s %s 를 차단했다면 /login으로 보내야 한다", method, path)
            .isEqualTo("/login");
        return false;
      }
    }
    return true;
  }

  /** 로그인 성공했을때 서버에 남는 상태를 그대로 재현한 것. 실제 memberController의 로그인 처리가 하는 일임 */
  private MockHttpSession loggedInSession() {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(SessionConst.LOGIN_MEMBER, loginMember);
    return session;
  }

  /** 목록 전체를 한 번에 확인한다. 하나가 틀려도 나머지 결과까지 함께 보여야 원인이 빨리 좁혀진다. */
  private void 전부(String[] 요청들, MockHttpSession session, boolean 도달해야_하는가) {
    SoftAssertions.assertSoftly(
        softly -> {
          for (String each : 요청들) {
            String[] parts = each.split(" ");
            try {
              softly
                  .assertThat(reachesController(parts[0], parts[1], session))
                  .as("%s — 컨트롤러 도달 여부", each)
                  .isEqualTo(도달해야_하는가);
            } catch (Exception e) {
              softly.fail("%s 검사 중 예외: %s", each, e);
            }
          }
        });
  }

  @Test
  @DisplayName("보호 경로는 비로그인 요청을 컨트롤러에 닿기 전에 끊는다")
  void 보호_경로는_비로그인을_막는다() {
    전부(보호_대상, null, false);
  }

  @Test
  @DisplayName("공개 경로는 비로그인 요청도 컨트롤러까지 통과시킨다")
  void 공개_경로는_비로그인도_통과시킨다() {
    전부(공개_대상, null, true);
  }

  @Test
  @DisplayName("로그인 상태면 보호 경로도 전부 통과한다")
  void 로그인하면_보호_경로도_통과한다() {
    전부(보호_대상, loggedInSession(), true);
  }

  @Test
  @DisplayName("상품 상세는 열려 있지만 등록 폼은 막힌다 - 숫자 제약이 둘을 가른다")
  void 숫자_제약이_상세와_등록폼을_가른다() throws Exception {
    // /products/{productId:[0-9]+}를 /products/*로 "단순화"하면 등록 폼이 조용히 공개된다.
    // 화면상 아무 이상이 없어 손으로는 발견되지 않는 종류의 사고라 여기서 고정한다.
    assertThat(reachesController("GET", "/products/5", null)).isTrue();
    assertThat(reachesController("GET", "/products/new", null)).isFalse();
  }

  @Test
  @DisplayName("같은 /products라도 목록 조회는 열리고 상품 등록은 막힌다")
  void 메서드가_products를_가른다() throws Exception {
    // 경로 패턴은 HTTP 메서드를 구분하지 못한다. 등록을 둘로 나눠 가른 부분이라 함께 고정한다.
    assertThat(reachesController("GET", "/products", null)).isTrue();
    assertThat(reachesController("POST", "/products", null)).isFalse();
  }
}
