/**
 * 서버와 이야기하는 통로. 모든 요청이 이 파일을 지난다.
 *
 * 화면마다 fetch를 직접 부르면 실패 처리가 화면 수만큼 복사된다. 서버가 모든 실패를
 * 같은 모양(RFC 9457 problem+json)으로 주기로 되어 있으니, 그걸 푸는 일도 한곳에 둔다.
 */

/**
 * 서버가 실패로 답한 것. 네트워크가 끊긴 경우는 status가 0이다.
 *
 * errors는 검증 실패(400)일 때만 있다. {필드이름: 메시지} 모양이라
 * 화면이 입력칸 아래에 그대로 붙일 수 있다.
 */
export class ApiError extends Error {
  constructor(status, detail, errors = null) {
    super(detail);
    this.name = 'ApiError';
    this.status = status;
    this.detail = detail;
    this.errors = errors;
  }
}

/** 값이 빈 조건은 아예 보내지 않는다. keyword= 처럼 보내면 서버가 조건으로 받는다. */
function toQueryString(params) {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value === null || value === undefined || value === '') continue;
    search.set(key, String(value));
  }
  return search.toString();
}

/** 실패 응답에서 사람이 읽을 메시지를 꺼낸다. 본문이 비었거나 JSON이 아닐 수도 있다. */
async function readProblem(response) {
  try {
    const body = await response.json();
    return {
      detail: body.detail || '요청을 처리하지 못했습니다.',
      errors: body.errors || null,
    };
  } catch {
    return { detail: '요청을 처리하지 못했습니다.', errors: null };
  }
}

/**
 * 로그인 화면으로 보내면서 돌아올 곳을 남긴다.
 *
 * 로그인 화면 자체에서는 보내지 않는다. 거기서 401이 나면 자기 자신으로 되돌아
 * 무한히 도는 꼴이 된다.
 *
 * /login은 WebConfig가 login.html로 넘겨준다. 스프링은 정적 파일을 확장자 없는 주소로
 * 매핑해주지 않아 그 설정이 없으면 404다.
 */
const LOGIN_PAGE = '/login';

function goToLogin() {
  if (location.pathname === LOGIN_PAGE) return;
  const here = location.pathname + location.search;
  location.href = `${LOGIN_PAGE}?redirect=${encodeURIComponent(here)}`;
}

/**
 * @param unauthorized 401을 만났을 때의 처리.
 *   'redirect' (기본) - 로그인 화면으로 보낸다. 로그인이 필요한 동작이었다는 뜻이다.
 *   'throw'           - ApiError로 던진다. 401이 오류가 아니라 "답"인 요청에 쓴다
 *                       (내 정보 조회의 401은 "비로그인"이고, 로그인 요청의 401은
 *                       "아이디나 비밀번호가 틀렸다"다. 둘 다 로그인 화면으로
 *                       보낼 일이 아니다).
 */
async function request(path, { method = 'GET', body, query, signal, unauthorized = 'redirect' } = {}) {
  const queryString = query ? toQueryString(query) : '';
  const url = queryString ? `${path}?${queryString}` : path;

  const options = { method, headers: {}, signal };
  if (body !== undefined) {
    options.headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(url, options);
  } catch (error) {
    // 호출한 쪽이 취소한 것은 실패가 아니다. 그대로 던져 부른 쪽이 알아보게 한다.
    if (error.name === 'AbortError') throw error;
    // 그 밖에 fetch가 예외를 던지는 건 서버에 닿지 못했을 때뿐이다.
    // 404나 500은 "정상적으로 받은 응답"이라 여기로 오지 않는다.
    throw new ApiError(0, '서버에 연결할 수 없습니다.');
  }

  // 반드시 직접 확인해야 한다. res.ok는 200~299일 때만 true다.
  if (!response.ok) {
    const problem = await readProblem(response);
    if (response.status === 401 && unauthorized === 'redirect') {
      goToLogin();
    }
    throw new ApiError(response.status, problem.detail, problem.errors);
  }

  // 204는 본문이 없다. json()을 부르면 파싱할 것이 없어 터진다.
  if (response.status === 204) return null;

  return response.json();
}

/* ── 엔드포인트 ─────────────────────────
   화면이 경로 문자열을 직접 알지 않도록 여기서 이름을 붙인다.
   경로가 바뀌면 고칠 곳이 한 군데다. */

/**
 * 상품 목록. cond는 {keyword, minPrice, maxPrice, grade}이고 전부 선택이다.
 *
 * signal로 취소할 수 있다. 검색을 연달아 하면 요청이 겹치는데, 먼저 보낸 응답이 늦게
 * 도착해 최신 결과를 덮어쓸 수 있다. 앞선 요청을 취소하면 그 일이 생기지 않는다.
 */
export function listProducts(cond = {}, { signal } = {}) {
  return request('/api/products', { query: cond, signal });
}

/**
 * 찜하기. 이미 찜한 상품이어도 204다 - "찜된 상태로 만들어라"는 요청이라 멱등하다.
 *
 * 401은 기본값대로 로그인 화면으로 보낸다. 찜은 로그인이 필요한 동작이므로 그게 맞다.
 */
export function likeProduct(productId) {
  return request(`/api/me/likes/${productId}`, { method: 'PUT' });
}

/** 찜 취소. 찜하지 않은 상품이어도 204다. 삭제된 상품에 남은 내 찜도 풀 수 있다. */
export function unlikeProduct(productId) {
  return request(`/api/me/likes/${productId}`, { method: 'DELETE' });
}

/**
 * 지금 로그인한 회원. 비로그인이면 null이다.
 *
 * 여기서 401은 오류가 아니라 "로그인하지 않았다"는 답이다. 다른 401과 똑같이 다뤄
 * 로그인 화면으로 보내면, 비로그인 방문자가 목록만 보러 왔다가 로그인 화면으로 튕긴다.
 * 상품 조회를 비로그인에 열어둔 서버 정책이 화면에서 무너지는 셈이다.
 */
export async function fetchMe() {
  try {
    return await request('/api/me', { unauthorized: 'throw' });
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) return null;
    throw error;
  }
}

/** 회원가입. 성공해도 로그인되지는 않는다 - 서버가 세션을 만들지 않는다. */
export function join({ loginId, name, password }) {
  // passwordConfirm은 보내지 않는다. 서버가 받지 않는 값이고, 평문 비밀번호를
  // 두 번 실어 보내면 로그에 남을 표면만 늘어난다. 두 칸 비교는 화면이 한다.
  return request('/api/members', { method: 'POST', body: { loginId, name, password } });
}

/** 로그인. 401은 "아이디나 비밀번호가 틀렸다"라 로그인 화면으로 보낼 일이 아니다. */
export function login({ loginId, password }) {
  return request('/api/login', {
    method: 'POST',
    body: { loginId, password },
    unauthorized: 'throw',
  });
}

/** 로그아웃. 세션이 없어도 204라 실패를 따로 다룰 것이 없다. */
export function logout() {
  return request('/api/logout', { method: 'POST' });
}
