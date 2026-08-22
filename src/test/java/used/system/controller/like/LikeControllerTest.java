package used.system.controller.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import used.system.like.LikeService;
import used.system.member.Member;

/**
 * LikeController 단위 테스트 - 컨트롤러 메서드를 직접 호출한다.
 *
 * <p>로그인 가드는 인터셉터의 책임이라 여기서 다루지 않는다. 여기서 볼 것은 서비스에 올바른 인자를 넘기는지와, 누른 화면으로 되돌리는 계산이 안전한지다.
 */
@ExtendWith(MockitoExtension.class)
class LikeControllerTest {

  @Mock private LikeService likeService;

  @InjectMocks private LikeController likeController;

  private final Member loginMember = new Member("userA", "에이", "password1");

  @Test
  @DisplayName("찜하기는 세션 회원의 loginId로 서비스에 위임한다")
  void like_delegates() {
    String view = likeController.like(1L, loginMember, "http://localhost:8080/products");

    verify(likeService).like(1L, "userA");
    assertThat(view).isEqualTo("redirect:/products");
  }

  @Test
  @DisplayName("찜 취소도 세션 회원의 loginId로 서비스에 위임한다")
  void unlike_delegates() {
    String view = likeController.unlike(1L, loginMember, "http://localhost:8080/my-page/likes");

    verify(likeService).unlike(1L, "userA");
    assertThat(view).isEqualTo("redirect:/my-page/likes");
  }

  @Test
  @DisplayName("검색 조건이 걸린 목록에서 눌렀다면 그 조건까지 유지한 채 되돌아간다")
  void like_keepsQueryString() {
    String view =
        likeController.like(
            1L, loginMember, "http://localhost:8080/products?keyword=%EB%A7%A5%EB%B6%81&grade=S");

    // 하트를 눌렀다고 검색 결과가 초기화되면 안 된다
    assertThat(view).isEqualTo("redirect:/products?keyword=%EB%A7%A5%EB%B6%81&grade=S");
  }

  @Test
  @DisplayName("Referer가 없으면 상품 목록으로 되돌린다")
  void like_noReferer() {
    assertThat(likeController.like(1L, loginMember, null)).isEqualTo("redirect:/products");
  }

  @Test
  @DisplayName("외부 주소를 Referer로 보내도 우리 경로로만 이동한다")
  void like_externalRefererIsNotFollowed() {
    // 오픈 리다이렉트 방지. 호스트를 버리고 경로만 쓰므로 evil.com으로는 갈 수 없다.
    String view = likeController.like(1L, loginMember, "https://evil.com/steal");

    assertThat(view).isEqualTo("redirect:/steal");
    assertThat(view).doesNotContain("evil.com");
  }

  @Test
  @DisplayName("경로가 없는 Referer면 상품 목록으로 되돌린다")
  void like_refererWithoutPath() {
    assertThat(likeController.like(1L, loginMember, "https://evil.com"))
        .isEqualTo("redirect:/products");
  }

  @Test
  @DisplayName("형식이 깨진 Referer면 상품 목록으로 되돌린다")
  void like_malformedReferer() {
    assertThat(likeController.like(1L, loginMember, "ht tp://[broken"))
        .isEqualTo("redirect:/products");
  }
}
