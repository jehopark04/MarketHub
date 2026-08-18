package used.system.controller.product;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import used.system.controller.member.SessionConst;
import used.system.member.Member;
import used.system.product.*;

@Controller
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;

  /**
   * BindingResult를 받아두기만 하고 검사하지 않는 것은 의도다. 등록·수정과 달리 조회는 잘못된 조건에 실패로 답하면 안 된다 — 주소창을 직접 고쳤거나 오래된
   * 링크를 눌렀을 때 목록이 에러 페이지가 되어버린다.
   *
   * <p>이 파라미터가 있으면 스프링이 타입 변환 실패에 예외를 던지는 대신 FieldError로 기록하고 그 필드를 null로 남긴다. 필터는 null을 "조건 없음"으로
   * 보므로, /products?minPrice=abc는 가격 조건만 빠진 채 나머지로 검색된다.
   */
  @GetMapping("/products")
  public String list(
      @ModelAttribute("productSearchCond") ProductSearchCond cond,
      BindingResult bindingResult,
      Model model) {
    List<Product> products = productService.search(cond);
    model.addAttribute("products", products);
    return "product/list";
  }

  @GetMapping("/products/new")
  public String addProductForm(Model model) {
    model.addAttribute("productCreateForm", new ProductForm());
    return "product/addForm";
  }

  /** 판매자는 폼 입력이 아니라 세션에서 결정한다. 남의 이름으로 등록되는 걸 막으려면 이 값은 클라이언트가 보낸 데이터에서 오면 안 된다. */
  @PostMapping("/products")
  public String addProduct(
      @Validated @ModelAttribute("productCreateForm") ProductForm productForm,
      BindingResult bindingResult,
      @SessionAttribute(name = SessionConst.LOGIN_MEMBER) Member loginMember) {

    if (bindingResult.hasErrors()) {
      return "product/addForm";
    }
    Product product =
        new Product(
            loginMember.getLoginId(),
            productForm.getTitle(),
            productForm.getDescription(),
            productForm.getPrice(),
            productForm.getGrade());
    productService.join(product);
    return "redirect:/products";
  }

  @GetMapping("/products/{productId}")
  public String item(@PathVariable Long productId, Model model) {
    Product product = productService.findById(productId);
    model.addAttribute("product", product);
    return "product/item";
  }

  @GetMapping("/products/{productId}/edit")
  public String editForm(
      @SessionAttribute(name = SessionConst.LOGIN_MEMBER) Member member,
      @PathVariable Long productId,
      Model model) {

    Product product = productService.findByIdAndOwner(productId, member.getLoginId());
    ProductUpdateForm productUpdateForm = new ProductUpdateForm();

    productUpdateForm.setTitle(product.getTitle());
    productUpdateForm.setDescription(product.getDescription());
    productUpdateForm.setPrice(product.getPrice());
    productUpdateForm.setGrade(product.getGrade());

    model.addAttribute("productUpdateForm", productUpdateForm);
    model.addAttribute("productId", productId);

    return "product/editForm";
  }

  @PostMapping("/products/{productId}/edit")
  public String edit(
      @SessionAttribute(name = SessionConst.LOGIN_MEMBER) Member member,
      @PathVariable Long productId,
      @Validated @ModelAttribute("productUpdateForm") ProductUpdateForm productUpdateForm,
      BindingResult bindingResult,
      Model model) {

    if (bindingResult.hasErrors()) {
      model.addAttribute("productId", productId);
      return "product/editForm";
    }

    ProductUpdateDto productUpdateDto = productUpdateForm.toDto();

    productService.editProduct(productId, member.getLoginId(), productUpdateDto);
    return "redirect:/my-page/products";
  }

  @PostMapping("/products/{productId}/delete")
  public String delete(
      @SessionAttribute(name = SessionConst.LOGIN_MEMBER) Member loginMember,
      @PathVariable Long productId) {
    productService.deleteProduct(productId, loginMember.getLoginId());
    return "redirect:/my-page/products";
  }

  @ModelAttribute("grades") // 아 이걸로 고를수았는 판을 주고 이 판에서 골라진걸 post로 바인딩해서 set시키는 그림
  public ProductGrade[] grades() {
    return ProductGrade.values();
  }
}
