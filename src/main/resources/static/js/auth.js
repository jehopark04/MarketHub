/**
 * 로그인 화면과 회원가입 화면.
 *
 * 두 화면이 하는 일이 거의 같아 한 파일에 둔다 - 폼을 잠그고, 보내고, 실패하면
 * 에러를 입력칸 아래에 뿌리고, 성공하면 옮긴다. 페이지에 있는 폼을 보고 갈라진다.
 */

import { ApiError, join, login } from './api.js';
import { goHome, renderHeader, safeRedirect } from './session.js';

/* ── 에러 표시 ─────────────────────────────
   서버가 검증 실패를 { 필드이름: 메시지 }로 주고, 그 이름이 입력칸 name과 같다.
   그래서 맞는 자리를 찾아 넣기만 하면 된다. */

function clearErrors(form) {
  form.querySelectorAll('.field__error').forEach((element) => {
    element.textContent = '';
  });
  const summary = form.querySelector('.form-error');
  summary.textContent = '';
  summary.hidden = true;
}

function showFieldError(form, field, message) {
  const target = form.querySelector(`.field__error[data-error-for="${field}"]`);
  if (target) {
    target.textContent = message;
  } else {
    showFormError(form, message); // 화면에 없는 필드라면 삼키지 말고 위쪽에 보여준다
  }
}

/** 특정 입력칸의 것이 아닌 실패(409 중복 아이디, 401 로그인 실패 등). */
function showFormError(form, message) {
  const summary = form.querySelector('.form-error');
  summary.textContent = message;
  summary.hidden = false;
}

function handleFailure(form, error) {
  if (!(error instanceof ApiError)) throw error; // 예상한 실패가 아니다

  if (error.errors) {
    for (const [field, message] of Object.entries(error.errors)) {
      showFieldError(form, field, message);
    }
    return;
  }
  showFormError(form, error.detail);
}

/** 제출하는 동안 버튼을 잠근다. 연타로 같은 요청이 두 번 나가는 것을 막는다. */
async function whileSubmitting(form, work) {
  const button = form.querySelector('button[type="submit"]');
  button.disabled = true;
  try {
    await work();
  } catch (error) {
    handleFailure(form, error);
  } finally {
    button.disabled = false;
  }
}

/* ── 로그인 ───────────────────────────── */

function setupLogin(form) {
  const redirectTo = safeRedirect(new URLSearchParams(location.search).get('redirect'));

  form.addEventListener('submit', (event) => {
    event.preventDefault();
    clearErrors(form);

    const data = new FormData(form);
    whileSubmitting(form, async () => {
      await login({ loginId: data.get('loginId'), password: data.get('password') });
      location.href = redirectTo; // 401을 만나 여기 왔다면 그 화면으로 되돌아간다
    });
  });
}

/* ── 회원가입 ─────────────────────────── */

function setupJoin(form) {
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    clearErrors(form);

    const data = new FormData(form);
    const loginId = data.get('loginId');
    const password = data.get('password');

    // 비밀번호 확인은 서버가 받지 않는 값이라 여기서 본다. 틀리면 요청을 보내지
    // 않고 그 자리에서 알려준다 - 서버까지 다녀올 이유가 없다.
    if (password !== data.get('passwordConfirm')) {
      showFieldError(form, 'passwordConfirm', '비밀번호가 일치하지 않습니다.');
      return;
    }

    // 가입 API가 세션까지 발급한다. 여기서 로그인을 한 번 더 부르면 방금 정한 평문
    // 비밀번호가 두 번 오가고, 가입만 되고 로그인은 실패하는 어중간한 상태도 생긴다.
    whileSubmitting(form, async () => {
      await join({ loginId, name: data.get('name'), password });
      location.href = '/';
    });
  });
}

/* ── 시작 ─────────────────────────────── */

async function start() {
  // 헤더를 그리면서 로그인 상태도 함께 받는다. 따로 물으면 요청이 두 번 나간다.
  if (await renderHeader()) {
    goHome(); // 이미 로그인했다면 인증 화면에 머물 이유가 없다
    return;
  }

  const loginForm = document.querySelector('#login-form');
  const joinForm = document.querySelector('#join-form');
  if (loginForm) setupLogin(loginForm);
  if (joinForm) setupJoin(joinForm);
}

start();
