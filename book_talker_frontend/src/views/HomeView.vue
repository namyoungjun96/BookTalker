<template>
  <div class="app-container">
    <!-- 미니멀 헤더 -->
    <header class="app-header">
      <div class="header-content">
        <h1 class="logo">BookTalker</h1>
        <nav class="nav-links">
          <router-link to="/" class="nav-link" :class="{ active: isActiveRoute('/') }">
            홈
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

    <!-- 메인 콘텐츠 (max-width: 720px, 단일 컬럼) -->
    <main class="main-content">
      <div class="content-wrapper">
        <!-- 로그인하지 않은 사용자에게 보여줄 메시지 -->
        <div v-if="!isLoggedIn" class="welcome-section">
          <h2 class="welcome-title">BookTalker에 오신 것을 환영합니다</h2>
          <p class="welcome-subtitle">독서 후기를 기록하고 공유하세요</p>
          <router-link to="/login" class="welcome-login-btn">
            로그인하고 시작하기
          </router-link>
        </div>

        <!-- 로그인한 사용자에게만 리뷰 목록 표시 -->
        <template v-else>
          <h2 class="page-title">내가 작성한 리뷰</h2>

          <!-- 로딩 중 -->
          <div v-if="isLoading" class="loading-state">
            <div class="spinner"></div>
            <p class="loading-text">리뷰를 불러오는 중...</p>
          </div>

          <!-- 리뷰가 없을 때 -->
          <div v-else-if="reviews.length === 0" class="empty-state">
            <p class="empty-title">작성한 리뷰가 없습니다</p>
            <p class="empty-subtitle">책을 검색해서 첫 리뷰를 작성해보세요</p>
          </div>

          <!-- 리뷰 카드 리스트 (브런치 스타일) -->
          <div v-else class="review-list">
            <article
              v-for="review in reviews"
              :key="review.id"
              class="review-card"
            >
              <!-- 책 정보 -->
              <div class="book-info">
                <div class="book-cover">
                  <img
                    v-if="review.book?.cover"
                    :src="review.book.cover"
                    :alt="review.book.title"
                    @error="handleImageError"
                  />
                  <div v-else class="cover-placeholder">📖</div>
                </div>
                <div class="book-details">
                  <h3 class="book-title">{{ review.book?.title || '-' }}</h3>
                  <p class="book-author">{{ review.book?.author || '-' }}</p>
                </div>
              </div>

              <!-- 리뷰 내용 -->
              <div class="review-content">
                <p class="review-text">{{ review.content || '-' }}</p>
              </div>

              <!-- 리뷰 메타 정보 -->
              <div class="review-meta">
                <span class="rating">⭐ {{ review.rating || '-' }}</span>
                <span class="date">{{ formatDate(review.regDate) }}</span>
              </div>
            </article>
          </div>
        </template>
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

const reviews = ref([]);
const isLoading = ref(false);
const isLoggedIn = ref(false);

const isActiveRoute = (path) => {
  return route.path === path;
};

// 로그인 상태 체크 함수
const checkLoginStatus = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/api/auth/session`, {
      withCredentials: true,
    });
    isLoggedIn.value = response.status === 200;
  } catch (error) {
    isLoggedIn.value = false;
  }
};

const fetchReviews = async () => {
  isLoading.value = true;
  try {
    const response = await apiClient.get('/api/review/list', {
      skipErrorCodes: [404]  // 404는 "리뷰 없음"이라 정상
    });
    reviews.value = response.data || [];
  } catch (error) {
    console.error('리뷰 목록 조회 실패:', error);
    
    if (error.response?.status === 404) {
      // 404는 정상 - 리뷰 없음
      reviews.value = [];
    } else {
      // 다른 에러는 Interceptor가 자동 처리함
      reviews.value = [];
    }
  } finally {
    isLoading.value = false;
  }
};

const formatDate = (dateString) => {
  if (!dateString) return '-';
  try {
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  } catch (error) {
    return dateString;
  }
};

const handleImageError = (event) => {
  event.target.style.display = 'none';
};

const onLogout = async () => {
  try {
    await apiClient.get('/logout');
    toast.success('로그아웃되었습니다.');
    setTimeout(() => {
      router.push({ name: 'login' });
    }, 500);
  } catch (error) {
    console.error('로그아웃 실패:', error);
    // Interceptor가 에러 메시지를 이미 표시함
    router.push({ name: 'login' });
  }
};

onMounted(async () => {
  // 로그인 성공 체크
  const loginAttempt = sessionStorage.getItem('login-attempt');
  
  await checkLoginStatus();
  
  // 로그인 시도 후 첫 방문이고 로그인 성공했다면
  if (loginAttempt === 'true' && isLoggedIn.value) {
    sessionStorage.removeItem('login-attempt');
    toast.success('로그인되었습니다!');
  }
  
  // 로그인한 사용자에게만 리뷰 목록 API 호출
  if (isLoggedIn.value) {
    fetchReviews();
  }
});
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background-color: #f9fafb;
}

/* 헤더 스타일 (노션 스타일) */
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

.logout-btn {
  color: #9ca3af;
}

.logout-btn:hover {
  color: #ef4444;
}

.login-btn {
  color: #2563eb;
}

.login-btn:hover {
  color: #1d4ed8;
}

/* 메인 콘텐츠 (max-width: 720px, 노션 스타일 여백) */
.main-content {
  padding: 48px 24px;
}

.content-wrapper {
  max-width: 720px;
  margin: 0 auto;
}

/* 환영 섹션 (로그인 안 한 사용자용) */
.welcome-section {
  text-align: center;
  padding: 80px 24px;
}

.welcome-title {
  font-size: 32px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 16px 0;
  line-height: 1.3;
}

.welcome-subtitle {
  font-size: 18px;
  color: #6b7280;
  margin: 0 0 32px 0;
}

.welcome-login-btn {
  display: inline-block;
  padding: 12px 24px;
  font-size: 16px;
  font-weight: 500;
  color: white;
  background: #2563eb;
  border: none;
  border-radius: 8px;
  text-decoration: none;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.welcome-login-btn:hover {
  background: #1d4ed8;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 32px 0;
  line-height: 1.3;
}

/* 로딩 상태 */
.loading-state {
  text-align: center;
  padding: 64px 0;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e5e7eb;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  color: #6b7280;
  font-size: 15px;
}

/* 빈 상태 */
.empty-state {
  text-align: center;
  padding: 64px 24px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.empty-title {
  font-size: 18px;
  font-weight: 500;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.empty-subtitle {
  font-size: 15px;
  color: #6b7280;
  margin: 0;
}

/* 리뷰 카드 리스트 (브런치 스타일) */
.review-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.review-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 24px;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.review-card:hover {
  border-color: #d1d5db;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

/* 책 정보 */
.book-info {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.book-cover {
  width: 60px;
  height: 80px;
  flex-shrink: 0;
  border-radius: 4px;
  overflow: hidden;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
}

.book-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  font-size: 32px;
  color: #9ca3af;
}

.book-details {
  flex: 1;
  min-width: 0;
}

.book-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px 0;
  line-height: 1.4;
}

.book-author {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

/* 리뷰 내용 */
.review-content {
  margin-bottom: 16px;
}

.review-text {
  font-size: 16px;
  line-height: 1.7;
  color: #1f2937;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 리뷰 메타 정보 */
.review-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid #f3f4f6;
  font-size: 14px;
  color: #6b7280;
}

.rating {
  color: #2563eb;
  font-weight: 500;
}

.date {
  color: #9ca3af;
}

@media (max-width: 640px) {
  .main-content {
    padding: 32px 16px;
  }

  .welcome-section {
    padding: 60px 16px;
  }

  .welcome-title {
    font-size: 28px;
  }

  .welcome-subtitle {
    font-size: 16px;
  }

  .page-title {
    font-size: 24px;
    margin-bottom: 24px;
  }

  .review-card {
    padding: 20px;
  }

  .book-info {
    gap: 12px;
  }

  .book-cover {
    width: 50px;
    height: 70px;
  }
}
</style>
