package used.system.controller.member;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import used.system.exception.LoginFailedException;
import used.system.member.Member;
import used.system.member.MemberService;

/**
 * 회원 API - 가입, 로그인, 로그아웃.
 *
 * <p>클래스 레벨 @RequestMapping이 없다. /api/members와 /api/login이 한 접두사로 묶이지 않아서다.
 *
 * <p>셋 다 로그인 없이 되어야 한다 - 가입하거나 로그인하려는 사람에게는 세션이 없고, 로그아웃은 세션이 없으면 할 일이 없다. 여는 경로는 WebConfig에 있다.
 *
 * <p>인증은 세션이다. JS 프론트가 같은 서버에서 서빙되면 쿠키가 자동으로 실려 나가 그대로 동작한다. 토큰으로 바꾸는 것은 별개의 작업이라, 한꺼번에 하면 무엇이 깨졌는지
 * 가릴 수 없다.
 */
@RestController
@RequiredArgsConstructor
public class MemberApiController {

  private final MemberService memberService;

  /**
   * 가입과 동시에 로그인 상태로 만든다. 가입만 시키고 로그인을 따로 요청하게 하면 방금 정한 평문 비밀번호가 두 번 오간다 - 회원가입에서 passwordConfirm을 뺀
   * 것과 같은 이유로 피한다. 두 단계로 나누면 원자적이지도 않다: 가입은 됐는데 로그인만 실패하면 되돌릴 수 없다.
   *
   * <p>Location을 붙이지 않는다. 201에 권장되는 헤더지만 가리킬 경로가 없다 - 남의 회원 정보를 조회하는 API가 없고, 만들 이유도 없다. 없는 경로를
   * 가리키는 것보다 빼는 편이 낫다.
   *
   * <p>중복 아이디를 여기서 잡지 않는다. 예외를 상태 코드로 옮기는 일은 ApiExceptionHandler에 모아두기로 한 규약이고, 여기서 잡으면 그 통로가 막힌다.
   */
  @PostMapping("/api/members")
  public ResponseEntity<MemberResponse> join(
      @Validated @RequestBody MemberJoinRequest request, HttpServletRequest servletRequest) {

    Member saved = memberService.join(request.toMember());
    startSession(servletRequest, saved);
    return ResponseEntity.status(HttpStatus.CREATED).body(MemberResponse.from(saved));
  }

  /**
   * 성공하면 200에 회원 정보. 세션 쿠키는 응답 헤더에 실려 나가므로 본문에 담을 것이 따로 없다.
   *
   * <p>세션을 새로 발급하는 이유는 startSession에 적혀 있다.
   */
  @PostMapping("/api/login")
  public MemberResponse login(
      @Validated @RequestBody LoginRequest request, HttpServletRequest servletRequest) {

    Member member = memberService.login(request.loginId(), request.password());
    if (member == null) {
      throw new LoginFailedException("아이디 또는 비밀번호가 맞지 않습니다.");
    }

    startSession(servletRequest, member);
    return MemberResponse.from(member);
  }

  /**
   * 로그인 상태로 만든다. 가입과 로그인이 함께 쓴다.
   *
   * <p>기존 세션을 버리고 새로 발급하는 것이 중요하다. 남이 심어둔 세션 id를 그대로 쓰면, 인증되는 순간 그 세션이 로그인 상태가 되어 심은 쪽이 함께 들어온다(세션
   * 고정). 인자 없는 getSession()은 기존 세션을 그대로 승격시키므로 쓰지 않는다.
   */
  private void startSession(HttpServletRequest servletRequest, Member member) {
    HttpSession previous = servletRequest.getSession(false);
    if (previous != null) {
      previous.invalidate();
    }
    servletRequest.getSession(true).setAttribute(SessionConst.LOGIN_MEMBER, member);
  }

  /** 세션이 없어도 204다. "로그아웃된 상태로 만들어라"는 요청이므로 몇 번을 보내든 결과가 같다. */
  @PostMapping("/api/logout")
  public ResponseEntity<Void> logout(HttpServletRequest servletRequest) {
    HttpSession session = servletRequest.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    return ResponseEntity.noContent().build();
  }
}
