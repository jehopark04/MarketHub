package used.system.controller.myPage;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import used.system.controller.member.SessionConst;
import used.system.member.Member;

@Controller
public class MyPageController {

    @GetMapping("/my-page")
    public String myPageForm(Model model, @SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false)Member member){
        if (member == null){
            return "redirect:/login";
        }
        model.addAttribute("member", member);
        return "member/myPage";
    }
}
