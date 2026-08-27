/**
 * 상품 목록 화면.
 *
 * 검색 조건을 주소(쿼리스트링)에 담는다. 그래야 새로고침해도 조건이 남고, 주소를 복사해
 * 공유할 수 있고, 뒤로 가기가 이전 검색으로 돌아간다. 조건을 자바스크립트 변수로만
 * 들고 있으면 이 셋을 전부 잃는다.
 */

import { ApiError, listProducts } from './api.js';
import { escapeHtml, showToast } from './dom.js';
import { heartMarkup, toggleLike } from './likes.js';
import { renderHeader } from './session.js';

const FIELDS = ['keyword', 'minPrice', 'maxPrice', 'grade'];

const form = document.querySelector('#search-form');
const results = document.querySelector('#results');

function formatPrice(price) {
  return `${price.toLocaleString('ko-KR')}원`;
}

/** 주소에서 검색 조건을 읽는다. 화면 상태의 출처는 항상 주소다. */
function condFromUrl() {
  const params = new URLSearchParams(location.search);
  const cond = {};
  for (const name of FIELDS) {
    const value = params.get(name);
    if (value) cond[name] = value;
  }
  return cond;
}

function fillForm(cond) {
  for (const name of FIELDS) {
    form.elements[name].value = cond[name] ?? '';
  }
}

function renderState(className, title, body) {
  results.innerHTML = `
    <div class="state ${className}">
      <p class="state__title">${escapeHtml(title)}</p>
      <p class="state__body">${escapeHtml(body)}</p>
    </div>`;
}

function renderLoading() {
  const cards = Array.from({ length: 6 }, () => '<li class="skeleton"></li>').join('');
  results.innerHTML = `<ul class="card-grid">${cards}</ul>`;
}

/**
 * span이 아니라 button인 이유: 누르는 것이 되었다. 버튼이어야 Tab으로 닿고
 * Space·Enter로 눌리며, 화면 낭독기가 "누를 수 있는 것"으로 읽는다.
 *
 * 상태는 aria-pressed로 알린다. 하트 모양은 눈으로만 보이는 정보다.
 */
function renderCard(product) {
  const gradeClass = product.grade === 'S' ? 'badge badge--s' : 'badge';

  // 제목 링크가 카드 전체를 덮는다(components.css의 ::after). 카드를 통째로 <a>로
  // 감싸지 않는 이유는 하트가 그 안에 들어가면 눌러도 상세로 튀기 때문이다.
  // 하트는 링크 위로 올라와 있어(z-index) 제 클릭을 받는다.
  return `
    <li class="card" data-product-id="${product.productId}">
      <div class="card__top">
        <h2 class="card__title">
          <a class="card__link" href="/products/${product.productId}">${escapeHtml(product.title)}</a>
        </h2>
        ${heartMarkup(product.liked)}
      </div>
      <p class="card__price">${formatPrice(product.price)}</p>
      <p><span class="${gradeClass}">상태 ${escapeHtml(product.grade)}</span></p>
    </li>`;
}

function renderProducts(products) {
  if (products.length === 0) {
    renderState('', '조건에 맞는 상품이 없습니다.', '검색어나 가격 범위를 넓혀 보세요.');
    return;
  }

  const count = `<p class="results__count">상품 ${products.length}개</p>`;
  const cards = products.map(renderCard).join('');
  results.innerHTML = `${count}<ul class="card-grid">${cards}</ul>`;
}

/**
 * 아직 응답을 기다리는 요청. 새 요청을 시작하기 전에 이걸 취소한다.
 *
 * 취소하지 않으면 먼저 보낸 응답이 늦게 도착해 최신 결과를 덮어쓴다. 검색하자마자
 * 뒤로 가기를 누르면 실제로 그렇게 된다 - 주소와 입력칸은 초기화됐는데 목록만
 * 이전 검색 결과가 남는다.
 */
let inFlight = null;

async function load() {
  const cond = condFromUrl();
  fillForm(cond);
  renderLoading();

  inFlight?.abort();
  const controller = new AbortController();
  inFlight = controller;

  try {
    renderProducts(await listProducts(cond, { signal: controller.signal }));
  } catch (error) {
    // 우리가 취소한 것이다. 뒤이은 요청이 화면을 그릴 테니 여기서는 아무것도 하지 않는다.
    if (error.name === 'AbortError') return;

    if (error instanceof ApiError) {
      renderState('state--error', '목록을 불러오지 못했습니다.', error.detail);
    } else {
      throw error; // 우리가 예상한 실패가 아니다. 콘솔에 그대로 드러나야 한다.
    }
  }
}

/* ── 찜 토글 ───────────────────────────
   누르는 동작은 likes.js가 맡는다. 여기서는 이 화면에서만 다른 것 - 상품이 사라졌을 때
   그 카드를 빼는 일 - 만 넘긴다. */

// 카드는 검색할 때마다 다시 그려진다. 카드마다 리스너를 달면 그릴 때마다 쌓이므로
// 목록을 담는 요소에 한 번만 건다.
results.addEventListener('click', (event) => {
  const button = event.target.closest('[data-like-button]');
  if (!button) return;

  const card = button.closest('.card');
  toggleLike(button, card.dataset.productId, {
    onGone: () => {
      card.remove(); // 되돌리기만 하면 눌러도 계속 실패한다
      showToast('이미 삭제된 상품입니다.');
    },
  });
});

/** 주소를 바꾸고 다시 그린다. 페이지를 새로 열지 않으므로 화면이 깜빡이지 않는다. */
function navigate(search) {
  history.pushState(null, '', search ? `?${search}` : location.pathname);
  load();
}

form.addEventListener('submit', (event) => {
  event.preventDefault(); // 폼 기본 동작(페이지 이동)을 막고 우리가 처리한다
  const data = new FormData(form);
  const params = new URLSearchParams();
  for (const name of FIELDS) {
    const value = data.get(name).trim();
    if (value) params.set(name, value);
  }
  navigate(params.toString());
});

form.addEventListener('reset', (event) => {
  event.preventDefault();
  navigate('');
});

// 뒤로/앞으로 가기. 주소가 바뀌었으니 그 조건으로 다시 그린다.
window.addEventListener('popstate', load);

/** 로그인한 사람에게만 보인다. 비로그인에게 보여주면 눌러도 로그인 화면으로 튕긴다. */
async function renderHeroActions() {
  if (await renderHeader()) {
    document.querySelector('#hero-actions').innerHTML =
      '<a class="btn" href="/products/new">상품 등록</a>';
  }
}

renderHeroActions();
load();
