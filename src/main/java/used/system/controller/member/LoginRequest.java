package used.system.controller.member;

import jakarta.validation.constraints.NotBlank;

/** 로그인 요청 본문. */
public record LoginRequest(
    @NotBlank(message = "아이디를 입력하세요.") String loginId,
    @NotBlank(message = "비밀번호를 입력하세요.") String password) {}
