package used.system.controller.member;

import used.system.member.Member;

/**
 * 로그인한 회원 정보 응답.
 *
 * <p>Member를 그대로 반환하지 않는 이유가 여기 있다 - password 필드가 있다. 도메인 객체를 응답에 실으면 비밀번호가 그대로 나간다. 담을 것을 고르는 게
 * 아니라 뺄 것을 놓치지 않는 쪽이 이 DTO의 목적이다.
 *
 * <p>loginId를 넣는다. 화면은 이름만 쓰지만, 상품 상세에서 sellerId와 견주어 "내 상품인가"를 판단하려면 클라이언트가 자기 loginId를 알아야 한다.
 */
public record MemberResponse(String loginId, String name) {

  public static MemberResponse from(Member member) {
    return new MemberResponse(member.getLoginId(), member.getName());
  }
}
