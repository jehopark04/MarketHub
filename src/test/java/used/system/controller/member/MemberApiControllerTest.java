package used.system.controller.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import used.system.exception.DuplicateLoginIdException;
import used.system.exception.LoginFailedException;
import used.system.member.Member;
import used.system.member.MemberService;

/**
 * MemberApiController 단위 테스트 - 컨트롤러 메서드를 직접 호출한다.
 *
 * <p>입력 검증은 MemberJoinRequestValidationTest가 본다. 컨트롤러를 직접 부르면 @Valid가 돌지 않아 여기서는 확인할 수 없다.
 *
 * <p>여기서 볼 것은 셋이다. 요청을 Member로 옮기는가, 201을 주는가, 응답에 비밀번호가 섞이지 않는가.
 */
@ExtendWith(MockitoExtension.class)
class MemberApiControllerTest {

  @Mock private MemberService memberService;

  @InjectMocks private MemberApiController memberApiController;

  private final MemberJoinRequest request =
      new MemberJoinRequest("userA", "에이", "password1", "password1");

  @Test
  @DisplayName("요청을 Member로 옮겨 가입시키고 201을 반환한다")
  void join_delegates() {
    given(memberService.join(any(Member.class))).willReturn(new Member("userA", "에이", "password1"));

    ResponseEntity<MemberResponse> response = memberApiController.join(request);

    ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
    verify(memberService).join(captor.capture());
    assertThat(captor.getValue().getLoginId()).isEqualTo("userA");
    assertThat(captor.getValue().getName()).isEqualTo("에이");
    assertThat(captor.getValue().getPassword()).isEqualTo("password1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  @Test
  @DisplayName("가입 응답에 비밀번호가 담기지 않는다")
  void join_hidesPassword() {
    given(memberService.join(any(Member.class))).willReturn(new Member("userA", "에이", "password1"));

    // MemberResponse에 password 필드를 더하면 이 비교가 컴파일되지 않는다.
    assertThat(memberApiController.join(request).getBody())
        .isEqualTo(new MemberResponse("userA", "에이"));
  }

  @Test
  @DisplayName("중복 아이디면 서비스의 예외를 그대로 통과시킨다")
  void join_propagatesDuplicate() {
    // 컨트롤러가 try-catch로 삼키면 ApiExceptionHandler에 닿지 않아 409가 나가지 않는다.
    given(memberService.join(any(Member.class)))
        .willThrow(new DuplicateLoginIdException("이미 사용중인 아이디입니다."));

    assertThatThrownBy(() -> memberApiController.join(request))
        .isInstanceOf(DuplicateLoginIdException.class);
  }

  // ---------- 로그인 ----------

  private final LoginRequest loginRequest = new LoginRequest("userA", "password1");

  @Test
  @DisplayName("로그인에 성공하면 세션에 회원을 담고 회원 정보를 반환한다")
  void login_storesSession() {
    Member member = new Member("userA", "에이", "password1");
    given(memberService.login("userA", "password1")).willReturn(member);
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();

    MemberResponse response = memberApiController.login(loginRequest, servletRequest);

    // 세션 키를 하드코딩하면 인터셉터가 읽는 키와 갈라져 로그인해도 계속 401이 난다.
    assertThat(servletRequest.getSession(false).getAttribute(SessionConst.LOGIN_MEMBER))
        .isSameAs(member);
    assertThat(response).isEqualTo(new MemberResponse("userA", "에이"));
  }

  @Test
  @DisplayName("로그인 전 세션은 버리고 새 세션을 발급한다")
  void login_rotatesSession() {
    // 남이 심어둔 세션 id를 그대로 승격시키면 심은 쪽이 함께 들어온다(세션 고정).
    Member member = new Member("userA", "에이", "password1");
    given(memberService.login("userA", "password1")).willReturn(member);

    MockHttpSession before = new MockHttpSession();
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    servletRequest.setSession(before);

    memberApiController.login(loginRequest, servletRequest);

    assertThat(servletRequest.getSession(false)).isNotSameAs(before);
    assertThat(before.isInvalid()).isTrue();
  }

  @Test
  @DisplayName("아이디나 비밀번호가 틀리면 예외를 던지고 세션을 만들지 않는다")
  void login_failure() {
    given(memberService.login("userA", "password1")).willReturn(null);
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();

    assertThatThrownBy(() -> memberApiController.login(loginRequest, servletRequest))
        .isInstanceOf(LoginFailedException.class);

    // 실패했는데 세션이 생기면 비로그인 방문자마다 빈 세션이 쌓인다.
    assertThat(servletRequest.getSession(false)).isNull();
  }

  // ---------- 로그아웃 ----------

  @Test
  @DisplayName("로그아웃하면 세션을 무효화하고 204를 반환한다")
  void logout_invalidates() {
    MockHttpSession session = new MockHttpSession();
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    servletRequest.setSession(session);

    ResponseEntity<Void> response = memberApiController.logout(servletRequest);

    assertThat(session.isInvalid()).isTrue();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  @DisplayName("세션이 없어도 로그아웃은 204다")
  void logout_withoutSession() {
    // 같은 요청을 두 번 보내도 결과가 같아야 한다. 없는 세션을 지우는 것은 실패가 아니다.
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();

    assertThat(memberApiController.logout(servletRequest).getStatusCode())
        .isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(servletRequest.getSession(false)).isNull();
  }
}
