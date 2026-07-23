package used.system.member;

import java.util.List;

public interface MemberService {
  Member join(Member member);

  Member findMember(Long id);

  List<Member> findMemberAll();

  Member login(String loginId, String password);
}
