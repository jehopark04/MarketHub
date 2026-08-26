package used.system.controller.member;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 비밀번호와 비밀번호 확인이 같은지 검사한다.
 *
 * <p>두 필드를 비교해야 해서 필드 하나에 붙는 어노테이션으로는 표현할 수 없다. 그래서 타입(클래스) 레벨이다.
 *
 * <p>컨트롤러에서 두 값을 직접 비교하지 않고 어노테이션에 맡기는 이유는 에러 응답 형식 때문이다. 컨트롤러에서 따로 처리하면 이 오류만 다른 모양의 400이 나가,
 * 클라이언트가 실패 처리를 두 벌 만들어야 한다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordMatchValidator.class)
public @interface PasswordMatch {

  String message() default "비밀번호가 일치하지 않습니다.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
