package used.system.controller.myPage;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import used.system.controller.member.SessionConst;
import used.system.like.LikeService;
import used.system.member.Member;
import used.system.product.Product;
import used.system.product.ProductService;

@Controller
@RequiredArgsConstructor
public class MyPageController {

  private final ProductService productService;
  private final LikeService likeService;

  @GetMapping("/my-page")
  public String myPageForm(
      Model model, @SessionAttribute(name = SessionConst.LOGIN_MEMBER) Member member) {
    model.addAttribute("member", member);
    return "member/myPage";
  }

  @GetMapping("/my-page/products")
  public String myProducts(
      Model model, @SessionAttribute(name = SessionConst.LOGIN_MEMBER) Member member) {

    List<Product> products = productService.findBySellerId(member.getLoginId());

    model.addAttribute("products", products);
    return "member/myProducts";
  }

  @GetMapping("/my-page/likes")
  public String myLikes(
      Model model, @SessionAttribute(name = SessionConst.LOGIN_MEMBER) Member member) {

    List<Product> products = likeService.findLikedProducts(member.getLoginId());

    model.addAttribute("products", products);
    return "member/myLikes";
  }
}
