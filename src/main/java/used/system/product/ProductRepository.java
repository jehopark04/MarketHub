package used.system.product;

import java.util.List;

public interface ProductRepository {
  Product save(Product product);

  Product findById(Long id);

  List<Product> findAll();

  List<Product> findBySellerId(String sellerId);

  void delete(Long id);
}
