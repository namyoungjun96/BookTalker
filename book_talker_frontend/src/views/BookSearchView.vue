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
          <router-link to="/mypage" class="nav-link" :class="{ active: isActiveRoute('/mypage') }">
            마이페이지
          </router-link>
          <router-link to="/book-search" class="nav-link" :class="{ active: isActiveRoute('/book-search') }">
            검색
          </router-link>
          <button type="button" @click="onLogout" class="nav-link logout-btn">
            로그아웃
          </button>
        </nav>
      </div>
    </header>

    <!-- 메인 콘텐츠 (max-width: 720px) -->
    <main class="main-content">
      <div class="content-wrapper">
        <h2 class="page-title">책 검색</h2>

        <!-- 검색 입력 (노션 스타일) -->
        <form @submit.prevent="onSearch" class="search-form">
          <div class="search-input-wrapper">
            <input
              v-model="searchQuery"
              type="text"
              placeholder="책 제목을 입력하세요..."
              class="search-input"
            />
            <button
              type="submit"
              :disabled="isLoading || !searchQuery.trim()"
              class="search-button"
            >
              {{ isLoading ? '검색 중...' : '검색' }}
            </button>
          </div>
        </form>

        <!-- 검색 결과 -->
        <div v-if="hasSearched">
          <!-- 로딩 -->
          <div v-if="isLoading" class="loading-state">
            <div class="spinner"></div>
            <p class="loading-text">검색 중...</p>
          </div>

          <!-- 결과 없음 -->
          <div v-else-if="books.length === 0" class="empty-state">
            <p class="empty-title">검색 결과가 없습니다</p>
            <p class="empty-subtitle">다른 검색어로 시도해보세요</p>
          </div>

          <!-- 검색 결과 카드 리스트 -->
          <div v-else>
            <!-- 페이지네이션 -->
            <div class="pagination-info">
              <span class="result-count">총 {{ totalResults }}개의 결과</span>
              <div class="pagination-controls">
                <button
                  type="button"
                  @click="goToPrevPage"
                  :disabled="currentPage === 1 || isLoading"
                  class="page-button"
                >
                  이전
                </button>
                <span class="page-number">{{ currentPage }} 페이지</span>
                <button
                  type="button"
                  @click="goToNextPage"
                  :disabled="!canGoNext || isLoading"
                  class="page-button"
                >
                  다음
                </button>
              </div>
            </div>

            <!-- 책 카드 리스트 -->
            <div class="book-list">
              <article
                v-for="book in books"
                :key="book.itemId"
                class="book-card"
              >
                <div class="book-card-content">
                  <!-- 책 표지 -->
                  <div class="book-cover">
                    <img
                      v-if="book.cover"
                      :src="book.cover"
                      :alt="book.title"
                      @error="handleImageError"
                    />
                    <div v-else class="cover-placeholder">📖</div>
                  </div>

                  <!-- 책 정보 -->
                  <div class="book-info">
                    <h3 class="book-title">{{ book.title }}</h3>
                    <p class="book-meta">
                      <span v-if="book.author">{{ book.author }}</span>
                      <span v-if="book.author && book.publisher"> · </span>
                      <span v-if="book.publisher">{{ book.publisher }}</span>
                    </p>
                    <p v-if="book.pubDate" class="book-date">
                      {{ formatDate(book.pubDate) }}
                    </p>
                    <p v-if="book.priceSales" class="book-price">
                      {{ formatPrice(book.priceSales) }}원
                    </p>
                  </div>
                </div>

                <!-- 독후감 쓰기 버튼 -->
                <button
                  type="button"
                  @click="goToReviewCreate(book)"
                  class="review-button"
                >
                  독후감 쓰기
                </button>
              </article>
            </div>
          </div>
        </div>

        <!-- 초기 상태 -->
        <div v-else class="initial-state">
          <div class="initial-icon">🔍</div>
          <h3 class="initial-title">책을 검색해보세요</h3>
          <p class="initial-subtitle">위의 검색창에 책 제목을 입력하고 검색 버튼을 클릭하세요</p>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useToast } from 'vue-toastification';
import apiClient from '../api/client';
import { useSelectionStore } from '../stores/selectionStore';

const router = useRouter();
const route = useRoute();
const selectionStore = useSelectionStore();
const toast = useToast();

const searchQuery = ref('');
const books = ref([]);
const isLoading = ref(false);
const hasSearched = ref(false);
const currentPage = ref(1);
const pageSize = 10;
const canGoNext = ref(false);
const totalResults = ref(0);

const isActiveRoute = (path) => {
  return route.path === path;
};

const goToReviewCreate = (book) => {
  if (!book) return;
  selectionStore.setSelectedBook(book);
  router.push({ name: 'review-create' });
};

const fetchBooks = async (page = 1) => {
  if (!searchQuery.value.trim()) return;

  isLoading.value = true;
  hasSearched.value = true;

  const startIndex = page;

  try {
    const response = await apiClient.get('/api/book/search', {
      params: {
        query: searchQuery.value.trim(),
        start: String(startIndex),
        maxResults: String(pageSize),
        cover: 'Small',
      },
      skipErrorCodes: [204]  // 204는 "검색 결과 없음"이라 정상
    });

    const data = response.data.items || [];
    const total = parseInt(response.data.totalResults || '0', 10);
    const responseStartIndex = response.data.startIndex || startIndex;

    books.value = data;
    totalResults.value = total;
    currentPage.value = page;

    const currentStartIndex = responseStartIndex;
    const remainingResults = total - (currentStartIndex - 1) - data.length;
    canGoNext.value = remainingResults > 0;
  } catch (error) {
    console.error('책 검색 실패:', error);

    if (error.response?.status === 204) {
      // 204는 정상 - 검색 결과 없음
      books.value = [];
      totalResults.value = 0;
      canGoNext.value = false;
      toast.info('검색 결과가 없습니다.');
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

const onLogout = async () => {
  try {
    await apiClient.get('/logout');
    toast.success('로그아웃되었습니다.');
    setTimeout(() => {
      router.push({ name: 'login' });
    }, 500);
  } catch (error) {
    console.error('로그아웃 실패:', error);
    // Interceptor가 에러 메시지를 이미 표시함
    router.push({ name: 'login' });
  }
};

const formatPrice = (price) => {
  if (!price) return '0';
  return new Intl.NumberFormat('ko-KR').format(price);
};

const formatDate = (date) => {
  if (!date) return '';
  if (typeof date === 'string') {
    return date.split('T')[0];
  }
  return date;
};

const handleImageError = (event) => {
  event.target.style.display = 'none';
};
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background-color: #f9fafb;
}

/* 헤더 (HomeView와 동일) */
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
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  transition: color 0.15s ease;
}

.nav-link:hover {
  color: #1f2937;
}

.nav-link.active {
  color: #2563eb;
  font-weight: 500;
}

.logout-btn {
  color: #9ca3af;
}

.logout-btn:hover {
  color: #ef4444;
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

/* 검색 폼 (노션 스타일) */
.search-form {
  margin-bottom: 32px;
}

.search-input-wrapper {
  display: flex;
  gap: 0;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
  background: white;
}

.search-input {
  flex: 1;
  border: none;
  padding: 12px 16px;
  font-size: 15px;
  outline: none;
}

.search-input:focus {
  border-color: #2563eb;
}

.search-button {
  padding: 12px 24px;
  background: #2563eb;
  color: white;
  font-size: 15px;
  font-weight: 500;
  border: none;
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

/* 로딩/빈 상태 (HomeView와 동일) */
.loading-state,
.empty-state {
  text-align: center;
  padding: 64px 24px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e5e7eb;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  color: #6b7280;
  font-size: 15px;
}

.empty-title {
  font-size: 18px;
  font-weight: 500;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.empty-subtitle {
  font-size: 15px;
  color: #6b7280;
  margin: 0;
}

/* 초기 상태 */
.initial-state {
  text-align: center;
  padding: 80px 24px;
}

.initial-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.initial-title {
  font-size: 20px;
  font-weight: 500;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.initial-subtitle {
  font-size: 15px;
  color: #6b7280;
  margin: 0;
}

/* 페이지네이션 */
.pagination-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px 0;
}

.result-count {
  font-size: 14px;
  color: #6b7280;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-button {
  padding: 6px 12px;
  font-size: 14px;
  color: #1f2937;
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
  opacity: 0.4;
  cursor: not-allowed;
}

.page-number {
  font-size: 14px;
  color: #6b7280;
}

/* 책 카드 리스트 */
.book-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.book-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 20px;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.book-card:hover {
  border-color: #d1d5db;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.book-card-content {
  display: flex;
  gap: 16px;
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

.book-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  font-size: 32px;
  color: #9ca3af;
}

.book-info {
  flex: 1;
  min-width: 0;
}

.book-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 8px 0;
  line-height: 1.4;
}

.book-meta {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 4px 0;
}

.book-date {
  font-size: 13px;
  color: #9ca3af;
  margin: 0 0 4px 0;
}

.book-price {
  font-size: 15px;
  font-weight: 500;
  color: #2563eb;
  margin: 0;
}

.review-button {
  width: 100%;
  padding: 10px 20px;
  background: #2563eb;
  color: white;
  font-size: 15px;
  font-weight: 500;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.review-button:hover {
  background: #1d4ed8;
}

@media (max-width: 640px) {
  .main-content {
    padding: 32px 16px;
  }

  .page-title {
    font-size: 24px;
    margin-bottom: 24px;
  }

  .book-card {
    padding: 16px;
  }

  .book-card-content {
    gap: 12px;
  }

  .book-cover {
    width: 50px;
    height: 70px;
  }

  .pagination-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
