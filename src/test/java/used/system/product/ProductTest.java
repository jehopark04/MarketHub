package used.system.product;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Product 도메인 단위 테스트 - 생성/수정 시 상태가 규칙대로 바뀌는지 검증한다. (의존성 없는 순수 객체 테스트) */
class ProductTest {

  @Test
  @DisplayName("생성 시 등록 시각과 수정 시각이 동일하게 초기화된다")
  void constructor_initializesTimestamps() {
    Product product = new Product("userA", "제목", "설명입니다", 10000, ProductGrade.A);

    assertThat(product.getCreateAt()).isNotNull();
    assertThat(product.getUpdatedAt()).isEqualTo(product.getCreateAt());
  }

  @Test
  @DisplayName("update는 전달받은 값으로 필드를 덮어쓴다")
  void update_overwritesFields() {
    Product product = new Product("userA", "원래제목", "원래설명", 10000, ProductGrade.A);

    product.update("새제목", "새설명", 20000, ProductGrade.S);

    assertThat(product.getTitle()).isEqualTo("새제목");
    assertThat(product.getDescription()).isEqualTo("새설명");
    assertThat(product.getPrice()).isEqualTo(20000);
    assertThat(product.getGrade()).isEqualTo(ProductGrade.S);
  }

  @Test
  @DisplayName("update는 판매자와 등록 시각은 바꾸지 않는다")
  void update_keepsSellerAndCreateAt() {
    Product product = new Product("userA", "원래제목", "원래설명", 10000, ProductGrade.A);
    var createdAt = product.getCreateAt();

    product.update("새제목", "새설명", 20000, ProductGrade.S);

    assertThat(product.getSellerId()).isEqualTo("userA");
    assertThat(product.getCreateAt()).isEqualTo(createdAt);
  }

  @Test
  @DisplayName("update 후 수정 시각은 등록 시각 이후이거나 같다")
  void update_refreshesUpdatedAt() {
    Product product = new Product("userA", "원래제목", "원래설명", 10000, ProductGrade.A);

    product.update("새제목", "새설명", 20000, ProductGrade.S);

    assertThat(product.getUpdatedAt()).isAfterOrEqualTo(product.getCreateAt());
  }
}
