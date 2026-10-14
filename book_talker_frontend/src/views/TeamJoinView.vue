<template>
  <div class="app-container">
    <main class="main-content">
      <div class="content-wrapper">
        <router-link :to="closeLinkTarget" class="back-link">← 초대 링크 닫기</router-link>

        <div v-if="isLoading" class="loading-state">
          <div class="spinner"></div>
          <p class="loading-text">초대 정보를 불러오는 중...</p>
        </div>

        <!-- 없는 코드 / 조회 실패: 닉네임 입력 전에 알린다 -->
        <section v-else-if="previewError" class="join-card">
          <div class="state-message">
            <p class="state-title">{{ previewError }}</p>
            <p class="state-subtitle">초대한 분께 링크를 다시 확인해 달라고 요청해 보세요.</p>
          </div>
          <router-link :to="{ name: 'home' }" class="btn-secondary card-btn">홈으로</router-link>
        </section>

        <section v-else class="join-card">
          <div class="preview">
            <div class="team-mark">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
                <circle cx="9" cy="7" r="4" />
                <path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
              </svg>
            </div>
            <p class="preview-label">초대받은 모임</p>
            <h2 class="preview-title">{{ preview.teamName }}</h2>
            <p class="preview-desc">{{ preview.teamDesc }}</p>
          </div>

          <div class="join-area">
            <!-- 409: 이미 모임원 또는 추방. 응답만으로 구분할 수 없어 공통 안내 (design 2-4) -->
            <template v-if="joinBlocked">
              <div class="state-message">
                <p class="state-title">이미 가입했거나 가입할 수 없는 모임이에요.</p>
                <p class="state-subtitle">이미 가입한 모임이라면 내 모임 목록에서 확인할 수 있어요.</p>
              </div>
              <router-link :to="{ name: 'teams' }" class="btn-primary card-btn">내 모임 목록</router-link>
            </template>

            <form v-else-if="isLoggedIn" class="join-form" @submit.prevent="onJoin">
              <label for="display-name" class="field-label">모임에서 쓸 닉네임</label>
              <input
                id="display-name"
                v-model="displayName"
                type="text"
                :maxlength="DISPLAY_NAME_LIMIT"
                placeholder="예: 책읽는 북토커"
              />
              <div class="field-footer">
                <span class="field-hint">이 모임의 멤버들에게 표시되는 이름이에요.</span>
                <span class="char-count">{{ displayName.length }} / {{ DISPLAY_NAME_LIMIT }}</span>
              </div>
              <button type="submit" class="btn-primary card-btn" :disabled="!canJoin">
                {{ isSubmitting ? '가입하는 중...' : '모임 가입하기' }}
              </button>
            </form>

            <!-- 비로그인: 닉네임 칸은 숨긴다. 로그인을 다녀오면 입력값이 사라지기 때문 (design 2-1) -->
            <button v-else type="button" class="btn-outline card-btn" @click="onLoginToJoin">
              로그인하고 가입하기
            </button>
          </div>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useToast } from 'vue-toastification';
import apiClient from '../api/client';
import { useAuth } from '../composables/useAuth';
import { savePendingRedirect } from '../composables/usePendingRedirect';

// API 명세 – 초대코드로 가입: 닉네임 1~10자
const DISPLAY_NAME_LIMIT = 10;

const route = useRoute();
const router = useRouter();
const toast = useToast();
const { isLoggedIn, checkLoginStatus } = useAuth();

// 초대코드는 URL에서 꺼낸다. 사용자는 코드를 입력하지 않음
const code = route.params.code;

const preview = ref({ teamName: '', teamDesc: '' });
const previewError = ref('');
const isLoading = ref(true);

const displayName = ref('');
const isSubmitting = ref(false);
const joinBlocked = ref(false);

const canJoin = computed(() => !isSubmitting.value && displayName.value.trim());

const closeLinkTarget = computed(() => (isLoggedIn.value ? { name: 'teams' } : { name: 'home' }));

// 미리보기는 비로그인도 조회 가능. 공통 404 토스트는 끄고 화면에서 안내
const fetchPreview = async () => {
  try {
    const response = await apiClient.get(`/teams/join/${encodeURIComponent(code)}`, { skipErrorCodes: [404] });
    preview.value = response.data;
  } catch (error) {
    previewError.value = error.response?.status === 404
      ? '유효하지 않은 초대 링크예요.'
      : '초대 정보를 불러오지 못했어요.';
  }
};

const onLoginToJoin = () => {
  // 로그인 성공 후 백엔드는 항상 홈으로 보내므로, 돌아올 경로를 남겨 두고 라우터 가드가 복귀시킨다
  savePendingRedirect(route.fullPath);
  router.push({ name: 'login' });
};

const onJoin = async () => {
  if (!canJoin.value) return;

  isSubmitting.value = true;
  try {
    const response = await apiClient.post(
      '/teams/join',
      { code, displayName: displayName.value.trim() },
      { skipErrorCodes: [404, 409] },
    );
    toast.success('모임에 가입했어요.');
    // 뒤로 가기로 가입 화면에 돌아오지 않게 replace
    router.replace({ name: 'team-detail', params: { teamId: response.data.teamId } });
  } catch (error) {
    const status = error.response?.status;
    if (status === 409) {
      joinBlocked.value = true;
    } else if (status === 404) {
      // 미리보기 이후 모임이 삭제된 경우
      previewError.value = '유효하지 않은 초대 링크예요.';
    } else if (status === 400) {
      toast.error('닉네임을 다시 확인해 주세요.');
    }
    isSubmitting.value = false;
  }
};

onMounted(async () => {
  await Promise.all([fetchPreview(), checkLoginStatus()]);
  isLoading.value = false;
});
</script>

<style scoped>
.app-container { min-height: 100%; }
.main-content { padding: 48px 24px; }
.content-wrapper { max-width: 720px; margin: 0 auto; }

.back-link { display: inline-block; margin-bottom: 20px; font-size: 14px; color: #6b7280; text-decoration: none; transition: color 0.15s ease; }
.back-link:hover { color: #2563eb; }

.loading-state { text-align: center; padding: 64px 0; }
.spinner { width: 32px; height: 32px; border: 3px solid #e5e7eb; border-top-color: #2563eb; border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto 16px; }
@keyframes spin { to { transform: rotate(360deg); } }
.loading-text { color: #6b7280; font-size: 15px; }

.join-card { max-width: 560px; margin: 0 auto; padding: 34px; background: white; border: 1px solid #e5e7eb; border-radius: 8px; }

.preview { display: flex; flex-direction: column; align-items: center; text-align: center; }
.team-mark { width: 56px; height: 56px; display: flex; align-items: center; justify-content: center; border-radius: 8px; background: #eff6ff; color: #2563eb; margin-bottom: 20px; }
.team-mark svg { width: 25px; height: 25px; }
.preview-label { margin: 0; font-size: 12px; font-weight: 600; color: #2563eb; }
.preview-title { margin: 8px 0 0; font-size: 22px; font-weight: 600; color: #1f2937; line-height: 1.4; word-break: break-word; }
.preview-desc { max-width: 400px; margin: 10px 0 0; font-size: 14px; color: #6b7280; line-height: 1.6; word-break: break-word; }

.join-area { margin-top: 28px; padding-top: 24px; border-top: 1px solid #e5e7eb; }

.join-form { display: flex; flex-direction: column; }
.field-label { margin-bottom: 8px; font-size: 14px; font-weight: 600; color: #1f2937; }
.field-footer { display: flex; justify-content: space-between; align-items: baseline; gap: 12px; margin: 7px 0 22px; }
.field-hint { font-size: 12px; color: #6b7280; }
.char-count { flex-shrink: 0; font-size: 12px; color: #9ca3af; }

.card-btn { display: flex; align-items: center; justify-content: center; width: 100%; min-height: 44px; font-weight: 600; border-radius: 6px; text-decoration: none; }
.btn-outline { background: white; color: #2563eb; border: 1px solid #93c5fd; padding: 10px 20px; }
.btn-outline:hover { background: #eff6ff; }

.state-message { text-align: center; margin-bottom: 22px; }
.state-title { margin: 0; font-size: 16px; font-weight: 600; color: #1f2937; }
.state-subtitle { margin: 8px 0 0; font-size: 14px; color: #6b7280; }

@media (max-width: 480px) {
  .main-content { padding: 32px 16px; }
  .join-card { padding: 24px 20px; }
}
</style>
