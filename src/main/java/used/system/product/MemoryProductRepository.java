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

  /**
   * 조건에 맞는 상품만 걸러낸다.
   *
   * <p>필드마다 "null이면 통과"로 시작한다. ||의 단축 평가 덕분에 조건이 비어 있으면 오른쪽 비교는 실행되지 않고 그대로 통과하므로, 조건을 하나도 주지 않으면
   * 결과가 findAll()과 같아진다. 덕분에 "검색이냐 전체 조회냐"를 호출하는 쪽에서 분기할 필요가 없다.
   */
  @Override
  public List<Product> search(ProductSearchCond cond) {
    return productMap.values().stream()
        .filter(product -> matchesKeyword(product, cond.getKeyword()))
        .filter(product -> cond.getMinPrice() == null || product.getPrice() >= cond.getMinPrice())
        .filter(product -> cond.getMaxPrice() == null || product.getPrice() <= cond.getMaxPrice())
        .filter(product -> cond.getGrade() == null || product.getGrade() == cond.getGrade())
        .collect(Collectors.toList());
  }

  /** 빈 문자열도 조건 없음으로 본다. 검색창을 비우고 제출하면 null이 아니라 ""로 들어오기 때문이다. */
  private boolean matchesKeyword(Product product, String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return true;
    }
    return product.getTitle().toLowerCase().contains(keyword.toLowerCase());
  }

  /** 넘겨받은 id 순서를 그대로 유지한다. 찜 목록이 찜한 순서로 보이려면 이 순서가 뒤집히면 안 된다. */
  @Override
  public List<Product> findAllByIds(Collection<Long> ids) {
    return ids.stream()
        .map(productMap::get)
        .filter(Objects::nonNull) // 그 사이 삭제된 상품은 제외한다
        .collect(Collectors.toList());
  }

  @Override
  public void delete(Long id) {
    productMap.remove(id);
  }
}
