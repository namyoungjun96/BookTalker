// 로그인 후 돌아갈 경로를 보관한다 (예: 비로그인 사용자가 연 초대 링크, 세션 만료(401)로 로그인 화면으로 간 화면).
// sessionStorage가 아닌 localStorage를 쓰는 이유: 네이버 앱 로그인 등으로 콜백이 같은 브라우저의 새 탭에서 열려도 남아 있어야 함.
// localStorage는 저절로 지워지지 않으므로 만료 시간을 두고, 한 번 꺼내면 삭제한다.
const STORAGE_KEY = 'pending-redirect';
const EXPIRES_IN_MS = 10 * 60 * 1000;

// 앱 내부 경로만 허용 ('//example.com' 같은 외부 주소 방지)
const isInternalPath = (path) =>
  typeof path === 'string' && path.startsWith('/') && !path.startsWith('//');

export function savePendingRedirect(path) {
  if (!isInternalPath(path)) return;
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ path, savedAt: Date.now() }));
  } catch {
    // 저장소를 쓸 수 없는 환경(사생활 보호 모드 등)이면 복귀 없이 진행
  }
}

// 유효한 경로가 있으면 반환하고 삭제한다. 없거나 만료됐으면 null
export function consumePendingRedirect() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    localStorage.removeItem(STORAGE_KEY);

    const { path, savedAt } = JSON.parse(raw);
    if (!isInternalPath(path) || Date.now() - savedAt > EXPIRES_IN_MS) return null;
    return path;
  } catch {
    return null;
  }
}
