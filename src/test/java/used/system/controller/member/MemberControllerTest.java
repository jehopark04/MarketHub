package used.system.controller.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import used.system.exception.DuplicateLoginIdException;
import used.system.member.Member;
import used.system.member.MemberService;

/**
 * MemberController 단위 테스트 - 스프링 컨텍스트/MockMvc 없이 컨트롤러 메서드를 직접 호출한다. BindingResult는
 * BeanPropertyBindingResult로, 세션은 MockHttpServletRequest로 직접 만들어 넘긴다. (둘 다 컨텍스트를 띄우지 않는 단순 구현체다)
 *
 * <p>주의: 직접 호출 방식이라 URL 매핑·@Validated 실행·폼 바인딩은 검증 범위 밖이다. 여기서 검증하는 것은 "분기가 규칙대로 흐르는가"와 "모델/세션에 무엇을
 * 담는가"이다.
 */
@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

  @Mock private MemberService memberService;

  @InjectMocks private MemberController memberController;

  private MemberForm joinForm(String loginId, String password, String passwordConfirm) {
    MemberForm form = new MemberForm();
    form.setLoginId(loginId);
    form.setName("에이");
    form.setPassword(password);
    form.setPasswordConfirm(passwordConfirm);
    return form;
  }

  private BindingResult bindingResultFor(Object target, String objectName) {
    return new BeanPropertyBindingResult(target, objectName);
  }

  // ---------- 회원가입 ----------

  @Test
  @DisplayName("회원가입 폼을 열면 값이 비어 있는 폼 객체를 모델에 담는다")
  void createForm_putsEmptyForm() {
    Model model = new ConcurrentModel();

    String view = memberController.createForm(model);

    assertThat(view).isEqualTo("member/addform");
    assertThat(model.getAttribute("memberJoinForm"))
        .isInstanceOf(MemberForm.class)
        .satisfies(
            form -> {
              MemberForm memberForm = (MemberForm) form;
              assertThat(memberForm.getLoginId()).isNull();
              assertThat(memberForm.getName()).isNull();
              assertThat(memberForm.getPassword()).isNull();
              assertThat(memberForm.getPasswordConfirm()).isNull();
            });
  }

  @Test
  @DisplayName("정상 가입이면 회원을 저장하고 홈으로 리다이렉트한다")
  void create_success() {
    MemberForm form = joinForm("userA", "password1", "password1");
    BindingResult bindingResult = bindingResultFor(form, "memberJoinForm");

    String view = memberController.create(form, bindingResult);

    assertThat(view).isEqualTo("redirect:/");

    ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
    verify(memberService).join(captor.capture());
    Member saved = captor.getValue();
    assertThat(saved.getLoginId()).isEqualTo("userA");
    assertThat(saved.getName()).isEqualTo("에이");
    assertThat(saved.getPassword()).isEqualTo("password1");
  }

  @Test
  @DisplayName("password 파라미터가 누락되어 null이어도 NPE 없이 폼을 다시 보여준다")
  void create_nullPassword() {
    // 폼에서 password 입력칸이 통째로 빠진 요청. 빈 문자열("")이 아니라 null로 바인딩된다.
    MemberForm form = joinForm("userA", null, null);
    BindingResult bindingResult = bindingResultFor(form, "memberJoinForm");
    bindingResult.rejectValue("password", "NotBlank"); // @Validated가 걸었을 에러

    String view = memberController.create(form, bindingResult);

    assertThat(view).isEqualTo("member/addform");
    verify(memberService, never()).join(any());
  }

  @Test
  @DisplayName("비밀번호와 비밀번호 확인이 다르면 passwordConfirm 필드 에러로 폼을 다시 보여준다")
  void create_passwordMismatch() {
    MemberForm form = joinForm("userA", "password1", "password2");
    BindingResult bindingResult = bindingResultFor(form, "memberJoinForm");

    String view = memberController.create(form, bindingResult);

    assertThat(view).isEqualTo("member/addform");
    assertThat(bindingResult.getFieldError("passwordConfirm")).isNotNull();
    verify(memberService, never()).join(any());
  }

  @Test
  @DisplayName("이미 검증 에러가 있으면 저장하지 않고 폼을 다시 보여준다")
  void create_validationError() {
    MemberForm form = joinForm("", "password1", "password1");
    BindingResult bindingResult = bindingResultFor(form, "memberJoinForm");
    bindingResult.rejectValue("loginId", "NotBlank"); // @Validated가 걸었을 에러를 흉내낸다

    String view = memberController.create(form, bindingResult);

    assertThat(view).isEqualTo("member/addform");
    verify(memberService, never()).join(any());
  }

  @Test
  @DisplayName("아이디가 중복이면 서비스 예외를 loginId 필드 에러로 바꿔 폼을 다시 보여준다")
  void create_duplicateLoginId() {
    MemberForm form = joinForm("userA", "password1", "password1");
    BindingResult bindingResult = bindingResultFor(form, "memberJoinForm");
    willThrow(new DuplicateLoginIdException("이미 사용중인 아이디입니다."))
        .given(memberService)
        .join(any(Member.class));

    String view = memberController.create(form, bindingResult);

    assertThat(view).isEqualTo("member/addform");
    assertThat(bindingResult.getFieldError("loginId")).isNotNull();
    assertThat(bindingResult.getFieldError("loginId").getDefaultMessage())
        .isEqualTo("이미 사용중인 아이디입니다.");
  }

  // ---------- 로그인 ----------

  @Test
  @DisplayName("로그인 폼을 열면 값이 비어 있는 폼 객체를 모델에 담는다")
  void loginForm_putsEmptyForm() {
    Model model = new ConcurrentModel();

    String view = memberController.loginForm(model);

    assertThat(view).isEqualTo("login/loginForm");
    assertThat(model.getAttribute("loginForm"))
        .isInstanceOf(LoginForm.class)
        .satisfies(
            form -> {
              LoginForm loginForm = (LoginForm) form;
              assertThat(loginForm.getLoginId()).isNull();
              assertThat(loginForm.getPassword()).isNull();
            });
  }

  @Test
  @DisplayName("로그인에 성공하면 세션에 회원을 담고 홈으로 리다이렉트한다")
  void login_success() {
    LoginForm form = new LoginForm("userA", "password1");
    BindingResult bindingResult = bindingResultFor(form, "loginForm");
    MockHttpServletRequest request = new MockHttpServletRequest();
    Member member = new Member("userA", "에이", "password1");
    given(memberService.login("userA", "password1")).willReturn(member);

    String view = memberController.login(form, bindingResult, request);

    assertThat(view).isEqualTo("redirect:/");
    assertThat(request.getSession(false)).isNotNull();
    assertThat(request.getSession(false).getAttribute(SessionConst.LOGIN_MEMBER)).isSameAs(member);
  }

  @Test
  @DisplayName("아이디나 비밀번호가 틀리면 글로벌 에러를 담고 세션을 만들지 않는다")
  void login_authenticationFail() {
    LoginForm form = new LoginForm("userA", "wrong");
    BindingResult bindingResult = bindingResultFor(form, "loginForm");
    MockHttpServletRequest request = new MockHttpServletRequest();
    given(memberService.login("userA", "wrong")).willReturn(null);

    String view = memberController.login(form, bindingResult, request);

    assertThat(view).isEqualTo("login/loginForm");
    assertThat(bindingResult.getGlobalErrorCount()).isEqualTo(1);
    assertThat(request.getSession(false)).isNull();
  }

  @Test
  @DisplayName("입력 검증 에러가 있으면 로그인 시도조차 하지 않는다")
  void login_validationError() {
    LoginForm form = new LoginForm("", "");
    BindingResult bindingResult = bindingResultFor(form, "loginForm");
    bindingResult.rejectValue("loginId", "NotBlank");
    MockHttpServletRequest request = new MockHttpServletRequest();

    String view = memberController.login(form, bindingResult, request);

    assertThat(view).isEqualTo("login/loginForm");
    verify(memberService, never()).login(any(), any());
    assertThat(request.getSession(false)).isNull();
  }

  // ---------- 로그아웃 ----------

  @Test
  @DisplayName("로그아웃하면 기존 세션이 무효화된다")
  void logout_invalidatesSession() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(SessionConst.LOGIN_MEMBER, new Member("userA", "에이", "password1"));
    request.setSession(session);

    String view = memberController.logout(request);

    assertThat(view).isEqualTo("redirect:/");
    assertThat(session.isInvalid()).isTrue();
  }

  @Test
  @DisplayName("세션이 없는 상태로 로그아웃해도 예외 없이 홈으로 리다이렉트한다")
  void logout_withoutSession() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    String view = memberController.logout(request);

    assertThat(view).isEqualTo("redirect:/");
    assertThat(request.getSession(false)).isNull(); // 없는 세션을 새로 만들지 않는다
  }
}
