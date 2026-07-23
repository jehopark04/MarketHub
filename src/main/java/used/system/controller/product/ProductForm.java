package used.system.controller.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import used.system.product.ProductGrade;

@Setter
@Getter
public class ProductForm {

  @NotBlank(message = "제목을 입력하세요.")
  @Size(max = 30, message = "30자 이내로 입력하셔야 합니다. ")
  private String title;

  @Size(max = 250, message = "250자 이내로 입력해주세요.")
  private String description;

  @NotNull(message = "가격을 입력해주셔야 합니다.")
  private Integer price; // int는 null이 될수 없다. -> TypeMismatchException이 발생함 그러니 Integer가 들어가야함

  @NotNull private ProductGrade grade;

  public ProductForm() {}

  public ProductForm(String title, String description, int price, ProductGrade grade) {
    this.title = title;
    this.description = description;
    this.price = price;
    this.grade = grade;
  }
}
