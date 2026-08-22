package used.system.like;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryLikeRepository implements LikeRepository {

  private final Map<Long, Like> likeMap = new HashMap<>();
  private Long sequence = 0L;

  @Override
  public Like save(Like like) {
    like.setId(++sequence);
    likeMap.put(like.getId(), like);
    return like;
  }

  @Override
  public Optional<Like> findByMemberIdAndProductId(String memberId, Long productId) {
    return likeMap.values().stream()
        .filter(like -> like.getMemberId().equals(memberId))
        .filter(like -> like.getProductId().equals(productId))
        .findFirst();
  }

  /** 찜한 순서대로 반환한다. id가 채번 순서라 그대로 정렬 기준이 된다. */
  @Override
  public List<Like> findByMemberId(String memberId) {
    return likeMap.values().stream()
        .filter(like -> like.getMemberId().equals(memberId))
        .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
        .collect(Collectors.toCollection(ArrayList::new));
  }

  @Override
  public void delete(Long id) {
    likeMap.remove(id);
  }
}
