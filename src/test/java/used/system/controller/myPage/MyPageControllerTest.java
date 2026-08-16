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
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import used.system.member.Member;
import used.system.product.Product;
import used.system.product.ProductGrade;
import used.system.product.ProductService;

/**
 * MyPageController 단위 테스트 - 스프링 컨텍스트/MockMvc 없이 컨트롤러 메서드를 직접 호출한다. 세션 회원(@SessionAttribute)은 파라미터로
 * 직접 넘기고, Model은 Spring이 제공하는 단순 구현체(ConcurrentModel)를 사용한다.
 *
 * <p>비로그인 차단은 LoginCheckInterceptor의 책임이라 여기서 다루지 않는다(LoginCheckInterceptorTest 참고).
 */
@ExtendWith(MockitoExtension.class)
class MyPageControllerTest {

  @Mock private ProductService productService;

  @InjectMocks private MyPageController myPageController;

  @Test
  @DisplayName("마이페이지에 접근하면 회원 정보를 담고 마이페이지 뷰를 반환한다")
  void myPageForm() {
    Model model = new ConcurrentModel();
    Member member = new Member("userA", "에이", "password1");

    String view = myPageController.myPageForm(model, member);

    assertThat(view).isEqualTo("member/myPage");
    assertThat(model.getAttribute("member")).isSameAs(member);
  }

  @Test
  @DisplayName("내 상품 목록에 접근하면 내 상품을 담고 목록 뷰를 반환한다")
  void myProducts() {
    Model model = new ConcurrentModel();
    Member member = new Member("userA", "에이", "password1");
    List<Product> myProducts = List.of(new Product("userA", "내상품", "설명입니다", 10000, ProductGrade.A));
    given(productService.findBySellerId("userA")).willReturn(myProducts);

    String view = myPageController.myProducts(model, member);

    assertThat(view).isEqualTo("member/myProducts");
    assertThat(model.getAttribute("products")).isEqualTo(myProducts);
  }
}
