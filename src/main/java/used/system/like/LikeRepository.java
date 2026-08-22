package used.system.like;

import java.util.List;
import java.util.Optional;

public interface LikeRepository {

  Like save(Like like);

  /** 중복 찜 확인과 찜 취소가 모두 이 조회를 거친다. 회원과 상품의 조합은 최대 하나뿐이라 Optional이다. */
  Optional<Like> findByMemberIdAndProductId(String memberId, Long productId);

  List<Like> findByMemberId(String memberId);

  void delete(Long id);
}
