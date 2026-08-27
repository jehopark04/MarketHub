package used.system.controller.product;

import used.system.product.Product;
import used.system.product.ProductGrade;

/**
 * 상품 상세 조회 응답.
 *
 * <p>sellerId를 담는다. 상세 화면이 이미 판매자를 표시하므로 감출 정보가 아니다. createAt/updatedAt은 화면이 쓰지 않아 뺐다.
 *
 * <p>liked는 상세 화면에 하트를 넣기로 하면서 더했다. 그전까지는 상세에 하트가 없어 담지 않았다 - 화면에 없는 필드를 미리 만들지 않는다. 필드를 더하는 것은 기존
 * 클라이언트를 깨지 않아, 필요해진 시점에 넣어도 늦지 않다.
 *
 * <p>liked는 보는 사람에 따라 달라지는 값이다. 같은 상품이라도 누가 조회하느냐에 따라 true도 false도 되고, 비로그인이면 늘 false다.
 */
public record ProductDetailResponse(
    Long productId,
    String title,
    String description,
    int price,
    ProductGrade grade,
    String sellerId,
    boolean liked) {

  static ProductDetailResponse from(Product product, boolean liked) {
    return new ProductDetailResponse(
        product.getId(),
        product.getTitle(),
        product.getDescription(),
        product.getPrice(),
        product.getGrade(),
        product.getSellerId(),
        liked);
  }
}
