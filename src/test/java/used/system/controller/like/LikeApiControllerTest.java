package used.system.controller.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import used.system.controller.product.ProductSummaryResponse;
import used.system.exception.ProductNotFoundException;
import used.system.like.LikeService;
import used.system.member.Member;
import used.system.product.Product;
import used.system.product.ProductGrade;

/**
 * LikeApiController 단위 테스트 - 컨트롤러 메서드를 직접 호출한다.
 *
 * <p>로그인 가드는 ApiLoginCheckInterceptor의 책임이라 여기서 다루지 않는다. 멱등성(중복 찜, 없는 찜 취소)도 LikeServiceImplTest가
 * 이미 검증한다 - 여기서 서비스는 목이라 같은 것을 다시 확인해도 아무것도 증명하지 못한다.
 *
 * <p>여기서 볼 것은 셋이다. 서비스에 올바른 인자를 넘기는가, 성공 응답이 204에 본문이 없는가, 서비스의 예외를 삼키지 않고 통과시키는가.
 */
@ExtendWith(MockitoExtension.class)
class LikeApiControllerTest {

  @Mock private LikeService likeService;

  @InjectMocks private LikeApiController likeApiController;

  private final Member loginMember = new Member("userA", "에이", "password1");

  @Test
  @DisplayName("찜하기는 세션 회원의 loginId로 서비스에 위임하고 204를 반환한다")
  void like_delegates() {
    ResponseEntity<Void> response = likeApiController.like(1L, loginMember);

    // 회원 식별자는 경로가 아니라 세션에서 온다. 클라이언트가 보낸 값이면 남의 찜을 조작할 수 있다.
    verify(likeService).like(1L, "userA");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(response.getBody()).isNull();
  }

  @Test
  @DisplayName("찜 취소도 세션 회원의 loginId로 위임하고 204를 반환한다")
  void unlike_delegates() {
    ResponseEntity<Void> response = likeApiController.unlike(2L, loginMember);

    verify(likeService).unlike(2L, "userA");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(response.getBody()).isNull();
  }

  @Test
  @DisplayName("없는 상품이면 서비스의 예외를 그대로 통과시킨다")
  void like_propagatesNotFound() {
    // 컨트롤러가 try-catch로 삼키면 ApiExceptionHandler에 닿지 않아 404가 나가지 않는다.
    // 예외 -> 상태 코드 매핑은 한곳에 모아두기로 한 규약이라, 그 통로가 막히지 않았는지 고정한다.
    willThrow(new ProductNotFoundException("상품을 찾을 수 없습니다.")).given(likeService).like(99L, "userA");

    assertThatThrownBy(() -> likeApiController.like(99L, loginMember))
        .isInstanceOf(ProductNotFoundException.class);
  }

  @Test
  @DisplayName("세션 회원의 찜 목록을 DTO로 변환해 반환한다")
  void myLikes_returnsDtoList() {
    Product product = new Product("sellerX", "제목", "설명", 1000, ProductGrade.A);
    product.setId(1L);
    given(likeService.findLikedProducts("userA")).willReturn(List.of(product));

    List<ProductSummaryResponse> result = likeApiController.myLikes(loginMember);

    assertThat(result).containsExactly(new ProductSummaryResponse(1L, "제목", 1000, ProductGrade.A));
  }

  @Test
  @DisplayName("찜한 상품이 없으면 빈 목록을 반환한다")
  void myLikes_empty() {
    given(likeService.findLikedProducts("userA")).willReturn(List.of());

    assertThat(likeApiController.myLikes(loginMember)).isEmpty();
  }
}
