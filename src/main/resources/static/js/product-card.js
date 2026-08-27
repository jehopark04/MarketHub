/**
 * 상품을 표로 나열하는 화면들이 함께 쓰는 카드.
 *
 * 상품 목록·내가 등록한 상품·내가 찜한 상품이 같은 필드를 보여준다. 화면마다 따로
 * 그리면 가격 표기나 링크 주소를 한쪽만 고치는 사고가 난다.
 *
 * 하트만 화면마다 다르다 - 목록에는 상태에 따라, 찜 목록에는 늘 채워진 채로 있고,
 * 내가 등록한 상품에는 아예 없다(내 물건을 내가 찜하는 화면이 아니다). 그래서
 * 하트를 그릴지 말지는 부르는 쪽이 정한다.
 */

import { escapeHtml } from './dom.js';
import { heartMarkup } from './likes.js';

export function formatPrice(price) {
  return `${price.toLocaleString('ko-KR')}원`;
}

/**
 * @param heart 'state' 상품의 liked를 따른다 / 'on' 늘 채워진 채 / 'none' 그리지 않는다
 */
export function renderCard(product, { heart = 'state' } = {}) {
  const gradeClass = product.grade === 'S' ? 'badge badge--s' : 'badge';
  const heartHtml = heart === 'none' ? '' : heartMarkup(heart === 'on' || product.liked);

  // 제목 링크의 ::after가 카드 전체를 덮는다(components.css). 카드를 통째로 <a>로
  // 감싸지 않는 이유는 하트가 그 안에 들어가면 눌러도 상세로 튀기 때문이다.
  // 하트는 z-index로 링크 위에 올라와 제 클릭을 받는다.
  return `
    <li class="card" data-product-id="${product.productId}">
      <div class="card__top">
        <h2 class="card__title">
          <a class="card__link" href="/products/${product.productId}">${escapeHtml(product.title)}</a>
        </h2>
        ${heartHtml}
      </div>
      <p class="card__price">${formatPrice(product.price)}</p>
      <p><span class="${gradeClass}">상태 ${escapeHtml(product.grade)}</span></p>
    </li>`;
}

/** 카드 여럿을 격자로. */
export function renderCardGrid(products, options) {
  return `<ul class="card-grid">${products.map((p) => renderCard(p, options)).join('')}</ul>`;
}
