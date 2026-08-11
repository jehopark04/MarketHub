package used.system.controller.home;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import used.system.member.Member;

/** HomeController 단위 테스트 - 홈 화면은 로그인 여부와 무관하게 열리되, 로그인한 경우에만 인사말용 회원 정보를 모델에 담는지 확인한다. */
class HomeControllerTest {

  private final HomeController homeController = new HomeController();

  @Test
  @DisplayName("비로그인 상태에서도 홈 화면은 열리며 회원 정보는 담기지 않는다")
  void home_notLoggedIn() {
    Model model = new ConcurrentModel();

    String view = homeController.home(null, model);

    assertThat(view).isEqualTo("home");
    assertThat(model.getAttribute("member")).isNull();
  }

  @Test
  @DisplayName("로그인 상태면 회원 정보를 모델에 담는다")
  void home_loggedIn() {
    Model model = new ConcurrentModel();
    Member member = new Member("userA", "에이", "password1");

    String view = homeController.home(member, model);

    assertThat(view).isEqualTo("home");
    assertThat(model.getAttribute("member")).isSameAs(member);
  }
}
