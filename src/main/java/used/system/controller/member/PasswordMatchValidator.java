package used.system.controller.member;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;

/** {@link PasswordMatch}의 실제 검사. */
public class PasswordMatchValidator
    implements ConstraintValidator<PasswordMatch, MemberJoinRequest> {

  @Override
  public boolean isValid(MemberJoinRequest request, ConstraintValidatorContext context) {
    // 둘 다 비어 있는 경우는 @NotBlank가 이미 잡는다. 여기서 또 잡으면 한 번의 실수에 메시지가 셋이 된다.
    if (request.password() == null || request.passwordConfirm() == null) {
      return true;
    }
    if (Objects.equals(request.password(), request.passwordConfirm())) {
      return true;
    }

    // 클래스 레벨 위반은 기본적으로 어느 필드의 것도 아니다. passwordConfirm에 매달아야
    // 응답의 errors가 다른 검증 실패와 같은 모양이 되고, 화면이 그 입력칸 아래에 표시할 수 있다.
    context.disableDefaultConstraintViolation();
    context
        .buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
        .addPropertyNode("passwordConfirm")
        .addConstraintViolation();
    return false;
  }
}
