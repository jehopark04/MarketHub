package used.system.like;

import java.util.List;
import java.util.Set;
import used.system.product.Product;

public interface LikeService {

  /** 이미 찜한 상품이면 아무 일도 하지 않는다. */
  void like(Long productId, String loginId);

  /** 찜하지 않은 상품이면 아무 일도 하지 않는다. */
  void unlike(Long productId, String loginId);

  /** 내가 찜한 상품 id 전부. 상품마다 조회하지 않도록 한 번에 받아간다 - attachLikeStatus가 이걸로 대조한다. */
  Set<Long> findLikedProductIds(String loginId);

  /** 내가 찜한 상품들. 찜한 뒤 삭제된 상품은 빠진다. */
  List<Product> findLikedProducts(String loginId);

  /**
   * 상품들에 내 찜 여부를 붙인다. 순서는 받은 그대로다.
   *
   * @param loginId 비로그인이면 null. 이때는 전부 false가 된다.
   */
  List<ProductLikeStatus> attachLikeStatus(List<Product> products, String loginId);

  /** 상품 하나의 찜 여부. 상세 화면처럼 목록이 아닌 곳에서 쓴다. loginId가 null이면 false다. */
  boolean isLiked(Long productId, String loginId);
}
