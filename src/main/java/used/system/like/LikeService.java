package used.system.like;

import java.util.List;
import java.util.Set;
import used.system.product.Product;

public interface LikeService {

  /** 이미 찜한 상품이면 아무 일도 하지 않는다. */
  void like(Long productId, String loginId);

  /** 찜하지 않은 상품이면 아무 일도 하지 않는다. */
  void unlike(Long productId, String loginId);

  /** 목록 화면에서 하트 상태를 정할 때 쓴다. 상품마다 조회하지 않도록 한 번에 받아간다. */
  Set<Long> findLikedProductIds(String loginId);

  /** 내가 찜한 상품들. 찜한 뒤 삭제된 상품은 빠진다. */
  List<Product> findLikedProducts(String loginId);
}
