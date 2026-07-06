package member;


import org.springframework.stereotype.Repository;

@Repository
public class MemoryMemberRepository implements MemberRepository{



    @Override
    public void save(Member member) {

    }

    @Override
    public void findById(Long id) {

    }

    @Override
    public void findAll() {

    }
}
