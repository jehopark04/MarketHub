/**
 * 로그인 상태와 헤더.
 *
 * 헤더는 모든 페이지에 있다. HTML을 페이지마다 복붙하면 로그인 표시를 고칠 때
 * 전부 손봐야 하므로, 안쪽 내용은 여기서만 그린다.
 */

import { fetchMe, logout } from './api.js';
import { escapeHtml } from './dom.js';

/**
 * 주소로 받은 이동 경로가 우리 사이트 안인지 확인한다.
 *
 * 값을 그대로 믿고 이동하면 외부 주소로 튕겨 보낼 수 있다(오픈 리다이렉트).
 * "//evil.example"처럼 슬래시로 시작해도 밖으로 나가므로 앞글자만 봐서는 모자라다.
 * 실제 주소로 해석해 출처가 같은지 본다.
 */
export function safeRedirect(value) {
  if (!value) return '/';
  try {
    const url = new URL(value, location.origin);
    if (url.origin !== location.origin) return '/';
    return url.pathname + url.search;
  } catch {
    return '/';
  }
}

/**
 * 헤더를 그리고, 확인한 로그인 상태를 돌려준다.
 *
 * 로그인 여부가 더 필요한 화면은 이 반환값을 쓴다. 따로 fetchMe를 부르면 같은 정보를
 * 두 번 물어보게 된다 - 페이지를 열 때마다 /api/me가 두 번 나갔다.
 *
 * @return 로그인한 회원 {loginId, name} 또는 비로그인이면 null
 */
export async function renderHeader() {
  const nav = document.querySelector('.site-header__nav');
  const me = await fetchMe();

  if (!me) {
    nav.innerHTML = '<a href="/login">로그인</a>';
    return null;
  }

  nav.innerHTML = `
    <a href="/my-page">${escapeHtml(me.name)}</a>
    <button class="btn btn--quiet btn--sm" type="button" id="logout-button">로그아웃</button>`;

  nav.querySelector('#logout-button').addEventListener('click', async (event) => {
    event.currentTarget.disabled = true;
    await logout();
    location.href = '/'; // 로그아웃했으니 로그인이 필요한 화면에 남아 있지 않는다
  });

  return me;
}

/**
 * 홈으로 보낸다. 로그인·회원가입 화면에 이미 로그인한 사람이 들어왔을 때 쓴다.
 *
 * replace를 쓰는 이유: 이 이동을 히스토리에 남기면 뒤로 가기가 다시 그 화면으로
 * 돌아와 또 홈으로 튕긴다.
 */
export function goHome() {
  location.replace('/');
}
