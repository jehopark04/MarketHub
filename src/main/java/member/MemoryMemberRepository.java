package member;


import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class MemoryMemberRepository implements MemberRepository{

    private final Map<Long, Member> memberMap = new HashMap<>();
    private Long sequence = 0L; // 짜피 싱글톤이라 스태틱 안해도 된다.

    @Override
    public Member save(Member member) {
        member.setId(++sequence);
        memberMap.put(member.getId(), member);
        return member;
    }

    @Override
    public Member findById(Long id) {
        Member member = memberMap.get(id);
        return member;
    }

    @Override
    public List<Member> findAll() {
        return new ArrayList<>(memberMap.values());
    }
}
