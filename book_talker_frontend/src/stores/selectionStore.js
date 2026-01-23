// 간단한 전역 상태 관리 (선택한 책 + 로그인 사용자)
// 작성자 정보(이름)는 상태관리로 저장 - 보안상 문제 없음 (프론트엔드에서만 사용)
import { reactive, toRefs } from 'vue';

const state = reactive({
  selectedBook: null,
  // /api/user/me 응답을 기준으로 최소 정보만 저장
  // { name, email, birthday, profileImage }
  currentUser: null,
});

export function useSelectionStore() {
  const setSelectedBook = (book) => {
    state.selectedBook = book;
  };

  const setCurrentUser = (user) => {
    // 보안적으로 "세션 자체"를 저장하는 게 위험한 거지,
    // 화면 표시용 사용자 프로필(이름/이메일/이미지 등)을 프론트 상태로 들고 있는 건 일반적입니다.
    // 단, 민감정보(토큰/권한/주민번호 등)는 저장하지 않도록 제한합니다.
    state.currentUser = user
      ? {
          name: user.name || '알 수 없는 사용자',
          email: user.email || null,
          birthday: user.birthday || null,
          profileImage: user.profileImage || null,
        }
      : null;
  };

  return {
    ...toRefs(state),
    setSelectedBook,
    setCurrentUser,
  };
}

