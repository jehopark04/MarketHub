package used.system.controller.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class MemberForm {

    @NotBlank(message = "아이디는 필수입니다.") // notBlank는 문자열 기반임
    private String loginId;

    @NotBlank(message = "이름은 필수 입니다.")
    private String name;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인 필수입니다.")
    private String passwordConfirm;


    public MemberForm() {

    }

    public MemberForm(String loginId, String name, String password) {
        this.loginId = loginId;
        this.name = name;
        this.password = password;

    }

}


