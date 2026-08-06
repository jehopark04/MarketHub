package used.system.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** MemoryProductRepository 단위 테스트 - 실제 인스턴스를 직접 생성해서 저장/조회/삭제 동작을 검증한다. */
class MemoryProductRepositoryTest {

  private MemoryProductRepository repository;

  @BeforeEach
  void setUp() {
    repository = new MemoryProductRepository();
  }

  @Test
  @DisplayName("저장하면 id가 1부터 순차적으로 부여된다")
  void save_assignsSequentialId() {
    Product first = repository.save(new Product("userA", "상품1", "설명입니다", 10000, ProductGrade.A));
    Product second = repository.save(new Product("userA", "상품2", "설명입니다", 20000, ProductGrade.B));

    assertThat(first.getId()).isEqualTo(1L);
    assertThat(second.getId()).isEqualTo(2L);
  }

  @Test
  @DisplayName("findBySellerId는 해당 판매자의 상품만 반환한다")
  void findBySellerId_filtersBySeller() {
    repository.save(new Product("userA", "A상품1", "설명입니다", 10000, ProductGrade.A));
    repository.save(new Product("userA", "A상품2", "설명입니다", 20000, ProductGrade.B));
    repository.save(new Product("userB", "B상품", "설명입니다", 30000, ProductGrade.C));

    List<Product> productsOfA = repository.findBySellerId("userA");

    assertThat(productsOfA).hasSize(2);
    assertThat(productsOfA).allMatch(p -> p.getSellerId().equals("userA"));
  }

  @Test
  @DisplayName("판매자의 상품이 없으면 빈 리스트를 반환한다")
  void findBySellerId_empty() {
    assertThat(repository.findBySellerId("nobody")).isEmpty();
  }

  @Test
  @DisplayName("delete로 삭제하면 이후 조회되지 않는다")
  void delete_removesProduct() {
    Product saved = repository.save(new Product("userA", "상품", "설명입니다", 10000, ProductGrade.A));

    repository.delete(saved.getId());

    assertThat(repository.findById(saved.getId())).isEmpty();
    assertThat(repository.findAll()).isEmpty();
  }
}
