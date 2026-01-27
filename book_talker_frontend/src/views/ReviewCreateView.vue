<template>
  <div class="app-container">
    <!-- 미니멀 헤더 -->
    <header class="app-header">
      <div class="header-content">
        <h1 class="logo">BookTalker</h1>
        <nav class="nav-links">
          <router-link to="/" class="nav-link" :class="{ active: isActiveRoute('/') }">
            홈
          </router-link>
          <router-link to="/book-search" class="nav-link" :class="{ active: isActiveRoute('/book-search') }">
            검색
          </router-link>
        </nav>
      </div>
    </header>

    <!-- 메인 콘텐츠 (브런치 글쓰기 스타일) -->
    <main class="main-content">
      <div class="content-wrapper">
        <h2 class="page-title">독후감 작성</h2>

        <!-- 선택된 책 정보 카드 -->
        <div class="info-card" v-if="selectedBook">
          <div class="book-preview">
            <div class="book-cover-small">
              <img
                v-if="selectedBook.cover"
                :src="selectedBook.cover"
                :alt="selectedBook.title"
              />
              <div v-else class="cover-placeholder-small">📖</div>
            </div>
            <div class="book-preview-info">
              <h3 class="book-preview-title">{{ selectedBook.title }}</h3>
              <p class="book-preview-meta">
                {{ selectedBook.author || '-' }} · {{ selectedBook.publisher || '-' }}
              </p>
            </div>
          </div>
          <div v-if="bookAddError" class="error-message">
            {{ bookAddError }}
          </div>
        </div>

        <!-- 작성자 정보 -->
        <div class="info-card" v-if="currentUser">
          <div class="author-info">
            <div class="author-avatar">
              <img
                v-if="currentUser.profileImage"
                :src="currentUser.profileImage"
                alt="profile"
              />
              <div v-else class="avatar-placeholder">{{ currentUser.name?.[0] || 'U' }}</div>
            </div>
            <div class="author-details">
              <p class="author-name">{{ currentUser.name }}</p>
              <p v-if="currentUser.email" class="author-email">{{ currentUser.email }}</p>
            </div>
          </div>
        </div>

        <!-- 독후감 작성 폼 (브런치 스타일) -->
        <form @submit.prevent="onSubmitReview" class="review-form">
          <!-- 독후감 내용 -->
          <div class="form-section">
            <label class="form-label">독후감 내용</label>
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
              :disabled="!selectedBook || !reviewContent.trim() || isSubmitting"
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
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useToast } from 'vue-toastification';
import apiClient from '../api/client';
import { useSelectionStore } from '../stores/selectionStore';

const router = useRouter();
const route = useRoute();
const { selectedBook, currentUser, setCurrentUser } = useSelectionStore();
const toast = useToast();

const reviewContent = ref('');
const rating = ref(5);
const isCheckingBook = ref(false);
const bookExists = ref(null);
const bookAddError = ref(null);
const isLoadingUser = ref(false);
const isSubmitting = ref(false);

const isActiveRoute = (path) => {
  return route.path === path;
};

const goBackToSearch = () => {
  router.push({ name: 'book-search' });
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
      await apiClient.post('/api/book', bookData);
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

const fetchCurrentUser = async () => {
  if (currentUser.value) return;

  isLoadingUser.value = true;
  try {
    const response = await apiClient.get('/api/user/me');
    setCurrentUser(response.data);
  } catch (error) {
    console.error('로그인 사용자 정보 조회 실패:', error);
  } finally {
    isLoadingUser.value = false;
  }
};

const onSubmitReview = async () => {
  if (!selectedBook.value || !reviewContent.value.trim() || isSubmitting.value) return;

  isSubmitting.value = true;

  try {
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
        await apiClient.post('/api/book', bookData);
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
      writer: currentUser.value?.email || 'anonymous',
      content: reviewContent.value.trim(),
      rating: String(rating.value),
    };

    await apiClient.post('/api/review', reviewData);

    toast.success('독후감이 성공적으로 등록되었습니다!');
    setTimeout(() => {
      router.push({ name: 'book-search' });
    }, 500);
  } catch (error) {
    console.error('독후감 등록 실패:', error);
    toast.error('독후감 등록 중 오류가 발생했습니다: ' + (error.response?.data?.message || error.message));
  } finally {
    isSubmitting.value = false;
  }
};

onMounted(async () => {
  if (!selectedBook.value) {
    router.push({ name: 'book-search' });
    return;
  }

  await Promise.all([fetchCurrentUser(), checkBookInBackend()]);
});
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background-color: #f9fafb;
}

/* 헤더 (공통) */
.app-header {
  background: white;
  border-bottom: 1px solid #e5e7eb;
  padding: 16px 0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 720px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.nav-links {
  display: flex;
  gap: 24px;
  align-items: center;
}

.nav-link {
  font-size: 15px;
  color: #6b7280;
  text-decoration: none;
  transition: color 0.15s ease;
}

.nav-link:hover {
  color: #1f2937;
}

.nav-link.active {
  color: #2563eb;
  font-weight: 500;
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

/* 작성자 정보 */
.author-info {
  display: flex;
  gap: 12px;
  align-items: center;
}

.author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  background: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
}

.author-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #2563eb;
  color: white;
  font-weight: 500;
  font-size: 16px;
}

.author-details {
  flex: 1;
}

.author-name {
  font-size: 15px;
  font-weight: 500;
  color: #1f2937;
  margin: 0 0 2px 0;
}

.author-email {
  font-size: 13px;
  color: #6b7280;
  margin: 0;
}

/* 독후감 폼 (브런치 스타일) */
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
