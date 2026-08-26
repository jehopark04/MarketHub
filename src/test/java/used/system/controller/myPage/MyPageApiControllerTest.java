package used.system.controller.myPage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import used.system.controller.member.MemberResponse;
import used.system.controller.product.ProductSummaryResponse;
import used.system.member.Member;
import used.system.product.Product;
import used.system.product.ProductGrade;
import used.system.product.ProductService;

/**
 * MyPageApiController 단위 테스트 - 컨트롤러 메서드를 직접 호출한다.
 *
 * <p>로그인 가드는 ApiLoginCheckInterceptor의 책임이라 여기서 다루지 않는다.
 *
 * <p>여기서 볼 것은 둘이다. 세션 회원의 loginId로 조회하는가, 응답에 담기지 말아야 할 것이 담기지 않는가.
 */
@ExtendWith(MockitoExtension.class)
class MyPageApiControllerTest {

  @Mock private ProductService productService;

  @InjectMocks private MyPageApiController myPageApiController;

  private final Member loginMember = new Member("userA", "에이", "password1");

  private Product product(Long id) {
    Product product = new Product("userA", "제목", "설명입니다", 1000, ProductGrade.A);
    product.setId(id);
    return product;
  }

  @Test
  @DisplayName("내 정보 응답에 비밀번호가 담기지 않는다")
  void me_hidesPassword() {
    // MemberResponse에 password 필드를 더하면 이 비교가 컴파일되지 않는다 - 그게 이 테스트의 가드다.
    assertThat(myPageApiController.me(loginMember)).isEqualTo(new MemberResponse("userA", "에이"));
  }

  @Test
  @DisplayName("내가 등록한 상품을 세션 loginId로 조회해 DTO로 반환한다")
  void myProducts_returnsDtoList() {
    given(productService.findBySellerId("userA")).willReturn(List.of(product(1L), product(2L)));

    List<ProductSummaryResponse> result = myPageApiController.myProducts(loginMember);

    // sellerId를 경로가 아니라 세션에서 가져온다. 클라이언트가 보낸 값이면 남의 상품 목록을 볼 수 있다.
    assertThat(result)
        .containsExactly(
            new ProductSummaryResponse(1L, "제목", 1000, ProductGrade.A),
            new ProductSummaryResponse(2L, "제목", 1000, ProductGrade.A));
  }

  @Test
  @DisplayName("등록한 상품이 없으면 빈 목록을 반환한다")
  void myProducts_empty() {
    given(productService.findBySellerId("userA")).willReturn(List.of());

    assertThat(myPageApiController.myProducts(loginMember)).isEmpty();
  }
}
