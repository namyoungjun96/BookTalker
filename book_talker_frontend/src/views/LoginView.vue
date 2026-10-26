<template>
  <div class="login-container">
    <div class="login-card">
      <h1 class="login-title">BookTalker</h1>
      <p class="login-subtitle">독서 후기를 기록하고 공유하세요</p>

      <button type="button" @click="onNaverLogin" class="naver-button">
        네이버로 시작하기
      </button>

      <p class="login-hint">
        회원가입 없이 네이버 아이디로 바로 시작할 수 있어요.<br />
        이름·이메일 같은 개인정보는 저장하지 않아요.
      </p>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useToast } from 'vue-toastification';
import { API_BASE_URL } from '../api/client';
import axios from 'axios';

const router = useRouter();
const toast = useToast();

const onNaverLogin = () => {
  sessionStorage.setItem('login-attempt', 'true');
  window.location.href = `${API_BASE_URL}/oauth2/authorization/naver`;
};

const checkLoginSuccess = async () => {
  const loginAttempt = sessionStorage.getItem('login-attempt');

  if (loginAttempt === 'true') {
    try {
      const response = await axios.get(`${API_BASE_URL}/auth/session`, {
        withCredentials: true,
      });

      if (response.status === 200) {
        sessionStorage.removeItem('login-attempt');
        toast.success('로그인되었습니다!');

        setTimeout(() => {
          router.push({ name: 'home' });
        }, 500);
      }
    } catch {
      sessionStorage.removeItem('login-attempt');
    }
  }
};

onMounted(() => {
  checkLoginSuccess();
});
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background-color: #f9fafb;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 40px;
}

.login-title {
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  text-align: center;
  margin: 0 0 8px 0;
}

.login-subtitle {
  font-size: 15px;
  color: #6b7280;
  text-align: center;
  margin: 0 0 32px 0;
}

.naver-button {
  width: 100%;
  padding: 12px;
  font-size: 15px;
  font-weight: 500;
  color: white;
  background: #03c75a;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.naver-button:hover {
  background: #02b350;
}

.login-hint {
  font-size: 13px;
  line-height: 1.6;
  color: #6b7280;
  text-align: center;
  margin: 16px 0 0 0;
}

@media (max-width: 640px) {
  .login-card {
    padding: 32px 24px;
  }
}
</style>
