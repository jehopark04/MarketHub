package used.system.controller.like;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.SessionAttribute;
import used.system.controller.member.SessionConst;
import used.system.like.LikeService;
import used.system.member.Member;

/**
 * 찜하기 / 찜 취소.
 *
 * <p>토글 하나가 아니라 둘로 나눈 이유: 토글은 같은 요청을 두 번 보내면 원래대로 돌아가 멱등하지 않다. 지금 상태에 따라 어느 폼을 그릴지는 화면이 정하므로,
 * 사용자에게는 하트 하나를 누르는 것으로 보인다.
 *
 * <p>로그인 검사는 여기에 없다. 이 경로들은 WebConfig의 공개 목록에 없어 LoginCheckInterceptor가 컨트롤러에 닿기 전에 막는다.
 */
@Controller
@RequiredArgsConstructor
public class LikeController {

  private static final String DEFAULT_REDIRECT = "/products";

  private final LikeService likeService;

  @PostMapping("/products/{productId}/likes")
  public String like(
      @PathVariable Long productId,
      @SessionAttribute(name = SessionConst.LOGIN_MEMBER) Member loginMember,
      @RequestHeader(value = "Referer", required = false) String referer) {

    likeService.like(productId, loginMember.getLoginId());
    return "redirect:" + backTo(referer);
  }

  @PostMapping("/products/{productId}/likes/delete")
  public String unlike(
      @PathVariable Long productId,
      @SessionAttribute(name = SessionConst.LOGIN_MEMBER) Member loginMember,
      @RequestHeader(value = "Referer", required = false) String referer) {

    likeService.unlike(productId, loginMember.getLoginId());
    return "redirect:" + backTo(referer);
  }

  /**
   * 하트를 누른 화면으로 되돌린다. 목록에서 눌렀다면 걸어둔 검색 조건까지 그대로 유지된다.
   *
   * <p>Referer 값을 그대로 redirect에 넣으면 외부 주소로 튕겨 보낼 수 있다(오픈 리다이렉트). 그래서 경로와 쿼리만 떼어 쓰고 호스트는 버린다 — 값이
   * 무엇이든 우리 사이트 안에서만 이동한다. 헤더가 없는 요청도 있으므로 그때는 목록으로 보낸다.
   */
  private String backTo(String referer) {
    if (referer == null) {
      return DEFAULT_REDIRECT;
    }
    try {
      URI uri = URI.create(referer);
      String path = uri.getPath();
      if (path == null || !path.startsWith("/")) {
        return DEFAULT_REDIRECT;
      }
      return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
    } catch (IllegalArgumentException e) {
      return DEFAULT_REDIRECT;
    }
  }
}
