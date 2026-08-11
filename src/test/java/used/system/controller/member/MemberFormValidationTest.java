package used.system.controller.member;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 회원 관련 폼(MemberForm, LoginForm)의 Bean Validation 어노테이션 단위 테스트.
 *
 * <p>스프링 없이 Validator를 직접 만들어 검증한다. 어노테이션을 빠뜨리거나 타입에 맞지 않는 어노테이션을 붙인 실수를 여기서 잡는다.
 */
class MemberFormValidationTest {

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

  private MemberForm memberForm(String loginId, String name, String password, String confirm) {
    MemberForm form = new MemberForm();
    form.setLoginId(loginId);
    form.setName(name);
    form.setPassword(password);
    form.setPasswordConfirm(confirm);
    return form;
  }

  @Test
  @DisplayName("모든 값이 올바르면 위반이 없다")
  void memberForm_valid() {
    MemberForm form = memberForm("userA", "에이", "password1", "password1");

    assertThat(validator.validate(form)).isEmpty();
  }

  @Test
  @DisplayName("모든 값이 비어 있으면 네 필드 모두 위반이 발생한다")
  void memberForm_allBlank() {
    MemberForm form = memberForm("", "", "", "");

    Set<ConstraintViolation<MemberForm>> violations = validator.validate(form);

    assertThat(violations)
        .extracting(v -> v.getPropertyPath().toString())
        .contains("loginId", "name", "password", "passwordConfirm");
  }

  @Test
  @DisplayName("공백만 입력해도 필수값 위반으로 처리된다")
  void memberForm_whitespaceOnly() {
    MemberForm form = memberForm("   ", "   ", "password1", "password1");

    assertThat(validator.validate(form))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("loginId", "name");
  }

  @Test
  @DisplayName("비밀번호가 8자 미만이면 위반이 발생한다")
  void memberForm_shortPassword() {
    MemberForm form = memberForm("userA", "에이", "pass123", "pass123"); // 7자

    assertThat(validator.validate(form))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("password");
  }

  @Test
  @DisplayName("비밀번호가 8자면 위반이 없다 (경계값)")
  void memberForm_passwordBoundary() {
    MemberForm form = memberForm("userA", "에이", "pass1234", "pass1234"); // 8자

    assertThat(validator.validate(form)).isEmpty();
  }

  @Test
  @DisplayName("로그인 폼에 아이디와 비밀번호가 있으면 위반이 없다")
  void loginForm_valid() {
    assertThat(validator.validate(new LoginForm("userA", "password1"))).isEmpty();
  }

  @Test
  @DisplayName("로그인 폼이 비어 있으면 두 필드 모두 위반이 발생한다")
  void loginForm_blank() {
    assertThat(validator.validate(new LoginForm("", "")))
        .extracting(v -> v.getPropertyPath().toString())
        .containsExactlyInAnyOrder("loginId", "password");
  }
}
