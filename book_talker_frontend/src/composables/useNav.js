import { useRoute, useRouter } from 'vue-router';
import { useToast } from 'vue-toastification';
import apiClient from '../api/client';

export function useNav() {
  const route = useRoute();
  const router = useRouter();
  const toast = useToast();

  // route.meta.activeNav가 있으면 그걸 기준으로, 없으면 현재 경로와 직접 비교
  const isActiveNav = (path) => {
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
