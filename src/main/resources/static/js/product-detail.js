/**
 * 상품 상세 화면.
 *
 * 상품 id는 주소에서 읽는다(/products/1). WebConfig가 이 주소를 product.html로
 * forward하므로 쿼리로는 오지 않는다.
 */

import { ApiError, deleteProduct, fetchProduct } from './api.js';
import { escapeHtml, showToast } from './dom.js';
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

/**
 * 소유자에게만 보이는 수정·삭제.
 *
 * 이것은 화면 편의일 뿐 방어가 아니다. 버튼을 숨겨도 남의 상품에 PUT이나 DELETE를
 * 쏘면 서버가 403으로 막는다. 화면은 편의, 서버는 규칙이다.
 */
function ownerActions(product, me) {
  if (!me || me.loginId !== product.sellerId) return '';

  return `
    <div class="detail__actions">
      <a class="btn btn--quiet btn--sm" href="/products/${product.productId}/edit">수정</a>
      <button class="btn btn--quiet btn--sm" type="button" id="delete-button">삭제</button>
    </div>`;
}

function setupDelete(product) {
  const dialog = document.querySelector('#delete-dialog');
  const button = view.querySelector('#delete-button');
  if (!button) return;

  // confirm()을 쓰지 않는다 - 페이지를 멈춰 세우고 브라우저 기본 모양이라 화면과
  // 겉돈다. <dialog>는 Esc로 닫히고 초점이 갇히는 것을 브라우저가 해준다.
  button.addEventListener('click', () => dialog.showModal());
  dialog.querySelector('[data-cancel]').addEventListener('click', () => dialog.close());

  dialog.querySelector('[data-confirm]').addEventListener('click', async (event) => {
    event.currentTarget.disabled = true;
    try {
      await deleteProduct(product.productId);
      location.href = '/'; // 보고 있던 상품이 없어졌으니 여기 남을 수 없다
    } catch (error) {
      dialog.close();
      if (!(error instanceof ApiError)) throw error;
      if (error.status === 401) return; // api.js가 로그인 화면으로 보냈다
      showToast(error.detail);
    } finally {
      event.currentTarget.disabled = false;
    }
  });
}

function renderProduct(product, me) {
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
      ${ownerActions(product, me)}
    </article>`;

  view.querySelector('[data-like-button]').addEventListener('click', (event) => {
    toggleLike(event.currentTarget, product.productId, {
      // 목록과 달리 뺄 카드가 없다. 보고 있던 상품 자체가 사라진 것이라 목록으로 보낸다.
      onGone: () => renderState('state--error', '이미 삭제된 상품입니다.', '목록에서 다른 상품을 찾아보세요.'),
    });
  });

  setupDelete(product);
}

async function load() {
  // 헤더를 그리면서 로그인 상태도 함께 받는다. 소유자인지 가리는 데 쓴다.
  const me = await renderHeader();

  try {
    renderProduct(await fetchProduct(productIdFromPath()), me);
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
