package used.system.controller.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import used.system.product.ProductGrade;
import used.system.product.ProductUpdateDto;

/**
 * 상품 수정 요청 본문.
 *
 * <p>일부가 아니라 네 필드를 전부 새 값으로 갈아끼운다(Product.update가 그렇게 생겼다). 그래서 PATCH가 아니라 PUT이다.
 */
public record ProductUpdateRequest(
    @NotBlank(message = "제목을 입력하세요.") @Size(max = 30, message = "30자 이내로 입력하셔야 합니다.") String title,
    @Size(max = 250, message = "250자 이내로 입력해주세요.") String description,
    @NotNull(message = "가격을 입력해주셔야 합니다.") @PositiveOrZero(message = "가격은 0원 이상이어야 합니다.")
        Integer price,
    @NotNull(message = "등급을 선택해주세요.") ProductGrade grade) {

  ProductUpdateDto toDto() {
    return new ProductUpdateDto(title, description, price, grade);
  }
}
