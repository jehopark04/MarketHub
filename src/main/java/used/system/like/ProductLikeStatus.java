package used.system.like;

import used.system.product.Product;

/**
 * 상품과 "내가 찜했는가"를 묶은 조회 결과.
 *
 * <p>Product에 liked 필드를 두지 않는 이유: 찜 여부는 상품의 속성이 아니라 보는 사람에 따라 달라지는 값이다. 같은 상품이라도 누가 보느냐에 따라 true도
 * false도 된다.
 */
public record ProductLikeStatus(Product product, boolean liked) {}
