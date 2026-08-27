package used.system.controller.member;

import static org.assertj.core.api.Assertions.assertThat;

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
 * <p>비밀번호 확인 일치 검사는 여기 없다. 서버가 그 필드를 받지 않기로 했다 - 이유는 MemberJoinRequest에 적혀 있다.
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
    assertThat(validator.validate(new MemberJoinRequest("userA", "에이", "password1"))).isEmpty();
  }

  @Test
  @DisplayName("비밀번호가 8자 미만이면 위반이다")
  void shortPassword() {
    assertThat(위반_필드(new MemberJoinRequest("userA", "에이", "pass1"))).containsExactly("password");
  }

  @Test
  @DisplayName("모든 값이 비어 있으면 세 필드 모두 위반이 발생한다")
  void allBlank() {
    assertThat(위반_필드(new MemberJoinRequest("", "", "")))
        .containsExactlyInAnyOrder("loginId", "name", "password");
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
