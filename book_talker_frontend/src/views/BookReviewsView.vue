<template>
  <div class="app-container">
    <main class="main-content">
      <div class="content-wrapper">

        <!-- 뒤로가기 -->
        <button @click="router.back()" class="back-link">← TOP 10으로</button>

        <!-- 책 정보 -->
        <div class="book-card">
          <div class="book-cover">
            <img v-if="cover" :src="cover" :alt="title" @error="handleImageError" />
            <div v-else class="cover-placeholder">📖</div>
          </div>
          <div class="book-info">
            <h2 class="book-title">{{ title }}</h2>
            <p class="book-sub">독자들의 공개 독후감</p>
          </div>
        </div>

        <!-- 로딩 -->
        <div v-if="isLoading" class="loading-state">
          <div class="spinner"></div>
        </div>

        <!-- 독후감 목록 -->
        <template v-else-if="reviews.length > 0">
          <ul class="review-list">
            <li v-for="review in reviews" :key="review.id" class="review-item">
              <div class="review-header">
                <span class="rating">★ {{ review.rating }}</span>
                <span class="date">{{ formatDate(review.regDate) }}</span>
              </div>
              <p class="headline">{{ review.headline }}</p>
              <p class="content">{{ review.content }}</p>
            </li>
          </ul>

          <!-- 페이지네이션 -->
          <div class="pagination">
            <button
              class="page-btn"
              :disabled="page === 0"
              @click="changePage(page - 1)"
            >이전</button>
            <span class="page-info">{{ page + 1 }} / {{ totalPages }}</span>
            <button
              class="page-btn"
              :disabled="page >= totalPages - 1"
              @click="changePage(page + 1)"
            >다음</button>
          </div>
        </template>

        <!-- 빈 상태 -->
        <div v-else class="empty-state">
          <p class="empty-title">아직 공개된 독후감이 없습니다</p>
          <p class="empty-desc">이 책의 독후감을 첫 번째로 남겨보세요!</p>
        </div>

      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import axios from 'axios';
import { API_BASE_URL } from '../api/client';

const router = useRouter();
const route = useRoute();

const isbn13 = route.params.isbn13;
const title = route.query.title || isbn13;
const cover = route.query.cover || null;

const reviews = ref([]);
const isLoading = ref(true);
const page = ref(0);
const totalPages = ref(1);

const fetchReviews = async () => {
  isLoading.value = true;
  try {
    const response = await axios.get(`${API_BASE_URL}/api/rank/reviews`, {
      params: { isbn13, page: page.value },
      withCredentials: true,
    });
    reviews.value = response.data.content;
    totalPages.value = response.data.totalPages || 1;
  } catch {
    reviews.value = [];
  } finally {
    isLoading.value = false;
  }
};

const changePage = (newPage) => {
  page.value = newPage;
  fetchReviews();
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const formatDate = (dateString) => {
  if (!dateString) return '';
  return new Date(dateString).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
};

const handleImageError = (event) => {
  event.target.style.display = 'none';
};

onMounted(fetchReviews);
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background-color: #f9fafb;
}

.main-content {
  padding: 48px 24px;
}

.content-wrapper {
  max-width: 720px;
  margin: 0 auto;
}

.back-link {
  display: inline-block;
  font-size: 14px;
  color: #6b7280;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  margin-bottom: 24px;
  transition: color 0.15s ease;
}

.back-link:hover { color: #2563eb; }

/* 책 카드 */
.book-card {
  display: flex;
  gap: 16px;
  align-items: center;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 24px;
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

.book-cover img { width: 100%; height: 100%; object-fit: cover; }
.cover-placeholder { font-size: 28px; }
.book-info { flex: 1; min-width: 0; }

.book-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px 0;
  line-height: 1.4;
}

.book-sub {
  font-size: 13px;
  color: #9ca3af;
  margin: 0;
}

/* 로딩 */
.loading-state {
  display: flex;
  justify-content: center;
  padding: 80px 0;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e5e7eb;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* 독후감 목록 */
.review-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-item {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 24px;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.rating {
  font-size: 14px;
  font-weight: 600;
  color: #f59e0b;
}

.date {
  font-size: 13px;
  color: #9ca3af;
}

.headline {
  font-size: 17px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 12px 0;
  line-height: 1.5;
}

.content {
  font-size: 15px;
  line-height: 1.8;
  color: #374151;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 페이지네이션 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  margin-top: 32px;
}

.page-btn {
  padding: 8px 20px;
  font-size: 14px;
  font-weight: 500;
  color: #2563eb;
  background: white;
  border: 1px solid #2563eb;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.page-btn:hover:not(:disabled) {
  background: #2563eb;
  color: white;
}

.page-btn:disabled {
  color: #d1d5db;
  border-color: #e5e7eb;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: #6b7280;
}

/* 빈 상태 */
.empty-state {
  text-align: center;
  padding: 80px 24px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
}

.empty-title {
  font-size: 18px;
  font-weight: 500;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.empty-desc {
  font-size: 15px;
  color: #6b7280;
  margin: 0;
}

@media (max-width: 640px) {
  .main-content { padding: 32px 16px; }
  .review-item { padding: 20px; }
}
</style>
