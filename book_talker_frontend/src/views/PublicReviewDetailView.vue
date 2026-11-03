<template>
  <div class="app-container">
    <main class="main-content">
      <div class="content-wrapper">

        <div v-if="isLoading" class="loading-state">
          <div class="spinner"></div>
        </div>

        <!-- 404: 비공개로 바뀌었거나 삭제됨 (서버가 구분하지 않음) -->
        <div v-else-if="notFound" class="empty-state">
          <p class="empty-title">독후감을 찾을 수 없어요</p>
          <p class="empty-subtitle">비공개로 바뀌었거나 삭제됐을 수 있어요</p>
          <button type="button" class="btn-primary" @click="goToList">독후감 목록으로</button>
        </div>

        <div v-else-if="!review" class="empty-state">
          <p class="empty-title">독후감을 불러오지 못했습니다</p>
          <p class="empty-subtitle">잠시 후 다시 시도해주세요</p>
        </div>

        <template v-else>
          <div class="top-bar">
            <button type="button" class="back-link" @click="goToList">
              <img :src="backIcon" alt="" width="14" height="14" />
              독후감 목록
            </button>
          </div>

          <ReviewArticle :review="review">
            <template #info-note>모든 사용자에게 공개된 독후감이에요.</template>
          </ReviewArticle>
        </template>

      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import apiClient from '../api/client';
import ReviewArticle from '../components/ReviewArticle.vue';
import backIcon from '../assets/icons/back.svg';

const route = useRoute();
const router = useRouter();

const { isbn13, reviewId } = route.params;

const review = ref(null);
const isLoading = ref(true);
const notFound = ref(false);

// 숫자가 아닌 id는 400 — 없는 독후감과 같은 화면으로 처리
const NOT_FOUND_CODES = [400, 404];

const fetchDetail = async () => {
  try {
    const response = await apiClient.get(`/rank/reviews/${reviewId}`, {
      skipErrorCodes: NOT_FOUND_CODES,
    });
    review.value = response.data;
  } catch (error) {
    if (NOT_FOUND_CODES.includes(error.response?.status)) notFound.value = true;
  } finally {
    isLoading.value = false;
  }
};

// 목록에서 들어왔으면 뒤로가기(보던 페이지 유지), 링크로 바로 들어왔으면 목록 1페이지로
const goToList = () => {
  if (window.history.state?.back) {
    router.back();
    return;
  }
  const query = review.value
    ? { title: review.value.bookTitle, ...(review.value.cover && { cover: review.value.cover }) }
    : {};
  router.push({ name: 'book-reviews', params: { isbn13 }, query });
};

onMounted(fetchDetail);
</script>

<style scoped>
.app-container { min-height: 100%; }
.main-content { padding: 42px 24px 90px; }
.content-wrapper { max-width: 720px; margin: 0 auto; }

/* 로딩·빈 상태 */
.loading-state { display: flex; justify-content: center; padding: 80px 0; }
.spinner { width: 32px; height: 32px; border: 3px solid #e5e7eb; border-top-color: #2563eb; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.empty-state { text-align: center; padding: 64px 24px; background: white; border: 1px solid #e5e7eb; border-radius: 8px; }
.empty-title { font-size: 18px; font-weight: 500; color: #1f2937; margin: 0 0 8px 0; }
.empty-subtitle { font-size: 15px; color: #6b7280; margin: 0 0 24px 0; }
.btn-primary { display: inline-block; padding: 10px 20px; font-size: 15px; font-weight: 500; color: white; background: #2563eb; border: none; border-radius: 8px; cursor: pointer; }
.btn-primary:hover { background: #1d4ed8; }

/* 상단 바 */
.top-bar { display: flex; align-items: center; min-height: 38px; margin-bottom: 12px; }
.back-link { display: inline-flex; align-items: center; gap: 7px; padding: 7px 0; font-size: 13px; color: #6b7280; background: none; border: none; cursor: pointer; transition: color 0.15s ease; }
.back-link:hover { color: #2563eb; }

@media (max-width: 640px) {
  .main-content { padding: 32px 16px 64px; }
}
</style>
