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
    <main class="flex-1 flex items-center justify-center px-4 sm:px-6 lg:px-8">
      <div class="w-full max-w-2xl text-center">
        <h2 class="text-2xl sm:text-3xl font-bold text-gray-900 mb-3">
          메인 페이지
        </h2>
        <p class="text-sm sm:text-base text-gray-500 mb-6">
          로그인에 성공한 사용자가 처음 도착하는 인덱스 페이지입니다.
        </p>
        <p class="text-xs sm:text-sm text-gray-400">
          추후 여기에서 책 목록, 추천 도서, 사용자 정보 등을 보여주면 됩니다.
        </p>
      </div>
    </main>
  </div>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router';
import axios from 'axios';

const router = useRouter();
const route = useRoute();

const isActiveRoute = (path) => {
  return route.path === path;
};

const onLogout = async () => {
  try {
      await axios.get('http://localhost:8010/logout', {
        withCredentials: true
      });
      // 성공하면 프론트에서 직접 라우팅
      router.push('/');
    } catch (error) {
      console.error('로그아웃 실패:', error);
    }

  router.push({ name: 'login' });
};
</script>

