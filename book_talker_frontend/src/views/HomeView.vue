<template>
  <div class="app-container">
    <!-- 헤더 -->
    <header class="app-header">
      <div class="header-content">
        <h1 class="logo">BookTalker</h1>
        <nav class="nav-links">
          <router-link to="/" class="nav-link" :class="{ active: isActiveRoute('/') }">
            홈
          </router-link>
          <router-link v-if="isLoggedIn" to="/mypage" class="nav-link" :class="{ active: isActiveRoute('/mypage') }">
            마이페이지
          </router-link>
          <router-link v-if="isLoggedIn" to="/book-search" class="nav-link" :class="{ active: isActiveRoute('/book-search') }">
            검색
          </router-link>
          <button v-if="isLoggedIn" type="button" @click="onLogout" class="nav-link logout-btn">
            로그아웃
          </button>
          <router-link v-else to="/login" class="nav-link login-btn">
            로그인
          </router-link>
        </nav>
      </div>
    </header>

    <main class="main-content">
      <div class="content-wrapper">
        <!-- 헤더 섹션 -->
        <div class="hero-section">
          <h2 class="hero-title">장르별 평점 TOP 10</h2>
          <p class="hero-subtitle">독자들이 가장 높게 평가한 책을 장르별로 만나보세요</p>
        </div>

        <!-- 장르 탭 -->
        <div class="genre-tabs">
          <button
            v-for="genre in genres"
            :key="genre.id"
            class="genre-tab"
            :class="{ active: selectedGenre === genre.id }"
            @click="selectedGenre = genre.id"
          >
            {{ genre.label }}
          </button>
        </div>

        <!-- 준비 중 안내 -->
        <div class="coming-soon">
          <div class="coming-soon-icon">📚</div>
          <p class="coming-soon-title">서비스 준비 중입니다</p>
          <p class="coming-soon-desc">장르별 평점 TOP 10 기능을 곧 오픈할 예정입니다.<br>먼저 책을 검색하고 리뷰를 남겨보세요!</p>
          <router-link v-if="isLoggedIn" to="/book-search" class="action-btn">
            책 검색하러 가기
          </router-link>
          <router-link v-else to="/login" class="action-btn">
            로그인하고 시작하기
          </router-link>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useToast } from 'vue-toastification';
import apiClient, { API_BASE_URL } from '../api/client';
import axios from 'axios';

const router = useRouter();
const route = useRoute();
const toast = useToast();

const isLoggedIn = ref(false);
const selectedGenre = ref('fiction');

const genres = [
  { id: 'fiction', label: '소설' },
  { id: 'nonfiction', label: '비문학' },
  { id: 'selfdev', label: '자기계발' },
  { id: 'science', label: '과학' },
  { id: 'history', label: '역사' },
  { id: 'essay', label: '에세이' },
];

const isActiveRoute = (path) => route.path === path;

const checkLoginStatus = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/api/auth/session`, {
      withCredentials: true,
    });
    isLoggedIn.value = response.status === 200;
  } catch {
    isLoggedIn.value = false;
  }
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

onMounted(async () => {
  const loginAttempt = sessionStorage.getItem('login-attempt');
  await checkLoginStatus();
  if (loginAttempt === 'true' && isLoggedIn.value) {
    sessionStorage.removeItem('login-attempt');
    toast.success('로그인되었습니다!');
  }
});
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background-color: #f9fafb;
}

.app-header {
  background: white;
  border-bottom: 1px solid #e5e7eb;
  padding: 16px 0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 720px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.nav-links {
  display: flex;
  gap: 24px;
  align-items: center;
}

.nav-link {
  font-size: 15px;
  color: #6b7280;
  text-decoration: none;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  transition: color 0.15s ease;
}

.nav-link:hover {
  color: #1f2937;
}

.nav-link.active {
  color: #2563eb;
  font-weight: 500;
}

.logout-btn:hover {
  color: #ef4444;
}

.login-btn {
  color: #2563eb;
}

.main-content {
  padding: 48px 24px;
}

.content-wrapper {
  max-width: 720px;
  margin: 0 auto;
}

/* 히어로 섹션 */
.hero-section {
  margin-bottom: 32px;
}

.hero-title {
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 8px 0;
  line-height: 1.3;
}

.hero-subtitle {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
}

/* 장르 탭 */
.genre-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 32px;
}

.genre-tab {
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.genre-tab:hover {
  border-color: #2563eb;
  color: #2563eb;
}

.genre-tab.active {
  background: #2563eb;
  border-color: #2563eb;
  color: white;
}

/* 준비 중 안내 */
.coming-soon {
  text-align: center;
  padding: 80px 24px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
}

.coming-soon-icon {
  font-size: 48px;
  margin-bottom: 20px;
}

.coming-soon-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 12px 0;
}

.coming-soon-desc {
  font-size: 15px;
  color: #6b7280;
  line-height: 1.7;
  margin: 0 0 32px 0;
}

.action-btn {
  display: inline-block;
  padding: 12px 24px;
  font-size: 15px;
  font-weight: 500;
  color: white;
  background: #2563eb;
  border-radius: 8px;
  text-decoration: none;
  transition: background-color 0.15s ease;
}

.action-btn:hover {
  background: #1d4ed8;
}

@media (max-width: 640px) {
  .main-content {
    padding: 32px 16px;
  }

  .hero-title {
    font-size: 24px;
  }

  .genre-tabs {
    gap: 6px;
  }

  .genre-tab {
    padding: 6px 12px;
    font-size: 13px;
  }

  .coming-soon {
    padding: 60px 16px;
  }
}
</style>
