/** 화면 여러 곳이 함께 쓰는 자잘한 도구. */

/**
 * 사용자 입력이 그대로 HTML이 되지 않게 한다. 상품 제목이나 이름에 <script>가
 * 들어와도 글자로만 보인다.
 *
 * 1단계에서는 목록 화면에만 필요해 그쪽에 두었는데, 헤더와 인증 화면도 쓰게 되어
 * 이리로 옮겼다. 세 곳에 같은 함수를 두면 한 곳만 고치는 사고가 난다.
 */
export function escapeHtml(value) {
  return String(value).replace(
    /[&<>"']/g,
    (ch) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch],
  );
}

/**
 * 잠깐 떴다 사라지는 알림.
 *
 * alert()를 쓰지 않는 이유: 페이지를 멈춰 세워 그동안 아무것도 못 하고, 브라우저 기본
 * 모양이라 화면과 겉돈다. role="status"라 화면 낭독기가 하던 일을 끊지 않고 읽어준다.
 */
let toastTimer;

export function showToast(message) {
  let toast = document.querySelector('.toast');
  if (!toast) {
    toast = document.createElement('div');
    toast.className = 'toast';
    toast.setAttribute('role', 'status');
    document.body.append(toast);
  }

  toast.textContent = message;
  toast.classList.add('toast--on');

  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('toast--on'), 3000);
}
