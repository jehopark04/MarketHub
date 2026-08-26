package used.system.member;

public interface MemberService {
  Member join(Member member);

  Member login(String loginId, String password);
}
