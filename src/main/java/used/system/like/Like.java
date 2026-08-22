package used.system.like;

import java.time.LocalDateTime;

/**
 * 회원이 상품을 찜한 기록.
 *
 * <p>회원을 loginId(String)로 가리킨다. Product.sellerId가 이미 같은 방식이라 맞춘 것이다. 한쪽만 PK(Long)로 바꾸면 프로젝트 안에 회원
 * 식별 방식이 두 가지가 되어 오히려 헷갈린다. 바꾼다면 Product와 함께 바꾼다.
 *
 * <p>찜은 "있다/없다"만 의미가 있어 수정할 상태가 없다. 그래서 update 메서드가 없고, 취소는 이 기록 자체를 지우는 것으로 표현한다.
 */
public class Like {

  private Long id;
  private String memberId;
  private Long productId;
  private LocalDateTime createAt;

  public Like(String memberId, Long productId) {
    this.memberId = memberId;
    this.productId = productId;
    this.createAt = LocalDateTime.now();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) { // 저장 시점에 리포지토리가 채번하여 부여 (그 외 용도 금지)
    this.id = id;
  }

  public String getMemberId() {
    return memberId;
  }

  public Long getProductId() {
    return productId;
  }

  public LocalDateTime getCreateAt() {
    return createAt;
  }
}
