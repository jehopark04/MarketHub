package used.system.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import used.system.exception.DuplicateLoginIdException;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

  private final MemberRepository memberRepository;

  @Override
  public Member join(Member member) { // 첫 회원가입시 중복 아이디 검증
    memberRepository
        .findByLoginId(member.getLoginId())
        .ifPresent(
            m -> {
              throw new DuplicateLoginIdException("이미 사용중인 아이디입니다."); // 이 예외를 누가 받을 것인가.
            });
    return memberRepository.save(member);
  }

  @Override
  public Member login(String loginId, String password) {
    return memberRepository
        .findByLoginId(loginId)
        .filter(member -> member.getPassword().equals(password))
        .orElse(null);
  }
}
