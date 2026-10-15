<template>
  <div class="app-container">
    <main class="main-content">
      <div class="content-wrapper">
        <router-link :to="{ name: 'teams' }" class="back-link">← 내 독서 모임</router-link>

        <div class="page-heading">
          <div class="team-mark">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
          </div>
          <h2 class="page-title">모임 만들기</h2>
          <p class="page-subtitle">함께 읽고 이야기할 새로운 독서 모임을 시작해 보세요.</p>
        </div>

        <form class="form-card" @submit.prevent="onSubmit">
          <div class="field">
            <label for="team-name" class="field-label">모임 제목</label>
            <input
              id="team-name"
              v-model="form.teamName"
              type="text"
              :maxlength="LIMITS.teamName"
              placeholder="예: 북적북적 독서모임"
            />
            <div class="field-footer">
              <span class="char-count">{{ form.teamName.length }} / {{ LIMITS.teamName }}</span>
            </div>
          </div>

          <div class="field">
            <label for="team-desc" class="field-label">모임 소개</label>
            <textarea
              id="team-desc"
              v-model="form.teamDesc"
              rows="4"
              :maxlength="LIMITS.teamDesc"
              placeholder="어떤 책을 읽고, 어떻게 이야기를 나눌지 소개해 주세요."
            ></textarea>
            <div class="field-footer">
              <span class="char-count">{{ form.teamDesc.length }} / {{ LIMITS.teamDesc }}</span>
            </div>
          </div>

          <div class="field">
            <label for="display-name" class="field-label">모임에서 쓸 닉네임</label>
            <input
              id="display-name"
              v-model="form.displayName"
              type="text"
              :maxlength="LIMITS.displayName"
              placeholder="예: 책읽는 북토커"
            />
            <div class="field-footer">
              <span class="field-hint">이 모임의 멤버들에게 표시되는 이름이에요.</span>
              <span class="char-count">{{ form.displayName.length }} / {{ LIMITS.displayName }}</span>
            </div>
          </div>

          <button type="submit" class="btn-primary submit-btn" :disabled="!canSubmit">
            {{ isSubmitting ? '만드는 중...' : '모임 만들기' }}
          </button>
        </form>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useToast } from 'vue-toastification';
import apiClient from '../api/client';

// API 명세 – 모임 생성의 글자 수 제약
const LIMITS = { teamName: 20, teamDesc: 100, displayName: 10 };

const router = useRouter();
const toast = useToast();

const form = reactive({ teamName: '', teamDesc: '', displayName: '' });
const isSubmitting = ref(false);

// 서버는 @NotBlank로 검증하므로 공백만 입력한 경우도 미리 막는다
const canSubmit = computed(() =>
  !isSubmitting.value
  && form.teamName.trim()
  && form.teamDesc.trim()
  && form.displayName.trim()
);

const onSubmit = async () => {
  if (!canSubmit.value) return;

  // 생성 API는 같은 이름을 허용하므로, 중복 클릭으로 모임이 두 개 생기지 않게 제출 중에는 막는다
  isSubmitting.value = true;
  try {
    const response = await apiClient.post('/teams', {
      teamName: form.teamName.trim(),
      teamDesc: form.teamDesc.trim(),
      displayName: form.displayName.trim(),
    });
    toast.success('모임을 만들었어요.');
    // 뒤로 가기로 생성 화면에 돌아오지 않게 replace
    router.replace({ name: 'team-detail', params: { teamId: response.data.teamId } });
  } catch (error) {
    // 500·네트워크 오류는 공통 인터셉터가 토스트를 띄움. 400은 화면에서 먼저 막으므로 예외 상황
    if (error.response?.status === 400) {
      toast.error('입력한 내용을 다시 확인해 주세요.');
    }
    isSubmitting.value = false;
  }
};
</script>

<style scoped>
.app-container { min-height: 100%; }
.main-content { padding: 48px 24px; }
.content-wrapper { max-width: 720px; margin: 0 auto; }

.back-link { display: inline-block; margin-bottom: 20px; font-size: 14px; color: #6b7280; text-decoration: none; transition: color 0.15s ease; }
.back-link:hover { color: #2563eb; }

.page-heading { display: flex; flex-direction: column; align-items: center; text-align: center; margin-bottom: 28px; }
.team-mark { width: 56px; height: 56px; display: flex; align-items: center; justify-content: center; border-radius: 8px; background: #eff6ff; color: #2563eb; margin-bottom: 16px; }
.team-mark svg { width: 25px; height: 25px; }
.page-title { font-size: 28px; font-weight: 600; color: #1f2937; margin: 0; line-height: 1.3; }
.page-subtitle { font-size: 15px; color: #6b7280; margin: 8px 0 0 0; }

.form-card { max-width: 560px; margin: 0 auto; display: flex; flex-direction: column; gap: 22px; padding: 28px; background: white; border: 1px solid #e5e7eb; border-radius: 8px; }

.field { display: flex; flex-direction: column; }
.field-label { margin-bottom: 8px; font-size: 14px; font-weight: 600; color: #1f2937; }
.field textarea { resize: vertical; min-height: 104px; }
.field-footer { display: flex; justify-content: flex-end; align-items: baseline; gap: 12px; margin-top: 7px; }
.field-footer .field-hint { margin-right: auto; }
.field-hint { font-size: 12px; color: #6b7280; }
.char-count { flex-shrink: 0; font-size: 12px; color: #9ca3af; }

.submit-btn { width: 100%; min-height: 44px; font-weight: 600; border-radius: 6px; }

@media (max-width: 480px) {
  .main-content { padding: 32px 16px; }
  .form-card { padding: 20px; }
}
</style>
