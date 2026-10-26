<template>
  <div class="app-container">
    <main class="main-content">
      <div class="content-wrapper">

        <div v-if="isLoading" class="loading-state">
          <div class="spinner"></div>
        </div>

        <!-- 404: 비공개로 바뀌었거나 삭제됐거나, 모임원이 아님 (서버가 구분하지 않음) -->
        <div v-else-if="notFound" class="empty-state">
          <p class="empty-title">독후감을 찾을 수 없어요</p>
          <p class="empty-subtitle">비공개로 바뀌었거나 삭제됐을 수 있어요</p>
          <router-link :to="teamRoute" class="btn-primary">모임으로 돌아가기</router-link>
        </div>

        <div v-else-if="!review" class="empty-state">
          <p class="empty-title">독후감을 불러오지 못했습니다</p>
          <p class="empty-subtitle">잠시 후 다시 시도해주세요</p>
        </div>

        <template v-else>
          <div class="top-bar">
            <router-link :to="teamRoute" class="back-link">
              <img :src="backIcon" alt="" width="14" height="14" />
              모임 독후감
            </router-link>
            <div v-if="review.mine && !isEditing" class="owner-actions">
              <button type="button" class="action-btn edit" @click="isEditing = true">
                <img :src="editIcon" alt="" width="14" height="14" />
                수정
              </button>
              <button type="button" class="action-btn delete" :disabled="isDeleting" @click="onDelete">
                <img :src="deleteIcon" alt="" width="14" height="14" />
                삭제
              </button>
            </div>
          </div>

          <!-- 수정 모드: 화면 이동 없이 제자리에서 폼으로 전환 -->
          <ReviewEditForm
            v-if="isEditing"
            :review-id="review.reviewId"
            :review="review"
            private-notice="비공개로 저장하면 모임 멤버에게 더 이상 보이지 않아요."
            @saved="onSaved"
            @cancel="isEditing = false"
          />

          <ReviewArticle v-else :review="review" show-writer :team-name="review.teamName">
            <template #info-note>{{ visibilityNote }}</template>
          </ReviewArticle>
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
import ReviewEditForm from '../components/ReviewEditForm.vue';
import ReviewArticle from '../components/ReviewArticle.vue';
import backIcon from '../assets/icons/back.svg';
import editIcon from '../assets/icons/edit.svg';
import deleteIcon from '../assets/icons/delete.svg';

const route = useRoute();
const router = useRouter();
const toast = useToast();

const { teamId, reviewId } = route.params;
const teamRoute = { name: 'team-detail', params: { teamId } };

const review = ref(null);
const isLoading = ref(true);
const notFound = ref(false);
const isDeleting = ref(false);
const isEditing = ref(false);

// 숫자가 아닌 id는 400 — 없는 독후감과 같은 화면으로 처리
const NOT_FOUND_CODES = [400, 404];

const visibilityNote = computed(() => (review.value.visibility === 'GROUP'
  ? `${review.value.teamName ?? '이 모임'} 멤버에게만 보여요.`
  : '모든 사용자에게 공개된 독후감이에요.'));

const fetchDetail = async () => {
  try {
    const response = await apiClient.get(`/teams/${teamId}/reviews/${reviewId}`, {
      skipErrorCodes: NOT_FOUND_CODES,
    });
    review.value = response.data;
  } catch (error) {
    if (NOT_FOUND_CODES.includes(error.response?.status)) notFound.value = true;
  } finally {
    isLoading.value = false;
  }
};

// 비공개로 바꾸면 모임에서 볼 수 없는 글이 되므로(새로고침 시 404) 피드로 보낸다
const onSaved = (updated) => {
  if (updated.visibility === 'PRIVATE') {
    toast.info('비공개로 바뀌어 모임에서 보이지 않아요.');
    router.replace(teamRoute);
    return;
  }
  review.value = { ...review.value, ...updated };
  isEditing.value = false;
  toast.success('독후감이 수정되었습니다.');
};

const onDelete = async () => {
  if (!confirm('독후감을 삭제할까요? 이 작업은 되돌릴 수 없습니다.')) return;
  isDeleting.value = true;
  try {
    await apiClient.delete('/review', { params: { reviewId } });
    toast.success('독후감이 삭제되었습니다.');
    router.replace(teamRoute);
  } catch (e) {
    if (e.response?.status === 403) {
      toast.error('삭제 권한이 없습니다.');
    } else {
      toast.error('삭제 중 오류가 발생했습니다.');
    }
  } finally {
    isDeleting.value = false;
  }
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
.btn-primary { display: inline-block; padding: 10px 20px; font-size: 15px; font-weight: 500; color: white; background: #2563eb; border-radius: 8px; text-decoration: none; }
.btn-primary:hover { background: #1d4ed8; }

/* 상단 바 */
.top-bar { display: flex; align-items: center; justify-content: space-between; min-height: 38px; margin-bottom: 12px; }
.back-link { display: inline-flex; align-items: center; gap: 7px; padding: 7px 0; font-size: 13px; color: #6b7280; text-decoration: none; transition: color 0.15s ease; }
.back-link:hover { color: #2563eb; }

.owner-actions { display: flex; align-items: center; gap: 4px; }
.action-btn { display: inline-flex; align-items: center; gap: 7px; min-height: 34px; padding: 7px 10px; font-size: 11px; font-weight: 700; border-radius: 4px; border: 1px solid transparent; background: none; cursor: pointer; transition: background-color 0.15s ease; }
.action-btn.edit { background: white; border-color: #93c5fd; color: #2563eb; }
.action-btn.edit:hover { background: #eff6ff; }
.action-btn.delete { color: #b42318; }
.action-btn.delete:hover { background: #fef3f2; }
.action-btn:disabled { opacity: 0.5; cursor: not-allowed; }

@media (max-width: 640px) {
  .main-content { padding: 32px 16px 64px; }
}
</style>
