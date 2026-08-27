/**
 * 하트 그리기와 토글. 목록과 상세가 함께 쓴다.
 *
 * 실패했을 때 할 일은 화면마다 다르다 - 목록은 사라진 상품의 카드를 빼면 되지만,
 * 상세는 보고 있던 상품 자체가 없어진 것이라 목록으로 돌아가야 한다. 그래서 여기서는
 * 되돌리기까지만 하고 "그다음 무엇을 할지"는 부르는 쪽이 onGone으로 넘긴다.
 * 여기서 정해버리면 화면이 늘어날 때마다 이 안에 분기가 쌓인다.
 */

import { ApiError, likeProduct, unlikeProduct } from './api.js';
import { showToast } from './dom.js';

/** 목록과 상세가 같은 마크업을 쓴다. 크기만 CSS로 다르게 준다. */
export function heartMarkup(liked, extraClass = '') {
  const classes = ['heart', extraClass, liked ? 'heart--on' : ''].filter(Boolean).join(' ');
  return `<button class="${classes}" type="button" data-like-button
                  aria-pressed="${liked}" aria-label="찜">${liked ? '&#9829;' : '&#9825;'}</button>`;
}

/** 모양과 상태를 함께 바꾼다. 둘이 어긋나면 눈과 화면 낭독기가 다른 말을 한다. */
export function paintHeart(button, liked) {
  button.classList.toggle('heart--on', liked);
  button.setAttribute('aria-pressed', String(liked));
  button.innerHTML = liked ? '&#9829;' : '&#9825;';
}

/**
 * @param onGone 404일 때 부른다 - 그 상품이 사라졌다는 뜻이다.
 */
export async function toggleLike(button, productId, { onGone }) {
  // 요청이 끝나기 전에 또 누르면 무시한다. 서버는 멱등이라 견디지만, 응답이 엇갈려
  // 도착하면 화면이 실제 상태와 어긋난다.
  if (button.disabled) return;

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

    if (error.status === 404) {
      onGone();
      return;
    }
    showToast(error.detail);
  } finally {
    button.disabled = false;
  }
}
