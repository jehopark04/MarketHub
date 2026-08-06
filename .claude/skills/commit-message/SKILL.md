---
name: commit-message
description: "스테이지된 변경으로 커밋 메시지 초안을 만들고 author↔reviewer 2인 루프로 검증한다. 다루는 것: git diff --cached 범위, 이 프로젝트의 한글 서술형 커밋 규약(CLAUDE.md). 다루지 않는 것: git add(호출 전에 끝나 있어야 한다), git commit 실행(호출자 몫), 이미 메시지가 붙은 커밋의 대체. '커밋 메시지 만들어줘' 같은 요청과 /polish [7]에서 사용."
allowed-tools: Bash, Read, Agent
---

# commit-message

`commit-msg-author` → `commit-msg-reviewer`를 순차 호출해 커밋 메시지를 만들고 검증한다.
**재시도 횟수·판정 분기·종료 조건은 이 스킬이 소유한다** — 에이전트는 무상태라 자기가 몇 번째
호출인지 알 수 없다.

## 반환 계약 (호출자와의 인터페이스)

**반환 텍스트의 마지막 줄은 반드시 다음 둘 중 하나다.**

```
판정: PASS
판정: FAIL
```

| 판정 | 의미 | `commit-draft.md` |
|---|---|---|
| PASS | reviewer가 검증한 최종본 | 그대로 커밋해도 되는 내용 |
| FAIL | 재시도 한도까지 REDO가 풀리지 않음 | 파일은 있으나 **미검증** |

> 이 한 줄이 규격인 이유: `/polish` [8]이 이 값으로 **커밋하느냐 [E4]로 끝내느냐**를 가른다.
> 자유 서술을 반환하면 상위 스킬이 문자열을 눈치로 판정하게 되고, 그 순간 결정론 단계에
> 확률성이 새어든다. 호출자는 이 마지막 줄만 보면 된다.

**한도 초과는 FAIL이다. PASS로 승격하지 않는다** — 검증하지 못한 메시지로 커밋이 나가면
안 된다. 이때도 draft는 남기므로 사용자가 직접 손보면 된다.

## Workflow

### 1. Precondition
`git diff --cached --quiet` → **exit 0(변경 없음)이면 종료.** `git add` 안내 후 `판정: FAIL`.

> 이 스킬은 `git add`를 하지 않는다. 스테이징은 호출자의 책임이다(/polish는 [7] 진입부에서
> 끝내고 들어온다). 여기서 add를 하면 호출자가 확정한 작업 범위를 하위 스킬이 넓히게 된다.

### 2. author 호출
`commit-msg-author` 에이전트. 출력은 `_workspace/commit-draft.md`.

### 3. reviewer 호출
`commit-msg-reviewer` 에이전트. 출력은 `_workspace/commit-review.md`.

> ⚠️ **`review-report.md`가 아니다.** 그 이름은 `/polish` [3] code-reviewer가 소유한다.
> 겹치면 [7]에서 하위 스킬이 상위 리포트를 덮어써 [8] 승인 요약의 "반영/미반영" 원본이 사라진다.
> `_workspace/`는 평면 네임스페이스라 이름 분리가 유일한 충돌 방지 수단이다.

### 4. 판정 분기
`commit-review.md`의 `판정:` 줄을 읽는다.

- **PASS** → draft 내용을 제시하고 `판정: PASS` 반환
- **REDO & 재작성 < 2** → 재작성 +1. reviewer의 **수정 지시를 프롬프트에 실어** author 재호출 → 3으로
- **REDO & 재작성 = 2** → "자동 검증 한계 도달 - 수동 확인 필요" 경고 + 마지막 draft 제시 +
  `판정: FAIL` 반환

## 카운터
- **재작성** — 4번에서만 증가. 최대 2(= author 최초 1회 + 재호출 2회).
- 에이전트에게 회차를 알려주지 않는다. 알아도 쓸 데가 없고, 재호출 시 무엇이 달라지는지는
  각 에이전트의 **재호출 모드** 프로토콜이 이미 선언한다.

## 산출물

| 파일 | 형식 소유 | 수명 |
|---|---|---|
| `_workspace/commit-draft.md` | `commit-msg-author.md` | 회차마다 덮어씀 |
| `_workspace/commit-review.md` | `commit-msg-reviewer.md` | 회차마다 덮어씀 |

두 파일 모두 덮어써지므로 **짝이 맞는지 확인할 수단**이 필요하다. reviewer가 리포트에
리뷰한 draft의 첫 줄을 적는다 — 그게 draft와 다르면 다른 회차의 산출물이다.
