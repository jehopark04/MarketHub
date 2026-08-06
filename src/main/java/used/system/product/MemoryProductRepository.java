package used.system.product;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryProductRepository implements ProductRepository {

  private final Map<Long, Product> productMap = new HashMap<>();
  private Long sequence = 0L;

  @Override
  public Product save(Product product) {
    product.setId(++sequence);
    productMap.put(product.getId(), product);
    return product;
  }

  @Override
  public Optional<Product> findById(Long id) {
    return Optional.ofNullable(productMap.get(id));
  }

  @Override
  public List<Product> findAll() {
    return new ArrayList<>(productMap.values());
  }

  @Override
  public List<Product> findBySellerId(String sellerId) {
    return productMap.values().stream()
        .filter(product -> product.getSellerId().equals(sellerId))
        .collect(Collectors.toList());
  }

  @Override
  public void delete(Long id) {
    productMap.remove(id);
  }
}
