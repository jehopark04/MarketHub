package used.system.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import used.system.exception.ForbiddenException;
import used.system.exception.ProductNotFoundException;

/**
 * ProductServiceImpl 단위 테스트 - 저장소는 Mockito mock으로 주입하고, 존재 검증(404)/소유권 검증(403) 같은 비즈니스 규칙이 지켜지는지에
 * 집중한다.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

  @Mock private ProductRepository productRepository;

  @InjectMocks private ProductServiceImpl productService;

  private Product ownedProduct() {
    return new Product("userA", "제목", "설명입니다", 10000, ProductGrade.A);
  }

  @Test
  @DisplayName("존재하지 않는 상품을 조회하면 ProductNotFoundException이 발생한다")
  void findById_notFound() {
    given(productRepository.findById(999L)).willReturn(null);

    assertThatThrownBy(() -> productService.findById(999L))
        .isInstanceOf(ProductNotFoundException.class);
  }

  @Test
  @DisplayName("본인 상품을 수정하면 값이 변경된다")
  void editProduct_owner() {
    Product product = ownedProduct();
    given(productRepository.findById(1L)).willReturn(product);
    ProductUpdateDto dto = new ProductUpdateDto("새제목", "새설명입니다", 20000, ProductGrade.S);

    productService.editProduct(1L, "userA", dto);

    assertThat(product.getTitle()).isEqualTo("새제목");
    assertThat(product.getPrice()).isEqualTo(20000);
    assertThat(product.getGrade()).isEqualTo(ProductGrade.S);
  }

  @Test
  @DisplayName("타인의 상품을 수정하려 하면 ForbiddenException이 발생하고 값은 그대로다")
  void editProduct_notOwner() {
    Product product = ownedProduct();
    given(productRepository.findById(1L)).willReturn(product);
    ProductUpdateDto dto = new ProductUpdateDto("탈취제목", "탈취설명입니다", 1, ProductGrade.C);

    assertThatThrownBy(() -> productService.editProduct(1L, "userB", dto))
        .isInstanceOf(ForbiddenException.class);

    assertThat(product.getTitle()).isEqualTo("제목");
  }

  @Test
  @DisplayName("본인 상품을 삭제하면 저장소의 delete가 호출된다")
  void deleteProduct_owner() {
    given(productRepository.findById(1L)).willReturn(ownedProduct());

    productService.deleteProduct(1L, "userA");

    verify(productRepository).delete(1L);
  }

  @Test
  @DisplayName("타인의 상품을 삭제하려 하면 ForbiddenException이 발생하고 삭제되지 않는다")
  void deleteProduct_notOwner() {
    given(productRepository.findById(1L)).willReturn(ownedProduct());

    assertThatThrownBy(() -> productService.deleteProduct(1L, "userB"))
        .isInstanceOf(ForbiddenException.class);

    verify(productRepository, never()).delete(1L);
  }

  @Test
  @DisplayName("없는 상품을 삭제하려 하면 ProductNotFoundException이 발생한다")
  void deleteProduct_notFound() {
    given(productRepository.findById(999L)).willReturn(null);

    assertThatThrownBy(() -> productService.deleteProduct(999L, "userA"))
        .isInstanceOf(ProductNotFoundException.class);

    verify(productRepository, never()).delete(999L);
  }

  @Test
  @DisplayName("findByIdAndOwner는 본인 상품이면 상품을 반환한다")
  void findByIdAndOwner_owner() {
    Product product = ownedProduct();
    given(productRepository.findById(1L)).willReturn(product);

    assertThat(productService.findByIdAndOwner(1L, "userA")).isSameAs(product);
  }
}
