<template>
  <div class="app-container">
    <main class="main-content">
      <div class="content-wrapper">

        <div v-if="isLoading" class="loading-state">
          <div class="spinner"></div>
        </div>

        <div v-else-if="error" class="empty-state">
          <p class="empty-title">{{ error }}</p>
          <button type="button" class="btn-primary" @click="router.back()">돌아가기</button>
        </div>

        <template v-else-if="review">
          <div class="top-bar">
            <button type="button" class="back-link" @click="router.back()">
              <img :src="backIcon" alt="" width="14" height="14" />
              마이페이지로
            </button>
            <button v-if="!isEditing" type="button" class="action-btn edit" @click="isEditing = true">
              <img :src="editIcon" alt="" width="14" height="14" />
              수정
            </button>
          </div>

          <!-- 수정 모드: 화면 이동 없이 제자리에서 폼으로 전환 -->
          <ReviewEditForm
            v-if="isEditing"
            :review-id="review.id"
            :review="review"
            @saved="onSaved"
            @cancel="isEditing = false"
          />

          <ReviewArticle v-else :review="article">
            <template #info-note>{{ visibilityNote }}</template>
          </ReviewArticle>
        </template>

      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useToast } from 'vue-toastification';
import apiClient from '../api/client';
import ReviewEditForm from '../components/ReviewEditForm.vue';
import ReviewArticle from '../components/ReviewArticle.vue';
import backIcon from '../assets/icons/back.svg';
import editIcon from '../assets/icons/edit.svg';

const router = useRouter();
const route = useRoute();
const toast = useToast();

const review = ref(null);
const isLoading = ref(true);
const error = ref(null);
const isEditing = ref(false);

// 내 독후감 API는 표지 필드명이 bookCover — 모임 독후감(cover)과 맞춰 넘긴다
const article = computed(() => ({ ...review.value, cover: review.value.bookCover }));

const VISIBILITY_NOTES = {
  PUBLIC: '모든 사용자에게 공개된 독후감이에요.',
  GROUP: '내가 속한 모임 멤버에게만 보여요.',
  PRIVATE: '나만 볼 수 있는 독후감이에요.',
};

const visibilityNote = computed(() => VISIBILITY_NOTES[review.value.visibility] ?? VISIBILITY_NOTES.PRIVATE);

const fetchDetail = async () => {
  try {
    const response = await apiClient.get('/review/detail', {
      params: { reviewId: route.params.id },
    });
    review.value = response.data;
  } catch (e) {
    if (e.response?.status === 403) {
      error.value = '접근 권한이 없는 독후감입니다.';
    } else {
      error.value = '독후감을 불러올 수 없습니다.';
    }
  } finally {
    isLoading.value = false;
  }
};

// "2026-10-26" — 수정일은 날짜만 표시하므로 로컬 날짜로 갱신
const today = () => {
  const now = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
};

// 저장 후 로컬 데이터 갱신
const onSaved = (updated) => {
  review.value = {
    ...review.value,
    ...updated,
    modDate: today(),
  };
  isEditing.value = false;
  toast.success('독후감이 수정되었습니다.');
};

onMounted(fetchDetail);
</script>

<style scoped>
.app-container { min-height: 100%; }
.main-content { padding: 42px 24px 90px; }
.content-wrapper { max-width: 720px; margin: 0 auto; }

/* 로딩·에러 */
.loading-state { display: flex; justify-content: center; padding: 80px 0; }
.spinner { width: 32px; height: 32px; border: 3px solid #e5e7eb; border-top-color: #2563eb; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.empty-state { text-align: center; padding: 64px 24px; background: white; border: 1px solid #e5e7eb; border-radius: 8px; }
.empty-title { font-size: 18px; font-weight: 500; color: #1f2937; margin: 0 0 24px 0; }
.btn-primary { display: inline-block; padding: 10px 20px; font-size: 15px; font-weight: 500; color: white; background: #2563eb; border: none; border-radius: 8px; cursor: pointer; }
.btn-primary:hover { background: #1d4ed8; }

/* 상단 바 */
.top-bar { display: flex; align-items: center; justify-content: space-between; min-height: 38px; margin-bottom: 12px; }
.back-link { display: inline-flex; align-items: center; gap: 7px; padding: 7px 0; font-size: 13px; color: #6b7280; background: none; border: none; cursor: pointer; transition: color 0.15s ease; }
.back-link:hover { color: #2563eb; }

.action-btn { display: inline-flex; align-items: center; gap: 7px; min-height: 34px; padding: 7px 10px; font-size: 11px; font-weight: 700; border-radius: 4px; border: 1px solid transparent; background: none; cursor: pointer; transition: background-color 0.15s ease; }
.action-btn.edit { background: white; border-color: #93c5fd; color: #2563eb; }
.action-btn.edit:hover { background: #eff6ff; }

@media (max-width: 640px) {
  .main-content { padding: 32px 16px 64px; }
}
</style>
