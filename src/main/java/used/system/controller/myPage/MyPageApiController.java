package used.system.controller.myPage;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import used.system.controller.member.MemberResponse;
import used.system.controller.member.SessionConst;
import used.system.controller.product.ProductSummaryResponse;
import used.system.member.Member;
import used.system.product.ProductService;

/**
 * 마이페이지 API.
 *
 * <p>찜 목록(GET /api/me/likes)만 LikeApiController에 있다. 경로는 /api/me 아래로 같지만 찜은 like 도메인의 자원이라 그쪽에 뒀다 -
 * 그 판단은 LikeApiController 주석에 있다.
 *
 * <p>로그인 검사는 여기에 없다. /api/**가 통째로 ApiLoginCheckInterceptor에 걸려 있고, 이 경로들은 열어둔 목록에 없다.
 *
 * <p>회원 식별자를 경로에 두지 않는다(/api/members/{loginId}가 아니라 /api/me). 누구의 것인지는 세션이 정하므로, 클라이언트가 남의 것을 가리킬
 * 방법 자체가 생기지 않는다.
 */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MyPageApiController {

  private final ProductService productService;

  /** 비밀번호는 MemberResponse가 담지 않는다. 세션의 Member를 그대로 반환하면 그대로 나간다. */
  @GetMapping
  public MemberResponse me(@SessionAttribute(name = SessionConst.LOGIN_MEMBER) Member loginMember) {
    return MemberResponse.from(loginMember);
  }

  /** 내가 등록한 상품. 등록한 적이 없으면 빈 목록에 200이다. */
  @GetMapping("/products")
  public List<ProductSummaryResponse> myProducts(
      @SessionAttribute(name = SessionConst.LOGIN_MEMBER) Member loginMember) {

    return productService.findBySellerId(loginMember.getLoginId()).stream()
        .map(ProductSummaryResponse::from)
        .toList();
  }
}
