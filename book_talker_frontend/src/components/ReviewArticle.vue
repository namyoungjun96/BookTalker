<template>
  <article class="review-article">
    <header class="article-head">
      <div class="head-main">
        <div class="badge-row">
          <span class="visibility-badge" :class="visibility.className">
            <img v-if="review.visibility === 'GROUP'" :src="groupIcon" alt="" width="11" height="11" />
            {{ visibility.label }}
          </span>
          <span v-if="teamName" class="team-name">{{ teamName }}</span>
        </div>
        <p class="headline-label">한 줄 요약</p>
        <h2 class="headline">{{ review.headline }}</h2>
        <p v-if="showWriter" class="writer">
          <strong>{{ review.displayName }}</strong>
          <span v-if="review.mine" class="writer-label">(나)</span>
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
      <section class="review-content" aria-label="독후감 본문">
        <p v-for="(paragraph, index) in paragraphs" :key="index" class="content-text">{{ paragraph }}</p>
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
          <strong class="info-value">{{ formatDate(review.regDate) }}</strong>
        </div>
        <div v-if="review.modDate" class="info-row">
          <span class="info-label">
            <img :src="calendarIcon" alt="" width="13" height="13" />
            수정일
          </span>
          <strong class="info-value">{{ formatDate(review.modDate) }}</strong>
        </div>
        <p class="info-note">
          <img :src="membersIcon" alt="" width="13" height="13" />
          <span><slot name="info-note" /></span>
        </p>
      </aside>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue';
import { getVisibilityOption } from '../constants/reviewVisibility';
import groupIcon from '../assets/icons/group.svg';
import starIcon from '../assets/icons/star.svg';
import repeatIcon from '../assets/icons/repeat.svg';
import calendarIcon from '../assets/icons/calendar.svg';
import membersIcon from '../assets/icons/members.svg';

// 표시만 담당 — 조회·수정·삭제는 화면에서 처리
// review: { headline, content, rating, readingCount, visibility, regDate, modDate?,
//           bookTitle, cover, author?, displayName?, mine? }
const props = defineProps({
  review: { type: Object, required: true },
  showWriter: { type: Boolean, default: false },
  teamName: { type: String, default: null },
});

const visibility = computed(() => getVisibilityOption(props.review.visibility));

// 빈 줄로 나눈 문단은 문단 간격으로 띄운다 (한 줄 바꿈은 pre-wrap으로 유지)
const paragraphs = computed(() => (props.review.content ?? '').split(/\n\s*\n/));

// "2026-03-22" 또는 "2026-03-22T10:15:00" → "2026년 3월 22일"
const formatDate = (date) => {
  if (!date) return '-';
  const [year, month, day] = date.slice(0, 10).split('-').map(Number);
  return `${year}년 ${month}월 ${day}일`;
};
</script>

<style scoped>
.review-article { background: white; border: 1px solid #e5e7eb; border-radius: 6px; overflow: hidden; }

.article-head { display: grid; grid-template-columns: minmax(0, 1fr) 185px; gap: 28px; align-items: center; padding: 28px 34px; background: #fbfdff; border-bottom: 1px solid #e5e7eb; }

.badge-row { display: flex; align-items: center; gap: 8px; min-height: 20px; }
.visibility-badge { display: inline-flex; align-items: center; gap: 4px; padding: 5px 8px; border-radius: 3px; font-size: 10px; font-weight: 700; line-height: 1.5; }
.visibility-badge.public { background: #eff6ff; color: #2563eb; }
.visibility-badge.group { background: #f3f0ff; color: #7658bd; }
.visibility-badge.private { background: #f3f4f6; color: #6b7280; }
.team-name { min-width: 0; font-size: 10px; color: #6b7280; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.headline-label { margin: 7px 0 0 0; font-size: 10px; font-weight: 700; color: #2563eb; letter-spacing: 0.4px; }
.headline { margin: 9px 0 0 0; font-size: 19px; font-weight: 400; line-height: 1.55; letter-spacing: -0.38px; color: #111827; word-break: break-word; }
.writer { display: flex; align-items: center; gap: 7px; margin: 7px 0 0 0; font-size: 10px; }
.writer strong { color: #374151; }
.writer .writer-label { color: #9ca3af; }

.head-book { display: flex; align-items: center; gap: 13px; min-width: 0; padding-left: 24px; border-left: 1px solid #e5e7eb; }
.book-cover { flex-shrink: 0; width: 54px; height: 76px; overflow: hidden; box-shadow: -2px 3px 0 0 #d8d2e0; }
.book-cover img { display: block; width: 100%; height: 100%; object-fit: cover; }
.book-cover-fallback { display: block; height: 100%; box-sizing: border-box; background: #e8d7b5; padding: 7px 5px; font-family: Georgia, serif; font-size: 9px; font-weight: 700; line-height: 1.4; color: #4a3a25; word-break: break-all; overflow: hidden; }
.book-info { min-width: 0; }
.book-label { margin: 0 0 6px 0; font-size: 9px; color: #9ca3af; }
.book-title { margin: 0; font-size: 15px; font-weight: 400; line-height: 1.3; color: #111827; word-break: break-word; }
.book-author { margin: 4px 0 0 0; font-size: 9px; color: #6b7280; word-break: break-word; }

.article-body { display: grid; grid-template-columns: minmax(0, 1fr) 190px; gap: 34px; align-items: start; padding: 32px 34px 38px; }
.review-content { display: flex; flex-direction: column; gap: 18px; }
.content-text { margin: 0; font-size: 13px; line-height: 1.9; color: #374151; white-space: pre-wrap; word-break: break-word; }

.review-info { padding: 4px 16px 16px; background: #f8fafc; border: 1px solid #e5e7eb; border-radius: 5px; }
.info-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 14px 0; border-bottom: 1px solid #e5e7eb; }
.info-label { display: inline-flex; align-items: center; gap: 6px; font-size: 10px; color: #6b7280; white-space: nowrap; }
.info-value { font-size: 11px; font-weight: 700; color: #374151; white-space: nowrap; }
.rating { font-size: 15px; font-weight: 700; color: #d97706; }
.rating-max { font-size: 9px; color: #9ca3af; }
.info-note { display: flex; align-items: flex-start; gap: 8px; margin: 0; padding-top: 15px; font-size: 9px; line-height: 1.5; color: #6b7280; }
.info-note img { flex-shrink: 0; margin-top: 2px; }

@media (max-width: 640px) {
  .article-head { grid-template-columns: 1fr; gap: 20px; padding: 24px 20px; }
  .head-book { padding-left: 0; padding-top: 20px; border-left: none; border-top: 1px solid #e5e7eb; }
  .article-body { grid-template-columns: 1fr; gap: 24px; padding: 24px 20px 28px; }
  .headline { font-size: 17px; }
}
</style>
