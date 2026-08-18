package used.system.product;

import java.util.List;

public interface ProductService {
  Product join(Product product);

  Product findById(Long id);

  List<Product> findBySellerId(String sellerId);

  List<Product> search(ProductSearchCond cond);

  void editProduct(Long productId, String loginId, ProductUpdateDto productUpdateDto);

  Product findByIdAndOwner(Long productId, String loginId);

  void deleteProduct(Long productId, String loginId);
}
