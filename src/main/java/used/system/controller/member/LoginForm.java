package used.system.controller.member;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginForm {

    @NotBlank(message = "아이디를 입력하세요.")
    private String loginId;

    @NotBlank(message = "비밀번호를 입력하세요.")
    private String password;


    public LoginForm(){

    }

    public LoginForm(String loginId, String password) {
        this.loginId = loginId;
        this.password = password;
    }


}
