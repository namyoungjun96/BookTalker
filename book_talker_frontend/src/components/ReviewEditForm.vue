<template>
  <form @submit.prevent="onSubmit" class="review-card edit-form">
    <!-- 헤드라인 -->
    <div class="form-section">
      <label class="section-label">
        한 줄 요약
        <span class="label-desc">랭킹 화면에 공개됩니다</span>
      </label>
      <input
        type="text"
        v-model="form.headline"
        class="form-input"
        placeholder="이 책을 한 문장으로 요약한다면?"
        maxlength="100"
      />
      <p class="char-count">{{ form.headline.length }} / 100</p>
    </div>

    <div class="divider"></div>

    <!-- 본문 -->
    <div class="form-section">
      <div class="content-header">
        <label class="section-label">
          독후감 본문
          <span class="label-desc">공개 범위를 설정하세요</span>
        </label>
        <div class="visibility-selector" role="radiogroup" aria-label="본문 공개 범위">
          <button
            v-for="option in VISIBILITY_OPTIONS"
            :key="option.value"
            type="button"
            role="radio"
            :aria-checked="form.visibility === option.value"
            class="visibility-badge selectable"
            :class="[option.className, { selected: form.visibility === option.value }]"
            @click="form.visibility = option.value"
          >
            {{ option.label }}
          </button>
        </div>
      </div>
      <p v-if="privateNotice && form.visibility === 'PRIVATE'" class="private-notice">{{ privateNotice }}</p>
      <textarea
        v-model="form.content"
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
          v-model.number="form.rating"
          class="rating-slider"
        />
        <div class="rating-value-wrap">
          <span class="rating-value">{{ form.rating }}</span>
          <span class="rating-unit">점</span>
        </div>
      </div>
    </div>

    <!-- 버튼 -->
    <div class="form-actions">
      <button type="button" @click="emit('cancel')" class="action-btn secondary">취소</button>
      <button
        type="submit"
        :disabled="isSubmitting || !form.headline.trim() || !form.content.trim()"
        class="action-btn primary"
      >
        {{ isSubmitting ? '저장 중...' : '저장하기' }}
      </button>
    </div>
  </form>
</template>

<script setup>
import { ref } from 'vue';
import { useToast } from 'vue-toastification';
import apiClient from '../api/client';
import { VISIBILITY_OPTIONS, DEFAULT_VISIBILITY } from '../constants/reviewVisibility';

// 저장 후 행동(화면 갱신, 이동, 성공 토스트)은 화면마다 달라서 saved 이벤트로 넘긴다
const props = defineProps({
  reviewId: { type: [Number, String], required: true },
  review: { type: Object, required: true },
  // PRIVATE를 골랐을 때 선택지 아래에 띄울 안내 — 비공개로 바꾸면 글이 안 보이게 되는 화면(모임 등)에서 넘김
  privateNotice: { type: String, default: '' },
});

const emit = defineEmits(['saved', 'cancel']);

const toast = useToast();
const isSubmitting = ref(false);

const form = ref({
  headline: props.review.headline || '',
  content: props.review.content || '',
  rating: props.review.rating ?? 5,
  visibility: props.review.visibility ?? DEFAULT_VISIBILITY,
});

const onSubmit = async () => {
  if (isSubmitting.value) return;
  isSubmitting.value = true;
  const updated = {
    headline: form.value.headline.trim(),
    content: form.value.content.trim(),
    rating: form.value.rating,
    visibility: form.value.visibility,
  };
  try {
    await apiClient.put('/review', { reviewId: props.reviewId, ...updated });
    emit('saved', updated);
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
</script>

<style scoped>
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

.content-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.visibility-badge {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 10px;
  cursor: default;
}

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

.visibility-selector {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
}

/* 선택 UI: 미선택은 흐리게, 선택된 항목만 색상 표시 */
.visibility-badge.selectable {
  border: 1px solid transparent;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
}

.visibility-badge.selectable.selected.public { background: #dbeafe; color: #2563eb; border-color: #bfdbfe; }
.visibility-badge.selectable.selected.group { background: #f3f0ff; color: #7658bd; border-color: #ddd6fe; }
.visibility-badge.selectable.selected.private { background: #f3f4f6; color: #6b7280; border-color: #e5e7eb; }

.private-notice {
  margin: 0 0 12px 0;
  padding: 10px 12px;
  font-size: 13px;
  color: #92400e;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 4px;
}

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
  .review-card { padding: 20px; }
  .form-actions { flex-direction: column-reverse; }
  .action-btn { width: 100%; }
}
</style>
