// 작성 중인 독후감을 임시저장한다 (카카오톡 웹뷰 이탈·새로고침·세션 만료로 입력값이 사라지는 문제).
// 서버가 아닌 localStorage를 쓰는 이유: 세션이 만료돼도 저장돼야 하고, 입력할 때마다 서버에 요청하지 않기 위해.
// 키를 책·회차별로 나눠 1회독과 회독 독후감이 섞이지 않게 한다.
const KEY_PREFIX = 'review-draft:';
const EXPIRES_IN_MS = 30 * 24 * 60 * 60 * 1000;

const draftKey = (isbn13, readingCount) => `${KEY_PREFIX}${isbn13}:${readingCount}`;

// 만료됐거나 깨진 값이면 삭제하고 null
const readDraft = (key) => {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return null;

    const draft = JSON.parse(raw);
    if (!draft?.savedAt || Date.now() - draft.savedAt > EXPIRES_IN_MS) {
      localStorage.removeItem(key);
      return null;
    }
    return draft;
  } catch {
    return null;
  }
};

const draftKeys = () => {
  try {
    return Object.keys(localStorage).filter((key) => key.startsWith(KEY_PREFIX));
  } catch {
    return [];
  }
};

export function loadReviewDraft(isbn13, readingCount) {
  if (!isbn13) return null;
  return readDraft(draftKey(isbn13, readingCount));
}

export function saveReviewDraft(isbn13, readingCount, draft) {
  if (!isbn13) return;
  try {
    localStorage.setItem(draftKey(isbn13, readingCount), JSON.stringify({ ...draft, savedAt: Date.now() }));
  } catch {
    // 저장소를 쓸 수 없는 환경(사생활 보호 모드 등)이면 임시저장 없이 진행
  }
}

export function clearReviewDraft(isbn13, readingCount) {
  if (!isbn13) return;
  try {
    localStorage.removeItem(draftKey(isbn13, readingCount));
  } catch {
    // 저장소를 쓸 수 없으면 지울 것도 없음
  }
}

// 책 정보 없이 작성 화면에 들어왔을 때(새로고침·로그인 후 복귀) 화면을 복구할 1회독 임시저장. 가장 최근 것 하나
export function findLatestFirstReadingDraft() {
  return draftKeys()
    .filter((key) => key.endsWith(':1'))
    .map(readDraft)
    .filter((draft) => draft?.book?.isbn13)
    .reduce((latest, draft) => (!latest || draft.savedAt > latest.savedAt ? draft : latest), null);
}

// 로그아웃 시 호출 — 공용 기기에서 비공개 본문이 남지 않게
export function clearAllReviewDrafts() {
  try {
    draftKeys().forEach((key) => localStorage.removeItem(key));
  } catch {
    // 저장소를 쓸 수 없으면 지울 것도 없음
  }
}
