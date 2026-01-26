// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router';
import axios from 'axios';
import LoginView from '../views/LoginView.vue';
import HomeView from '../views/HomeView.vue';
import BookSearchView from '../views/BookSearchView.vue';
import ReviewCreateView from '../views/ReviewCreateView.vue';
import { useSelectionStore } from '../stores/selectionStore';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
  },
  {
    path: '/',
    name: 'home',
    component: HomeView,
  },
  {
    path: '/book-search',
    name: 'book-search',
    component: BookSearchView,
  },
  {
    path: '/reviews/new',
    name: 'review-create',
    component: ReviewCreateView,
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 세션 기반 로그인 여부 체크용 헬퍼
async function checkSession() {
  try {
    // 백엔드에 세션 확인용 API를 하나 두고 (예: GET /api/auth/session),
    // 로그인 상태면 200, 아니면 401/403 을 내려주도록 구현하면 됩니다.
    const response = await axios.get(API_BASE_URL+'/api/auth/session', {
      withCredentials: true, // 다른 도메인일 때 세션 쿠키 전송
    });
    return response.status === 200;
  } catch (error) {
    return false;
  }
}

let userLoadPromise = null;
let userLoaded = false;

async function ensureCurrentUserLoaded() {
  if (userLoaded) return;
  if (userLoadPromise) return userLoadPromise;

  const store = useSelectionStore();

  userLoadPromise = axios
    .get(API_BASE_URL+'/api/user/me', { withCredentials: true })
    .then((res) => {
      store.setCurrentUser(res.data);
      userLoaded = true;
    })
    .catch(() => {
      // /api/user/me 가 아직 없거나(개발중), 일시적 오류여도 라우팅은 계속 진행
    })
    .finally(() => {
      userLoadPromise = null;
    });

  return userLoadPromise;
}

router.beforeEach(async (to, from, next) => {
  const isAuthenticated = await checkSession();
  const store = useSelectionStore();

  // 이미 로그인된 사용자가 /login 으로 가려 하면 홈으로 보냄
  if (to.name === 'login' && isAuthenticated) {
    return next({ name: 'home' });
  }

  // 로그인 안 된 사용자가 보호된 페이지(로그인 페이지 제외)를 가려 하면 /login 으로
  if (to.name !== 'login' && !isAuthenticated) {
    store.setCurrentUser(null);
    userLoaded = false;
    return next({ name: 'login' });
  }

  // 로그인 상태라면(세션 유효), 최초 1회 /api/user/me 를 호출해 작성자 이름을 상태에 저장
  if (isAuthenticated) {
    await ensureCurrentUserLoaded();
  }

  return next();
});

export default router;
