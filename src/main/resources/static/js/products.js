/**
 * 상품 목록 화면.
 *
 * 검색 조건을 주소(쿼리스트링)에 담는다. 그래야 새로고침해도 조건이 남고, 주소를 복사해
 * 공유할 수 있고, 뒤로 가기가 이전 검색으로 돌아간다. 조건을 자바스크립트 변수로만
 * 들고 있으면 이 셋을 전부 잃는다.
 */

import { ApiError, likeProduct, listProducts, unlikeProduct } from './api.js';
import { escapeHtml, showToast } from './dom.js';
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

  return `
    <li class="card" data-product-id="${product.productId}">
      <div class="card__top">
        <h2 class="card__title">${escapeHtml(product.title)}</h2>
        <button class="heart${product.liked ? ' heart--on' : ''}" type="button"
                data-like-button aria-pressed="${product.liked}" aria-label="찜">
          ${product.liked ? '&#9829;' : '&#9825;'}
        </button>
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

/* ── 찜 토글 ─────────────────────────── */

/** 하트 모양과 상태를 함께 바꾼다. 둘이 어긋나면 눈과 낭독기가 다른 말을 한다. */
function paintHeart(button, liked) {
  button.classList.toggle('heart--on', liked);
  button.setAttribute('aria-pressed', String(liked));
  button.innerHTML = liked ? '&#9829;' : '&#9825;';
}

async function toggleLike(button) {
  // 요청이 끝나기 전에 또 누르면 무시한다. 서버는 멱등이라 견디지만, 응답이 엇갈려
  // 도착하면 화면이 실제 상태와 어긋난다.
  if (button.disabled) return;

  const card = button.closest('.card');
  const productId = card.dataset.productId;
  const wasLiked = button.getAttribute('aria-pressed') === 'true';

  // 누르는 즉시 바꾼다. 응답을 기다렸다가 바꾸면 눌렀는데 반응이 없는 순간이 생긴다.
  paintHeart(button, !wasLiked);
  button.disabled = true;

  try {
    await (wasLiked ? unlikeProduct(productId) : likeProduct(productId));
  } catch (error) {
    paintHeart(button, wasLiked); // 실패했으니 되돌린다

    if (!(error instanceof ApiError)) throw error;

    // 401이면 api.js가 이미 로그인 화면으로 보냈다. 떠나는 화면에 알림을 띄우지 않는다.
    if (error.status === 401) return;

    // 다른 사람이 지운 상품이다. 하트만 되돌리면 눌러도 계속 실패하므로 카드를 뺀다.
    if (error.status === 404) {
      card.remove();
      showToast('이미 삭제된 상품입니다.');
      return;
    }
    showToast(error.detail);
  } finally {
    button.disabled = false;
  }
}

// 카드는 검색할 때마다 다시 그려진다. 카드마다 리스너를 달면 그릴 때마다 쌓이므로
// 목록을 담는 요소에 한 번만 건다.
results.addEventListener('click', (event) => {
  const button = event.target.closest('[data-like-button]');
  if (button) toggleLike(button);
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

renderHeader();
load();
