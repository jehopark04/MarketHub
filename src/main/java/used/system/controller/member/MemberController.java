package used.system.controller.member;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import used.system.member.Member;
import used.system.member.MemberService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/members/new")
    public String createForm(Model model){
        model.addAttribute("memberJoinForm", new MemberForm());
        return "member/addform";
    }

    @PostMapping("/members")
    public String create(MemberForm memberForm){
        Member member = new Member(memberForm.getLoginId(), memberForm.getName(), memberForm.getPassword());
        memberService.join(member);
        return "redirect:/";
    }

    @GetMapping("/login")
    public String loginForm(Model model){
        model.addAttribute("loginForm", new LoginForm());
        return "login/loginForm";
    }

    @PostMapping("/login")
    public String login(LoginForm loginForm){
        Member member = memberService.login(loginForm.getLoginId(), loginForm.getPassword());
        return "redirect:/";

    }

}
