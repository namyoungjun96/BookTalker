<template>
  <div class="min-h-screen flex flex-col bg-gray-50">
    <!-- 상단 네비게이션 메뉴 -->
    <header class="w-full border-b bg-white shadow-sm">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between h-16">
          <!-- 로고/제목 -->
          <div class="flex items-center">
            <h1 class="text-xl font-bold text-gray-900">
              BookTalker
            </h1>
          </div>

          <!-- 네비게이션 탭 메뉴 -->
          <nav
            class="flex items-center space-x-0 rounded-full bg-gray-100 px-1 py-1 border-2 border-gray-300"
          >
            <router-link
              to="/"
              class="px-4 py-2 text-sm font-medium rounded-l-full transition-colors border-r border-gray-300"
              :class="isActiveRoute('/') 
                ? 'text-white bg-indigo-600 shadow-sm' 
                : 'text-gray-600 hover:text-gray-900 hover:bg-white'"
            >
              홈
            </router-link>
            <router-link
              to="/book-search"
              class="px-4 py-2 text-sm font-medium rounded-r-full transition-colors"
              :class="isActiveRoute('/book-search') 
                ? 'text-white bg-indigo-600 shadow-sm' 
                : 'text-gray-600 hover:text-gray-900 hover:bg-white'"
            >
              책 검색
            </router-link>
            <button
              type="button"
              @click="onLogout"
              class="ml-4 px-4 py-2 text-sm font-medium text-gray-600 hover:text-red-500 transition-colors"
            >
              로그아웃
            </button>
          </nav>
        </div>
      </div>
    </header>

    <!-- 메인 콘텐츠 -->
    <main class="flex-1 max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-8">
      <!-- 검색 섹션 -->
      <div class="mb-8">
        <h2 class="text-2xl font-bold text-gray-900 mb-6">책 검색</h2>

        <form
          @submit.prevent="onSearch"
          class="flex items-center gap-0"
        >
          <div class="flex-1 relative">
            <input
              v-model="searchQuery"
              type="text"
              placeholder="책 제목을 입력하세요..."
              class="w-full rounded-l-lg border border-gray-300 border-r-0 px-4 py-3 pr-12
                     text-gray-900 shadow-sm
                     focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500
                     placeholder:text-gray-400"
            />
            <!-- 검색 아이콘 -->
            <button
              type="submit"
              :disabled="isLoading || !searchQuery.trim()"
              class="absolute right-2 top-1/2 -translate-y-1/2 p-2 text-gray-400 hover:text-indigo-600
                     disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              <svg
                class="w-5 h-5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                />
              </svg>
            </button>
          </div>
          <button
            type="submit"
            :disabled="isLoading || !searchQuery.trim()"
            class="px-6 py-3 rounded-r-lg border border-l-0 border-indigo-600 text-indigo-600 font-semibold
                   bg-white hover:bg-indigo-50
                   focus:outline-none focus:ring-2 focus:ring-indigo-500
                   transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ isLoading ? '검색 중...' : '검색' }}
          </button>
        </form>
      </div>

      <!-- 검색 결과 -->
      <div v-if="hasSearched">
        <div v-if="isLoading" class="text-center py-12">
          <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
          <p class="mt-4 text-gray-600">검색 중...</p>
        </div>

        <div v-else-if="books.length === 0" class="text-center py-12">
          <p class="text-gray-500 text-lg">검색 결과가 없습니다.</p>
          <p class="text-gray-400 text-sm mt-2">다른 검색어로 시도해보세요.</p>
        </div>

        <div v-else>
          <div class="flex items-center justify-between mb-4">
            <p class="text-sm text-gray-600">
              총 {{ totalResults }}개의 결과 (페이지 {{ currentPage }})
            </p>

            <!-- 페이지네이션 -->
            <div class="inline-flex items-center gap-2">
              <button
                type="button"
                @click="goToPrevPage"
                :disabled="currentPage === 1 || isLoading"
                class="px-3 py-1.5 text-xs sm:text-sm rounded-full border border-gray-300 text-gray-600
                       bg-white hover:bg-gray-50
                       disabled:opacity-40 disabled:cursor-not-allowed"
              >
                이전
              </button>
              <span class="text-xs text-gray-500">
                {{ currentPage }} 페이지
              </span>
              <button
                type="button"
                @click="goToNextPage"
                :disabled="!canGoNext || isLoading"
                class="px-3 py-1.5 text-xs sm:text-sm rounded-full border border-gray-300 text-gray-600
                       bg-white hover:bg-gray-50
                       disabled:opacity-40 disabled:cursor-not-allowed"
              >
                다음
              </button>
            </div>
          </div>

          <!-- 책 목록 테이블 -->
          <div class="bg-white rounded-lg shadow-md overflow-hidden">
            <table class="min-w-full divide-y divide-gray-200">
              <thead class="bg-gray-50">
                <tr>
                  <th scope="col" class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-24">
                    표지
                  </th>
                  <th scope="col" class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    책 이름
                  </th>
                  <th scope="col" class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    지은이
                  </th>
                  <th scope="col" class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    출판사
                  </th>
                  <th scope="col" class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    출간날짜
                  </th>
                  <th scope="col" class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    가격
                  </th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-200">
                <tr
                  v-for="book in books"
                  :key="book.itemId"
                  class="hover:bg-gray-50 transition-colors"
                >
                  <!-- 책 표지 -->
                  <td class="px-4 py-3 whitespace-nowrap">
                    <div class="w-16 h-20 bg-gray-200 rounded overflow-hidden flex-shrink-0">
                      <img
                        v-if="book.cover"
                        :src="book.cover"
                        :alt="book.title"
                        class="w-full h-full object-cover"
                        @error="handleImageError"
                      />
                      <div v-else class="w-full h-full flex items-center justify-center text-gray-400 text-xs p-1 text-center">
                        이미지 없음
                      </div>
                    </div>
                  </td>
                  
                  <!-- 책 이름 -->
                  <td class="px-4 py-3">
                    <div class="text-sm font-medium text-gray-900 max-w-xs">
                      {{ book.title }}
                    </div>
                    <div class="text-xs text-gray-500 mt-1">
                      <a
                        :href="book.link"
                        target="_blank"
                        rel="noopener noreferrer"
                        class="text-indigo-600 hover:text-indigo-700"
                      >
                        책 정보 →
                      </a>
                    </div>
                  </td>
                  
                  <!-- 지은이 -->
                  <td class="px-4 py-3">
                    <div class="text-sm text-gray-900">
                      {{ book.author || '-' }}
                    </div>
                  </td>
                  
                  <!-- 출판사 -->
                  <td class="px-4 py-3">
                    <div class="text-sm text-gray-900">
                      {{ book.publisher || '-' }}
                    </div>
                  </td>
                  
                  <!-- 출간날짜 -->
                  <td class="px-4 py-3 whitespace-nowrap">
                    <div class="text-sm text-gray-900">
                      {{ formatDate(book.pubDate) || '-' }}
                    </div>
                  </td>
                  
                  <!-- 가격 -->
                  <td class="px-4 py-3 whitespace-nowrap">
                    <div class="text-sm font-semibold text-indigo-600">
                      {{ formatPrice(book.priceSales) }}원
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- 초기 상태 -->
      <div v-else class="text-center py-12">
        <div class="max-w-md mx-auto">
          <svg
            class="mx-auto h-12 w-12 text-gray-400"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
            />
          </svg>
          <h3 class="mt-4 text-lg font-medium text-gray-900">책을 검색해보세요</h3>
          <p class="mt-2 text-sm text-gray-500">
            위의 검색창에 책 제목을 입력하고 검색 버튼을 클릭하세요.
          </p>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import axios from 'axios';

const router = useRouter();
const route = useRoute();

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

const fetchBooks = async (page = 1) => {
  if (!searchQuery.value.trim()) return;

  isLoading.value = true;
  hasSearched.value = true;

  // 페이지에 따른 start 값 계산 (1부터 시작해서 1씩 증가)
  const startIndex = page;

  try {
    const response = await axios.get('http://localhost:8010/api/book/search', {
      params: {
        query: searchQuery.value.trim(),
        start: String(startIndex),
        maxResults: String(pageSize),
        cover: 'Small',
      },
      withCredentials: true,
    });

    // 응답 형식: { items: [], totalResults: string, startIndex: number }
    const data = response.data.items || [];
    const total = parseInt(response.data.totalResults || '0', 10);
    const responseStartIndex = response.data.startIndex || startIndex;

    books.value = data;
    totalResults.value = total;
    currentPage.value = page;

    // 다음 페이지 가능 여부 계산
    // 현재 startIndex부터 시작해서 pageSize만큼 가져왔을 때
    // 남은 결과가 있으면 다음 페이지 가능
    const currentStartIndex = responseStartIndex;
    const remainingResults = total - (currentStartIndex - 1) - data.length;
    canGoNext.value = remainingResults > 0;
  } catch (error) {
    console.error('책 검색 실패:', error);

    if (error.response?.status === 204) {
      // No Content - 검색 결과 없음
      books.value = [];
      totalResults.value = 0;
      canGoNext.value = false;
    } else {
      alert('책 검색 중 오류가 발생했습니다.');
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
    await axios.get('http://localhost:8010/logout', {
      withCredentials: true
    });
    router.push({ name: 'login' });
  } catch (error) {
    console.error('로그아웃 실패:', error);
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
  event.target.parentElement.innerHTML = '<div class="w-full h-full flex items-center justify-center text-gray-400">이미지 없음</div>';
};
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
