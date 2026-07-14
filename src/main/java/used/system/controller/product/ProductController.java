package used.system.controller.product;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import used.system.controller.member.SessionConst;
import used.system.member.Member;
import used.system.product.Product;
import used.system.product.ProductGrade;
import used.system.product.ProductService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProductController {


    private final ProductService productService;

    @GetMapping("/products")
    public String list(Model model){
        List<Product> products = productService.findAll();
        model.addAttribute("products", products);
        return "product/list";
    }


    @GetMapping("products/new")
    public String addProductForm(Model model){
        model.addAttribute("productCreateForm", new ProductForm());
        return "product/addForm";
    }


    /**
     * 우선 로그인 후 제품 추가가 가능하기때문에 세션을 통해서 검증및 회원이 누군지를 알아야하는 상황임
     * session을 통해서 현재 로그인중인 사람을 가져오고 만약 null이면 로그인화면으로 견인
     *
     * 로그인을 했다 싶으면 이제 제품 등록
     *
     */
    @PostMapping("products")
    public String addProduct(@Validated @ModelAttribute("productCreateForm") ProductForm productForm, BindingResult bindingResult,
                             @SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false)Member loginMember){

        if (loginMember == null){
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()){
            return "product/addForm";
        }
        Product product = new Product(loginMember.getLoginId(), productForm.getTitle(), productForm.getDescription(), productForm.getPrice(), productForm.getGrade());
        productService.join(product);
        return "redirect:/products";
    }

    @GetMapping("/products/{productId}")
    public String item(@PathVariable Long productId, Model model){
        Product product = productService.findById(productId);
        model.addAttribute("product", product);
        return "product/item";
    }



    @ModelAttribute("grades") // 아 이걸로 고를수았는 판을 주고 이 판에서 골라진걸 post로 바인딩해서 set시키는 그림
    public ProductGrade[] grades(){
        return ProductGrade.values();
    }




}
