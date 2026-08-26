package used.system.product;

import java.util.Collection;
import java.util.List;

public interface ProductService {
  Product join(Product product);

  Product findById(Long id);

  List<Product> findBySellerId(String sellerId);

  List<Product> search(ProductSearchCond cond);

  /** 주어진 id들의 상품을 모아온다. 그 사이 삭제된 id는 결과에서 빠진다. */
  List<Product> findAllByIds(Collection<Long> ids);

  void editProduct(Long productId, String loginId, ProductUpdateDto productUpdateDto);

  void deleteProduct(Long productId, String loginId);
}
