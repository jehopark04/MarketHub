package used.system.controller.product;

import used.system.product.Product;
import used.system.product.ProductGrade;

/**
 * 상품 상세 조회 응답.
 *
 * <p>sellerId를 담는다. 상세 화면이 이미 판매자를 표시하므로 감출 정보가 아니다. createAt/updatedAt은 화면이 쓰지 않아 뺐다.
 *
 * <p>liked가 없는 이유: 상세 화면에 하트가 없다. 화면에 없는 필드를 미리 만들지 않는다 - 나중에 하트를 넣기로 하면 그때 추가하면 되고, 필드 추가는 기존
 * 클라이언트를 깨지 않는다.
 */
public record ProductDetailResponse(
    Long productId,
    String title,
    String description,
    int price,
    ProductGrade grade,
    String sellerId) {

  static ProductDetailResponse from(Product product) {
    return new ProductDetailResponse(
        product.getId(),
        product.getTitle(),
        product.getDescription(),
        product.getPrice(),
        product.getGrade(),
        product.getSellerId());
  }
}
