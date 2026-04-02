<template>
  <div class="app-container">
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
          <router-link to="/book-search" class="search-btn">책 검색하러 가기</router-link>
        </div>

        <!-- 리뷰 카드 리스트 -->
        <div v-else class="review-list">
          <article
            v-for="review in reviews"
            :key="review.id"
            class="review-card"
            @click="goToDetail(review.id)"
          >
            <div class="book-info">
              <div class="book-cover">
                <img
                  v-if="review.bookCover"
                  :src="review.bookCover"
                  :alt="review.bookTitle"
                  @error="handleImageError"
                />
                <div v-else class="cover-placeholder">📖</div>
              </div>
              <div class="book-details">
                <div class="book-title-row">
                  <h3 class="book-title">{{ review.bookTitle || '-' }}</h3>
                  <span class="date">{{ formatDate(review.regDate) }}</span>
                </div>
                <p class="headline-text">{{ review.headline || '-' }}</p>
              </div>
            </div>

            <div class="review-meta">
              <span class="rating">⭐ {{ review.rating || '-' }}</span>
              <button
                class="delete-btn"
                @click.stop="onDeleteReview(review.id)"
                :disabled="deletingId === review.id"
              >
                {{ deletingId === review.id ? '삭제 중...' : '🗑 삭제하기' }}
              </button>
            </div>
          </article>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useToast } from 'vue-toastification';
import apiClient, { API_BASE_URL } from '../api/client';
import axios from 'axios';

const router = useRouter();
const toast = useToast();

const reviews = ref([]);
const isLoading = ref(false);
const isLoggedIn = ref(false);
const deletingId = ref(null);

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

const fetchReviews = async () => {
  isLoading.value = true;
  try {
    const response = await apiClient.get('/api/review/list', {
      skipErrorCodes: [404],
    });
    reviews.value = response.data || [];
  } catch (error) {
    reviews.value = [];
  } finally {
    isLoading.value = false;
  }
};

const formatDate = (dateString) => {
  if (!dateString) return '-';
  try {
    return new Date(dateString).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  } catch {
    return dateString;
  }
};

const goToDetail = (reviewId) => {
  router.push({ name: 'review-detail', params: { id: reviewId } });
};

const onDeleteReview = async (reviewId) => {
  if (!confirm('독후감을 삭제할까요? 이 작업은 되돌릴 수 없습니다.')) return;
  deletingId.value = reviewId;
  try {
    await apiClient.delete('/api/review', { params: { reviewId } });
    reviews.value = reviews.value.filter((r) => r.id !== reviewId);
    toast.success('독후감이 삭제되었습니다.');
  } catch (e) {
    if (e.response?.status === 403) {
      toast.error('삭제 권한이 없습니다.');
    } else {
      toast.error('삭제 중 오류가 발생했습니다.');
    }
  } finally {
    deletingId.value = null;
  }
};

const handleImageError = (event) => {
  event.target.style.display = 'none';
};

onMounted(async () => {
  await checkLoginStatus();
  if (isLoggedIn.value) {
    fetchReviews();
  } else {
    router.push({ name: 'login' });
  }
});
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

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 32px 0;
  line-height: 1.3;
}

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
  margin: 0 0 24px 0;
}

.search-btn {
  display: inline-block;
  padding: 10px 20px;
  font-size: 15px;
  font-weight: 500;
  color: white;
  background: #2563eb;
  border-radius: 8px;
  text-decoration: none;
  transition: background-color 0.15s ease;
}

.search-btn:hover {
  background: #1d4ed8;
}

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
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.review-card:hover {
  border-color: #2563eb;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.08);
}

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

.book-title-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 4px;
}

.book-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.date {
  font-size: 13px;
  color: #9ca3af;
  flex-shrink: 0;
}

.headline-text {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.review-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid #f3f4f6;
  margin-top: 20px;
}

.rating {
  font-size: 14px;
  color: #2563eb;
  font-weight: 500;
}

.delete-btn {
  font-size: 13px;
  font-weight: 500;
  color: #ef4444;
  background: none;
  border: 1px solid #fca5a5;
  border-radius: 6px;
  cursor: pointer;
  padding: 7px 16px;
  transition: all 0.15s ease;
}

.delete-btn:hover:not(:disabled) {
  background: #fef2f2;
  border-color: #ef4444;
}

.delete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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

  .book-cover {
    width: 50px;
    height: 70px;
  }
}
</style>
