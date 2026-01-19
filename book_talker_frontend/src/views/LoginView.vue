<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 px-4">
    <div
      class="w-full max-w-md bg-white shadow-lg rounded-2xl px-8 py-10 sm:px-10 sm:py-12"
    >
      <!-- 제목 -->
      <h1 class="text-2xl sm:text-3xl font-bold text-gray-900 mb-2 text-center">
        로그인
      </h1>
      <p class="text-sm text-gray-500 mb-8 text-center">
        BookTalker에 로그인해 주세요.
      </p>

      <!-- 이메일 -->
      <form @submit.prevent="onSubmit" class="space-y-6">
        <div>
          <label
            for="email"
            class="block text-sm font-medium text-gray-700 mb-1"
          >
            이메일
          </label>
          <input
            id="email"
            v-model="email"
            type="email"
            required
            placeholder="you@example.com"
            class="block w-full rounded-lg border border-gray-300 px-3 py-2
                   text-gray-900 text-sm shadow-sm
                   focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500
                   placeholder:text-gray-400"
          />
        </div>

        <!-- 비밀번호 -->
        <div>
          <label
            for="password"
            class="block text-sm font-medium text-gray-700 mb-1"
          >
            비밀번호
          </label>
          <input
            id="password"
            v-model="password"
            type="password"
            required
            placeholder="••••••••"
            class="block w-full rounded-lg border border-gray-300 px-3 py-2
                   text-gray-900 text-sm shadow-sm
                   focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500
                   placeholder:text-gray-400"
          />
        </div>

        <!-- 일반 로그인 버튼 -->
        <button
          type="submit"
          class="w-full flex items-center justify-center gap-2
                 rounded-lg bg-indigo-600 px-4 py-2.5
                 text-sm sm:text-base font-semibold text-white
                 shadow hover:bg-indigo-700
                 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500
                 transition-colors"
        >
          이메일로 로그인
        </button>
      </form>

      <!-- 구분선 -->
      <div class="flex items-center my-6">
        <div class="flex-1 h-px bg-gray-200"></div>
        <span class="mx-3 text-xs text-gray-400">또는</span>
        <div class="flex-1 h-px bg-gray-200"></div>
      </div>

      <!-- 네이버 로그인 버튼 (이미지) -->
      <button
        type="button"
        @click="onNaverLogin"
        class="flex justify-center mx-auto focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-[#03C75A]"
      >
        <span>
            네이버 로그인
        </span>
      </button>

      <!-- 반응형 하단 텍스트 -->
      <p class="mt-6 text-xs sm:text-sm text-center text-gray-400">
        아직 회원이 아니신가요?
        <button
          type="button"
          class="text-indigo-500 hover:text-indigo-600 font-medium"
        >
          회원가입
        </button>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import naverLoginIcon from '../assets/img/naverLoginIcon.png';

const email = ref('');
const password = ref('');

const onSubmit = () => {
  // TODO: 실제 로그인 API 연동
  console.log('email:', email.value, 'password:', password.value);
  // 예: await login({ email: email.value, password: password.value });
};

const onNaverLogin = () => {
  // TODO: 네이버 로그인 연동 (OAuth2 리다이렉트 등)
  window.location.href = 'http://localhost:8010/oauth2/authorization/naver';
  console.log('네이버 로그인 시도');
};
</script>