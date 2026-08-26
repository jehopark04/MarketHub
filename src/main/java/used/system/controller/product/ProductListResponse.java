package used.system.controller.product;

import used.system.like.ProductLikeStatus;
import used.system.product.Product;
import used.system.product.ProductGrade;

/** 상품 목록 조회 응답. 목록 화면이 쓰는 필드만 담는다 - 설명은 상세에서 본다. */
public record ProductListResponse(
    Long productId, String title, int price, ProductGrade grade, boolean liked) {

  static ProductListResponse from(ProductLikeStatus status) {
    Product product = status.product();
    return new ProductListResponse(
        product.getId(),
        product.getTitle(),
        product.getPrice(),
        product.getGrade(),
        status.liked());
  }
}
