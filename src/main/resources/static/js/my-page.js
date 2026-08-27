/**
 * 마이페이지 - 내 정보, 내가 등록한 상품, 내가 찜한 상품.
 *
 * 세 화면이 뼈대를 공유해 한 파일에서 다룬다. 어느 것을 그릴지는 주소로 정한다 -
 * 탭 상태를 자바스크립트 변수로만 들고 있으면 뒤로 가기가 동작하지 않고 링크를
 * 공유할 수도 없다.
 */

import { fetchMyLikes, fetchMyProducts } from './api.js';
import { escapeHtml } from './dom.js';
import { toggleLike } from './likes.js';
import { renderCardGrid } from './product-card.js';
import { renderHeader } from './session.js';

const TABS = [
  { path: '/my-page', label: '내 정보' },
  { path: '/my-page/products', label: '내 상품' },
  { path: '/my-page/likes', label: '찜한 상품' },
];

const view = document.querySelector('#my-page');

function renderTabs(current) {
  const links = TABS.map(
    (tab) =>
      `<a class="tabs__item${tab.path === current ? ' tabs__item--on' : ''}"
          href="${tab.path}"${tab.path === current ? ' aria-current="page"' : ''}
       >${tab.label}</a>`,
  ).join('');

  return `<nav class="tabs">${links}</nav>`;
}

function renderEmpty(body, linkHref, linkLabel) {
  return `
    <div class="state">
      <p class="state__title">아직 없습니다.</p>
      <p class="state__body">${escapeHtml(body)}</p>
      <a class="btn btn--quiet" href="${linkHref}">${escapeHtml(linkLabel)}</a>
    </div>`;
}

/* ── 내 정보 ─────────────────────────── */

async function renderSummary(me) {
  // 개수를 보여주려면 두 목록이 필요하다. 나란히 보내 왕복을 한 번으로 줄인다.
  const [products, likes] = await Promise.all([fetchMyProducts(), fetchMyLikes()]);

  return `
    <section class="profile">
      <p class="profile__name">${escapeHtml(me.name)}</p>
      <p class="profile__id">@${escapeHtml(me.loginId)}</p>
    </section>

    <section class="counts">
      <a class="counts__item" href="/my-page/products">
        <span class="counts__number">${products.length}</span>
        <span class="counts__label">등록한 상품</span>
      </a>
      <a class="counts__item" href="/my-page/likes">
        <span class="counts__number">${likes.length}</span>
        <span class="counts__label">찜한 상품</span>
      </a>
    </section>`;
}

/* ── 목록 두 개 ───────────────────────── */

async function renderMyProducts() {
  const products = await fetchMyProducts();
  if (products.length === 0) {
    return renderEmpty('등록한 상품이 없습니다.', '/products/new', '상품 등록');
  }
  // 내 물건을 내가 찜하는 화면이 아니라 하트를 그리지 않는다.
  return renderCardGrid(products, { heart: 'none' });
}

async function renderMyLikes() {
  const products = await fetchMyLikes();
  if (products.length === 0) {
    return renderEmpty('찜한 상품이 없습니다.', '/', '상품 둘러보기');
  }
  // 이 목록에 있다는 것 자체가 찜했다는 뜻이라 응답에 liked가 없다. 늘 채워서 그린다.
  return renderCardGrid(products, { heart: 'on' });
}

/**
 * 찜 목록의 하트.
 *
 * 풀어도 그 자리에 남긴다. 즉시 빼면 실수로 눌렀을 때 되돌릴 수가 없다. 빈 하트로
 * 남으면 다시 눌러 되돌릴 수 있고, 새로고침하면 목록에서 사라진다.
 */
function bindLikes() {
  view.addEventListener('click', (event) => {
    const button = event.target.closest('[data-like-button]');
    if (!button) return;

    const card = button.closest('.card');
    toggleLike(button, card.dataset.productId, {
      // 404는 다르다. 그 상품이 없어진 것이니 되돌릴 것이 없다.
      onGone: () => card.remove(),
    });
  });
}

/* ── 시작 ─────────────────────────────── */

async function start() {
  const me = await renderHeader();

  // fetchMe는 401을 "비로그인"으로 돌려준다. 그래서 여기서 직접 보낸다.
  if (!me) {
    const here = location.pathname;
    location.replace(`/login?redirect=${encodeURIComponent(here)}`);
    return;
  }

  const path = location.pathname;
  let body;
  if (path === '/my-page/products') {
    body = await renderMyProducts();
  } else if (path === '/my-page/likes') {
    body = await renderMyLikes();
  } else {
    body = await renderSummary(me);
  }

  view.innerHTML = renderTabs(path) + body;
  bindLikes();
}

start();
