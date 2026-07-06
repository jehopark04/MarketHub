package controller.member;


import lombok.RequiredArgsConstructor;
import member.Member;
import member.MemberRepository;
import member.MemberService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/members/new")
    public String createForm(){
        return "member/addform";
    }

    @PostMapping("/members")
    public String create(MemberForm memberForm){
        Member member = new Member(memberForm.getLoginId(), memberForm.getName(), memberForm.getPassword());
        memberService.join(member);
        return "redirect:/";

    }

}
