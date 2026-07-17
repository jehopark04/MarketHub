package used.system.product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);

    Product findById(Long id);

    List<Product> findAll();

    List<Product> findBySellerId(String sellerId);
}
