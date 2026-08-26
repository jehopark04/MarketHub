package used.system.controller.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import used.system.member.Member;

/**
 * 회원가입 요청 본문.
 *
 * <p>중복 아이디는 여기서 걸러지지 않는다. 저장소를 봐야 알 수 있는 것이라 서비스의 몫이고, DuplicateLoginIdException이 409로 나간다.
 */
@PasswordMatch
public record MemberJoinRequest(
    @NotBlank(message = "아이디는 필수입니다.") String loginId,
    @NotBlank(message = "이름은 필수 입니다.") String name,
    @NotBlank(message = "비밀번호는 필수입니다.") @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password,
    @NotBlank(message = "비밀번호 확인 필수입니다.") String passwordConfirm) {

  Member toMember() {
    return new Member(loginId, name, password);
  }
}
