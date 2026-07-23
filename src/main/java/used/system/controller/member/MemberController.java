package used.system.controller.member;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import used.system.exception.DuplicateLoginIdException;
import used.system.member.Member;
import used.system.member.MemberService;

@Controller
@RequiredArgsConstructor
public class MemberController {
  private final MemberService memberService;

  @GetMapping("/members/new")
  public String createForm(Model model) {
    model.addAttribute("memberJoinForm", new MemberForm());
    return "member/addform";
  }

  @PostMapping("/members")
  public String create(
      @Validated @ModelAttribute("memberJoinForm") MemberForm memberForm,
      BindingResult bindingResult) {
    if (!memberForm.getPassword().equals(memberForm.getPasswordConfirm())) { // 비밀번호 확인 검증
      bindingResult.rejectValue("passwordConfirm", "passwordMismatch", "비밀번호가 일치하지 않습니다.");
    }

    if (bindingResult.hasErrors()) {
      return "member/addform";
    }
    try {
      Member member =
          new Member(memberForm.getLoginId(), memberForm.getName(), memberForm.getPassword());
      memberService.join(member);
    } catch (DuplicateLoginIdException e) {
      bindingResult.rejectValue("loginId", "duplicate", e.getMessage());
      return "member/addform";
    }

    return "redirect:/";
  }

  @GetMapping("/login")
  public String loginForm(Model model) {
    model.addAttribute("loginForm", new LoginForm());
    return "login/loginForm";
  }

  @PostMapping("/login")
  public String login(
      @Validated @ModelAttribute LoginForm loginForm,
      BindingResult bindingResult,
      HttpServletRequest request) {
    if (bindingResult.hasErrors()) { // 1차: 검증
      return "login/loginForm";
    }
    Member member = memberService.login(loginForm.getLoginId(), loginForm.getPassword());

    if (member == null) { // 2차: 인증실패
      bindingResult.reject("loginFail", "아이디 또는 비밀번호가 맞지 않습니다.");
      return "login/loginForm";
    }

    HttpSession session = request.getSession(); // 세션 작업
    session.setAttribute(SessionConst.LOGIN_MEMBER, member);
    return "redirect:/";
  }

  @PostMapping("/logout")
  public String logout(HttpServletRequest request) {
    HttpSession session = request.getSession(false); // -> false는 있으면 있는거고 없으면 null
    if (session != null) {
      session.invalidate();
    }
    return "redirect:/";
  }
}
