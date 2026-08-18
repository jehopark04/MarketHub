package used.system.controller.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import used.system.member.Member;
import used.system.product.Product;
import used.system.product.ProductGrade;
import used.system.product.ProductSearchCond;
import used.system.product.ProductService;
import used.system.product.ProductUpdateDto;

/**
 * ProductController 단위 테스트 - 스프링 컨텍스트/MockMvc 없이 컨트롤러 메서드를 직접 호출한다. 세션 회원(@SessionAttribute)은 파라미터로
 * 직접 넘긴다.
 *
 * <p>로그인 가드는 LoginCheckInterceptor의 책임이라 여기서 비로그인 경우를 다루지 않는다(LoginCheckInterceptorTest 참고). 소유권
 * 검증(403)도 서비스의 책임이다. 여기서는 "서비스에 올바른 인자를 넘기는가", "모델에 무엇을 담는가"에 집중한다.
 */
@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

  @Mock private ProductService productService;

  @InjectMocks private ProductController productController;

  private final Member loginMember = new Member("userA", "에이", "password1");

  private ProductForm createForm() {
    ProductForm form = new ProductForm();
    form.setTitle("제목");
    form.setDescription("설명입니다");
    form.setPrice(10000);
    form.setGrade(ProductGrade.A);
    return form;
  }

  private ProductUpdateForm updateForm() {
    ProductUpdateForm form = new ProductUpdateForm();
    form.setTitle("새제목");
    form.setDescription("새설명입니다");
    form.setPrice(20000);
    form.setGrade(ProductGrade.S);
    return form;
  }

  // ---------- 목록 / 상세 ----------

  @Test
  @DisplayName("상품 목록은 로그인 없이도 조회되며 모델에 상품들을 담는다")
  void list() {
    Model model = new ConcurrentModel();
    List<Product> products = List.of(new Product("userA", "상품", "설명입니다", 10000, ProductGrade.A));
    ProductSearchCond cond = new ProductSearchCond();
    given(productService.search(cond)).willReturn(products);

    String view = productController.list(cond, model);

    assertThat(view).isEqualTo("product/list");
    assertThat(model.getAttribute("products")).isEqualTo(products);
  }

  @Test
  @DisplayName("검색 조건은 그대로 서비스에 전달된다")
  void list_passesSearchCond() {
    Model model = new ConcurrentModel();
    ProductSearchCond cond = new ProductSearchCond();
    cond.setKeyword("맥북");
    cond.setMinPrice(10000);
    cond.setMaxPrice(50000);
    cond.setGrade(ProductGrade.S);
    given(productService.search(cond)).willReturn(List.of());

    productController.list(cond, model);

    // 컨트롤러는 조건을 해석하지 않는다. 바인딩된 그대로 넘기는 것이 이 계층의 책임이다.
    ArgumentCaptor<ProductSearchCond> captor = ArgumentCaptor.forClass(ProductSearchCond.class);
    verify(productService).search(captor.capture());
    ProductSearchCond passed = captor.getValue();
    assertThat(passed.getKeyword()).isEqualTo("맥북");
    assertThat(passed.getMinPrice()).isEqualTo(10000);
    assertThat(passed.getMaxPrice()).isEqualTo(50000);
    assertThat(passed.getGrade()).isEqualTo(ProductGrade.S);
  }

  @Test
  @DisplayName("상품 상세는 조회한 상품을 모델에 담는다")
  void item() {
    Model model = new ConcurrentModel();
    Product product = new Product("userA", "상품", "설명입니다", 10000, ProductGrade.A);
    given(productService.findById(1L)).willReturn(product);

    String view = productController.item(1L, model);

    assertThat(view).isEqualTo("product/item");
    assertThat(model.getAttribute("product")).isSameAs(product);
  }

  // ---------- 등록 ----------

  @Test
  @DisplayName("등록 폼에 접근하면 값이 비어 있는 폼 객체를 모델에 담는다")
  void addProductForm() {
    Model model = new ConcurrentModel();

    String view = productController.addProductForm(model);

    assertThat(view).isEqualTo("product/addForm");
    assertThat(model.getAttribute("productCreateForm"))
        .isInstanceOf(ProductForm.class)
        .satisfies(
            attribute -> {
              ProductForm form = (ProductForm) attribute;
              assertThat(form.getTitle()).isNull();
              assertThat(form.getDescription()).isNull();
              assertThat(form.getPrice()).isNull();
              assertThat(form.getGrade()).isNull();
            });
  }

  @Test
  @DisplayName("검증 에러가 있으면 저장하지 않고 등록 폼을 다시 보여준다")
  void addProduct_validationError() {
    ProductForm form = createForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "productCreateForm");
    bindingResult.rejectValue("title", "NotBlank");

    String view = productController.addProduct(form, bindingResult, loginMember);

    assertThat(view).isEqualTo("product/addForm");
    verify(productService, never()).join(any());
  }

  @Test
  @DisplayName("등록에 성공하면 판매자를 세션 회원으로 채워 저장한다")
  void addProduct_success() {
    ProductForm form = createForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "productCreateForm");

    String view = productController.addProduct(form, bindingResult, loginMember);

    assertThat(view).isEqualTo("redirect:/products");

    ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
    verify(productService).join(captor.capture());
    Product saved = captor.getValue();
    // 판매자는 폼 입력이 아니라 세션에서 결정되어야 한다
    assertThat(saved.getSellerId()).isEqualTo("userA");
    assertThat(saved.getTitle()).isEqualTo("제목");
    assertThat(saved.getPrice()).isEqualTo(10000);
    assertThat(saved.getGrade()).isEqualTo(ProductGrade.A);
  }

  // ---------- 수정 ----------

  @Test
  @DisplayName("수정 폼에는 기존 상품 값이 채워지고 폼 전송용 productId가 함께 담긴다")
  void editForm_fillsExistingValues() {
    Model model = new ConcurrentModel();
    Product product = new Product("userA", "원래제목", "원래설명입니다", 10000, ProductGrade.B);
    given(productService.findByIdAndOwner(1L, "userA")).willReturn(product);

    String view = productController.editForm(loginMember, 1L, model);

    assertThat(view).isEqualTo("product/editForm");
    ProductUpdateForm form = (ProductUpdateForm) model.getAttribute("productUpdateForm");
    assertThat(form.getTitle()).isEqualTo("원래제목");
    assertThat(form.getDescription()).isEqualTo("원래설명입니다");
    assertThat(form.getPrice()).isEqualTo(10000);
    assertThat(form.getGrade()).isEqualTo(ProductGrade.B);
    assertThat(model.getAttribute("productId")).isEqualTo(1L);
  }

  @Test
  @DisplayName("검증 에러로 수정 폼을 다시 보여줄 때도 productId를 모델에 담는다")
  void edit_validationError_keepsProductId() {
    ProductUpdateForm form = updateForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "productUpdateForm");
    bindingResult.rejectValue("title", "NotBlank");
    Model model = new ConcurrentModel();

    String view = productController.edit(loginMember, 1L, form, bindingResult, model);

    assertThat(view).isEqualTo("product/editForm");
    // 폼 action URL을 만들 때 필요하므로 빠지면 화면이 깨진다
    assertThat(model.getAttribute("productId")).isEqualTo(1L);
    verify(productService, never()).editProduct(anyLong(), anyString(), any());
  }

  @Test
  @DisplayName("수정에 성공하면 폼을 DTO로 변환해 서비스에 넘기고 내 상품 목록으로 리다이렉트한다")
  void edit_success() {
    ProductUpdateForm form = updateForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "productUpdateForm");
    Model model = new ConcurrentModel();

    String view = productController.edit(loginMember, 1L, form, bindingResult, model);

    assertThat(view).isEqualTo("redirect:/my-page/products");

    ArgumentCaptor<ProductUpdateDto> captor = ArgumentCaptor.forClass(ProductUpdateDto.class);
    verify(productService).editProduct(eq(1L), eq("userA"), captor.capture());
    ProductUpdateDto dto = captor.getValue();
    assertThat(dto.title()).isEqualTo("새제목");
    assertThat(dto.price()).isEqualTo(20000);
    assertThat(dto.grade()).isEqualTo(ProductGrade.S);
  }

  // ---------- 삭제 ----------

  @Test
  @DisplayName("삭제에 성공하면 요청자의 loginId와 함께 서비스에 위임하고 내 상품 목록으로 리다이렉트한다")
  void delete_success() {
    String view = productController.delete(loginMember, 1L);

    assertThat(view).isEqualTo("redirect:/my-page/products");
    verify(productService).deleteProduct(1L, "userA");
  }

  // ---------- 공통 모델 ----------

  @Test
  @DisplayName("등급 선택지는 정의된 모든 등급을 제공한다")
  void grades() {
    assertThat(productController.grades()).containsExactly(ProductGrade.values());
  }
}
