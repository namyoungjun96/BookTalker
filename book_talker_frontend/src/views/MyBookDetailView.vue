<template>
  <div class="app-container">
    <main class="main-content">
      <div class="content-wrapper">

        <div v-if="isLoading" class="loading-state">
          <div class="spinner"></div>
          <p class="loading-text">불러오는 중...</p>
        </div>

        <template v-else>
          <div class="book-header">
            <button @click="router.back()" class="back-link">← 마이페이지로</button>
            <div class="book-header-inner">
              <div class="book-cover">
                <img
                  v-if="bookInfo?.bookCover"
                  :src="bookInfo.bookCover"
                  :alt="bookInfo.bookTitle"
                  @error="handleImageError"
                />
                <div v-else class="cover-placeholder">📖</div>
              </div>
              <div class="book-meta">
                <h2 class="book-title">{{ bookInfo?.bookTitle || '-' }}</h2>
              </div>
            </div>
            <button class="reread-btn" @click="onNextReading">한번 더 봤어요</button>
          </div>

          <div v-if="bookReviews.length === 0" class="empty-state">
            <p>이 책의 리뷰가 없습니다.</p>
          </div>

          <div v-else class="reading-list">
            <article
              v-for="review in bookReviews"
              :key="review.id"
              class="reading-row"
              @click="goToDetail(review.id)"
            >
              <div class="row-main">
                <span class="reading-badge">{{ review.readingCount }}회독</span>
                <span class="date">{{ formatDate(review.regDate) }}</span>
                <span class="rating">⭐ {{ review.rating || '-' }}</span>
                <button
                  class="delete-btn"
                  @click.stop="onDeleteReview(review.id)"
                  :disabled="deletingId === review.id"
                >
                  {{ deletingId === review.id ? '삭제 중...' : '🗑' }}
                </button>
              </div>
              <p class="headline">{{ review.headline || '-' }}</p>
            </article>
          </div>
        </template>

      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useToast } from 'vue-toastification';
import apiClient from '../api/client';

const route = useRoute();
const router = useRouter();
const toast = useToast();

const isbn13 = route.params.isbn13;
const allReviews = ref([]);
const isLoading = ref(false);
const deletingId = ref(null);

const bookReviews = computed(() =>
  allReviews.value
    .filter((r) => r.bookIsbn13 === isbn13)
    .sort((a, b) => b.readingCount - a.readingCount)
);

const bookInfo = computed(() => bookReviews.value[0] || null);

const fetchReviews = async () => {
  isLoading.value = true;
  try {
    const response = await apiClient.get('/review/list', { skipErrorCodes: [404] });
    allReviews.value = response.data || [];
    if (bookReviews.value.length === 0) {
      router.replace({ name: 'mypage' });
    }
  } catch {
    allReviews.value = [];
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

const onNextReading = () => {
  const maxCount = Math.max(...bookReviews.value.map((r) => r.readingCount || 0));
  router.push({
    name: 'review-create',
    query: {
      mode: 'next-reading',
      isbn13,
      title: bookInfo.value?.bookTitle,
      cover: bookInfo.value?.bookCover || '',
      readingCount: maxCount,
    },
  });
};

const onDeleteReview = async (reviewId) => {
  if (!confirm('독후감을 삭제할까요? 이 작업은 되돌릴 수 없습니다.')) return;
  deletingId.value = reviewId;
  try {
    await apiClient.delete('/review', { params: { reviewId } });
    allReviews.value = allReviews.value.filter((r) => r.id !== reviewId);
    toast.success('독후감이 삭제되었습니다.');
    if (bookReviews.value.length === 0) {
      router.replace({ name: 'mypage' });
    }
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

onMounted(fetchReviews);
</script>

<style scoped>
.app-container { min-height: 100vh; background-color: #f9fafb; }
.main-content { padding: 48px 24px; }
.content-wrapper { max-width: 720px; margin: 0 auto; }

.loading-state { text-align: center; padding: 64px 0; }
.spinner { width: 32px; height: 32px; border: 3px solid #e5e7eb; border-top-color: #2563eb; border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto 16px; }
@keyframes spin { to { transform: rotate(360deg); } }
.loading-text { color: #6b7280; font-size: 15px; }

.back-link { font-size: 14px; color: #6b7280; background: none; border: none; cursor: pointer; padding: 0; margin-bottom: 20px; display: block; }
.back-link:hover { color: #2563eb; }

.book-header { margin-bottom: 32px; display: flex; flex-direction: column; gap: 16px; }
.book-header-inner { display: flex; gap: 16px; align-items: flex-start; }
.book-cover { width: 64px; height: 86px; flex-shrink: 0; border-radius: 4px; overflow: hidden; background: #f3f4f6; display: flex; align-items: center; justify-content: center; }
.book-cover img { width: 100%; height: 100%; object-fit: cover; }
.cover-placeholder { font-size: 32px; color: #9ca3af; }
.book-meta { display: flex; flex-direction: column; justify-content: center; }
.book-title { font-size: 20px; font-weight: 600; color: #1f2937; margin: 0; line-height: 1.4; }
.reread-btn { align-self: flex-start; font-size: 14px; font-weight: 500; color: #059669; background: none; border: 1px solid #6ee7b7; border-radius: 6px; cursor: pointer; padding: 8px 16px; transition: all 0.15s ease; }
.reread-btn:hover { background: #ecfdf5; border-color: #059669; }

.empty-state { text-align: center; padding: 48px; color: #6b7280; font-size: 15px; }

.reading-list { display: flex; flex-direction: column; gap: 12px; }
.reading-row { background: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px 20px; cursor: pointer; transition: border-color 0.15s ease, box-shadow 0.15s ease; }
.reading-row:hover { border-color: #2563eb; box-shadow: 0 2px 8px rgba(37, 99, 235, 0.08); }

.row-main { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.reading-badge { font-size: 12px; font-weight: 600; color: #2563eb; background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 12px; padding: 2px 10px; flex-shrink: 0; }
.date { font-size: 13px; color: #9ca3af; }
.rating { font-size: 13px; color: #2563eb; font-weight: 500; }
.delete-btn { margin-left: auto; font-size: 13px; color: #ef4444; background: none; border: 1px solid #fca5a5; border-radius: 6px; cursor: pointer; padding: 4px 10px; transition: all 0.15s ease; flex-shrink: 0; }
.delete-btn:hover:not(:disabled) { background: #fef2f2; border-color: #ef4444; }
.delete-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.headline { font-size: 14px; color: #6b7280; margin: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

@media (max-width: 640px) {
  .main-content { padding: 32px 16px; }
  .row-main { flex-wrap: wrap; gap: 8px; }
}
</style>
