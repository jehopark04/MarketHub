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
  @DisplayName("id로 저장된 상품을 조회한다")
  void findById() {
    Product saved = repository.save(new Product("userA", "상품", "설명입니다", 10000, ProductGrade.A));

    assertThat(repository.findById(saved.getId())).containsSame(saved);
  }

  @Test
  @DisplayName("없는 id로 조회하면 빈 Optional을 반환한다")
  void findById_notFound() {
    assertThat(repository.findById(999L)).isEmpty();
  }

  @Test
  @DisplayName("findAll은 판매자와 무관하게 저장된 모든 상품을 반환한다")
  void findAll() {
    repository.save(new Product("userA", "상품1", "설명입니다", 10000, ProductGrade.A));
    repository.save(new Product("userB", "상품2", "설명입니다", 20000, ProductGrade.B));

    assertThat(repository.findAll())
        .extracting(Product::getTitle)
        .containsExactlyInAnyOrder("상품1", "상품2");
  }

  @Test
  @DisplayName("저장된 상품이 없으면 findAll은 빈 리스트를 반환한다")
  void findAll_empty() {
    assertThat(repository.findAll()).isEmpty();
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

  // ---------- 검색 / 필터 ----------

  /** 검색 테스트용 고정 데이터. 제목·가격·등급이 서로 달라 조건별로 걸러지는 대상이 갈린다. */
  private void 검색용_상품_저장() {
    repository.save(new Product("userA", "맥북 프로 16", "설명입니다", 2_000_000, ProductGrade.S));
    repository.save(new Product("userA", "맥북 에어", "설명입니다", 1_000_000, ProductGrade.A));
    repository.save(new Product("userB", "아이패드", "설명입니다", 500_000, ProductGrade.B));
  }

  private ProductSearchCond cond(String keyword, Integer min, Integer max, ProductGrade grade) {
    ProductSearchCond cond = new ProductSearchCond();
    cond.setKeyword(keyword);
    cond.setMinPrice(min);
    cond.setMaxPrice(max);
    cond.setGrade(grade);
    return cond;
  }

  @Test
  @DisplayName("조건이 전부 비어 있으면 전체 목록과 같다")
  void search_noCondition() {
    검색용_상품_저장();

    assertThat(repository.search(cond(null, null, null, null)))
        .containsExactlyInAnyOrderElementsOf(repository.findAll());
  }

  @Test
  @DisplayName("키워드는 제목에 포함된 상품만 남긴다")
  void search_byKeyword() {
    검색용_상품_저장();

    assertThat(repository.search(cond("맥북", null, null, null)))
        .extracting(Product::getTitle)
        .containsExactlyInAnyOrder("맥북 프로 16", "맥북 에어");
  }

  @Test
  @DisplayName("키워드가 빈 문자열이면 조건 없음으로 취급한다")
  void search_blankKeywordIsNoCondition() {
    // 검색창을 비우고 제출하면 null이 아니라 ""로 들어온다. 이걸 걸러내지 않으면 결과가 0건이 된다.
    검색용_상품_저장();

    assertThat(repository.search(cond("", null, null, null))).hasSize(3);
  }

  @Test
  @DisplayName("키워드는 대소문자를 가리지 않는다")
  void search_keywordIgnoresCase() {
    repository.save(new Product("userA", "MacBook Air", "설명입니다", 1_000_000, ProductGrade.A));

    assertThat(repository.search(cond("macbook", null, null, null))).hasSize(1);
  }

  @Test
  @DisplayName("최소 가격은 그 값 이상만 남긴다 (경계 포함)")
  void search_byMinPrice() {
    검색용_상품_저장();

    assertThat(repository.search(cond(null, 1_000_000, null, null)))
        .extracting(Product::getPrice)
        .containsExactlyInAnyOrder(2_000_000, 1_000_000);
  }

  @Test
  @DisplayName("최대 가격은 그 값 이하만 남긴다 (경계 포함)")
  void search_byMaxPrice() {
    검색용_상품_저장();

    assertThat(repository.search(cond(null, null, 1_000_000, null)))
        .extracting(Product::getPrice)
        .containsExactlyInAnyOrder(1_000_000, 500_000);
  }

  @Test
  @DisplayName("등급은 정확히 일치하는 상품만 남긴다")
  void search_byGrade() {
    검색용_상품_저장();

    assertThat(repository.search(cond(null, null, null, ProductGrade.S)))
        .extracting(Product::getTitle)
        .containsExactly("맥북 프로 16");
  }

  @Test
  @DisplayName("조건을 여러 개 주면 모두 만족하는 상품만 남는다")
  void search_combinedConditions() {
    검색용_상품_저장();

    // "맥북"이면서 150만원 이하 → 맥북 에어만
    assertThat(repository.search(cond("맥북", null, 1_500_000, null)))
        .extracting(Product::getTitle)
        .containsExactly("맥북 에어");
  }

  @Test
  @DisplayName("조건에 맞는 상품이 없으면 빈 리스트를 반환한다")
  void search_noMatch() {
    검색용_상품_저장();

    assertThat(repository.search(cond("존재하지않는상품", null, null, null))).isEmpty();
  }
}
