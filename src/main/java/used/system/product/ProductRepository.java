package used.system.product;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
  Product save(Product product);

  Optional<Product> findById(Long id);

  List<Product> findAll();

  List<Product> findBySellerId(String sellerId);

  List<Product> search(ProductSearchCond cond);

  /**
   * 주어진 id들에 해당하는 상품을 반환한다. 없는 id는 결과에서 조용히 빠진다.
   *
   * <p>찜 목록처럼 다른 곳에 보관된 id로 상품을 모아올 때 쓴다. 그 사이 상품이 삭제됐을 수 있어 "없으면 예외"가 아니라 "없으면 제외"여야 화면이 깨지지 않는다.
   * JpaRepository.findAllById와 같은 규약이라 나중에 JPA로 옮겨도 의미가 유지된다.
   */
  List<Product> findAllByIds(Collection<Long> ids);

  void delete(Long id);
}
