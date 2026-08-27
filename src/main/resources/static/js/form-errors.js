/**
 * 폼 실패를 화면에 붙이는 방식. 인증 폼과 상품 폼이 함께 쓴다.
 *
 * 서버가 검증 실패를 { 필드이름: 메시지 }로 주고 그 이름이 입력칸 name과 같아서,
 * 맞는 자리를 찾아 넣기만 하면 된다. 특정 칸의 것이 아닌 실패(409·403·404)는
 * 폼 위쪽에 한 줄로 보여준다.
 */

import { ApiError } from './api.js';

export function clearErrors(form) {
  form.querySelectorAll('.field__error').forEach((element) => {
    element.textContent = '';
  });
  const summary = form.querySelector('.form-error');
  summary.textContent = '';
  summary.hidden = true;
}

/** 특정 입력칸의 것이 아닌 실패. */
export function showFormError(form, message) {
  const summary = form.querySelector('.form-error');
  summary.textContent = message;
  summary.hidden = false;
}

export function showFieldError(form, field, message) {
  const target = form.querySelector(`.field__error[data-error-for="${field}"]`);
  if (target) {
    target.textContent = message;
  } else {
    showFormError(form, message); // 화면에 없는 필드라면 삼키지 말고 위쪽에 보여준다
  }
}

export function handleFailure(form, error) {
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
export async function whileSubmitting(form, work) {
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
