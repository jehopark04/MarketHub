/**
 * 상품 상세 화면.
 *
 * 상품 id는 주소에서 읽는다(/products/1). WebConfig가 이 주소를 product.html로
 * forward하므로 쿼리로는 오지 않는다.
 */

import { ApiError, fetchProduct } from './api.js';
import { escapeHtml } from './dom.js';
import { heartMarkup, toggleLike } from './likes.js';
import { renderHeader } from './session.js';

const view = document.querySelector('#product');

function productIdFromPath() {
  return location.pathname.split('/').filter(Boolean).pop();
}

function formatPrice(price) {
  return `${price.toLocaleString('ko-KR')}원`;
}

function renderState(className, title, body) {
  view.innerHTML = `
    <div class="state ${className}">
      <p class="state__title">${escapeHtml(title)}</p>
      <p class="state__body">${escapeHtml(body)}</p>
      <a class="btn btn--quiet" href="/">상품 목록으로</a>
    </div>`;
}

function renderProduct(product) {
  const gradeClass = product.grade === 'S' ? 'badge badge--s' : 'badge';

  view.innerHTML = `
    <article class="detail">
      <div class="detail__head">
        <h1 class="detail__title">${escapeHtml(product.title)}</h1>
        ${heartMarkup(product.liked, 'heart--lg')}
      </div>
      <p class="detail__price">${formatPrice(product.price)}</p>
      <p class="detail__meta">
        <span class="${gradeClass}">상태 ${escapeHtml(product.grade)}</span>
        <span class="detail__seller">판매자 ${escapeHtml(product.sellerId)}</span>
      </p>
      <p class="detail__description">${escapeHtml(product.description ?? '')}</p>
    </article>`;

  view.querySelector('[data-like-button]').addEventListener('click', (event) => {
    toggleLike(event.currentTarget, product.productId, {
      // 목록과 달리 뺄 카드가 없다. 보고 있던 상품 자체가 사라진 것이라 목록으로 보낸다.
      onGone: () => renderState('state--error', '이미 삭제된 상품입니다.', '목록에서 다른 상품을 찾아보세요.'),
    });
  });
}

async function load() {
  renderHeader();

  try {
    renderProduct(await fetchProduct(productIdFromPath()));
  } catch (error) {
    if (!(error instanceof ApiError)) throw error;

    // 주소를 직접 치고 들어올 수 있다. 빈 화면을 남기지 않는다.
    if (error.status === 404) {
      renderState('state--error', '상품을 찾을 수 없습니다.', '이미 삭제되었거나 없는 주소입니다.');
      return;
    }
    renderState('state--error', '상품을 불러오지 못했습니다.', error.detail);
  }
}

load();
