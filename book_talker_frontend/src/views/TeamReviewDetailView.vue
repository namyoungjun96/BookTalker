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

          <article v-else class="review-article">
            <header class="article-head">
              <div class="head-main">
                <div class="badge-row">
                  <span class="visibility-badge" :class="visibility.className">
                    <img v-if="review.visibility === 'GROUP'" :src="groupIcon" alt="" width="11" height="11" />
                    {{ visibility.label }}
                  </span>
                  <span v-if="review.teamName" class="team-name">{{ review.teamName }}</span>
                </div>
                <p class="headline-label">한 줄 요약</p>
                <h2 class="headline">{{ review.headline }}</h2>
                <p class="writer">
                  <strong>{{ review.displayName }}</strong>
                  <span v-if="review.mine" class="me-mark">(나)</span>
                </p>
              </div>

              <div class="head-book">
                <div class="book-cover">
                  <img v-if="review.cover" :src="review.cover" alt="" />
                  <span v-else class="book-cover-fallback">{{ review.bookTitle }}</span>
                </div>
                <div class="book-info">
                  <p class="book-label">읽은 책</p>
                  <h1 class="book-title">{{ review.bookTitle }}</h1>
                  <p v-if="review.author" class="book-author">{{ review.author }}</p>
                </div>
              </div>
            </header>

            <div class="article-body">
              <section class="review-content">
                <h3 class="content-title">독후감</h3>
                <p class="content-text">{{ review.content }}</p>
              </section>

              <aside class="review-info" aria-label="독후감 정보">
                <div class="info-row">
                  <span class="info-label">
                    <img :src="starIcon" alt="" width="13" height="13" />
                    평점
                  </span>
                  <strong class="rating">
                    {{ review.rating ?? '-' }}<span class="rating-max"> / 5</span>
                  </strong>
                </div>
                <div class="info-row">
                  <span class="info-label">
                    <img :src="repeatIcon" alt="" width="13" height="13" />
                    회독 수
                  </span>
                  <strong class="info-value">{{ review.readingCount }}회독</strong>
                </div>
                <div class="info-row">
                  <span class="info-label">
                    <img :src="calendarIcon" alt="" width="13" height="13" />
                    작성일
                  </span>
                  <strong class="info-value">{{ formatRegDate(review.regDate) }}</strong>
                </div>
                <p class="info-note">
                  <img :src="membersIcon" alt="" width="13" height="13" />
                  <span>{{ visibilityNote }}</span>
                </p>
              </aside>
            </div>
          </article>
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
import { getVisibilityOption } from '../constants/reviewVisibility';
import ReviewEditForm from '../components/ReviewEditForm.vue';
import backIcon from '../assets/icons/back.svg';
import editIcon from '../assets/icons/edit.svg';
import deleteIcon from '../assets/icons/delete.svg';
import groupIcon from '../assets/icons/group.svg';
import starIcon from '../assets/icons/star.svg';
import repeatIcon from '../assets/icons/repeat.svg';
import calendarIcon from '../assets/icons/calendar.svg';
import membersIcon from '../assets/icons/members.svg';

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

const visibility = computed(() => getVisibilityOption(review.value.visibility));

const visibilityNote = computed(() => (review.value.visibility === 'GROUP'
  ? `${review.value.teamName ?? '이 모임'} 멤버에게만 보여요.`
  : '모든 사용자에게 공개된 독후감이에요.'));

// "2026-03-22" → "2026년 3월 22일"
const formatRegDate = (regDate) => {
  const [year, month, day] = regDate.split('-').map(Number);
  return `${year}년 ${month}월 ${day}일`;
};

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
.action-btn { display: inline-flex; align-items: center; gap: 7px; min-height: 34px; padding: 7px 10px; font-size: 12px; font-weight: 700; border-radius: 4px; border: 1px solid transparent; background: none; cursor: pointer; transition: background-color 0.15s ease; }
.action-btn.edit { background: white; border-color: #93c5fd; color: #2563eb; }
.action-btn.edit:hover { background: #eff6ff; }
.action-btn.delete { color: #b42318; }
.action-btn.delete:hover { background: #fef3f2; }
.action-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* 본문 카드 */
.review-article { background: white; border: 1px solid #e5e7eb; border-radius: 6px; overflow: hidden; }

.article-head { display: grid; grid-template-columns: minmax(0, 1fr) 185px; gap: 28px; align-items: center; padding: 28px 34px; background: #fbfdff; border-bottom: 1px solid #e5e7eb; }

.badge-row { display: flex; align-items: center; gap: 8px; min-height: 20px; }
.visibility-badge { display: inline-flex; align-items: center; gap: 4px; padding: 4px 8px; border-radius: 3px; font-size: 11px; font-weight: 700; line-height: 1.4; }
.visibility-badge.public { background: #eff6ff; color: #2563eb; }
.visibility-badge.group { background: #f3f0ff; color: #7658bd; }
.team-name { min-width: 0; font-size: 12px; color: #6b7280; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.headline-label { margin: 10px 0 0 0; font-size: 11px; font-weight: 700; color: #2563eb; letter-spacing: 0.4px; }
.headline { margin: 9px 0 0 0; font-size: 20px; font-weight: 400; line-height: 1.55; letter-spacing: -0.38px; color: #111827; word-break: break-word; }
.writer { display: flex; align-items: center; gap: 4px; margin: 10px 0 0 0; font-size: 12px; }
.writer strong { color: #374151; }
.writer .me-mark { color: #9ca3af; }

.head-book { display: flex; align-items: center; gap: 13px; min-width: 0; padding-left: 24px; border-left: 1px solid #e5e7eb; }
.book-cover { flex-shrink: 0; width: 54px; height: 76px; overflow: hidden; box-shadow: -2px 3px 0 0 #d8d2e0; }
.book-cover img { display: block; width: 100%; height: 100%; object-fit: cover; }
.book-cover-fallback { display: block; height: 100%; box-sizing: border-box; background: #e8d7b5; padding: 7px 5px; font-family: Georgia, serif; font-size: 9px; font-weight: 700; line-height: 1.4; color: #4a3a25; word-break: break-all; overflow: hidden; }
.book-info { min-width: 0; }
.book-label { margin: 0 0 6px 0; font-size: 11px; color: #9ca3af; }
.book-title { margin: 0; font-size: 16px; font-weight: 400; line-height: 1.3; color: #111827; word-break: break-word; }
.book-author { margin: 4px 0 0 0; font-size: 11px; color: #6b7280; word-break: break-word; }

.article-body { display: grid; grid-template-columns: minmax(0, 1fr) 190px; gap: 34px; align-items: start; padding: 32px 34px 38px; }
.content-title { margin: 0; font-size: 15px; font-weight: 400; color: #111827; }
.content-text { margin: 20px 0 0 0; font-size: 14px; line-height: 1.9; color: #374151; white-space: pre-wrap; word-break: break-word; }

.review-info { padding: 4px 16px 16px; background: #f8fafc; border: 1px solid #e5e7eb; border-radius: 5px; }
.info-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 14px 0; border-bottom: 1px solid #e5e7eb; }
.info-label { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: #6b7280; white-space: nowrap; }
.info-value { font-size: 12px; font-weight: 700; color: #374151; white-space: nowrap; }
.rating { font-size: 16px; font-weight: 700; color: #d97706; }
.rating-max { font-size: 11px; color: #9ca3af; }
.info-note { display: flex; align-items: flex-start; gap: 8px; margin: 0; padding-top: 15px; font-size: 11px; line-height: 1.5; color: #6b7280; }
.info-note img { flex-shrink: 0; margin-top: 2px; }

@media (max-width: 640px) {
  .main-content { padding: 32px 16px 64px; }
  .article-head { grid-template-columns: 1fr; gap: 20px; padding: 24px 20px; }
  .head-book { padding-left: 0; padding-top: 20px; border-left: none; border-top: 1px solid #e5e7eb; }
  .article-body { grid-template-columns: 1fr; gap: 24px; padding: 24px 20px 28px; }
  .headline { font-size: 18px; }
}
</style>
