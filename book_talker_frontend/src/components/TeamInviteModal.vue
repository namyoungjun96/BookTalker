<template>
  <Teleport to="body">
    <div class="modal-backdrop" @mousedown.self="emit('close')">
      <section class="modal" role="dialog" aria-modal="true" aria-labelledby="invite-title">
        <div class="modal-top">
          <div class="team-mark">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
          </div>
          <button type="button" class="close-button" aria-label="닫기" @click="emit('close')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="m6 6 12 12M18 6 6 18" />
            </svg>
          </button>
        </div>

        <h3 id="invite-title">멤버 초대</h3>
        <p class="modal-desc">함께 책을 읽을 친구에게 초대 링크를 공유하세요.</p>

        <label for="invite-link">초대 링크</label>
        <div class="copy-field">
          <input id="invite-link" type="text" readonly :value="inviteUrl" />
          <button type="button" class="btn-primary copy-btn" @click="copyInviteLink">
            <svg v-if="copied" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="m5 12 4 4L19 6" />
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <rect x="9" y="9" width="11" height="11" rx="2" />
              <path d="M15 9V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h4" />
            </svg>
            {{ copied ? '복사됨' : '복사' }}
          </button>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useToast } from 'vue-toastification';

const emit = defineEmits(['close']);
const toast = useToast();

// 스켈레톤: 초대 링크 발급 API 설계 전이므로 목업 링크를 사용한다
const inviteUrl = 'booktalker.co.kr/team/join/BK24';
const copied = ref(false);

const copyInviteLink = async () => {
  try {
    await navigator.clipboard.writeText(inviteUrl);
    copied.value = true;
  } catch {
    toast.error('링크를 복사하지 못했습니다. 직접 선택해서 복사해주세요.');
  }
};

const onKeydown = (event) => {
  if (event.key === 'Escape') emit('close');
};

onMounted(() => window.addEventListener('keydown', onKeydown));
onUnmounted(() => window.removeEventListener('keydown', onKeydown));
</script>

<style scoped>
/* 헤더(z-index: 100, sticky)보다 위에 표시 */
.modal-backdrop { position: fixed; inset: 0; z-index: 200; display: grid; place-items: center; padding: 20px; background: rgba(17, 24, 39, 0.35); }
.modal { width: min(440px, 100%); background: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 24px; box-shadow: 0 24px 60px rgba(17, 24, 39, 0.16); }

.modal-top { display: flex; justify-content: space-between; }
.team-mark { width: 42px; height: 42px; display: flex; align-items: center; justify-content: center; border-radius: 8px; background: #eff6ff; color: #2563eb; }
.team-mark svg { width: 20px; height: 20px; }
.close-button { width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; background: none; padding: 0; color: #6b7280; }
.close-button:hover { background: #f3f4f6; }
.close-button svg { width: 18px; height: 18px; }

h3 { font-size: 20px; font-weight: 600; color: #1f2937; margin: 18px 0 0 0; }
.modal-desc { font-size: 14px; color: #6b7280; margin: 6px 0 0 0; }

label { display: block; margin-top: 24px; font-size: 14px; font-weight: 600; color: #1f2937; }
.copy-field { display: flex; gap: 8px; margin-top: 8px; }
.copy-field input { flex: 1; min-width: 0; font-size: 14px; color: #6b7280; }
.copy-btn { display: inline-flex; align-items: center; gap: 6px; flex-shrink: 0; padding: 8px 16px; }
.copy-btn svg { width: 16px; height: 16px; }
</style>
