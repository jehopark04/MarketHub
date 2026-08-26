package used.system.controller.product;

import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import used.system.controller.member.SessionConst;
import used.system.like.LikeService;
import used.system.member.Member;
import used.system.product.Product;
import used.system.product.ProductSearchCond;
import used.system.product.ProductService;

/**
 * 상품 API.
 *
 * <p>조회(GET)는 로그인이 필요 없고 쓰기(POST·PUT·DELETE)는 필요하다. 둘러보다 마음에 들면 가입하는 흐름이라 목록과 상세를 막으면 가입 전 방문자가 볼
 * 것이 없다. 경로 패턴은 HTTP 메서드를 구분하지 못하므로, 조회를 연 뒤 쓰기만 메서드로 다시 막는다. 배선은 WebConfig에 있다.
 *
 * <p>찜 여부를 붙이는 일은 LikeService가 한다. 여기서 두 서비스의 결과를 합치면 "비로그인은 찜이 없는 것으로 친다"는 규칙이 컨트롤러마다 복사된다.
 *
 * <p>소유권 검사도 여기에 없다. ProductService가 던지는 예외를 ApiExceptionHandler가 상태 코드로 바꾼다.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductApiController {

  private final ProductService productService;
  private final LikeService likeService;

  /**
   * BindingResult를 받아두기만 하고 검사하지 않는 것은 의도다. 조회는 잘못된 조건에 실패로 답하지 않는다 - 이 파라미터가 있으면 스프링이 타입 변환 실패에
   * 예외를 던지는 대신 그 필드를 null로 남기고, 필터는 null을 "조건 없음"으로 본다.
   *
   * <p>오래된 링크나 손으로 고친 주소로 /api/products?minPrice=abc가 와도 가격 조건만 빠진 채 나머지로 검색된다. 400으로 끊으면 클라이언트가 그
   * 400을 받아 "무시하고 목록을 열까"를 다시 판단해야 해서, 같은 정책을 두 곳에 두게 된다.
   */
  @GetMapping
  public List<ProductListResponse> list(
      ProductSearchCond cond,
      BindingResult bindingResult,
      @SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) Member loginMember) {

    List<Product> products = productService.search(cond);
    String loginId = loginMember == null ? null : loginMember.getLoginId();

    return likeService.attachLikeStatus(products, loginId).stream()
        .map(ProductListResponse::from)
        .toList();
  }

  /** 없는 상품이면 findById가 ProductNotFoundException을 던지고 ApiExceptionHandler가 404로 바꾼다. */
  @GetMapping("/{productId}")
  public ProductDetailResponse item(@PathVariable Long productId) {
    return ProductDetailResponse.from(productService.findById(productId));
  }

  /**
   * 판매자는 요청 본문이 아니라 세션에서 결정한다. 남의 이름으로 등록되는 걸 막으려면 이 값이 클라이언트가 보낸 데이터에서 오면 안 된다.
   *
   * <p>201에 Location을 붙인다. 방금 만든 상품의 id를 클라이언트가 알아야 상세로 이동하거나 하트를 누를 수 있다.
   */
  @PostMapping
  public ResponseEntity<ProductDetailResponse> create(
      @Validated @RequestBody ProductCreateRequest request,
      @SessionAttribute(name = SessionConst.LOGIN_MEMBER) Member loginMember) {

    Product saved =
        productService.join(
            new Product(
                loginMember.getLoginId(),
                request.title(),
                request.description(),
                request.price(),
                request.grade()));

    return ResponseEntity.created(URI.create("/api/products/" + saved.getId()))
        .body(ProductDetailResponse.from(saved));
  }

  /**
   * 소유권 검사는 여기에 없다. editProduct가 남의 상품이면 ForbiddenException, 없는 상품이면 ProductNotFoundException을 던지고
   * ApiExceptionHandler가 403·404로 바꾼다.
   */
  @PutMapping("/{productId}")
  public ResponseEntity<Void> edit(
      @PathVariable Long productId,
      @Validated @RequestBody ProductUpdateRequest request,
      @SessionAttribute(name = SessionConst.LOGIN_MEMBER) Member loginMember) {

    productService.editProduct(productId, loginMember.getLoginId(), request.toDto());
    return ResponseEntity.noContent().build();
  }

  /** 이미 지워진 상품이면 404다. 멱등이라 보려면 통과시켜야 하지만, 남의 상품인지 가릴 수 없어 소유권 검사를 건너뛰게 된다. */
  @DeleteMapping("/{productId}")
  public ResponseEntity<Void> delete(
      @PathVariable Long productId,
      @SessionAttribute(name = SessionConst.LOGIN_MEMBER) Member loginMember) {

    productService.deleteProduct(productId, loginMember.getLoginId());
    return ResponseEntity.noContent().build();
  }
}
