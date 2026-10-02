<template>
  <div class="app-container">
    <main class="main-content">
      <div class="content-wrapper">
        <router-link :to="{ name: 'teams' }" class="back-link">← 내 독서 모임</router-link>

        <div class="page-heading">
          <div>
            <h2 class="page-title">독서 모임</h2>
            <p class="page-subtitle">함께 읽고, 서로의 생각을 나눠보세요.</p>
          </div>
          <button type="button" class="btn-primary invite-btn" @click="onNotReady">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M12 5v14M5 12h14" />
            </svg>
            멤버 초대
          </button>
        </div>

        <section class="team-summary">
          <div class="team-mark">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
          </div>
          <div class="team-copy">
            <div class="team-title-row">
              <h3>{{ team.name }}</h3>
              <span class="badge">{{ members.length }}명</span>
            </div>
            <p>{{ team.description }}</p>
          </div>
        </section>

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

        <div v-if="tab === '모임 홈'" class="home-content">
          <section>
            <div class="section-title">
              <div>
                <h3>3월 독후감 현황</h3>
                <p>이번 달 모임 멤버의 독후감 작성 현황입니다.</p>
              </div>
            </div>
            <div class="review-summary">
              <div class="review-count">
                <strong>{{ writtenMemberCount }}</strong><span>/ {{ members.length }}명</span>
              </div>
              <div class="review-summary-copy">
                <strong>{{ writtenMemberCount }}명이 독후감을 작성했어요</strong>
                <p>모든 멤버가 작성하기까지 {{ members.length - writtenMemberCount }}명 남았어요.</p>
                <div class="review-progress">
                  <span :style="{ width: `${(writtenMemberCount / members.length) * 100}%` }"></span>
                </div>
              </div>
            </div>
          </section>

          <section>
            <div class="section-title">
              <div>
                <h3>멤버들의 독후감</h3>
                <p>전체공개 및 모임공개로 작성된 독후감만 표시됩니다.</p>
              </div>
              <span class="review-total">{{ visibleReviews.length }}개</span>
            </div>
            <div class="review-list">
              <button
                v-for="review in visibleReviews"
                :key="review.id"
                type="button"
                class="review-item"
                @click="onNotReady"
              >
                <span class="review-body">
                  <span class="review-meta">
                    <strong>{{ review.author.name }}</strong>
                    <span :class="['visibility-badge', review.visibility === '전체공개' ? 'public' : 'team']">
                      {{ review.visibility }}
                    </span>
                    <time>{{ review.date }}</time>
                  </span>
                  <strong class="review-title">{{ review.title }}</strong>
                  <span class="review-excerpt">{{ review.content }}</span>
                </span>
                <span class="card-arrow">→</span>
              </button>
            </div>
          </section>
        </div>

        <section v-else-if="tab === '멤버'" class="member-list">
          <article v-for="member in members" :key="member.name">
            <div>
              <strong>{{ member.name }}</strong>
              <p>{{ member.role }} · 2026년 3월 가입</p>
            </div>
            <span v-if="member.role === '모임장'" class="badge">모임장</span>
          </article>
        </section>

        <section v-else class="archive">
          <div class="past-cover">이방인</div>
          <div class="archive-copy">
            <h3>이방인</h3>
            <p>알베르 카뮈 · 2026년 2월</p>
            <span>독후감 4개</span>
          </div>
          <button type="button" class="link-button" @click="onNotReady">기록 보기 →</button>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useToast } from 'vue-toastification';

const toast = useToast();

// 스켈레톤 페이지: API 설계 전이므로 모든 팀이 동일한 목업 데이터를 보여준다
const team = {
  name: '북적북적 독서모임',
  description: '한 달에 한 권, 좋아하는 문장을 함께 나누는 모임입니다.',
};

const members = [
  { name: '북토커', role: '모임장' },
  { name: '책읽는 민지', role: '모임 멤버' },
  { name: '김독자', role: '모임 멤버' },
  { name: '이야기꾼', role: '모임 멤버' },
];

const reviews = [
  {
    id: 101,
    author: members[1],
    visibility: '전체공개',
    title: '나를 깨고 나오는 일에 대하여',
    content: '싱클레어가 안전한 세계를 벗어나 자기 자신을 찾아가는 과정이 오래 기억에 남았다. 성장한다는 건 결국 익숙한 세계를 깨는 일인지도 모른다.',
    date: '3월 22일',
  },
  {
    id: 102,
    author: members[2],
    visibility: '모임공개',
    title: '두 세계 사이에서 발견한 나',
    content: '밝은 세계와 어두운 세계를 오가는 싱클레어를 보며 누구에게나 타인에게 보여주지 않는 세계가 있다는 생각이 들었다.',
    date: '3월 19일',
  },
  {
    id: 103,
    author: members[3],
    visibility: '비공개',
    title: '개인 독서 기록',
    content: '작성자만 볼 수 있는 독후감입니다.',
    date: '3월 17일',
  },
];

const tabs = ['모임 홈', '멤버', '지난 책'];
const tab = ref('모임 홈');

const visibleReviews = computed(() => reviews.filter((review) => review.visibility !== '비공개'));
const writtenMemberCount = new Set(reviews.map((review) => review.author.name)).size;

const onNotReady = () => {
  toast.info('준비 중인 기능입니다.');
};
</script>

<style scoped>
.app-container { min-height: 100%; }
.main-content { padding: 48px 24px; }
.content-wrapper { max-width: 720px; margin: 0 auto; }

.back-link { display: inline-block; margin-bottom: 20px; font-size: 14px; color: #6b7280; text-decoration: none; transition: color 0.15s ease; }
.back-link:hover { color: #2563eb; }

.page-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; margin-bottom: 32px; }
.page-title { font-size: 28px; font-weight: 600; color: #1f2937; margin: 0; line-height: 1.3; }
.page-subtitle { font-size: 15px; color: #6b7280; margin: 8px 0 0 0; }
.invite-btn { display: inline-flex; align-items: center; gap: 6px; flex-shrink: 0; white-space: nowrap; border-radius: 8px; }
.invite-btn svg { width: 16px; height: 16px; }

.badge { flex-shrink: 0; display: inline-flex; padding: 2px 8px; border-radius: 999px; background: #eff6ff; color: #2563eb; font-size: 12px; font-weight: 600; }

/* 팀 요약 */
.team-summary { display: flex; align-items: center; background: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 22px 24px; }
.team-mark { width: 56px; height: 56px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; border-radius: 8px; background: #eff6ff; color: #2563eb; }
.team-mark svg { width: 25px; height: 25px; }
.team-copy { flex: 1; min-width: 0; padding-left: 18px; }
.team-title-row { display: flex; align-items: center; gap: 10px; }
.team-title-row h3 { font-size: 20px; font-weight: 600; color: #1f2937; margin: 0; line-height: 1.4; }
.team-copy > p { font-size: 15px; color: #6b7280; margin: 6px 0 0 0; }

/* 탭 */
.tabs { display: flex; gap: 26px; margin-top: 28px; border-bottom: 1px solid #e5e7eb; }
.tabs button { position: relative; background: none; border-radius: 0; padding: 0 2px 13px; font-weight: 400; color: #6b7280; }
.tabs button.selected { color: #2563eb; font-weight: 600; }
.tabs button.selected::after { content: ''; position: absolute; left: 0; right: 0; bottom: -1px; height: 2px; background: #2563eb; }

/* 모임 홈 */
.home-content { display: flex; flex-direction: column; gap: 32px; margin-top: 28px; }
.section-title { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 12px; }
.section-title h3 { font-size: 18px; font-weight: 600; color: #1f2937; margin: 0; }
.section-title p { font-size: 13px; color: #6b7280; margin: 4px 0 0 0; }
.review-total { font-size: 13px; color: #6b7280; }

.review-summary { display: flex; align-items: center; gap: 20px; background: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 22px 24px; }
.review-count { min-width: 90px; padding-right: 20px; border-right: 1px solid #e5e7eb; }
.review-count strong { font-size: 32px; color: #2563eb; }
.review-count span { margin-left: 4px; font-size: 14px; color: #6b7280; }
.review-summary-copy { flex: 1; min-width: 0; }
.review-summary-copy > strong { font-size: 15px; color: #1f2937; }
.review-summary-copy > p { font-size: 13px; color: #6b7280; margin: 4px 0 0 0; }
.review-progress { height: 6px; margin-top: 12px; overflow: hidden; border-radius: 99px; background: #e5e7eb; }
.review-progress span { display: block; height: 100%; border-radius: inherit; background: #2563eb; }

.review-list { overflow: hidden; background: white; border: 1px solid #e5e7eb; border-radius: 8px; }
.review-item { width: 100%; display: flex; align-items: flex-start; gap: 14px; background: white; border-bottom: 1px solid #f3f4f6; border-radius: 0; padding: 20px; font-weight: 400; text-align: left; color: #1f2937; }
.review-item:last-child { border-bottom: none; }
.review-item:hover { background: #f9fbff; }
.review-item:focus-visible { outline: 2px solid #2563eb; outline-offset: -2px; }
.review-body { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.review-meta { display: flex; align-items: center; gap: 8px; }
.review-meta > strong { font-size: 14px; }
.review-meta time { margin-left: auto; font-size: 13px; color: #9ca3af; }
.visibility-badge { padding: 2px 6px; border-radius: 4px; font-size: 11px; font-weight: 600; }
.visibility-badge.public { background: #eff6ff; color: #2563eb; }
.visibility-badge.team { background: #f3f0ff; color: #7658bd; }
.review-title { margin-top: 8px; font-size: 16px; font-weight: 600; }
.review-excerpt { display: -webkit-box; margin-top: 6px; overflow: hidden; font-size: 14px; line-height: 1.6; color: #4b5563; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.card-arrow { flex-shrink: 0; align-self: center; font-size: 17px; color: #9ca3af; }

/* 멤버 */
.member-list { margin-top: 28px; background: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 0 22px; }
.member-list article { min-height: 76px; display: flex; align-items: center; gap: 13px; border-bottom: 1px solid #f3f4f6; }
.member-list article:last-child { border-bottom: none; }
.member-list article > div { flex: 1; }
.member-list strong { font-size: 15px; color: #1f2937; }
.member-list p { font-size: 13px; color: #6b7280; margin: 4px 0 0 0; }

/* 지난 책 */
.archive { display: flex; align-items: center; gap: 16px; margin-top: 28px; background: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 18px; }
.past-cover { width: 48px; height: 68px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; background: #65549b; color: white; font-size: 11px; }
.archive-copy { flex: 1; min-width: 0; }
.archive-copy h3 { font-size: 16px; font-weight: 600; color: #1f2937; margin: 0; }
.archive-copy p, .archive-copy span { display: block; font-size: 13px; color: #6b7280; margin: 4px 0 0 0; }
.link-button { background: none; padding: 2px 0; font-size: 13px; font-weight: 400; color: #6b7280; }
.link-button:hover { color: #2563eb; }

@media (max-width: 480px) {
  .main-content { padding: 32px 16px; }
  .page-heading { align-items: flex-start; }
  .invite-btn { padding: 10px 12px; }
  .team-summary { align-items: flex-start; padding: 18px; }
  .team-mark { width: 46px; height: 46px; }
  .review-summary { align-items: flex-start; gap: 14px; padding: 18px; }
  .review-count { min-width: 70px; padding-right: 14px; }
  .review-count strong { font-size: 26px; }
  .review-item { gap: 10px; padding: 16px; }
}
</style>
