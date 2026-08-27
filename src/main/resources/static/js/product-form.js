/**
 * 상품 등록 화면과 수정 화면.
 *
 * 입력칸도 검증 처리도 같아 한 곳에서 다룬다. 다른 것은 셋뿐이다 - 기존값을 채우는가,
 * POST인가 PUT인가, 성공 후 어디로 가는가. 두 벌로 두면 검증 메시지나 필드를
 * 한쪽만 고치는 사고가 난다.
 */

import { ApiError, createProduct, fetchProduct, updateProduct } from './api.js';
import { clearErrors, showFormError, whileSubmitting } from './form-errors.js';
import { renderHeader } from './session.js';

const FIELDS = ['title', 'description', 'price', 'grade'];

const form = document.querySelector('#product-form');
const heading = document.querySelector('#form-title');

/** /products/5/edit 에서 5를 꺼낸다. 등록 화면(/products/new)이면 null이다. */
function editingId() {
  const match = location.pathname.match(/^\/products\/(\d+)\/edit$/);
  return match ? match[1] : null;
}

function readForm() {
  const data = new FormData(form);
  return {
    title: data.get('title').trim(),
    description: data.get('description').trim(),
    // 빈 칸은 null로 보낸다. 빈 문자열을 숫자로 바꾸면 0이 되어 "공짜"가 등록된다.
    price: data.get('price') === '' ? null : Number(data.get('price')),
    grade: data.get('grade') || null,
  };
}

function fillForm(product) {
  for (const name of FIELDS) {
    form.elements[name].value = product[name] ?? '';
  }
}

/** 폼을 열 수 없는 상태. 입력칸을 남겨두면 채워 넣고 제출했다가 실패한다. */
function renderBlocked(title, body) {
  document.querySelector('#product-form-view').innerHTML = `
    <div class="state state--error">
      <p class="state__title">${title}</p>
      <p class="state__body">${body}</p>
      <a class="btn btn--quiet" href="/">상품 목록으로</a>
    </div>`;
}

function setupCreate() {
  heading.textContent = '상품 등록';
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    clearErrors(form);

    whileSubmitting(form, async () => {
      const saved = await createProduct(readForm());
      // 응답에 productId가 있다. 목록으로 보내면 방금 올린 것을 다시 찾아야 한다.
      location.href = `/products/${saved.productId}`;
    });
  });
}

async function setupEdit(productId, me) {
  heading.textContent = '상품 수정';

  let product;
  try {
    product = await fetchProduct(productId);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      renderBlocked('상품을 찾을 수 없습니다.', '이미 삭제되었거나 없는 주소입니다.');
      return;
    }
    throw error;
  }

  // 주소를 직접 치고 들어올 수 있다. 다만 이것은 화면 편의일 뿐이고, 진짜 방어는
  // 서버다 - 여기를 지나쳐 PUT을 쏘아도 ForbiddenException이 403으로 막는다.
  if (!me || me.loginId !== product.sellerId) {
    renderBlocked('내 상품이 아닙니다.', '본인이 등록한 상품만 수정할 수 있습니다.');
    return;
  }

  fillForm(product);

  form.addEventListener('submit', (event) => {
    event.preventDefault();
    clearErrors(form);

    whileSubmitting(form, async () => {
      await updateProduct(productId, readForm());
      location.href = `/products/${productId}`; // 204라 돌려받는 것이 없다
    });
  });
}

async function start() {
  const me = await renderHeader();
  const productId = editingId();

  // 로그인 검사는 서버가 한다. 다만 폼을 다 채운 뒤 401을 만나는 것보다
  // 들어올 때 알려주는 편이 낫다.
  if (!me) {
    renderBlocked('로그인이 필요합니다.', '상품을 등록하거나 고치려면 먼저 로그인하세요.');
    return;
  }

  if (productId) {
    await setupEdit(productId, me);
  } else {
    setupCreate();
  }
}

start().catch((error) => {
  showFormError(form, '화면을 여는 중 문제가 생겼습니다.');
  throw error;
});
