<template>
  <div class="app-container">
    <main class="main-content">
      <div class="content-wrapper">
        <div class="page-heading">
          <div>
            <h2 class="page-title">내 독서 모임</h2>
            <p class="page-subtitle">참여 중인 독서 모임을 확인하고 이야기를 이어가세요.</p>
          </div>
          <button type="button" class="btn-primary create-btn" @click="onCreateTeam">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M12 5v14M5 12h14" />
            </svg>
            새 모임 만들기
          </button>
        </div>

        <div v-if="isLoading" class="loading-state">
          <div class="spinner"></div>
          <p class="loading-text">모임을 불러오는 중...</p>
        </div>

        <div v-else-if="teams.length === 0" class="empty-state">
          <p class="empty-title">참여 중인 모임이 없습니다</p>
          <p class="empty-subtitle">새 모임을 만들거나 초대 링크로 참여해보세요</p>
        </div>

        <template v-else>
          <div class="team-count">참여 중인 모임 <strong>{{ teams.length }}</strong></div>
          <section class="team-list">
            <article
              v-for="team in teams"
              :key="team.teamId"
              class="team-card"
            >
              <div class="team-mark">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
                  <circle cx="9" cy="7" r="4" />
                  <path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
                </svg>
              </div>
              <div class="team-body">
                <div class="team-title">
                  <h3>{{ team.teamName }}</h3>
                  <span v-if="team.role === 'OWNER'" class="role-badge owner">모임장</span>
                  <span v-else-if="team.role === 'MEMBER'" class="role-badge member">활동 중</span>
                </div>
                <p v-if="team.teamDesc" class="team-desc">{{ team.teamDesc }}</p>
                <div class="team-meta">
                  <span>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                      <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
                      <circle cx="9" cy="7" r="4" />
                      <path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
                    </svg>
                    멤버 {{ team.memberCount }}명
                  </span>
                </div>
              </div>
            </article>
          </section>
        </template>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useToast } from 'vue-toastification';
import apiClient from '../api/client';

const toast = useToast();

const teams = ref([]);
const isLoading = ref(false);

const fetchTeams = async () => {
  isLoading.value = true;
  try {
    const response = await apiClient.get('/teams');
    teams.value = response.data || [];
  } catch {
    teams.value = [];
  } finally {
    isLoading.value = false;
  }
};

// 모임 생성 화면은 이번 범위에 포함되지 않음
const onCreateTeam = () => {
  toast.info('모임 만들기는 준비 중입니다.');
};

onMounted(fetchTeams);
</script>

<style scoped>
.app-container { min-height: 100%; }
.main-content { padding: 48px 24px; }
.content-wrapper { max-width: 720px; margin: 0 auto; }

.page-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; margin-bottom: 32px; }
.page-title { font-size: 28px; font-weight: 600; color: #1f2937; margin: 0; line-height: 1.3; }
.page-subtitle { font-size: 15px; color: #6b7280; margin: 8px 0 0 0; }
.create-btn { display: inline-flex; align-items: center; gap: 6px; flex-shrink: 0; white-space: nowrap; border-radius: 8px; }
.create-btn svg { width: 16px; height: 16px; }

.loading-state { text-align: center; padding: 64px 0; }
.spinner { width: 32px; height: 32px; border: 3px solid #e5e7eb; border-top-color: #2563eb; border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto 16px; }
@keyframes spin { to { transform: rotate(360deg); } }
.loading-text { color: #6b7280; font-size: 15px; }

.empty-state { text-align: center; padding: 64px 24px; background: white; border: 1px solid #e5e7eb; border-radius: 8px; }
.empty-title { font-size: 18px; font-weight: 500; color: #1f2937; margin: 0 0 8px 0; }
.empty-subtitle { font-size: 15px; color: #6b7280; margin: 0; }

.team-count { margin: 0 0 12px; font-size: 13px; color: #6b7280; }
.team-count strong { margin-left: 3px; color: #2563eb; }

.team-list { display: flex; flex-direction: column; gap: 12px; }
.team-card { display: flex; align-items: center; background: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 20px; }

.team-mark { width: 54px; height: 54px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; border-radius: 8px; background: #eff6ff; color: #2563eb; }
.team-mark svg { width: 23px; height: 23px; }

.team-body { flex: 1; min-width: 0; padding: 0 16px; }
.team-title { display: flex; align-items: center; gap: 8px; }
.team-title h3 { font-size: 17px; font-weight: 600; color: #1f2937; margin: 0; line-height: 1.4; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.role-badge { flex-shrink: 0; padding: 2px 8px; border-radius: 999px; font-size: 12px; font-weight: 600; }
.role-badge.owner { background: #f3f4f6; color: #6b7280; }
.role-badge.member { background: #eff6ff; color: #2563eb; }
.team-desc { font-size: 14px; color: #6b7280; margin: 6px 0 0 0; line-height: 1.5; }
.team-meta { display: flex; gap: 18px; margin-top: 12px; font-size: 13px; color: #6b7280; }
.team-meta span { display: inline-flex; align-items: center; gap: 5px; }
.team-meta svg { width: 14px; height: 14px; }

@media (max-width: 480px) {
  .main-content { padding: 32px 16px; }
  .page-heading { align-items: flex-start; }
  .create-btn { padding: 10px 12px; }
  .team-card { align-items: flex-start; padding: 16px; }
  .team-mark { width: 44px; height: 44px; }
  .team-body { padding: 0 12px; }
}
</style>
