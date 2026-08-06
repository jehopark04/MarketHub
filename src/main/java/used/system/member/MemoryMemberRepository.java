package used.system.member;

import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryMemberRepository implements MemberRepository {

  private final Map<Long, Member> memberMap = new HashMap<>();
  private Long sequence = 0L; // 짜피 싱글톤이라 스태틱 안해도 된다.

  @Override
  public Member save(Member member) {
    member.setId(++sequence);
    memberMap.put(member.getId(), member);
    return member;
  }

  @Override
  public Optional<Member> findById(Long id) {
    return Optional.ofNullable(memberMap.get(id));
  }

  @Override
  public List<Member> findAll() {
    return new ArrayList<>(memberMap.values());
  }

  @Override
  public Optional<Member> findByLoginId(String loginId) {
    return memberMap.values().stream()
        .filter(member -> member.getLoginId().equals(loginId))
        .findFirst();
  }
}
