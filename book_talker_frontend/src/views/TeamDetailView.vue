<template>
  <div class="app-container">
    <main class="main-content">
      <div class="content-wrapper">
        <router-link :to="{ name: 'teams' }" class="back-link">← 내 독서 모임</router-link>

        <div v-if="isLoading" class="loading-state">
          <div class="spinner"></div>
          <p class="loading-text">모임을 불러오는 중...</p>
        </div>

        <div v-else-if="!team" class="empty-state">
          <p class="empty-title">모임 정보를 불러오지 못했습니다</p>
          <p class="empty-subtitle">잠시 후 다시 시도해주세요</p>
        </div>

        <template v-else>
          <div class="page-heading">
            <div class="heading-copy">
              <div class="title-row">
                <h2 class="page-title">{{ team.teamName }}</h2>
                <span class="badge">{{ team.memberCount }}명</span>
                <span class="badge neutral">{{ roleLabel(team.role) }}</span>
              </div>
              <p class="page-subtitle">{{ team.teamDesc }}</p>
            </div>
            <!-- 초대코드 조회는 모임장 전용 API라 모임원에게는 버튼을 표시하지 않는다 -->
            <button v-if="isOwner" type="button" class="btn-primary invite-btn" @click="inviteOpen = true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M12 5v14M5 12h14" />
              </svg>
              멤버 초대
            </button>
          </div>

          <div class="tabs">
            <button
              v-for="item in tabs"
              :key="item"
              type="button"
              :class="{ selected: tab === item }"
              @click="tab = item"
            >
              {{ item }}
            </button>
          </div>

          <section v-if="tab === '모임 홈'" class="feed tab-panel">
            <div class="section-head">
              <div>
                <h3 class="section-title">멤버들의 독후감</h3>
                <p class="section-desc">전체공개 및 모임공개로 작성된 독후감만 표시됩니다.</p>
              </div>
              <span v-if="feedLoaded" class="section-count">{{ feedTotalCount }}개</span>
            </div>

            <div v-if="!feedLoaded && isFeedLoading" class="loading-state">
              <div class="spinner"></div>
              <p class="loading-text">독후감을 불러오는 중...</p>
            </div>

            <div v-else-if="feedLoaded && reviews.length === 0" class="empty-state feed-empty">
              <p class="empty-title">아직 공유된 독후감이 없어요</p>
              <p class="empty-subtitle">전체공개나 모임공개로 독후감을 쓰면 이곳에 표시됩니다</p>
            </div>

            <template v-else-if="reviews.length > 0">
              <div class="feed-list">
                <article v-for="review in reviews" :key="review.reviewId" class="feed-item">
                  <div class="feed-book">
                    <div class="feed-cover">
                      <img v-if="review.cover" :src="review.cover" alt="" loading="lazy" />
                      <span v-else class="feed-cover-fallback">{{ review.bookTitle }}</span>
                    </div>
                    <p class="feed-book-title" :title="review.bookTitle">{{ review.bookTitle }}</p>
                  </div>
                  <div class="feed-body">
                    <div class="feed-meta">
                      <strong class="feed-writer">{{ review.displayName }}</strong>
                      <span class="visibility-badge" :class="review.visibility.toLowerCase()">
                        {{ visibilityLabel(review.visibility) }}
                      </span>
                      <time class="feed-date" :datetime="review.regDate">{{ formatRegDate(review.regDate) }}</time>
                    </div>
                    <p class="feed-headline">{{ review.headline }}</p>
                    <p class="feed-summary">{{ review.summary }}</p>
                  </div>
                  <span class="feed-arrow" aria-hidden="true">→</span>
                </article>
              </div>

              <button
                v-if="hasMoreReviews"
                type="button"
                class="more-btn"
                :disabled="isFeedLoading"
                @click="fetchReviews(feedPage + 1)"
              >
                {{ isFeedLoading ? '불러오는 중...' : '더 보기' }}
              </button>
            </template>
          </section>

          <template v-else-if="tab === '멤버'">
            <div v-if="isMembersLoading" class="loading-state">
              <div class="spinner"></div>
              <p class="loading-text">멤버를 불러오는 중...</p>
            </div>

            <section v-else-if="members.length > 0" class="member-list tab-panel">
              <article v-for="member in members" :key="member.memberId">
                <div>
                  <strong>{{ member.displayName }}<span v-if="member.me" class="me-mark"> (나)</span></strong>
                  <p>{{ roleLabel(member.role) }} · {{ formatJoinedAt(member.joinedAt) }}</p>
                </div>
                <span v-if="member.role === 'OWNER'" class="badge">모임장</span>
              </article>
            </section>
          </template>

          <div v-else class="empty-state tab-panel">
            <p class="empty-title">지난 책 기록은 준비 중입니다</p>
            <p class="empty-subtitle">모임에서 함께 읽은 책을 곧 이곳에서 모아볼 수 있어요</p>
          </div>
        </template>
      </div>
    </main>

    <TeamInviteModal v-if="inviteOpen" :team-id="teamId" @close="inviteOpen = false" />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useToast } from 'vue-toastification';
import apiClient from '../api/client';
import TeamInviteModal from '../components/TeamInviteModal.vue';

const route = useRoute();
const router = useRouter();
const toast = useToast();

const teamId = route.params.teamId;

const team = ref(null);
const isLoading = ref(false);

const members = ref([]);
const isMembersLoading = ref(false);
const membersLoaded = ref(false);

const reviews = ref([]);
const feedPage = ref(0);
const feedTotalPages = ref(0);
const feedTotalCount = ref(0);
const isFeedLoading = ref(false);
const feedLoaded = ref(false);

const tabs = ['모임 홈', '멤버', '지난 책'];
const tab = ref('모임 홈');
const inviteOpen = ref(false);

const isOwner = computed(() => team.value?.role === 'OWNER');
const hasMoreReviews = computed(() => feedPage.value < feedTotalPages.value);

const roleLabel = (role) => (role === 'OWNER' ? '모임장' : '모임원');

const visibilityLabel = (visibility) => (visibility === 'PUBLIC' ? '전체공개' : '모임공개');

// "2026-03-22" → "3월 22일", 올해가 아니면 "2025년 3월 22일"
const formatRegDate = (regDate) => {
  const [year, month, day] = regDate.split('-').map(Number);
  const monthDay = `${month}월 ${day}일`;
  return year === new Date().getFullYear() ? monthDay : `${year}년 ${monthDay}`;
};

// "2026-03-02" → "2026년 3월 가입"
const formatJoinedAt = (joinedAt) => {
  const [year, month] = joinedAt.split('-');
  return `${year}년 ${Number(month)}월 가입`;
};

// 404: 없는 모임·모임원이 아닌 사용자(추방·탈퇴 포함), 400: 숫자가 아닌 teamId
// 공통 인터셉터 토스트 대신 화면에서 안내하고 목록으로 돌려보낸다
const NOT_FOUND_CODES = [400, 404];

const leaveIfNotFound = (error) => {
  if (!NOT_FOUND_CODES.includes(error.response?.status)) return false;

  toast.warning('모임을 찾을 수 없습니다.');
  router.replace({ name: 'teams' });
  return true;
};

const fetchTeam = async () => {
  isLoading.value = true;
  let leaving = false;
  try {
    const response = await apiClient.get(`/teams/${teamId}`, { skipErrorCodes: NOT_FOUND_CODES });
    team.value = response.data;
  } catch (error) {
    leaving = leaveIfNotFound(error);
  } finally {
    // 목록으로 이동하는 동안 오류 화면이 잠깐 보이지 않도록 로딩 상태 유지
    if (!leaving) isLoading.value = false;
  }
};

// 명세: 모임원 목록은 멤버 탭을 열 때만 조회
const fetchMembers = async () => {
  isMembersLoading.value = true;
  try {
    const response = await apiClient.get(`/teams/${teamId}/members`, { skipErrorCodes: NOT_FOUND_CODES });
    members.value = response.data || [];
    membersLoaded.value = true;
  } catch (error) {
    leaveIfNotFound(error);
  } finally {
    isMembersLoading.value = false;
  }
};

// 명세: 페이지는 1부터, 크기 4 고정. "더 보기"로 다음 페이지를 이어 붙인다
const fetchReviews = async (page = 1) => {
  isFeedLoading.value = true;
  try {
    const response = await apiClient.get(`/teams/${teamId}/reviews`, {
      params: { page },
      skipErrorCodes: NOT_FOUND_CODES,
    });
    const data = response.data;
    // 페이지 사이에 새 독후감이 등록되면 이전 페이지 항목이 밀려 다시 올 수 있어 중복 제거
    const loadedIds = new Set(reviews.value.map((review) => review.reviewId));
    reviews.value = [...reviews.value, ...data.reviews.filter((review) => !loadedIds.has(review.reviewId))];
    feedPage.value = data.page;
    feedTotalPages.value = data.totalPages;
    feedTotalCount.value = data.totalCount;
    feedLoaded.value = true;
  } catch (error) {
    leaveIfNotFound(error);
  } finally {
    isFeedLoading.value = false;
  }
};

watch(tab, (value) => {
  if (value === '멤버' && !membersLoaded.value && !isMembersLoading.value) fetchMembers();
});

onMounted(async () => {
  await fetchTeam();
  // 모임 정보를 불러온 경우에만 피드 조회 (404면 이미 목록으로 이동 중)
  if (team.value) fetchReviews();
});
</script>

<style scoped>
.app-container { min-height: 100%; }
.main-content { padding: 48px 24px; }
.content-wrapper { max-width: 720px; margin: 0 auto; }

.back-link { display: inline-block; margin-bottom: 20px; font-size: 14px; color: #6b7280; text-decoration: none; transition: color 0.15s ease; }
.back-link:hover { color: #2563eb; }

.page-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; }
.heading-copy { min-width: 0; }
.title-row { display: flex; align-items: center; flex-wrap: wrap; gap: 9px; }
.page-title { font-size: 28px; font-weight: 600; color: #1f2937; margin: 0; line-height: 1.3; word-break: break-word; }
.page-subtitle { font-size: 15px; color: #6b7280; margin: 8px 0 0 0; }
.invite-btn { display: inline-flex; align-items: center; gap: 6px; flex-shrink: 0; white-space: nowrap; border-radius: 8px; }
.invite-btn svg { width: 16px; height: 16px; }

.badge { flex-shrink: 0; display: inline-flex; padding: 2px 8px; border-radius: 999px; background: #eff6ff; color: #2563eb; font-size: 12px; font-weight: 600; }
.badge.neutral { background: #f3f4f6; color: #4b5563; }

.loading-state { text-align: center; padding: 64px 0; }
.spinner { width: 32px; height: 32px; border: 3px solid #e5e7eb; border-top-color: #2563eb; border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto 16px; }
@keyframes spin { to { transform: rotate(360deg); } }
.loading-text { color: #6b7280; font-size: 15px; }

.empty-state { text-align: center; padding: 64px 24px; background: white; border: 1px solid #e5e7eb; border-radius: 8px; }
.empty-title { font-size: 18px; font-weight: 500; color: #1f2937; margin: 0 0 8px 0; }
.empty-subtitle { font-size: 15px; color: #6b7280; margin: 0; }

/* 탭 */
.tabs { display: flex; gap: 26px; margin-top: 28px; border-bottom: 1px solid #e5e7eb; }
.tabs button { position: relative; background: none; border-radius: 0; padding: 0 2px 13px; font-weight: 400; color: #6b7280; }
.tabs button.selected { color: #2563eb; font-weight: 600; }
.tabs button.selected::after { content: ''; position: absolute; left: 0; right: 0; bottom: -1px; height: 2px; background: #2563eb; }
.tab-panel { margin-top: 24px; }

/* 멤버 */
.member-list { background: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 0 22px; }
.member-list article { min-height: 76px; display: flex; align-items: center; gap: 13px; border-bottom: 1px solid #f3f4f6; }
.member-list article:last-child { border-bottom: none; }
.member-list article > div { flex: 1; min-width: 0; }
.member-list strong { font-size: 15px; color: #1f2937; }
.member-list .me-mark { font-weight: 400; color: #6b7280; }
.member-list p { font-size: 13px; color: #6b7280; margin: 4px 0 0 0; }

/* 모임 홈 — 멤버들의 독후감 */
.section-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.section-title { font-size: 18px; font-weight: 600; color: #111827; margin: 0; }
.section-desc { font-size: 13px; color: #6b7280; margin: 5px 0 0 0; }
.section-count { flex-shrink: 0; font-size: 13px; color: #6b7280; }
.feed .loading-state { padding: 48px 0; }
.feed-empty { margin-top: 12px; padding: 48px 24px; }

.feed-list { margin-top: 12px; background: white; border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden; }
.feed-item { display: flex; align-items: flex-start; gap: 13px; padding: 19px 20px; border-bottom: 1px solid #f3f4f6; }
.feed-item:last-child { border-bottom: none; }
.feed-body { flex: 1; min-width: 0; }
.feed-meta { display: flex; align-items: center; gap: 7px; }
.feed-writer { font-size: 13px; font-weight: 700; color: #111827; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.visibility-badge { flex-shrink: 0; padding: 3px 6px; border-radius: 3px; font-size: 11px; font-weight: 700; line-height: 1.5; }
.visibility-badge.public { background: #eff6ff; color: #2563eb; }
.visibility-badge.group { background: #f3f0ff; color: #7658bd; }
.feed-date { flex-shrink: 0; margin-left: auto; font-size: 12px; color: #9ca3af; }
.feed-headline { margin: 8px 0 0 0; font-size: 15px; font-weight: 700; color: #111827; word-break: break-word; }
.feed-summary { margin: 7px 0 0 0; font-size: 13px; line-height: 1.6; color: #4b5563; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; word-break: break-word; }
.feed-arrow { flex-shrink: 0; align-self: center; font-size: 19px; color: #9ca3af; }
.feed-book { flex-shrink: 0; width: 48px; display: flex; flex-direction: column; align-items: center; gap: 7px; }
.feed-cover { width: 44px; height: 62px; border-radius: 2px; overflow: hidden; }
.feed-cover img { display: block; width: 100%; height: 100%; object-fit: cover; }
/* 표지 URL이 없을 때만 디자인의 가짜 표지(베이지 배경)를 쓴다 */
.feed-cover-fallback { display: block; height: 100%; box-sizing: border-box; background: #e8d7b5; padding: 7px 5px; font-family: Georgia, serif; font-size: 9px; font-weight: 700; line-height: 1.4; color: #4a3a25; word-break: break-all; overflow: hidden; }
.feed-book-title { width: 100%; margin: 0; font-size: 11px; font-weight: 700; color: #4b5563; text-align: center; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.more-btn { display: block; width: 100%; margin-top: 12px; padding: 12px; background: white; border: 1px solid #e5e7eb; border-radius: 8px; color: #4b5563; font-size: 14px; font-weight: 500; }
.more-btn:hover:not(:disabled) { border-color: #2563eb; color: #2563eb; }
.more-btn:disabled { color: #9ca3af; cursor: default; }

@media (max-width: 480px) {
  .main-content { padding: 32px 16px; }
  .page-heading { align-items: flex-start; }
  .invite-btn { padding: 10px 12px; }
  .page-title { font-size: 24px; }
}
</style>
