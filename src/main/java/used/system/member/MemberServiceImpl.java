package used.system.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService{

    private final MemberRepository memberRepository;


    @Override
    public Member join(Member member) {
        return memberRepository.save(member);
    }

    @Override
    public Member findMember(Long id) {
        return memberRepository.findById(id);
    }

    @Override
    public List<Member> findMemberAll() {
        return memberRepository.findAll();
    }

//    @Override
//    public Member login(String loginId, String password) {
//        Optional<Member> optionalMember = memberRepository.findByLoginId(loginId);
//        if (optionalMember.isEmpty()){
//            return null;
//        }
//        Member member = optionalMember.get();
//        if(!member.getPassword().equals(password)){
//            return null;
//        }
//        return member;
//    }


    @Override
    public  Member login(String loginId, String password){
        return memberRepository.findByLoginId(loginId)
                .filter(member -> member.getPassword().equals(password)).orElse(null);

    }
}
