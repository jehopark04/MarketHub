package used.system.member;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** MemoryMemberRepository 단위 테스트 - 실제 인스턴스를 직접 생성해서 저장/조회 동작을 검증한다. */
class MemoryMemberRepositoryTest {

  private MemoryMemberRepository repository;

  @BeforeEach
  void setUp() {
    // 테스트마다 새 인스턴스를 만들어 sequence/저장소가 격리되도록 한다.
    repository = new MemoryMemberRepository();
  }

  @Test
  @DisplayName("저장하면 id가 1부터 순차적으로 부여된다")
  void save_assignsSequentialId() {
    Member first = repository.save(new Member("userA", "에이", "password1"));
    Member second = repository.save(new Member("userB", "비", "password2"));

    assertThat(first.getId()).isEqualTo(1L);
    assertThat(second.getId()).isEqualTo(2L);
  }

  @Test
  @DisplayName("id로 저장된 회원을 조회한다")
  void findById() {
    Member saved = repository.save(new Member("userA", "에이", "password1"));

    Optional<Member> found = repository.findById(saved.getId());

    assertThat(found).containsSame(saved);
  }

  @Test
  @DisplayName("없는 id로 조회하면 빈 Optional을 반환한다")
  void findById_notFound() {
    assertThat(repository.findById(999L)).isEmpty();
  }

  @Test
  @DisplayName("loginId로 회원을 찾으면 Optional에 담겨 반환된다")
  void findByLoginId_present() {
    repository.save(new Member("userA", "에이", "password1"));

    Optional<Member> found = repository.findByLoginId("userA");

    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("에이");
  }

  @Test
  @DisplayName("없는 loginId로 찾으면 빈 Optional을 반환한다")
  void findByLoginId_empty() {
    Optional<Member> found = repository.findByLoginId("nobody");

    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("findAll은 저장된 모든 회원을 반환한다")
  void findAll() {
    repository.save(new Member("userA", "에이", "password1"));
    repository.save(new Member("userB", "비", "password2"));

    assertThat(repository.findAll())
        .extracting(Member::getLoginId)
        .containsExactlyInAnyOrder("userA", "userB");
  }

  @Test
  @DisplayName("저장된 회원이 없으면 findAll은 빈 리스트를 반환한다")
  void findAll_empty() {
    assertThat(repository.findAll()).isEmpty();
  }
}
