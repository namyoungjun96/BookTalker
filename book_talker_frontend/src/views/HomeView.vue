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
          <router-link to="/book-search" class="nav-link" :class="{ active: isActiveRoute('/book-search') }">
            검색
          </router-link>
          <button type="button" @click="onLogout" class="nav-link logout-btn">
            로그아웃
          </button>
        </nav>
      </div>
    </header>

    <!-- 메인 콘텐츠 (max-width: 720px, 단일 컬럼) -->
    <main class="main-content">
      <div class="content-wrapper">
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
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import apiClient from '../api/client';

const router = useRouter();
const route = useRoute();

const reviews = ref([]);
const isLoading = ref(false);

const isActiveRoute = (path) => {
  return route.path === path;
};

const fetchReviews = async () => {
  isLoading.value = true;
  try {
    const response = await apiClient.get('/api/review/list');
    reviews.value = response.data || [];
  } catch (error) {
    console.error('리뷰 목록 조회 실패:', error);
    reviews.value = [];
    if (error.response?.status !== 404) {
      alert('리뷰 목록을 불러오는 중 오류가 발생했습니다.');
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
    router.push({ name: 'login' });
  } catch (error) {
    console.error('로그아웃 실패:', error);
    router.push({ name: 'login' });
  }
};

onMounted(() => {
  fetchReviews();
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

/* 메인 콘텐츠 (max-width: 720px, 노션 스타일 여백) */
.main-content {
  padding: 48px 24px;
}

.content-wrapper {
  max-width: 720px;
  margin: 0 auto;
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
