package used.system.product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
  Product save(Product product);

  Optional<Product> findById(Long id);

  List<Product> findAll();

  List<Product> findBySellerId(String sellerId);

  List<Product> search(ProductSearchCond cond);

  void delete(Long id);
}
