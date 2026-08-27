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
 * 상품 요청 본문(ProductCreateRequest, ProductUpdateRequest)의 Bean Validation 단위 테스트.
 *
 * <p>스프링 없이 Validator를 직접 만들어 검증한다. 개별 테스트는 각 요청을 따로 확인하고, 둘의 규칙이 어긋나는지는 제약 메타데이터 대조 테스트가 담당한다 -
 * 한쪽에만 어노테이션을 더하거나 메시지를 고치는 실수는 값 몇 개를 넣어보는 것으로는 드러나지 않는다.
 */
class ProductRequestValidationTest {

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

  private Set<String> 위반_필드(Object request) {
    return validator.validate(request).stream()
        .map(violation -> violation.getPropertyPath().toString())
        .collect(Collectors.toSet());
  }

  private ProductCreateRequest create(String title, String description, Integer price) {
    return new ProductCreateRequest(title, description, price, ProductGrade.A);
  }

  @Test
  @DisplayName("모든 값이 올바르면 위반이 없다")
  void valid() {
    assertThat(validator.validate(create("제목", "설명입니다", 10000))).isEmpty();
  }

  @Test
  @DisplayName("제목이 비어 있으면 위반이 발생한다")
  void blankTitle() {
    assertThat(위반_필드(create("", "설명입니다", 10000))).containsExactly("title");
  }

  @Test
  @DisplayName("제목이 30자를 넘으면 위반이 발생한다")
  void tooLongTitle() {
    assertThat(위반_필드(create("가".repeat(31), "설명입니다", 10000))).containsExactly("title");
  }

  @Test
  @DisplayName("제목이 정확히 30자면 위반이 없다 (경계값)")
  void titleAtBoundary() {
    assertThat(위반_필드(create("가".repeat(30), "설명입니다", 10000))).isEmpty();
  }

  @Test
  @DisplayName("가격이 없으면 위반이 발생한다")
  void nullPrice() {
    assertThat(위반_필드(create("제목", "설명입니다", null))).containsExactly("price");
  }

  @Test
  @DisplayName("가격이 음수면 위반이 발생한다")
  void negativePrice() {
    assertThat(위반_필드(create("제목", "설명입니다", -1))).containsExactly("price");
  }

  @Test
  @DisplayName("가격이 0이면 위반이 없다 (경계값)")
  void zeroPrice() {
    // 무료로 나누는 것도 중고거래에서 흔한 일이라 막지 않는다. @Positive였다면 여기서 깨진다.
    assertThat(위반_필드(create("제목", "설명입니다", 0))).isEmpty();
  }

  @Test
  @DisplayName("등급이 선택되지 않으면 위반이 발생한다")
  void nullGrade() {
    assertThat(위반_필드(new ProductCreateRequest("제목", "설명입니다", 10000, null)))
        .containsExactly("grade");
  }

  @Test
  @DisplayName("설명이 250자를 넘으면 위반이 발생한다")
  void tooLongDescription() {
    assertThat(위반_필드(create("제목", "가".repeat(251), 10000))).containsExactly("description");
  }

  @Test
  @DisplayName("설명은 선택 항목이라 비어 있어도 위반이 없다")
  void blankDescriptionIsAllowed() {
    assertThat(위반_필드(create("제목", "", 10000))).isEmpty();
  }

  @Test
  @DisplayName("수정 요청도 모든 값이 올바르면 위반이 없다")
  void updateRequest_valid() {
    assertThat(validator.validate(new ProductUpdateRequest("제목", "설명입니다", 10000, ProductGrade.S)))
        .isEmpty();
  }

  @Test
  @DisplayName("수정 요청도 값이 비면 등록 요청과 같은 필드에서 위반이 발생한다")
  void updateRequest_blank() {
    assertThat(위반_필드(new ProductUpdateRequest("", "설명입니다", null, null)))
        .containsExactlyInAnyOrder("title", "price", "grade");
  }

  /**
   * 등록과 수정은 같은 규칙이어야 한다. 한쪽에만 어노테이션을 더하거나 메시지를 고치면 같은 입력이 등록에서는 통과하고 수정에서는 막힌다.
   *
   * <p>값을 넣어보는 방식으로는 "빠뜨린 제약"을 잡을 수 없어, 제약 메타데이터 자체를 통째로 견준다.
   */
  @Test
  @DisplayName("등록 요청과 수정 요청의 검증 규칙은 완전히 동일하다")
  void createRequest_andUpdateRequest_haveIdenticalConstraints() {
    assertThat(constraintsOf(ProductUpdateRequest.class))
        .as("수정 요청의 제약이 등록 요청과 어긋났다")
        .isEqualTo(constraintsOf(ProductCreateRequest.class));
  }

  /** 필드명 -> 그 필드에 걸린 제약들의 설명 집합. */
  private static Map<String, Set<String>> constraintsOf(Class<?> requestType) {
    Map<String, Set<String>> byProperty = new TreeMap<>();
    for (PropertyDescriptor property :
        validator.getConstraintsForClass(requestType).getConstrainedProperties()) {
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
  @DisplayName("toDto는 요청의 값을 그대로 DTO로 옮긴다")
  void updateRequest_toDto() {
    ProductUpdateDto dto = new ProductUpdateRequest("제목", "설명입니다", 10000, ProductGrade.S).toDto();

    assertThat(dto.title()).isEqualTo("제목");
    assertThat(dto.description()).isEqualTo("설명입니다");
    assertThat(dto.price()).isEqualTo(10000);
    assertThat(dto.grade()).isEqualTo(ProductGrade.S);
  }
}
