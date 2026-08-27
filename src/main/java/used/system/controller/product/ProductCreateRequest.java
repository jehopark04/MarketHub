package used.system.controller.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import used.system.product.ProductGrade;

/**
 * 상품 등록 요청 본문.
 *
 * <p>sellerId가 없는 것은 의도다. 판매자는 세션에서 정한다 - 클라이언트가 보낸 값을 쓰면 남의 이름으로 등록할 수 있다.
 *
 * <p>record라 setter가 없다. 요청 본문은 읽기만 하면 되고, 값이 도중에 바뀌지 않는 편이 안전하다.
 *
 * <p>가격은 0원을 허용한다(@Positive가 아니라 @PositiveOrZero). 무료로 나누는 것도 중고거래에서 흔한 일이라 막을 이유가 없다. 음수만 막는다.
 *
 * <p>price가 int가 아니라 Integer인 이유: 필드를 빼고 보내면 int는 역직렬화 단계에서 터져 @NotNull 메시지가 나갈 기회조차 없다.
 */
public record ProductCreateRequest(
    @NotBlank(message = "제목을 입력하세요.") @Size(max = 30, message = "30자 이내로 입력하셔야 합니다.") String title,
    @Size(max = 250, message = "250자 이내로 입력해주세요.") String description,
    @NotNull(message = "가격을 입력해주셔야 합니다.") @PositiveOrZero(message = "가격은 0원 이상이어야 합니다.")
        Integer price,
    @NotNull(message = "등급을 선택해주세요.") ProductGrade grade) {}
