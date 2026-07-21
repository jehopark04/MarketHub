package used.system.product;


import java.util.List;

public interface ProductService {
    Product join(Product product);
    Product findById(Long id);
    List<Product> findAll();
    List<Product> findBySellerId(String sellerId);
    void editProduct(Long productId, String loginId, ProductUpdateDto productUpdateDto);
    Product findByIdAndOwner(Long productId, String loginId);
    void deleteProduct(Long productId, String loginId);


}
