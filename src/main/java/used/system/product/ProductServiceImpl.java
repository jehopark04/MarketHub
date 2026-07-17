package used.system.product;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import used.system.exception.ProductNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService{

    private final ProductRepository productRepository;

    @Override
    public Product join(Product product) {
         return productRepository.save(product);
    }

    @Override
    public Product findById(Long id) {
        Product product = productRepository.findById(id);
        if (product == null){
            throw new ProductNotFoundException("상품을 찾을 수 없습니다. id = " + id);
        }
        return product;
    }

    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> findBySellerId(String sellerId){
        return productRepository.findBySellerId(sellerId);
    }

}
