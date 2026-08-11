package used.system.controller.product;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.metadata.ConstraintDescriptor;
import jakarta.validation.metadata.PropertyDescriptor;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import used.system.product.ProductGrade;
import used.system.product.ProductUpdateDto;

/**
 * 상품 관련 폼(ProductForm, ProductUpdateForm)의 Bean Validation 어노테이션 단위 테스트.
 *
 * <p>스프링 없이 Validator를 직접 만들어 검증한다. 개별 테스트는 각 폼을 따로 확인하고, 두 폼의 규칙이 어긋나는지는 마지막의 제약 메타데이터 대조 테스트가
 * 담당한다.
 */
class ProductFormValidationTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    factory.close();
  }

  private ProductForm productForm(String title, String description, Integer price, ProductGrade g) {
    ProductForm form = new ProductForm();
    form.setTitle(title);
    form.setDescription(description);
    form.setPrice(price);
    form.setGrade(g);
    return form;
  }

  private ProductUpdateForm updateForm(
      String title, String description, Integer price, ProductGrade g) {
    ProductUpdateForm form = new ProductUpdateForm();
    form.setTitle(title);
    form.setDescription(description);
    form.setPrice(price);
    form.setGrade(g);
    return form;
  }

  // ---------- 등록 폼 ----------

  @Test
  @DisplayName("모든 값이 올바르면 위반이 없다")
  void productForm_valid() {
    assertThat(validator.validate(productForm("제목", "설명입니다", 10000, ProductGrade.A))).isEmpty();
  }

  @Test
  @DisplayName("제목이 비어 있으면 위반이 발생한다")
  void productForm_blankTitle() {
    assertThat(validator.validate(productForm("", "설명입니다", 10000, ProductGrade.A)))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("title");
  }

  @Test
  @DisplayName("제목이 30자를 넘으면 위반이 발생한다")
  void productForm_tooLongTitle() {
    assertThat(validator.validate(productForm("제".repeat(31), "설명입니다", 10000, ProductGrade.A)))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("title");
  }

  @Test
  @DisplayName("제목이 정확히 30자면 위반이 없다 (경계값)")
  void productForm_titleBoundary() {
    assertThat(validator.validate(productForm("제".repeat(30), "설명입니다", 10000, ProductGrade.A)))
        .isEmpty();
  }

  @Test
  @DisplayName("가격이 없으면 위반이 발생한다")
  void productForm_nullPrice() {
    assertThat(validator.validate(productForm("제목", "설명입니다", null, ProductGrade.A)))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("price");
  }

  @Test
  @DisplayName("등급이 선택되지 않으면 위반이 발생한다")
  void productForm_nullGrade() {
    assertThat(validator.validate(productForm("제목", "설명입니다", 10000, null)))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("grade");
  }

  @Test
  @DisplayName("설명이 250자를 넘으면 위반이 발생한다")
  void productForm_tooLongDescription() {
    assertThat(validator.validate(productForm("제목", "설".repeat(251), 10000, ProductGrade.A)))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("description");
  }

  @Test
  @DisplayName("설명은 선택 항목이라 비어 있어도 위반이 없다")
  void productForm_blankDescriptionAllowed() {
    assertThat(validator.validate(productForm("제목", "", 10000, ProductGrade.A))).isEmpty();
  }

  // ---------- 수정 폼 ----------

  @Test
  @DisplayName("수정 폼도 모든 값이 올바르면 위반이 없다")
  void updateForm_valid() {
    assertThat(validator.validate(updateForm("제목", "설명입니다", 10000, ProductGrade.A))).isEmpty();
  }

  @Test
  @DisplayName("수정 폼도 값이 비면 등록 폼과 같은 필드에서 위반이 발생한다")
  void updateForm_blankValues() {
    assertThat(validator.validate(updateForm("", "설명입니다", null, null)))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("title", "price", "grade");
  }

  /**
   * 두 폼의 제약을 실제로 대조한다. 위의 개별 테스트들은 각 폼을 따로 검증할 뿐이라, 한쪽에만 제약이 추가·삭제되거나 max 값이 달라지는 어긋남은 잡지 못한다. 여기서는
   * Bean Validation 메타데이터를 꺼내 어노테이션 종류와 속성(max, message 등)까지 비교한다.
   *
   * <p>두 폼이 의도적으로 달라져야 한다면 이 테스트를 지우거나 기대값을 바꾸는 판단이 필요하다 — 조용히 어긋나는 것을 막는 것이 목적이다.
   */
  @Test
  @DisplayName("등록 폼과 수정 폼의 검증 규칙은 완전히 동일하다")
  void createForm_andUpdateForm_haveIdenticalConstraints() {
    assertThat(constraintsOf(ProductUpdateForm.class))
        .as("수정 폼의 제약이 등록 폼과 어긋났다")
        .isEqualTo(constraintsOf(ProductForm.class));
  }

  /** 필드명 -> 그 필드에 걸린 제약들의 설명 집합. */
  private static Map<String, Set<String>> constraintsOf(Class<?> formType) {
    Map<String, Set<String>> byProperty = new TreeMap<>();
    for (PropertyDescriptor property :
        validator.getConstraintsForClass(formType).getConstrainedProperties()) {
      Set<String> descriptions = new TreeSet<>();
      for (ConstraintDescriptor<?> constraint : property.getConstraintDescriptors()) {
        descriptions.add(describe(constraint));
      }
      byProperty.put(property.getPropertyName(), descriptions);
    }
    return byProperty;
  }

  /** 예: {@code Size(max=30, message=30자 이내로..., min=0)} */
  private static String describe(ConstraintDescriptor<?> constraint) {
    String attributes =
        constraint.getAttributes().entrySet().stream()
            .filter(e -> !e.getKey().equals("groups") && !e.getKey().equals("payload"))
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(", ", "(", ")"));
    return constraint.getAnnotation().annotationType().getSimpleName() + attributes;
  }

  @Test
  @DisplayName("toDto는 폼의 값을 그대로 DTO로 옮긴다")
  void updateForm_toDto() {
    ProductUpdateForm form = updateForm("제목", "설명입니다", 10000, ProductGrade.S);

    ProductUpdateDto dto = form.toDto();

    assertThat(dto.title()).isEqualTo("제목");
    assertThat(dto.description()).isEqualTo("설명입니다");
    assertThat(dto.price()).isEqualTo(10000);
    assertThat(dto.grade()).isEqualTo(ProductGrade.S);
  }
}
