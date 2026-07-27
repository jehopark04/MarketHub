package used.system.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import used.system.exception.DuplicateLoginIdException;

/**
 * MemberServiceImpl 단위 테스트 - 저장소는 Mockito로 가짜(mock)를 주입해 서비스의 비즈니스 로직만 격리해서 검증한다. (실제 저장 여부가 아니라 "규칙이
 * 지켜지는가"가 관심사)
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

  @Mock private MemberRepository memberRepository;

  @InjectMocks private MemberServiceImpl memberService;

  @Test
  @DisplayName("중복되지 않은 아이디로 가입하면 저장소에 저장된다")
  void join_success() {
    Member member = new Member("userA", "에이", "password1");
    given(memberRepository.findByLoginId("userA")).willReturn(Optional.empty());
    given(memberRepository.save(member)).willReturn(member);

    Member joined = memberService.join(member);

    assertThat(joined).isSameAs(member);
    verify(memberRepository).save(member);
  }

  @Test
  @DisplayName("이미 존재하는 아이디로 가입하면 예외가 발생하고 저장되지 않는다")
  void join_duplicateLoginId() {
    Member existing = new Member("userA", "기존", "password1");
    Member newMember = new Member("userA", "신규", "password2");
    given(memberRepository.findByLoginId("userA")).willReturn(Optional.of(existing));

    assertThatThrownBy(() -> memberService.join(newMember))
        .isInstanceOf(DuplicateLoginIdException.class);

    verify(memberRepository, never()).save(newMember);
  }

  @Test
  @DisplayName("아이디와 비밀번호가 일치하면 로그인에 성공한다")
  void login_success() {
    Member member = new Member("userA", "에이", "password1");
    given(memberRepository.findByLoginId("userA")).willReturn(Optional.of(member));

    Member result = memberService.login("userA", "password1");

    assertThat(result).isSameAs(member);
  }

  @Test
  @DisplayName("비밀번호가 틀리면 로그인 결과는 null이다")
  void login_wrongPassword() {
    Member member = new Member("userA", "에이", "password1");
    given(memberRepository.findByLoginId("userA")).willReturn(Optional.of(member));

    Member result = memberService.login("userA", "wrong");

    assertThat(result).isNull();
  }

  @Test
  @DisplayName("존재하지 않는 아이디로 로그인하면 결과는 null이다")
  void login_noSuchMember() {
    given(memberRepository.findByLoginId("nobody")).willReturn(Optional.empty());

    Member result = memberService.login("nobody", "password1");

    assertThat(result).isNull();
  }

  @Test
  @DisplayName("findMember는 저장소 조회를 그대로 위임한다")
  void findMember_delegates() {
    Member member = new Member("userA", "에이", "password1");
    given(memberRepository.findById(1L)).willReturn(member);

    assertThat(memberService.findMember(1L)).isSameAs(member);
  }
}
