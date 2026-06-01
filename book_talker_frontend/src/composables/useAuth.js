import { ref } from 'vue';
import axios from 'axios';
import { API_BASE_URL } from '../api/client';

// 모듈 레벨 싱글턴 — AppHeader와 useNav가 같은 인스턴스를 공유
const isLoggedIn = ref(false);

export function useAuth() {
  const checkLoginStatus = async () => {
    try {
      const response = await axios.get(`${API_BASE_URL}/auth/session`, {
        withCredentials: true,
      });
      isLoggedIn.value = response.status === 200;
    } catch {
      isLoggedIn.value = false;
    }
  };

  return { isLoggedIn, checkLoginStatus };
}
