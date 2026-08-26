package used.system.controller.product;

import used.system.product.Product;
import used.system.product.ProductGrade;

/**
 * 상품을 표로 나열하는 화면들이 공통으로 쓰는 응답. 내가 등록한 상품, 내가 찜한 상품이 같은 필드를 쓴다.
 *
 * <p>상품 목록(ProductListResponse)만 따로 두는 이유는 liked가 있어서다. 나머지 둘은 하트가 없는 화면이라 그 필드가 늘 무의미하다.
 *
 * <p>sellerId와 description은 담지 않는다. 표에 없는 값이고, 상세를 열면 나온다.
 */
public record ProductSummaryResponse(Long productId, String title, int price, ProductGrade grade) {

  public static ProductSummaryResponse from(Product product) {
    return new ProductSummaryResponse(
        product.getId(), product.getTitle(), product.getPrice(), product.getGrade());
  }
}
