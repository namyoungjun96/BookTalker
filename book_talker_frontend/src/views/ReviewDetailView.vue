<template>
  <div class="app-container">
    <main class="main-content">
      <div class="content-wrapper">

        <!-- 로딩 -->
        <div v-if="isLoading" class="loading-state">
          <div class="spinner"></div>
        </div>

        <!-- 에러 -->
        <div v-else-if="error" class="error-state">
          <p class="error-title">{{ error }}</p>
          <button @click="router.back()" class="back-btn">돌아가기</button>
        </div>

        <!-- 상세 내용 -->
        <template v-else-if="review">
          <!-- 상단 액션 바 -->
          <div class="top-bar">
            <button @click="router.back()" class="back-link">← 마이페이지로</button>
            <button v-if="!isEditing" @click="startEdit" class="edit-btn">수정하기</button>
          </div>

          <!-- 책 정보 카드 -->
          <div class="book-card">
            <div class="book-cover">
              <img
                v-if="review.bookCover"
                :src="review.bookCover"
                :alt="review.bookTitle"
                @error="handleImageError"
              />
              <div v-else class="cover-placeholder">📖</div>
            </div>
            <div class="book-info">
              <h2 class="book-title">{{ review.bookTitle || '-' }}</h2>
            </div>
          </div>

          <!-- 조회 모드 -->
          <div v-if="!isEditing" class="review-card">
            <div class="headline-section">
              <p class="section-label">한 줄 요약</p>
              <p class="headline-text">{{ review.headline || '-' }}</p>
            </div>

            <div class="divider"></div>

            <div class="content-section">
              <div class="content-header">
                <p class="section-label">독후감 본문</p>
                <span class="visibility-badge" :class="review.isPublic ? 'public' : 'private'">
                  {{ review.isPublic ? '공개' : '나만 보기' }}
                </span>
              </div>
              <p class="content-text">{{ review.content || '-' }}</p>
            </div>

            <div class="divider"></div>

            <div class="meta-section">
              <span class="rating-display">★ {{ review.rating || '-' }}점</span>
              <div class="dates">
                <span class="date">작성 {{ formatDate(review.regDate) }}</span>
                <span v-if="review.modDate" class="date modified">수정 {{ formatDate(review.modDate) }}</span>
              </div>
            </div>
          </div>

          <!-- 수정 모드 -->
          <form v-else @submit.prevent="onSubmitEdit" class="review-card edit-form">
            <!-- 헤드라인 -->
            <div class="form-section">
              <label class="section-label">
                한 줄 요약
                <span class="label-desc">랭킹 화면에 공개됩니다</span>
              </label>
              <input
                type="text"
                v-model="editForm.headline"
                class="form-input"
                placeholder="이 책을 한 문장으로 요약한다면?"
                maxlength="100"
              />
              <p class="char-count">{{ editForm.headline.length }} / 100</p>
            </div>

            <div class="divider"></div>

            <!-- 본문 -->
            <div class="form-section">
              <div class="content-header">
                <label class="section-label">
                  독후감 본문
                  <span class="label-desc">공개 여부를 설정하세요</span>
                </label>
                <label class="toggle-label">
                  <input type="checkbox" v-model="editForm.isPublic" class="toggle-input" />
                  <span class="visibility-badge" :class="editForm.isPublic ? 'public' : 'private'">
                    {{ editForm.isPublic ? '공개' : '나만 보기' }}
                  </span>
                </label>
              </div>
              <textarea
                v-model="editForm.content"
                rows="12"
                class="form-textarea"
                placeholder="책을 읽고 느낀 점, 기억에 남는 문장 등을 자유롭게 적어주세요."
              ></textarea>
            </div>

            <div class="divider"></div>

            <!-- 평점 -->
            <div class="form-section">
              <label class="section-label">평점</label>
              <div class="rating-control">
                <input
                  type="range"
                  min="1"
                  max="5"
                  step="1"
                  v-model.number="editForm.rating"
                  class="rating-slider"
                />
                <div class="rating-value-wrap">
                  <span class="rating-value">{{ editForm.rating }}</span>
                  <span class="rating-unit">점</span>
                </div>
              </div>
            </div>

            <!-- 버튼 -->
            <div class="form-actions">
              <button type="button" @click="cancelEdit" class="action-btn secondary">취소</button>
              <button
                type="submit"
                :disabled="isSubmitting || !editForm.headline.trim() || !editForm.content.trim()"
                class="action-btn primary"
              >
                {{ isSubmitting ? '저장 중...' : '저장하기' }}
              </button>
            </div>
          </form>

        </template>

      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useToast } from 'vue-toastification';
import apiClient from '../api/client';

const router = useRouter();
const route = useRoute();
const toast = useToast();

const review = ref(null);
const isLoading = ref(true);
const error = ref(null);
const isEditing = ref(false);
const isSubmitting = ref(false);

const editForm = ref({
  headline: '',
  content: '',
  rating: 5,
  isPublic: false,
});

const fetchDetail = async () => {
  try {
    const response = await apiClient.get('/api/review/detail', {
      params: { reviewId: route.params.id },
    });
    review.value = response.data;
  } catch (e) {
    if (e.response?.status === 403) {
      error.value = '접근 권한이 없는 독후감입니다.';
    } else {
      error.value = '독후감을 불러올 수 없습니다.';
    }
  } finally {
    isLoading.value = false;
  }
};

const startEdit = () => {
  editForm.value = {
    headline: review.value.headline || '',
    content: review.value.content || '',
    rating: review.value.rating ?? 5,
    isPublic: review.value.isPublic ?? false,
  };
  isEditing.value = true;
};

const cancelEdit = () => {
  isEditing.value = false;
};

const onSubmitEdit = async () => {
  if (isSubmitting.value) return;
  isSubmitting.value = true;
  try {
    await apiClient.put('/api/review', {
      reviewId: review.value.id,
      headline: editForm.value.headline.trim(),
      content: editForm.value.content.trim(),
      rating: editForm.value.rating,
      isPublic: editForm.value.isPublic,
    });
    // 저장 후 로컬 데이터 갱신
    review.value = {
      ...review.value,
      headline: editForm.value.headline.trim(),
      content: editForm.value.content.trim(),
      rating: editForm.value.rating,
      isPublic: editForm.value.isPublic,
      modDate: new Date().toISOString(),
    };
    isEditing.value = false;
    toast.success('독후감이 수정되었습니다.');
  } catch (e) {
    if (e.response?.status === 403) {
      toast.error('수정 권한이 없습니다.');
    } else if (e.response?.status === 400) {
      toast.error(e.response.data?.message || '입력값을 확인해주세요.');
    } else {
      toast.error('수정 중 오류가 발생했습니다.');
    }
  } finally {
    isSubmitting.value = false;
  }
};

const formatDate = (dateString) => {
  if (!dateString) return '-';
  try {
    return new Date(dateString).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  } catch {
    return dateString;
  }
};

const handleImageError = (event) => {
  event.target.style.display = 'none';
};

onMounted(fetchDetail);
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background-color: #f9fafb;
}

.main-content {
  padding: 48px 24px;
}

.content-wrapper {
  max-width: 720px;
  margin: 0 auto;
}

/* 로딩/에러 */
.loading-state {
  display: flex;
  justify-content: center;
  padding: 80px 0;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e5e7eb;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.error-state {
  text-align: center;
  padding: 80px 24px;
}

.error-title {
  font-size: 18px;
  color: #6b7280;
  margin: 0 0 24px 0;
}

/* 상단 바 */
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.back-link {
  font-size: 14px;
  color: #6b7280;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  transition: color 0.15s ease;
}

.back-link:hover { color: #2563eb; }

.edit-btn {
  padding: 8px 18px;
  font-size: 14px;
  font-weight: 500;
  color: #2563eb;
  background: white;
  border: 1px solid #2563eb;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.edit-btn:hover {
  background: #2563eb;
  color: white;
}

.back-btn {
  padding: 10px 20px;
  font-size: 15px;
  font-weight: 500;
  color: white;
  background: #2563eb;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.back-btn:hover { background: #1d4ed8; }

/* 책 카드 */
.book-card {
  display: flex;
  gap: 16px;
  align-items: center;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
}

.book-cover {
  width: 60px;
  height: 80px;
  flex-shrink: 0;
  border-radius: 4px;
  overflow: hidden;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
}

.book-cover img { width: 100%; height: 100%; object-fit: cover; }
.cover-placeholder { font-size: 28px; }
.book-info { flex: 1; min-width: 0; }

.book-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
  line-height: 1.4;
}

/* 공통 카드 */
.review-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 28px;
}

.divider {
  border: none;
  border-top: 1px solid #f3f4f6;
  margin: 24px 0;
}

.section-label {
  font-size: 12px;
  font-weight: 500;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin: 0 0 10px 0;
}

/* 조회 모드 */
.headline-text {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.5;
  margin: 0;
}

.content-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.content-text {
  font-size: 16px;
  line-height: 1.8;
  color: #1f2937;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.visibility-badge {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 10px;
  cursor: default;
}

.visibility-badge.public { background: #dbeafe; color: #2563eb; }
.visibility-badge.private { background: #f3f4f6; color: #6b7280; }

.meta-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.rating-display {
  font-size: 15px;
  font-weight: 600;
  color: #f59e0b;
}

.dates { display: flex; flex-direction: column; align-items: flex-end; gap: 2px; }
.date { font-size: 13px; color: #9ca3af; }
.date.modified { font-size: 12px; }

/* 수정 모드 */
.form-section { margin-bottom: 4px; }

.label-desc {
  font-size: 11px;
  font-weight: 400;
  color: #d1d5db;
  margin-left: 6px;
  text-transform: none;
  letter-spacing: 0;
}

.form-input {
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

.form-input:focus { outline: none; border-color: #2563eb; }

.char-count {
  font-size: 12px;
  color: #9ca3af;
  text-align: right;
  margin: 6px 0 0 0;
}

.form-textarea {
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
  box-sizing: border-box;
}

.form-textarea:focus { outline: none; border-color: #2563eb; }

.toggle-label {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  margin-bottom: 12px;
}

.toggle-input { display: none; }

.toggle-label .visibility-badge { cursor: pointer; }

/* 평점 */
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

.rating-value-wrap { display: flex; align-items: baseline; gap: 4px; min-width: 52px; }
.rating-value { font-size: 24px; font-weight: 600; color: #2563eb; }
.rating-unit { font-size: 14px; color: #6b7280; }

/* 폼 버튼 */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 24px;
  border-top: 1px solid #f3f4f6;
  margin-top: 24px;
}

.action-btn {
  padding: 10px 24px;
  font-size: 15px;
  font-weight: 500;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  transition: all 0.15s ease;
}

.action-btn.primary { background: #2563eb; color: white; }
.action-btn.primary:hover:not(:disabled) { background: #1d4ed8; }
.action-btn.primary:disabled { opacity: 0.5; cursor: not-allowed; }
.action-btn.secondary { background: transparent; color: #1f2937; border: 1px solid #e5e7eb; }
.action-btn.secondary:hover { background: #f9fafb; border-color: #d1d5db; }

@media (max-width: 640px) {
  .main-content { padding: 32px 16px; }
  .review-card { padding: 20px; }
  .headline-text { font-size: 18px; }
  .form-actions { flex-direction: column-reverse; }
  .action-btn { width: 100%; }
}
</style>
