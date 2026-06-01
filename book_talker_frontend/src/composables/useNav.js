import { useRoute, useRouter } from 'vue-router';
import { useToast } from 'vue-toastification';
import apiClient from '../api/client';
import { useAuth } from './useAuth';

export function useNav() {
  const route = useRoute();
  const router = useRouter();
  const toast = useToast();
  const { isLoggedIn } = useAuth();

  const isActiveNav = (path) => {
    if (route.name === 'review-create') {
      return route.query.mode === 'next-reading'
        ? path === '/mypage'
        : path === '/book-search';
    }
    return (route.meta.activeNav ?? route.path) === path;
  };

  const onLogout = async () => {
    try {
      await apiClient.get('/logout');
      isLoggedIn.value = false;
      toast.success('로그아웃되었습니다.');
      setTimeout(() => router.push({ name: 'home' }), 500);
    } catch {
      isLoggedIn.value = false;
      router.push({ name: 'home' });
    }
  };

  return { isActiveNav, onLogout };
}
