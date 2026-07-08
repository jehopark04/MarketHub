package used.system.product;

import java.util.List;

public interface ProductService {
    Product join(Product product);
    Product findById(Long id);
    List<Product> findAll();
}
