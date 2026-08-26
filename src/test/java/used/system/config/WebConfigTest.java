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

  /**
   * 로그인이 필요한 요청. "메서드 경로" 형태.
   *
   * <p>상품 쓰기 셋이 특히 중요하다. 같은 경로의 조회를 열어둔 상태라, 메서드 가드가 빠지면 비로그인이 남의 상품을 등록·수정·삭제할 수 있게 된다.
   */
  private static final String[] 보호_대상 = {
    "POST /api/products",
    "PUT /api/products/5",
    "DELETE /api/products/5",
    "PUT /api/me/likes/5",
    "DELETE /api/me/likes/5",
    "GET /api/me/likes",
    "GET /api/me",
    "GET /api/me/products",
  };

  /** 로그인 없이 열려야 하는 요청. */
  private static final String[] 공개_대상 = {
    "GET /api/products",
    "GET /api/products/5",
    "POST /api/members",
    "POST /api/login",
    "POST /api/logout",
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

  /** 인터셉터 체인을 돌린 결과 — 컨트롤러까지 갔는지와, 끊겼다면 클라이언트가 받은 응답. */
  private record Chain(boolean reached, MockHttpServletResponse response) {}

  /**
   * DispatcherServlet이 하는 일을 축약한다 — 경로가 맞는 인터셉터만 골라 preHandle을 순서대로 돌리고, 하나라도 false를 반환하면 요청이 끊긴
   * 것이다.
   */
  private Chain run(String method, String path, MockHttpSession session) throws Exception {
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
        return new Chain(false, response);
      }
    }
    return new Chain(true, response);
  }

  /** 로그인 성공했을때 서버에 남는 상태를 그대로 재현한 것. 실제 로그인 처리가 하는 일임 */
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
                  .assertThat(run(parts[0], parts[1], session).reached())
                  .as("%s — 컨트롤러 도달 여부", each)
                  .isEqualTo(도달해야_하는가);
            } catch (Exception e) {
              softly.fail("%s 검사 중 예외: %s", each, e);
            }
          }
        });
  }

  @Test
  @DisplayName("보호 경로는 비로그인을 401로 끊는다 - 리다이렉트가 아니다")
  void 보호_경로는_비로그인을_401로_막는다() {
    SoftAssertions.assertSoftly(
        softly -> {
          for (String each : 보호_대상) {
            String[] parts = each.split(" ");
            try {
              Chain chain = run(parts[0], parts[1], null);
              softly.assertThat(chain.reached()).as("%s — 컨트롤러 도달 여부", each).isFalse();
              softly.assertThat(chain.response().getStatus()).as("%s — 상태 코드", each).isEqualTo(401);
              // 302로 로그인 페이지에 보내면 클라이언트는 실패를 200 HTML로 받는다.
              softly
                  .assertThat(chain.response().getRedirectedUrl())
                  .as("%s — 리다이렉트가 없어야 한다", each)
                  .isNull();
            } catch (Exception e) {
              softly.fail("%s 검사 중 예외: %s", each, e);
            }
          }
        });
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
  @DisplayName("같은 /api/products라도 조회는 열리고 쓰기는 막힌다")
  void 메서드가_상품_경로를_가른다() throws Exception {
    // 조회를 열려고 excludePathPatterns에 넣은 경로다. 메서드 가드를 지우면 이 경로의 POST·PUT·DELETE가
    // 통째로 무인증으로 열리는데, 목록을 열어보는 것만으로는 전혀 드러나지 않는다.
    assertThat(run("GET", "/api/products", null).reached()).isTrue();
    assertThat(run("POST", "/api/products", null).reached()).isFalse();

    assertThat(run("GET", "/api/products/5", null).reached()).isTrue();
    assertThat(run("PUT", "/api/products/5", null).reached()).isFalse();
    assertThat(run("DELETE", "/api/products/5", null).reached()).isFalse();
  }

  @Test
  @DisplayName("상품 조회를 열어도 찜 API까지 열리지는 않는다")
  void 공개_경로가_찜_API까지_열지_않는다() throws Exception {
    // 패턴을 /api/products/**로 넓히거나 /api/**를 통째로 열면 조용히 함께 열릴 수 있는 부분이다.
    assertThat(run("GET", "/api/products/5", null).reached()).isTrue();
    assertThat(run("GET", "/api/me/likes", null).reached()).isFalse();
  }
}
