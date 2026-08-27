package used.system.controller.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import used.system.member.Member;

/**
 * 회원가입 요청 본문.
 *
 * <p>비밀번호 확인(passwordConfirm)을 받지 않는 것은 의도다. 오타 방지용 UX 장치라 서버가 지킬 무결성이 아니고 - 우회해도 본인이 오타 난 비밀번호를 쓰게
 * 될 뿐이다 - 평문 비밀번호를 두 번 실어 보내면 로그에 남을 표면만 늘어난다. 입력칸 두 개를 비교하는 일은 화면을 붙일 때 그쪽에서 한다.
 *
 * <p>중복 아이디는 여기서 걸러지지 않는다. 저장소를 봐야 알 수 있는 것이라 서비스의 몫이고, DuplicateLoginIdException이 409로 나간다.
 */
public record MemberJoinRequest(
    @NotBlank(message = "아이디는 필수입니다.") String loginId,
    @NotBlank(message = "이름은 필수 입니다.") String name,
    @NotBlank(message = "비밀번호는 필수입니다.") @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password) {

  Member toMember() {
    return new Member(loginId, name, password);
  }
}
