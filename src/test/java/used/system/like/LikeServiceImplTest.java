package used.system.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import used.system.exception.ProductNotFoundException;
import used.system.product.Product;
import used.system.product.ProductGrade;
import used.system.product.ProductService;

/** LikeServiceImpl 단위 테스트 - 저장소와 ProductService를 Mockito mock으로 주입하고 찜의 비즈니스 규칙만 검증한다. */
@ExtendWith(MockitoExtension.class)
class LikeServiceImplTest {

  @Mock private LikeRepository likeRepository;

  @Mock private ProductService productService;

  @InjectMocks private LikeServiceImpl likeService;

  private Product product() {
    return new Product("seller", "상품", "설명입니다", 10000, ProductGrade.A);
  }

  // ---------- 찜하기 ----------

  @Test
  @DisplayName("찜하지 않은 상품을 찜하면 저장된다")
  void like_saves() {
    given(productService.findById(1L)).willReturn(product());
    given(likeRepository.findByMemberIdAndProductId("userA", 1L)).willReturn(Optional.empty());

    likeService.like(1L, "userA");

    ArgumentCaptor<Like> captor = ArgumentCaptor.forClass(Like.class);
    verify(likeRepository).save(captor.capture());
    assertThat(captor.getValue().getMemberId()).isEqualTo("userA");
    assertThat(captor.getValue().getProductId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("이미 찜한 상품을 다시 찜해도 중복 저장되지 않는다")
  void like_doesNotDuplicate() {
    given(productService.findById(1L)).willReturn(product());
    given(likeRepository.findByMemberIdAndProductId("userA", 1L))
        .willReturn(Optional.of(new Like("userA", 1L)));

    likeService.like(1L, "userA"); // 예외 없이 통과해야 한다

    verify(likeRepository, never()).save(any());
  }

  @Test
  @DisplayName("존재하지 않는 상품은 찜할 수 없다")
  void like_productNotFound() {
    willThrow(new ProductNotFoundException("없음")).given(productService).findById(999L);

    assertThatThrownBy(() -> likeService.like(999L, "userA"))
        .isInstanceOf(ProductNotFoundException.class);

    verify(likeRepository, never()).save(any());
  }

  // ---------- 찜 취소 ----------

  @Test
  @DisplayName("찜한 상품을 취소하면 그 기록이 삭제된다")
  void unlike_deletes() {
    Like like = new Like("userA", 1L);
    like.setId(7L);
    given(likeRepository.findByMemberIdAndProductId("userA", 1L)).willReturn(Optional.of(like));

    likeService.unlike(1L, "userA");

    verify(likeRepository).delete(7L);
  }

  @Test
  @DisplayName("찜하지 않은 상품을 취소해도 예외 없이 통과한다")
  void unlike_notLiked() {
    given(likeRepository.findByMemberIdAndProductId("userA", 1L)).willReturn(Optional.empty());

    likeService.unlike(1L, "userA");

    verify(likeRepository, never()).delete(anyLong());
  }

  // ---------- 조회 ----------

  @Test
  @DisplayName("찜한 상품 id들을 집합으로 반환한다")
  void findLikedProductIds() {
    given(likeRepository.findByMemberId("userA"))
        .willReturn(List.of(new Like("userA", 1L), new Like("userA", 3L)));

    assertThat(likeService.findLikedProductIds("userA")).containsExactlyInAnyOrder(1L, 3L);
  }

  @Test
  @DisplayName("찜한 적이 없으면 빈 집합을 반환한다")
  void findLikedProductIds_empty() {
    given(likeRepository.findByMemberId("userA")).willReturn(List.of());

    assertThat(likeService.findLikedProductIds("userA")).isEmpty();
  }

  @Test
  @DisplayName("찜한 상품들을 찜한 순서 그대로 상품 조회에 넘긴다")
  void findLikedProducts_passesIdsInOrder() {
    given(likeRepository.findByMemberId("userA"))
        .willReturn(List.of(new Like("userA", 3L), new Like("userA", 1L)));
    given(productService.findAllByIds(List.of(3L, 1L))).willReturn(List.of(product()));

    assertThat(likeService.findLikedProducts("userA")).hasSize(1);

    // 삭제된 상품을 걸러내는 일은 findAllByIds의 규약이다. 여기서는 id를 순서대로 넘기는지만 본다.
    verify(productService).findAllByIds(List.of(3L, 1L));
  }

  // ---------- 찜 여부 붙이기 ----------

  private Product product(Long id) {
    Product product = product();
    product.setId(id);
    return product;
  }

  @Test
  @DisplayName("찜한 상품에만 liked가 붙고 받은 순서는 그대로다")
  void attachLikeStatus() {
    given(likeRepository.findByMemberId("userA")).willReturn(List.of(new Like("userA", 3L)));

    List<ProductLikeStatus> result =
        likeService.attachLikeStatus(List.of(product(5L), product(3L)), "userA");

    assertThat(result).extracting(ProductLikeStatus::liked).containsExactly(false, true);
  }

  @Test
  @DisplayName("비로그인이면 저장소를 보지 않고 전부 false다")
  void attachLikeStatus_anonymous() {
    // loginId가 null인데 조회로 넘어가면 "찜한 적 없는 회원"을 찾는 헛질의가 목록마다 나간다.
    List<ProductLikeStatus> result = likeService.attachLikeStatus(List.of(product(3L)), null);

    assertThat(result).extracting(ProductLikeStatus::liked).containsExactly(false);
    verify(likeRepository, never()).findByMemberId(any());
  }

  @Test
  @DisplayName("상품이 없으면 빈 목록이다")
  void attachLikeStatus_emptyProducts() {
    assertThat(likeService.attachLikeStatus(List.of(), "userA")).isEmpty();
  }
}
