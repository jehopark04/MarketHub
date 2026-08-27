package used.system.controller.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import used.system.exception.ForbiddenException;
import used.system.exception.ProductNotFoundException;
import used.system.like.LikeService;
import used.system.like.ProductLikeStatus;
import used.system.member.Member;
import used.system.product.Product;
import used.system.product.ProductGrade;
import used.system.product.ProductSearchCond;
import used.system.product.ProductService;
import used.system.product.ProductUpdateDto;

/**
 * ProductApiController 단위 테스트 - 컨트롤러 메서드를 직접 호출한다.
 *
 * <p>찜 여부를 어떻게 판정하는지는 LikeServiceImplTest가 본다. 여기 서비스는 목이라 같은 것을 다시 확인해도 아무것도 증명하지 못한다.
 *
 * <p>여기서 볼 것은 셋이다. 세션에서 꺼낸 loginId를 그대로 넘기는가, 서비스 결과를 DTO로 옮기며 필드를 흘리지 않는가, 서비스의 예외를 삼키지 않는가.
 *
 * <p>BindingResult에 null을 넘기는 것은 의도다. 이 파라미터는 스프링이 바인딩 실패를 예외 대신 기록하게 만드는 용도라 컨트롤러 코드가 읽지 않는다.
 */
@ExtendWith(MockitoExtension.class)
class ProductApiControllerTest {

  @Mock private ProductService productService;

  @Mock private LikeService likeService;

  @InjectMocks private ProductApiController productApiController;

  private final Member loginMember = new Member("userA", "에이", "password1");

  private Product product(Long id) {
    Product product = new Product("sellerX", "제목", "설명입니다", 1000, ProductGrade.A);
    product.setId(id);
    return product;
  }

  @Test
  @DisplayName("검색 결과에 찜 여부를 붙여 DTO로 반환한다")
  void list_attachesLikeStatus() {
    ProductSearchCond cond = new ProductSearchCond();
    Product liked = product(1L);
    Product notLiked = product(2L);

    given(productService.search(cond)).willReturn(List.of(liked, notLiked));
    given(likeService.attachLikeStatus(List.of(liked, notLiked), "userA"))
        .willReturn(
            List.of(new ProductLikeStatus(liked, true), new ProductLikeStatus(notLiked, false)));

    List<ProductListResponse> result = productApiController.list(cond, null, loginMember);

    assertThat(result)
        .containsExactly(
            new ProductListResponse(1L, "제목", 1000, ProductGrade.A, true),
            new ProductListResponse(2L, "제목", 1000, ProductGrade.A, false));
  }

  @Test
  @DisplayName("비로그인이면 loginId 자리에 null을 넘긴다")
  void list_anonymousPassesNull() {
    ProductSearchCond cond = new ProductSearchCond();
    Product product = product(1L);

    given(productService.search(cond)).willReturn(List.of(product));
    given(likeService.attachLikeStatus(List.of(product), null))
        .willReturn(List.of(new ProductLikeStatus(product, false)));

    List<ProductListResponse> result = productApiController.list(cond, null, null);

    // 세션이 없는데 loginMember.getLoginId()를 부르면 NPE다. 비로그인 처리는 서비스에 맡긴다.
    verify(likeService).attachLikeStatus(List.of(product), null);
    assertThat(result).extracting(ProductListResponse::liked).containsExactly(false);
  }

  @Test
  @DisplayName("상세는 설명과 판매자까지 담아 반환한다")
  void item_returnsDetail() {
    given(productService.findById(1L)).willReturn(product(1L));
    given(likeService.isLiked(1L, "userA")).willReturn(false);

    ProductDetailResponse result = productApiController.item(1L, loginMember);

    assertThat(result)
        .isEqualTo(
            new ProductDetailResponse(1L, "제목", "설명입니다", 1000, ProductGrade.A, "sellerX", false));
  }

  @Test
  @DisplayName("찜한 상품을 조회하면 liked가 true다")
  void item_liked() {
    given(productService.findById(1L)).willReturn(product(1L));
    given(likeService.isLiked(1L, "userA")).willReturn(true);

    assertThat(productApiController.item(1L, loginMember).liked()).isTrue();
  }

  @Test
  @DisplayName("비로그인 상세 조회는 loginId 없이 물어 liked가 false다")
  void item_notLoggedIn() {
    given(productService.findById(1L)).willReturn(product(1L));
    given(likeService.isLiked(1L, null)).willReturn(false);

    // 세션이 없는데 loginMember.getLoginId()를 부르면 NPE다. 비로그인 판정은 서비스에 맡긴다.
    assertThat(productApiController.item(1L, null).liked()).isFalse();
    verify(likeService).isLiked(1L, null);
  }

  @Test
  @DisplayName("없는 상품이면 서비스의 예외를 그대로 통과시킨다")
  void item_propagatesNotFound() {
    // 컨트롤러가 try-catch로 삼키면 ApiExceptionHandler에 닿지 않아 404가 나가지 않는다.
    given(productService.findById(99L)).willThrow(new ProductNotFoundException("상품을 찾을 수 없습니다."));

    assertThatThrownBy(() -> productApiController.item(99L, loginMember))
        .isInstanceOf(ProductNotFoundException.class);
  }

  // ---------- 등록 ----------

  @Test
  @DisplayName("판매자를 세션 회원으로 채워 저장하고 201에 Location을 붙인다")
  void create_usesSessionAsSeller() {
    ProductCreateRequest request = new ProductCreateRequest("제목", "설명입니다", 1000, ProductGrade.A);
    given(productService.join(any(Product.class))).willReturn(product(7L));

    ResponseEntity<ProductDetailResponse> response =
        productApiController.create(request, loginMember);

    // 요청 본문에 sellerId가 없는 것이 방어의 절반이고, 세션 값을 쓰는 것이 나머지 절반이다.
    ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
    verify(productService).join(captor.capture());
    assertThat(captor.getValue().getSellerId()).isEqualTo("userA");
    assertThat(captor.getValue().getTitle()).isEqualTo("제목");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getHeaders().getLocation()).hasToString("/api/products/7");
    assertThat(response.getBody().productId()).isEqualTo(7L);
  }

  // ---------- 수정 ----------

  @Test
  @DisplayName("수정은 세션 loginId와 함께 위임하고 204를 반환한다")
  void edit_delegates() {
    ProductUpdateRequest request = new ProductUpdateRequest("새제목", "새설명입니다", 2000, ProductGrade.S);

    ResponseEntity<Void> response = productApiController.edit(1L, request, loginMember);

    verify(productService)
        .editProduct(1L, "userA", new ProductUpdateDto("새제목", "새설명입니다", 2000, ProductGrade.S));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(response.getBody()).isNull();
  }

  @Test
  @DisplayName("남의 상품 수정이면 서비스의 예외를 그대로 통과시킨다")
  void edit_propagatesForbidden() {
    ProductUpdateRequest request = new ProductUpdateRequest("새제목", "새설명입니다", 2000, ProductGrade.S);
    willThrow(new ForbiddenException("본인 상품만 접근할 수 있습니다."))
        .given(productService)
        .editProduct(eq(1L), eq("userA"), any(ProductUpdateDto.class));

    assertThatThrownBy(() -> productApiController.edit(1L, request, loginMember))
        .isInstanceOf(ForbiddenException.class);
  }

  // ---------- 삭제 ----------

  @Test
  @DisplayName("삭제는 세션 loginId와 함께 위임하고 204를 반환한다")
  void delete_delegates() {
    ResponseEntity<Void> response = productApiController.delete(1L, loginMember);

    // 소유권 검사는 서비스 몫이다. 컨트롤러가 productId만 넘기면 남의 상품도 지워진다.
    verify(productService).deleteProduct(1L, "userA");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  @DisplayName("남의 상품 삭제면 서비스의 예외를 그대로 통과시킨다")
  void delete_propagatesForbidden() {
    willThrow(new ForbiddenException("본인 상품만 접근할 수 있습니다."))
        .given(productService)
        .deleteProduct(1L, "userA");

    assertThatThrownBy(() -> productApiController.delete(1L, loginMember))
        .isInstanceOf(ForbiddenException.class);
  }
}
