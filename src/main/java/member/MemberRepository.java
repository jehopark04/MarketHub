package member;

public interface MemberRepository {

    void save(Member member);
    void findById(Long id);
    void findAll();
}
