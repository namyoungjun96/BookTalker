<template>
  <header class="app-header">
    <div class="header-content">
      <h1 class="logo">BookTalker</h1>
      <nav class="nav-links">
        <router-link to="/" class="nav-link" :class="{ active: isActiveNav('/') }">
          홈
        </router-link>
        <router-link v-if="isLoggedIn" to="/mypage" class="nav-link" :class="{ active: isActiveNav('/mypage') }">
          마이페이지
        </router-link>
        <router-link v-if="isLoggedIn" to="/book-search" class="nav-link" :class="{ active: isActiveNav('/book-search') }">
          검색
        </router-link>
        <button v-if="isLoggedIn" type="button" @click="onLogout" class="nav-link logout-btn">
          로그아웃
        </button>
        <router-link v-else to="/login" class="nav-link login-btn">
          로그인
        </router-link>
      </nav>
    </div>
  </header>
</template>

<script setup>
import { ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import axios from 'axios';
import { useNav } from '../composables/useNav';
import { API_BASE_URL } from '../api/client';

const route = useRoute();
const { isActiveNav, onLogout } = useNav();

const isLoggedIn = ref(false);

const checkLoginStatus = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/api/auth/session`, {
      withCredentials: true,
    });
    isLoggedIn.value = response.status === 200;
  } catch {
    isLoggedIn.value = false;
  }
};

// 라우트 변경 시마다 로그인 상태 재확인 (로그인/로그아웃 반영)
watch(() => route.path, checkLoginStatus, { immediate: true });
</script>

<style scoped>
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

.nav-link:hover { color: #1f2937; }
.nav-link.active { color: #2563eb; font-weight: 500; }
.logout-btn:hover { color: #ef4444; }
.login-btn { color: #2563eb; }
</style>
