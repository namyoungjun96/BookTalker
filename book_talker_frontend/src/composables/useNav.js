import { useRoute, useRouter } from 'vue-router';
import { useToast } from 'vue-toastification';
import apiClient from '../api/client';

export function useNav() {
  const route = useRoute();
  const router = useRouter();
  const toast = useToast();

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
      toast.success('로그아웃되었습니다.');
      setTimeout(() => router.push({ name: 'login' }), 500);
    } catch {
      router.push({ name: 'login' });
    }
  };

  return { isActiveNav, onLogout };
}
