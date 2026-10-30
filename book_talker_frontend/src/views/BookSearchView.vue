<template>
  <div class="app-container">
    <!-- 메인 콘텐츠 (max-width: 720px) -->
    <main class="main-content">
      <div class="content-wrapper">
        <!-- 검색 카드 -->
        <section class="search-hero">
          <div class="hero-heading">
            <div class="hero-badge">
              <img :src="searchBadgeIcon" alt="" width="19" height="19" />
            </div>
            <div>
              <h2 class="hero-title">어떤 책을 읽으셨나요?</h2>
              <p class="hero-subtitle">책 제목을 검색하고 나만의 독후감을 남겨보세요.</p>
            </div>
          </div>

          <form @submit.prevent="onSearch" class="search-form">
            <input
              v-model="searchQuery"
              type="text"
              placeholder="책 제목을 입력해 주세요"
              class="search-input"
            />
            <button
              type="submit"
              :disabled="isLoading || !searchQuery.trim()"
              class="search-button"
            >
              <img :src="searchButtonIcon" alt="" width="13" height="13" />
              {{ isLoading ? '검색 중' : '검색' }}
            </button>
          </form>
        </section>

        <!-- 로딩 -->
        <div v-if="isLoading" class="state-card">
          <div class="spinner"></div>
          <p class="state-subtitle">검색 중...</p>
        </div>

        <!-- 검색 결과 -->
        <section v-else-if="hasSearched && !isSearchUnavailable && books.length > 0" class="result-card">
          <div class="result-header">
            <div>
              <h3 class="result-title">검색 결과</h3>
              <p class="result-count">{{ totalResults }}권의 책을 찾았어요.</p>
            </div>
            <div class="pagination-controls">
              <button
                type="button"
                @click="goToPrevPage"
                :disabled="currentPage === 1 || isLoading"
                class="page-button"
                aria-label="이전 페이지"
              >
                ←
              </button>
              <span class="page-number">
                <strong>{{ currentPage }}</strong><span class="page-total"> / {{ totalPages }}</span>
              </span>
              <button
                type="button"
                @click="goToNextPage"
                :disabled="!canGoNext || isLoading"
                class="page-button"
                aria-label="다음 페이지"
              >
                →
              </button>
            </div>
          </div>

          <div class="book-list">
            <article
              v-for="book in books"
              :key="book.isbn13"
              class="book-row"
            >
              <div class="book-cover">
                <img
                  v-if="book.cover"
                  :src="book.cover"
                  :alt="book.title"
                  @error="handleImageError"
                />
              </div>
              <div class="book-info">
                <h4 class="book-title">{{ book.title }}</h4>
                <p v-if="book.author" class="book-author">{{ book.author }}</p>
              </div>
              <button
                type="button"
                @click="goToReviewCreate(book)"
                class="review-button"
              >
                독후감 쓰기
              </button>
            </article>
          </div>
        </section>

        <!-- 검색 전 · 결과 없음 · 검색 서비스 장애 -->
        <div v-else class="state-card">
          <div class="state-icon">
            <img :src="bookIcon" alt="" width="22" height="22" />
          </div>
          <template v-if="isSearchUnavailable">
            <p class="state-title">책 검색 서비스 점검 중입니다</p>
            <p class="state-subtitle">잠시 후 다시 시도해주세요</p>
          </template>
          <template v-else-if="hasSearched">
            <p class="state-title">검색 결과가 없습니다</p>
            <p class="state-subtitle">다른 검색어로 시도해보세요</p>
          </template>
          <template v-else>
            <p class="state-title">찾고 싶은 책이 있나요?</p>
            <p class="state-subtitle">위 검색창에 책 제목을 입력하면 독후감을 작성할 책을 찾을 수 있어요.</p>
          </template>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useToast } from 'vue-toastification';
import apiClient from '../api/client';
import { useSelectionStore } from '../stores/selectionStore';
import searchBadgeIcon from '../assets/icons/search-badge.svg';
import searchButtonIcon from '../assets/icons/search-button.svg';
import bookIcon from '../assets/icons/book.svg';

const router = useRouter();
const selectionStore = useSelectionStore();
const toast = useToast();

// 서버 검색 페이지 크기 (BE에서 고정)
const PAGE_SIZE = 10;

const searchQuery = ref('');
const books = ref([]);
const isLoading = ref(false);
const hasSearched = ref(false);
const currentPage = ref(1);
const canGoNext = ref(false);
const totalResults = ref(0);
const isSearchUnavailable = ref(false);

const totalPages = computed(() => Math.max(currentPage.value, Math.ceil(totalResults.value / PAGE_SIZE)));

const goToReviewCreate = (book) => {
  if (!book) return;
  selectionStore.setSelectedBook(book);
  router.push({ name: 'review-create' });
};

const fetchBooks = async (page = 1) => {
  if (!searchQuery.value.trim()) return;

  isLoading.value = true;
  hasSearched.value = true;
  isSearchUnavailable.value = false;

  try {
    const response = await apiClient.get('/book/search', {
      params: {
        query: searchQuery.value.trim(),
        start: String(page),
      },
      // 204는 "검색 결과 없음"이라 정상, 5xx는 화면에 점검 안내를 띄우므로 토스트 생략
      skipErrorCodes: [204, 500, 502, 503, 504]
    });

    books.value = response.data.items || [];
    totalResults.value = response.data.totalResults || 0;
    currentPage.value = page;
    canGoNext.value = response.data.hasNext === true;
  } catch (error) {
    console.error('책 검색 실패:', error);

    if (error.response?.status === 204) {
      // 204는 정상 - 검색 결과 없음
      books.value = [];
      totalResults.value = 0;
      canGoNext.value = false;
      toast.info('검색 결과가 없습니다.');
    } else if (error.response?.status >= 500) {
      // 검색 제공처 장애 등 서버 오류 - 이전 결과를 지우고 점검 안내 표시
      books.value = [];
      totalResults.value = 0;
      canGoNext.value = false;
      isSearchUnavailable.value = true;
    } else {
      // 다른 에러는 Interceptor가 자동 처리함
      totalResults.value = 0;
      canGoNext.value = false;
    }
  } finally {
    isLoading.value = false;
  }
};

const onSearch = async () => {
  if (!searchQuery.value.trim() || isLoading.value) return;

  currentPage.value = 1;
  totalResults.value = 0;
  await fetchBooks(1);
};

const goToPrevPage = async () => {
  if (currentPage.value === 1 || isLoading.value) return;
  const target = currentPage.value - 1;
  await fetchBooks(target);
};

const goToNextPage = async () => {
  if (!canGoNext.value || isLoading.value) return;
  const target = currentPage.value + 1;
  await fetchBooks(target);
};

const handleImageError = (event) => {
  event.target.style.display = 'none';
};
</script>

<style scoped>
.app-container {
  min-height: 100%;
}

/* 메인 콘텐츠 */
.main-content {
  padding: 42px 24px 90px;
}

.content-wrapper {
  max-width: 720px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 검색 카드 */
.search-hero {
  padding: 26px 28px 28px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: linear-gradient(166deg, #f4f8ff 0%, #fbfdff 100%);
}

.hero-heading {
  display: flex;
  align-items: center;
  gap: 14px;
}

.hero-badge {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: white;
  filter: drop-shadow(0 5px 8px rgba(37, 99, 235, 0.1));
}

.hero-title {
  font-size: 21px;
  font-weight: 400;
  line-height: 31.5px;
  color: #111827;
  margin: 0;
}

.hero-subtitle {
  font-size: 12px;
  line-height: 18px;
  color: #6b7280;
  margin: 6px 0 0 0;
}

.search-form {
  position: relative;
  margin-top: 22px;
}

.search-input {
  width: 100%;
  height: 46px;
  box-sizing: border-box;
  padding: 0 86px 0 12px;
  font-size: 13px;
  color: #111827;
  background: white;
  border: 1px solid #dbeafe;
  border-radius: 5px;
  box-shadow: 0 5px 18px rgba(37, 99, 235, 0.05);
  outline: none;
  transition: border-color 0.15s ease;
}

.search-input::placeholder {
  color: #9ca3af;
}

.search-input:focus {
  border-color: #93c5fd;
}

.search-button {
  position: absolute;
  top: 5px;
  right: 5px;
  height: 36px;
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 0 13px;
  font-size: 12px;
  font-weight: 700;
  color: white;
  background: #2563eb;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.search-button:hover:not(:disabled) {
  background: #1d4ed8;
}

.search-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 검색 전 · 결과 없음 · 장애 · 로딩 */
.state-card {
  min-height: 330px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.state-icon {
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #eff6ff;
}

.state-title {
  font-size: 17px;
  font-weight: 700;
  line-height: 25.5px;
  color: #111827;
  margin: 17px 0 0 0;
}

.state-subtitle {
  font-size: 13px;
  line-height: 21.45px;
  color: #6b7280;
  margin: 9px 0 0 0;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e5e7eb;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 검색 결과 */
.result-card {
  overflow: hidden;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 17px 22px;
  background: #fbfdff;
  border-bottom: 1px solid #e5e7eb;
}

.result-title {
  font-size: 17px;
  font-weight: 400;
  line-height: 25.5px;
  color: #111827;
  margin: 0;
}

.result-count {
  font-size: 11px;
  line-height: 16.5px;
  color: #6b7280;
  margin: 5px 0 0 0;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-button {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  font-size: 12px;
  color: #4b5563;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.page-button:hover:not(:disabled) {
  border-color: #d1d5db;
  background: #f9fafb;
}

.page-button:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.page-number {
  min-width: 42px;
  text-align: center;
  font-size: 12px;
  font-weight: 700;
  line-height: 18px;
  color: #2563eb;
}

.page-total {
  font-size: 10px;
  font-weight: 400;
  color: #9ca3af;
}

.book-list {
  padding: 0 20px;
}

.book-row {
  min-height: 112px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 15px 4px;
  border-bottom: 1px solid #f0f1f3;
}

.book-row:last-child {
  border-bottom: none;
}

.book-cover {
  width: 52px;
  height: 76px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 2px;
  background: #f3f4f6;
  box-shadow: -2px 3px 0 rgba(17, 24, 39, 0.12);
}

.book-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.book-info {
  flex: 1;
  min-width: 0;
}

.book-title {
  font-size: 15px;
  font-weight: 400;
  line-height: 22.5px;
  color: #111827;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-author {
  font-size: 11px;
  line-height: 16.5px;
  color: #6b7280;
  margin: 7px 0 0 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-button {
  min-width: 94px;
  min-height: 38px;
  flex-shrink: 0;
  padding: 9px 15px;
  font-size: 13px;
  font-weight: 700;
  color: #2563eb;
  background: white;
  border: 1px solid #93c5fd;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.review-button:hover {
  background: #eff6ff;
}

@media (max-width: 640px) {
  .main-content {
    padding: 32px 16px 64px;
  }

  .search-hero {
    padding: 22px 18px 20px;
  }

  .hero-title {
    font-size: 18px;
    line-height: 26px;
  }

  /* iOS는 16px 미만 입력창에 포커스하면 화면을 확대함 */
  .search-input {
    font-size: 16px;
  }

  .result-header {
    padding: 15px 16px;
  }

  .book-list {
    padding: 0 12px;
  }

  .book-row {
    gap: 12px;
  }

  .review-button {
    min-width: 0;
    padding: 8px 12px;
  }
}
</style>
