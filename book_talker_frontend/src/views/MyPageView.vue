<template>
  <div class="app-container">
    <main class="main-content">
      <div class="content-wrapper">
        <h2 class="page-title">
          내가 작성한 리뷰
          <span v-if="!isLoading" class="review-count">({{ bookGroups.length }}권)</span>
        </h2>

        <div v-if="isLoading" class="loading-state">
          <div class="spinner"></div>
          <p class="loading-text">리뷰를 불러오는 중...</p>
        </div>

        <div v-else-if="bookGroups.length === 0" class="empty-state">
          <p class="empty-title">작성한 리뷰가 없습니다</p>
          <p class="empty-subtitle">책을 검색해서 첫 리뷰를 작성해보세요</p>
          <router-link to="/book-search" class="search-btn">책 검색하러 가기</router-link>
        </div>

        <div v-else class="review-list">
          <article
            v-for="group in bookGroups"
            :key="group.isbn13"
            class="review-card"
            @click="onCardClick(group)"
          >
            <div class="book-info">
              <div class="book-cover">
                <img
                  v-if="group.cover"
                  :src="group.cover"
                  :alt="group.title"
                  @error="handleImageError"
                />
                <div v-else class="cover-placeholder">📖</div>
              </div>
              <div class="book-details">
                <div class="book-title-row">
                  <h3 class="book-title">{{ group.title || '-' }}</h3>
                  <span class="date">{{ formatDate(group.latestDate) }}</span>
                </div>
                <span v-if="group.maxReadingCount >= 2" class="reading-badge">
                  {{ group.maxReadingCount }}회독까지
                </span>
              </div>
            </div>

            <div v-if="group.reviews.length === 1" class="review-meta">
              <span class="rating">⭐ {{ group.reviews[0].rating || '-' }}</span>
              <div class="meta-actions">
                <button
                  class="reread-btn"
                  @click="onNextReading(group.reviews[0], $event)"
                >
                  한번 더 봤어요
                </button>
                <button
                  class="delete-btn"
                  @click="onDeleteReview(group.reviews[0].id, $event)"
                  :disabled="deletingId === group.reviews[0].id"
                >
                  {{ deletingId === group.reviews[0].id ? '삭제 중...' : '🗑 삭제하기' }}
                </button>
              </div>
            </div>

            <div v-else class="review-meta">
              <span class="multi-hint">회독 목록 보기 →</span>
            </div>
          </article>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
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
    const response = await axios.get(`${API_BASE_URL}/auth/session`, { withCredentials: true });
    isLoggedIn.value = response.status === 200;
  } catch {
    isLoggedIn.value = false;
  }
};

const fetchReviews = async () => {
  isLoading.value = true;
  try {
    const response = await apiClient.get('/review/list', { skipErrorCodes: [404] });
    reviews.value = response.data || [];
  } catch {
    reviews.value = [];
  } finally {
    isLoading.value = false;
  }
};

const bookGroups = computed(() => {
  const map = {};
  reviews.value.forEach((r) => {
    if (!map[r.bookIsbn13]) {
      map[r.bookIsbn13] = {
        isbn13: r.bookIsbn13,
        title: r.bookTitle,
        cover: r.bookCover,
        reviews: [],
      };
    }
    map[r.bookIsbn13].reviews.push(r);
  });

  return Object.values(map)
    .map((group) => {
      const sorted = [...group.reviews].sort((a, b) => new Date(b.regDate) - new Date(a.regDate));
      return {
        ...group,
        reviews: sorted,
        maxReadingCount: Math.max(...group.reviews.map((r) => r.readingCount || 0)),
        latestDate: sorted[0]?.regDate,
      };
    })
    .sort((a, b) => new Date(b.latestDate) - new Date(a.latestDate));
});

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

const onCardClick = (group) => {
  if (group.reviews.length === 1) {
    router.push({ name: 'review-detail', params: { id: group.reviews[0].id } });
  } else {
    router.push({ name: 'my-book-detail', params: { isbn13: group.isbn13 } });
  }
};

const onNextReading = (review, event) => {
  event.stopPropagation();
  router.push({
    name: 'review-create',
    query: {
      mode: 'next-reading',
      isbn13: review.bookIsbn13,
      title: review.bookTitle,
      cover: review.bookCover || '',
      readingCount: review.readingCount,
    },
  });
};

const onDeleteReview = async (reviewId, event) => {
  event.stopPropagation();
  if (!confirm('독후감을 삭제할까요? 이 작업은 되돌릴 수 없습니다.')) return;
  deletingId.value = reviewId;
  try {
    await apiClient.delete('/review', { params: { reviewId } });
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
.app-container { min-height: 100vh; background-color: #f9fafb; }
.main-content { padding: 48px 24px; }
.content-wrapper { max-width: 720px; margin: 0 auto; }

.page-title { font-size: 28px; font-weight: 600; color: #1f2937; margin: 0 0 32px 0; line-height: 1.3; }
.review-count { font-size: 20px; font-weight: 400; color: #9ca3af; }

.loading-state { text-align: center; padding: 64px 0; }
.spinner { width: 32px; height: 32px; border: 3px solid #e5e7eb; border-top-color: #2563eb; border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto 16px; }
@keyframes spin { to { transform: rotate(360deg); } }
.loading-text { color: #6b7280; font-size: 15px; }

.empty-state { text-align: center; padding: 64px 24px; background: white; border: 1px solid #e5e7eb; border-radius: 8px; }
.empty-title { font-size: 18px; font-weight: 500; color: #1f2937; margin: 0 0 8px 0; }
.empty-subtitle { font-size: 15px; color: #6b7280; margin: 0 0 24px 0; }
.search-btn { display: inline-block; padding: 10px 20px; font-size: 15px; font-weight: 500; color: white; background: #2563eb; border-radius: 8px; text-decoration: none; transition: background-color 0.15s ease; }
.search-btn:hover { background: #1d4ed8; }

.review-list { display: flex; flex-direction: column; gap: 24px; }
.review-card { background: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 24px; cursor: pointer; transition: border-color 0.15s ease, box-shadow 0.15s ease; }
.review-card:hover { border-color: #2563eb; box-shadow: 0 2px 8px rgba(37, 99, 235, 0.08); }

.book-info { display: flex; gap: 16px; margin-bottom: 20px; }
.book-cover { width: 60px; height: 80px; flex-shrink: 0; border-radius: 4px; overflow: hidden; background: #f3f4f6; display: flex; align-items: center; justify-content: center; }
.book-cover img { width: 100%; height: 100%; object-fit: cover; }
.cover-placeholder { font-size: 32px; color: #9ca3af; }

.book-details { flex: 1; min-width: 0; }
.book-title-row { display: flex; justify-content: space-between; align-items: baseline; gap: 8px; margin-bottom: 6px; }
.book-title { font-size: 18px; font-weight: 600; color: #1f2937; margin: 0; line-height: 1.4; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.date { font-size: 13px; color: #9ca3af; flex-shrink: 0; }

.reading-badge { display: inline-block; font-size: 12px; font-weight: 600; color: #2563eb; background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 12px; padding: 2px 10px; }

.review-meta { display: flex; justify-content: space-between; align-items: center; padding-top: 16px; border-top: 1px solid #f3f4f6; }
.rating { font-size: 14px; color: #2563eb; font-weight: 500; }
.multi-hint { font-size: 13px; color: #9ca3af; margin-left: auto; }

.meta-actions { display: flex; gap: 8px; align-items: center; }
.reread-btn { font-size: 13px; font-weight: 500; color: #059669; background: none; border: 1px solid #6ee7b7; border-radius: 6px; cursor: pointer; padding: 7px 14px; transition: all 0.15s ease; }
.reread-btn:hover { background: #ecfdf5; border-color: #059669; }
.delete-btn { font-size: 13px; font-weight: 500; color: #ef4444; background: none; border: 1px solid #fca5a5; border-radius: 6px; cursor: pointer; padding: 7px 16px; transition: all 0.15s ease; }
.delete-btn:hover:not(:disabled) { background: #fef2f2; border-color: #ef4444; }
.delete-btn:disabled { opacity: 0.5; cursor: not-allowed; }

@media (max-width: 640px) {
  .main-content { padding: 32px 16px; }
  .page-title { font-size: 24px; margin-bottom: 24px; }
  .review-card { padding: 20px; }
  .book-cover { width: 50px; height: 70px; }
}
</style>
