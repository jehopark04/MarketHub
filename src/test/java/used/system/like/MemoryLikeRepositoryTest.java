package used.system.like;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** MemoryLikeRepository 단위 테스트 - 실제 인스턴스를 직접 생성해서 저장/조회/삭제를 검증한다. */
class MemoryLikeRepositoryTest {

  private MemoryLikeRepository repository;

  @BeforeEach
  void setUp() {
    repository = new MemoryLikeRepository();
  }

  @Test
  @DisplayName("저장하면 id가 1부터 순차적으로 부여된다")
  void save_assignsSequentialId() {
    assertThat(repository.save(new Like("userA", 1L)).getId()).isEqualTo(1L);
    assertThat(repository.save(new Like("userA", 2L)).getId()).isEqualTo(2L);
  }

  @Test
  @DisplayName("회원과 상품이 모두 일치해야 찾는다")
  void findByMemberIdAndProductId() {
    repository.save(new Like("userA", 1L));

    assertThat(repository.findByMemberIdAndProductId("userA", 1L)).isPresent();
    assertThat(repository.findByMemberIdAndProductId("userB", 1L)).isEmpty(); // 회원이 다르다
    assertThat(repository.findByMemberIdAndProductId("userA", 2L)).isEmpty(); // 상품이 다르다
  }

  @Test
  @DisplayName("회원의 찜만 찜한 순서대로 반환한다")
  void findByMemberId() {
    repository.save(new Like("userA", 3L));
    repository.save(new Like("userB", 9L)); // 다른 회원 것은 섞이면 안 된다
    repository.save(new Like("userA", 1L));

    assertThat(repository.findByMemberId("userA"))
        .extracting(Like::getProductId)
        .containsExactly(3L, 1L); // 상품 id가 아니라 찜한 순서다
  }

  @Test
  @DisplayName("찜한 적이 없으면 빈 리스트를 반환한다")
  void findByMemberId_empty() {
    assertThat(repository.findByMemberId("nobody")).isEmpty();
  }

  @Test
  @DisplayName("삭제하면 이후 조회되지 않는다")
  void delete() {
    Like saved = repository.save(new Like("userA", 1L));

    repository.delete(saved.getId());

    assertThat(repository.findByMemberIdAndProductId("userA", 1L)).isEmpty();
    assertThat(repository.findByMemberId("userA")).isEmpty();
  }
}
