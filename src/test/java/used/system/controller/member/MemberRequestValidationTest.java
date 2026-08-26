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
 * 회원 관련 요청 본문(MemberJoinRequest, LoginRequest)의 Bean Validation 단위 테스트.
 *
 * <p>비밀번호 확인은 화면 쪽에서 컨트롤러가 손으로 하던 검사다. 어노테이션으로 옮겼으니 그 검사가 실제로 도는지, 그리고 위반이 passwordConfirm 필드에 붙는지를
 * 여기서 고정한다 - 필드에 안 붙으면 응답 errors가 비어 클라이언트가 어디를 고쳐야 할지 알 수 없다.
 */
class MemberRequestValidationTest {

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
        .collect(java.util.stream.Collectors.toSet());
  }

  @Test
  @DisplayName("모든 값이 올바르면 위반이 없다")
  void valid() {
    assertThat(validator.validate(new MemberJoinRequest("userA", "에이", "password1", "password1")))
        .isEmpty();
  }

  @Test
  @DisplayName("비밀번호가 일치하지 않으면 passwordConfirm에 위반이 붙는다")
  void passwordMismatch() {
    Set<ConstraintViolation<MemberJoinRequest>> violations =
        validator.validate(new MemberJoinRequest("userA", "에이", "password1", "password2"));

    assertThat(violations).hasSize(1);
    ConstraintViolation<MemberJoinRequest> violation = violations.iterator().next();
    assertThat(violation.getPropertyPath()).hasToString("passwordConfirm");
    assertThat(violation.getMessage()).isEqualTo("비밀번호가 일치하지 않습니다.");
  }

  @Test
  @DisplayName("비밀번호가 비어 있으면 불일치 위반을 겹쳐 내지 않는다")
  void blankPasswordDoesNotAddMismatch() {
    // NotBlank가 이미 말하는 것을 또 말하면 한 번의 실수에 메시지가 여러 개 나간다.
    assertThat(위반_필드(new MemberJoinRequest("userA", "에이", null, null)))
        .containsExactlyInAnyOrder("password", "passwordConfirm");
  }

  @Test
  @DisplayName("비밀번호가 8자 미만이면 위반이다")
  void shortPassword() {
    assertThat(위반_필드(new MemberJoinRequest("userA", "에이", "pass1", "pass1")))
        .containsExactly("password");
  }

  @Test
  @DisplayName("모든 값이 비어 있으면 네 필드 모두 위반이 발생한다")
  void allBlank() {
    assertThat(위반_필드(new MemberJoinRequest("", "", "", "")))
        .containsExactlyInAnyOrder("loginId", "name", "password", "passwordConfirm");
  }

  // ---------- 로그인 ----------

  @Test
  @DisplayName("로그인 요청은 두 값이 모두 있으면 위반이 없다")
  void loginRequest_valid() {
    assertThat(validator.validate(new LoginRequest("userA", "password1"))).isEmpty();
  }

  @Test
  @DisplayName("로그인 요청의 값이 비어 있으면 두 필드 모두 위반이다")
  void loginRequest_allBlank() {
    assertThat(위반_필드(new LoginRequest("", ""))).containsExactlyInAnyOrder("loginId", "password");
  }
}
