package used.system.like;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import used.system.product.Product;
import used.system.product.ProductService;

/**
 * 찜하기/취소와 찜 목록 조회.
 *
 * <p>ProductService에 의존한다. 상품이 존재하는지 확인하고 찜한 상품을 모아오려면 상품 쪽 규칙을 그대로 써야 하기 때문이다. 같은 계층끼리의 의존이라 방향은
 * 어긋나지 않는다 — 반대로 ProductService가 LikeService를 알게 되면 순환이 되므로 그 방향은 만들지 않는다.
 */
@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

  private final LikeRepository likeRepository;
  private final ProductService productService;

  /**
   * 이미 찜했으면 조용히 통과시킨다. 예외를 던지지 않는 이유는 같은 요청을 두 번 보내도 결과가 같아야 하기 때문이다 — 하트를 연달아 누르거나 새로고침으로 요청이 다시
   * 나가도 사용자가 에러 화면을 볼 이유가 없다. 중복 저장이 막히는 것은 그대로다.
   */
  @Override
  public void like(Long productId, String loginId) {
    productService.findById(productId); // 없는 상품이면 여기서 ProductNotFoundException

    if (likeRepository.findByMemberIdAndProductId(loginId, productId).isPresent()) {
      return;
    }
    likeRepository.save(new Like(loginId, productId));
  }

  /** 찜하지 않은 상품을 취소해도 통과시킨다. like()와 같은 이유다. */
  @Override
  public void unlike(Long productId, String loginId) {
    likeRepository
        .findByMemberIdAndProductId(loginId, productId)
        .ifPresent(like -> likeRepository.delete(like.getId()));
  }

  @Override
  public Set<Long> findLikedProductIds(String loginId) {
    return likeRepository.findByMemberId(loginId).stream()
        .map(Like::getProductId)
        .collect(Collectors.toSet());
  }

  @Override
  public List<Product> findLikedProducts(String loginId) {
    List<Long> productIds =
        likeRepository.findByMemberId(loginId).stream().map(Like::getProductId).toList();

    // 찜한 뒤 판매자가 삭제한 상품이 있을 수 있다. findAllByIds가 없는 id를 빼주므로
    // 목록이 예외로 끊기지 않고, 남아 있는 찜 기록은 화면에서 자연히 보이지 않게 된다.
    return productService.findAllByIds(productIds);
  }

  /**
   * 내가 찜한 상품 id들. 로그인하지 않았으면 빈 집합이다.
   *
   * <p>"로그인하지 않았으면 찜이 없는 것으로 친다"는 판단을 여기 한 곳에만 둔다. 목록과 단건이 각자 null을 검사하면 한쪽만 고치는 사고가 난다.
   */
  private Set<Long> likedIdsOf(String loginId) {
    return loginId == null ? Set.of() : findLikedProductIds(loginId);
  }

  @Override
  public boolean isLiked(Long productId, String loginId) {
    return likedIdsOf(loginId).contains(productId);
  }

  /** 상품마다 "내가 찜했나"를 묻지 않고 내 찜 id를 한 번에 받아 대조한다. 상품마다 조회하면 목록 길이만큼 질의가 늘어난다. */
  @Override
  public List<ProductLikeStatus> attachLikeStatus(List<Product> products, String loginId) {
    Set<Long> likedIds = likedIdsOf(loginId);

    return products.stream()
        .map(product -> new ProductLikeStatus(product, likedIds.contains(product.getId())))
        .toList();
  }
}
