package used.system.product;

import lombok.Getter;
import lombok.Setter;

/**
 * 상품 목록 조회 조건.
 *
 * <p>모든 필드가 선택 사항이다. 검색하지 않고 목록만 열면 전부 null로 들어오고, 그 상태는 "조건 없음 = 전체 조회"를 뜻한다. 따라서 걸러내는 쪽에서 필드별로
 * null을 통과로 처리한다.
 *
 * <p>가격을 int가 아니라 Integer로 두는 이유: 가격 조건을 비우면 파라미터가 아예 오지 않는데, int는 null을 받을 수 없어 바인딩 단계에서 실패한다.
 *
 * <p>Form이 아니라 이 패키지(product)에 두는 이유: 검증 실패로 되돌아갈 폼이 없다. 조건이 이상하면 결과가 0건일 뿐이라 웹 계층 전용 객체로 나눌 이유가
 * 없다.
 */
@Getter
@Setter
public class ProductSearchCond {

  private String keyword;
  private Integer minPrice;
  private Integer maxPrice;
  private ProductGrade grade;
}
