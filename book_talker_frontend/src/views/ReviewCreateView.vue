<template>
  <div class="app-container">
    <!-- 메인 콘텐츠 (브런치 글쓰기 스타일) -->
    <main class="main-content">
      <div class="content-wrapper">
        <h2 class="page-title">{{ isNextReading ? `${nextReadingCount}회독 독후감 작성` : '독후감 작성' }}</h2>

        <!-- 선택된 책 정보 카드 -->
        <div class="info-card" v-if="activeBook">
          <div class="book-preview">
            <div class="book-cover-small">
              <img
                v-if="activeBook.cover"
                :src="activeBook.cover"
                :alt="activeBook.title"
              />
              <div v-else class="cover-placeholder-small">📖</div>
            </div>
            <div class="book-preview-info">
              <h3 class="book-preview-title">{{ activeBook.title }}</h3>
              <p v-if="!isNextReading" class="book-preview-meta">
                {{ activeBook.author || '-' }} · {{ activeBook.publisher || '-' }}
              </p>
            </div>
          </div>
          <div v-if="bookAddError" class="error-message">
            {{ bookAddError }}
          </div>
        </div>

        <!-- 임시저장 복원 안내 -->
        <div v-if="restoredAt" class="draft-notice">
          <span>임시저장된 글을 불러왔어요 ({{ formatSavedAt(restoredAt) }})</span>
          <button type="button" class="draft-reset" @click="onResetDraft">새로 쓰기</button>
        </div>

        <!-- 독후감 작성 폼 (브런치 스타일) -->
        <form @submit.prevent="onSubmitReview" class="review-form">
          <!-- 헤드라인 -->
          <div class="form-section">
            <label class="form-label">
              한 줄 요약
              <span class="form-label-desc">랭킹 화면에 공개됩니다</span>
            </label>
            <input
              type="text"
              v-model="reviewHeadline"
              class="headline-input"
              placeholder="이 책을 한 문장으로 요약한다면?"
              maxlength="100"
            />
            <p class="char-count">{{ reviewHeadline.length }} / 100</p>
          </div>

          <div class="section-divider"></div>

          <!-- 독후감 내용 -->
          <div class="form-section">
            <div class="form-label-row">
              <label class="form-label">독후감 본문</label>
              <div class="visibility-selector" role="radiogroup" aria-label="본문 공개 범위">
                <button
                  v-for="option in VISIBILITY_OPTIONS"
                  :key="option.value"
                  type="button"
                  role="radio"
                  :aria-checked="visibility === option.value"
                  class="visibility-badge"
                  :class="[option.className, { selected: visibility === option.value }]"
                  @click="visibility = option.value"
                >
                  {{ option.label }}
                </button>
              </div>
            </div>
            <textarea
              v-model="reviewContent"
              rows="12"
              class="review-textarea"
              placeholder="책을 읽고 느낀 점, 기억에 남는 문장 등을 자유롭게 적어주세요."
            ></textarea>
          </div>

          <!-- 평점 -->
          <div class="form-section">
            <label class="form-label">평점</label>
            <div class="rating-control">
              <input
                type="range"
                min="1"
                max="5"
                step="1"
                v-model.number="rating"
                class="rating-slider"
              />
              <div class="rating-display">
                <span class="rating-value">{{ rating }}</span>
                <span class="rating-label">점</span>
              </div>
            </div>
          </div>

          <!-- 버튼 영역 -->
          <div class="form-actions">
            <button
              type="button"
              @click="goBackToSearch"
              class="action-button secondary"
            >
              취소
            </button>
            <button
              type="submit"
              :disabled="!activeBook || !reviewHeadline.trim() || !reviewContent.trim() || isSubmitting"
              class="action-button primary"
            >
              {{ isSubmitting ? '등록 중...' : '등록하기' }}
            </button>
          </div>
        </form>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useToast } from 'vue-toastification';
import apiClient from '../api/client';
import { useSelectionStore } from '../stores/selectionStore';
import { VISIBILITY_OPTIONS, DEFAULT_VISIBILITY } from '../constants/reviewVisibility';
import {
  loadReviewDraft,
  saveReviewDraft,
  clearReviewDraft,
  findLatestFirstReadingDraft,
} from '../composables/useReviewDraft';

const router = useRouter();
const route = useRoute();
const { selectedBook, setSelectedBook } = useSelectionStore();
const toast = useToast();

// next-reading 모드 판별
const isNextReading = computed(() => route.query.mode === 'next-reading');
const nextReadingCount = computed(() => Number(route.query.readingCount || 1) + 1);

// next-reading 모드일 때 query params로 책 정보 구성
const activeBook = computed(() => {
  if (isNextReading.value) {
    return {
      isbn13: route.query.isbn13,
      title: route.query.title || route.query.isbn13,
      cover: route.query.cover || null,
      author: null,
      publisher: null,
    };
  }
  return selectedBook.value;
});

const reviewHeadline = ref('');
const reviewContent = ref('');
const rating = ref(5);
const visibility = ref(DEFAULT_VISIBILITY);
const isCheckingBook = ref(false);
const bookExists = ref(null);
const bookAddError = ref(null);
const isSubmitting = ref(false);

// ===== 임시저장 =====
// 키: 책(isbn13) + 작성할 회차
const draftIsbn = computed(() => activeBook.value?.isbn13 || activeBook.value?.isbn || '');
const draftReadingCount = computed(() => (isNextReading.value ? nextReadingCount.value : 1));
const restoredAt = ref(null);
let saveTimer = null;

const persistDraft = () => {
  clearTimeout(saveTimer);
  saveTimer = null;
  if (!draftIsbn.value) return;

  // 아무것도 안 썼으면 저장하지 않음 (화면만 열어도 임시저장이 생기는 것 방지)
  if (!reviewHeadline.value.trim() && !reviewContent.value.trim()) {
    clearReviewDraft(draftIsbn.value, draftReadingCount.value);
    return;
  }
  saveReviewDraft(draftIsbn.value, draftReadingCount.value, {
    // 새로고침 후 selectedBook 없이 들어왔을 때 화면 복구용
    book: { ...activeBook.value, isbn13: draftIsbn.value },
    headline: reviewHeadline.value,
    content: reviewContent.value,
    rating: rating.value,
    visibility: visibility.value,
  });
};

// 웹뷰가 강제 종료되면 beforeunload가 불리지 않으므로 입력할 때마다 저장
const scheduleSave = () => {
  clearTimeout(saveTimer);
  saveTimer = setTimeout(persistDraft, 500);
};

// 대기 중인 저장이 있으면 바로 저장 (다른 앱으로 전환·화면 이동 직전)
const flushDraft = () => {
  if (saveTimer) persistDraft();
};

const discardDraft = () => {
  clearTimeout(saveTimer);
  saveTimer = null;
  clearReviewDraft(draftIsbn.value, draftReadingCount.value);
};

const restoreDraft = () => {
  const draft = loadReviewDraft(draftIsbn.value, draftReadingCount.value);
  if (!draft) return;

  reviewHeadline.value = draft.headline || '';
  reviewContent.value = draft.content || '';
  rating.value = Number.isInteger(draft.rating) && draft.rating >= 1 && draft.rating <= 5 ? draft.rating : 5;
  visibility.value = VISIBILITY_OPTIONS.some((option) => option.value === draft.visibility)
    ? draft.visibility
    : DEFAULT_VISIBILITY;
  restoredAt.value = draft.savedAt;
};

const onResetDraft = () => {
  if (!confirm('임시저장된 글을 지우고 새로 쓸까요?')) return;
  discardDraft();
  reviewHeadline.value = '';
  reviewContent.value = '';
  rating.value = 5;
  visibility.value = DEFAULT_VISIBILITY;
  restoredAt.value = null;
};

const formatSavedAt = (savedAt) => {
  const date = new Date(savedAt);
  const hh = String(date.getHours()).padStart(2, '0');
  const mm = String(date.getMinutes()).padStart(2, '0');
  return `${date.getMonth() + 1}월 ${date.getDate()}일 ${hh}:${mm}`;
};

const onVisibilityChange = () => {
  if (document.visibilityState === 'hidden') flushDraft();
};

watch([reviewHeadline, reviewContent, rating, visibility], scheduleSave);

const goBackToSearch = () => {
  if (isNextReading.value) {
    router.push({ name: 'mypage' });
  } else {
    router.push({ name: 'book-search' });
  }
};

const checkBookInBackend = async () => {
  if (!selectedBook.value) return;

  isCheckingBook.value = true;
  bookAddError.value = null;
  
  try {
    const bookData = {
      title: selectedBook.value.title,
      author: selectedBook.value.author || '',
      isbn: selectedBook.value.isbn || selectedBook.value.isbn13 || '',
      isbn13: selectedBook.value.isbn13 || selectedBook.value.isbn || '',
      cover: selectedBook.value.cover || '',
      categoryName: selectedBook.value.categoryName || '',
      publisher: selectedBook.value.publisher || '',
    };

    try {
      await apiClient.post('/book', bookData);
      bookExists.value = false;
    } catch (error) {
      if (error.response?.status === 409 || error.response?.status === 400) {
        bookExists.value = true;
      } else {
        throw error;
      }
    }
  } catch (error) {
    console.error('책 존재 여부 확인 실패:', error);
    bookExists.value = null;
    bookAddError.value = error.response?.data?.message || '책 정보 확인 중 오류가 발생했습니다.';
  } finally {
    isCheckingBook.value = false;
  }
};

const onSubmitReview = async () => {
  if (!activeBook.value || !reviewHeadline.value.trim() || !reviewContent.value.trim() || isSubmitting.value) return;

  isSubmitting.value = true;
  // 세션 만료(401)로 로그인 화면으로 이동해도 마지막 입력이 남도록 등록 전에 저장
  persistDraft();

  try {
    if (isNextReading.value) {
      // 회독 독후감: next-reading 엔드포인트
      const reviewData = {
        isbn13: activeBook.value.isbn13,
        headline: reviewHeadline.value.trim(),
        content: reviewContent.value.trim(),
        rating: rating.value,
        visibility: visibility.value,
      };
      await apiClient.post('/review/next-reading', reviewData);
      discardDraft();
      toast.success('회독 독후감이 등록되었습니다!');
      router.push({ name: 'mypage' });
    } else {
      // 1회독: 기존 플로우
      if (bookExists.value === false) {
        const bookData = {
          title: selectedBook.value.title,
          author: selectedBook.value.author || '',
          isbn: selectedBook.value.isbn || selectedBook.value.isbn13 || '',
          isbn13: selectedBook.value.isbn13 || selectedBook.value.isbn || '',
          cover: selectedBook.value.cover || '',
          categoryName: selectedBook.value.categoryName || '',
          publisher: selectedBook.value.publisher || '',
        };
        try {
          await apiClient.post('/book', bookData);
          bookExists.value = true;
        } catch (error) {
          if (error.response?.status !== 409) {
            throw new Error('책 추가 실패: ' + (error.response?.data?.message || error.message));
          }
          bookExists.value = true;
        }
      }

      const reviewData = {
        isbn13: selectedBook.value.isbn13 || selectedBook.value.isbn || '',
        headline: reviewHeadline.value.trim(),
        content: reviewContent.value.trim(),
        rating: rating.value,
        visibility: visibility.value,
      };
      await apiClient.post('/review', reviewData);
      discardDraft();
      toast.success('독후감이 성공적으로 등록되었습니다!');
      setTimeout(() => {
        router.push({ name: 'book-search' });
      }, 500);
    }
  } catch (error) {
    if (error.response?.status === 409) {
      toast.error('이미 이 책의 독후감이 존재합니다.');
    } else if (error.response?.status === 400) {
      toast.error('선행 회독 독후감이 없습니다. 1회독부터 작성해주세요.');
    } else {
      toast.error('독후감 등록 중 오류가 발생했습니다.');
    }
  } finally {
    isSubmitting.value = false;
  }
};

onMounted(async () => {
  document.addEventListener('visibilitychange', onVisibilityChange);

  if (isNextReading.value) {
    if (!route.query.isbn13) {
      router.push({ name: 'mypage' });
      return;
    }
    restoreDraft();
    return;
  }

  if (!selectedBook.value) {
    // 새로고침·로그인 후 복귀로 선택한 책이 사라졌으면, 가장 최근 1회독 임시저장의 책으로 복구
    const latestDraft = findLatestFirstReadingDraft();
    if (!latestDraft) {
      router.push({ name: 'book-search' });
      return;
    }
    setSelectedBook(latestDraft.book);
  }

  restoreDraft();
  await checkBookInBackend();
});

onBeforeUnmount(() => {
  document.removeEventListener('visibilitychange', onVisibilityChange);
  flushDraft();
});
</script>

<style scoped>
.app-container {
  min-height: 100%;
}

/* 메인 콘텐츠 */
.main-content {
  padding: 48px 24px;
}

.content-wrapper {
  max-width: 720px;
  margin: 0 auto;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 32px 0;
  line-height: 1.3;
}

/* 정보 카드 */
.info-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 24px;
}

.book-preview {
  display: flex;
  gap: 16px;
}

.book-cover-small {
  width: 50px;
  height: 70px;
  flex-shrink: 0;
  border-radius: 4px;
  overflow: hidden;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
}

.book-cover-small img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder-small {
  font-size: 24px;
  color: #9ca3af;
}

.book-preview-info {
  flex: 1;
  min-width: 0;
}

.book-preview-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px 0;
}

.book-preview-meta {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.error-message {
  margin-top: 12px;
  padding: 12px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 4px;
  color: #dc2626;
  font-size: 14px;
}

/* 임시저장 복원 안내 */
.draft-notice {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 16px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  font-size: 14px;
  color: #1e40af;
}

.draft-reset {
  flex-shrink: 0;
  padding: 0;
  border: none;
  background: transparent;
  font-size: 14px;
  font-weight: 500;
  color: #2563eb;
  text-decoration: underline;
  cursor: pointer;
}

/* 독후감 폼 (브런치 스타일) */
.form-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.form-label-row .form-label {
  margin-bottom: 0;
}

.visibility-selector {
  display: flex;
  align-items: center;
  gap: 6px;
}

.visibility-badge {
  font-size: 11px;
  font-weight: 500;
  padding: 3px 10px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
}

.visibility-badge.selected.public { background: #dbeafe; color: #2563eb; border-color: #bfdbfe; }
.visibility-badge.selected.group { background: #f3f0ff; color: #7658bd; border-color: #ddd6fe; }
.visibility-badge.selected.private { background: #f3f4f6; color: #6b7280; border-color: #e5e7eb; }

.form-label-desc {
  font-size: 12px;
  font-weight: 400;
  color: #9ca3af;
  margin-left: 8px;
}

.headline-input {
  width: 100%;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  padding: 12px 16px;
  font-size: 16px;
  color: #1f2937;
  font-family: inherit;
  transition: border-color 0.15s ease;
  box-sizing: border-box;
}

.headline-input:focus {
  outline: none;
  border-color: #2563eb;
}

.headline-input::placeholder {
  color: #9ca3af;
}

.char-count {
  font-size: 12px;
  color: #9ca3af;
  text-align: right;
  margin: 6px 0 0 0;
}

.section-divider {
  border: none;
  border-top: 1px dashed #e5e7eb;
  margin: 8px 0 32px 0;
}

.review-form {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 32px;
}

.form-section {
  margin-bottom: 32px;
}

.form-section:last-of-type {
  margin-bottom: 0;
}

.form-label {
  display: block;
  font-size: 15px;
  font-weight: 500;
  color: #1f2937;
  margin-bottom: 12px;
}

.review-textarea {
  width: 100%;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  padding: 12px 16px;
  font-size: 16px;
  line-height: 1.7;
  color: #1f2937;
  font-family: inherit;
  resize: vertical;
  transition: border-color 0.15s ease;
}

.review-textarea:focus {
  outline: none;
  border-color: #2563eb;
}

.review-textarea::placeholder {
  color: #9ca3af;
}

/* 평점 컨트롤 */
.rating-control {
  display: flex;
  align-items: center;
  gap: 16px;
}

.rating-slider {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  background: #e5e7eb;
  outline: none;
  -webkit-appearance: none;
}

.rating-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #2563eb;
  cursor: pointer;
}

.rating-slider::-moz-range-thumb {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #2563eb;
  cursor: pointer;
  border: none;
}

.rating-display {
  display: flex;
  align-items: baseline;
  gap: 4px;
  min-width: 60px;
}

.rating-value {
  font-size: 24px;
  font-weight: 600;
  color: #2563eb;
}

.rating-label {
  font-size: 14px;
  color: #6b7280;
}

/* 버튼 영역 */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 24px;
  border-top: 1px solid #f3f4f6;
  margin-top: 32px;
}

.action-button {
  padding: 10px 24px;
  font-size: 15px;
  font-weight: 500;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  transition: all 0.15s ease;
}

.action-button.primary {
  background: #2563eb;
  color: white;
}

.action-button.primary:hover:not(:disabled) {
  background: #1d4ed8;
}

.action-button.primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-button.secondary {
  background: transparent;
  color: #1f2937;
  border: 1px solid #e5e7eb;
}

.action-button.secondary:hover {
  background: #f9fafb;
  border-color: #d1d5db;
}

@media (max-width: 640px) {
  .main-content {
    padding: 32px 16px;
  }

  .page-title {
    font-size: 24px;
    margin-bottom: 24px;
  }

  .review-form {
    padding: 24px;
  }

  .form-actions {
    flex-direction: column-reverse;
  }

  .action-button {
    width: 100%;
  }
}
</style>
