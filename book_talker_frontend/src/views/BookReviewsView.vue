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
            <p class="book-sub">독자들의 독후감</p>
          </div>
        </div>

        <!-- 로딩 -->
        <div v-if="isLoading" class="loading-state">
          <div class="spinner"></div>
        </div>

        <!-- 독후감 목록 -->
        <template v-else-if="reviews.length > 0">
          <ul class="review-list">
            <li v-for="review in reviews" :key="review.id">
              <!-- 본문(상세)은 전체공개만 열람 가능 — 모임공개·비공개는 한 줄 요약만 보여 주고 이동하지 않음 -->
              <component
                :is="isOpen(review) ? RouterLink : 'div'"
                v-bind="isOpen(review) ? { to: { name: 'public-review-detail', params: { isbn13, reviewId: review.id } } } : {}"
                class="review-item"
                :class="{ locked: !isOpen(review) }"
              >
                <div class="review-header">
                  <span class="review-meta">
                    <span class="rating">★ {{ review.rating }}</span>
                    <span class="reading-count">{{ review.readingCount }}회독</span>
                    <span v-if="!isOpen(review)" class="lock-badge">
                      <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="4" y="11" width="16" height="10" rx="2" /><path d="M8 11V7a4 4 0 0 1 8 0v4" /></svg>
                      {{ getVisibilityOption(review.visibility).label }}
                    </span>
                  </span>
                  <span class="date">{{ formatDate(review.regDate) }}</span>
                </div>
                <p class="headline">{{ review.headline }}</p>
                <p v-if="!isOpen(review)" class="locked-desc">본문은 작성자가 정한 범위에서만 볼 수 있어요</p>
              </component>
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
          <p class="empty-title">아직 작성된 독후감이 없습니다</p>
          <p class="empty-desc">이 책의 독후감을 첫 번째로 남겨보세요!</p>
        </div>

      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useRouter, useRoute, RouterLink } from 'vue-router';
import axios from 'axios';
import { API_BASE_URL } from '../api/client';
import { getVisibilityOption } from '../constants/reviewVisibility';

const router = useRouter();
const route = useRoute();

const isbn13 = route.params.isbn13;
const title = route.query.title || isbn13;
const cover = route.query.cover || null;

const reviews = ref([]);
const isLoading = ref(true);
const totalPages = ref(1);

// 페이지는 쿼리(?page=, 1부터)로 들고 있어야 상세에서 뒤로 왔을 때 보던 페이지가 유지된다
const page = computed(() => Math.max(Number(route.query.page) || 1, 1) - 1);

const fetchReviews = async () => {
  isLoading.value = true;
  try {
    const response = await axios.get(`${API_BASE_URL}/rank/reviews`, {
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
  router.replace({ query: { ...route.query, page: newPage + 1 } });
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

// visibility가 없는 응답(BE 배포 전)은 예전처럼 공개 목록으로 취급
const isOpen = (review) => !review.visibility || review.visibility === 'PUBLIC';

const handleImageError = (event) => {
  event.target.style.display = 'none';
};

watch(page, fetchReviews, { immediate: true });
</script>

<style scoped>
.app-container {
  min-height: 100%;
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
  display: block;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 20px 24px;
  text-decoration: none;
  transition: border-color 0.15s ease;
}

a.review-item:hover { border-color: #93c5fd; }
a.review-item:hover .headline { color: #2563eb; }

.review-item.locked { background: #fafafa; }
.review-item.locked .headline { color: #4b5563; }

.lock-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f3f4f6;
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
}

.locked-desc {
  margin: 8px 0 0 0;
  font-size: 13px;
  color: #9ca3af;
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

.review-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.reading-count {
  font-size: 12px;
  color: #6b7280;
}

.date {
  font-size: 13px;
  color: #9ca3af;
}

.headline {
  font-size: 17px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
  line-height: 1.5;
  word-break: break-word;
  transition: color 0.15s ease;
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
