package used.system.product;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class MemoryProductRepository implements ProductRepository{

    private final Map<Long, Product> productMap = new HashMap<>();
    private Long sequence = 0L;


    @Override
    public Product save(Product product) {
        product.setId(++sequence);
        productMap.put(product.getId(), product);
        return product;
    }

    @Override
    public Product findById(Long id) {
        return productMap.get(id);
    }

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(productMap.values());
    }
}
