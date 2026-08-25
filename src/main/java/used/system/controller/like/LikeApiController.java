package used.system.controller.like;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import used.system.controller.member.SessionConst;
import used.system.like.LikeService;
import used.system.member.Member;

/**
 * 찜하기 / 찜 취소 API.
 *
 * <p>경로를 상품이 아니라 "나"에 매달았다(/api/me/likes/{productId}). 찜은 상품의 속성이 아니라 회원과 상품의 관계이고, LikeRepository의
 * 조회가 전부 memberId 기준이라 소유자는 회원 쪽이다. 나중에 찜 목록 조회(GET /api/me/likes)까지 한 경로로 묶인다.
 *
 * <p>PUT과 DELETE를 쓴다. 둘 다 멱등이라 같은 요청이 두 번 도착해도 결과가 같다 — LikeService가 이미 중복 찜과 없는 찜 취소를 조용히 통과시키므로
 * 서비스 계층은 그대로 쓴다. HTML 폼이 GET/POST만 보낼 수 있어 POST 두 개로 나눴던 SSR 쪽과 달리, 여기서는 그 제약이 없다.
 *
 * <p>반환 타입이 ResponseEntity인 이유는 테스트 때문이다. @ResponseStatus는 스프링 디스패처가 실행돼야 적용되는 메타데이터라, 컨트롤러 메서드를 직접
 * 호출하는 이 프로젝트의 단위테스트로는 상태 코드를 검증할 수 없다. ResponseEntity는 반환값 자체라 검증된다.
 *
 * <p>로그인 검사는 여기에 없다. ApiLoginCheckInterceptor가 컨트롤러에 닿기 전에 401로 끊는다.
 */
@RestController
@RequestMapping("/api/me/likes")
@RequiredArgsConstructor
public class LikeApiController {

  private final LikeService likeService;

  /** 이미 찜한 상품이어도 204. "찜된 상태로 만들어라"는 요청이므로 몇 번을 보내든 결과가 같다. */
  @PutMapping("/{productId}")
  public ResponseEntity<Void> like(
      @PathVariable Long productId,
      @SessionAttribute(name = SessionConst.LOGIN_MEMBER) Member loginMember) {

    likeService.like(productId, loginMember.getLoginId());
    return ResponseEntity.noContent().build();
  }

  /** 찜하지 않은 상품이어도 204. 최종 상태가 "찜 없음"으로 같다. */
  @DeleteMapping("/{productId}")
  public ResponseEntity<Void> unlike(
      @PathVariable Long productId,
      @SessionAttribute(name = SessionConst.LOGIN_MEMBER) Member loginMember) {

    likeService.unlike(productId, loginMember.getLoginId());
    return ResponseEntity.noContent().build();
  }
}
