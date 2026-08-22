package used.system.product;

import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import used.system.exception.ForbiddenException;
import used.system.exception.ProductNotFoundException;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

  private final ProductRepository productRepository;

  @Override
  public Product join(Product product) {
    return productRepository.save(product);
  }

  @Override
  public Product findById(Long id) {
    return productRepository
        .findById(id)
        .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다. id = " + id));
  }

  @Override
  public List<Product> findBySellerId(String sellerId) {
    return productRepository.findBySellerId(sellerId);
  }

  @Override
  public List<Product> search(ProductSearchCond cond) {
    return productRepository.search(cond);
  }

  @Override
  public List<Product> findAllByIds(Collection<Long> ids) {
    return productRepository.findAllByIds(ids);
  }

  @Override
  public Product findByIdAndOwner(Long productId, String loginId) {
    return getOwnerProduct(productId, loginId);
  }

  @Override
  public void deleteProduct(Long productId, String loginId) {
    getOwnerProduct(productId, loginId);
    productRepository.delete(productId);
  }

  @Override
  public void editProduct(Long productId, String loginId, ProductUpdateDto dto) {
    Product product = getOwnerProduct(productId, loginId);
    product.update(dto.title(), dto.description(), dto.price(), dto.grade());
  }

  private Product getOwnerProduct(Long productId, String loginId) {
    Product product = findById(productId);
    if (!product.getSellerId().equals(loginId)) {
      throw new ForbiddenException("본인 상품만 접근할 수 있습니다.");
    }

    return product;
  }
}
