<template>
  <div class="login-container">
    <div class="login-card">
      <h1 class="login-title">BookTalker</h1>
      <p class="login-subtitle">독서 후기를 기록하고 공유하세요</p>

      <form @submit.prevent="onSubmit" class="login-form">
        <div class="form-group">
          <label for="email" class="form-label">이메일</label>
          <input
            id="email"
            v-model="email"
            type="email"
            required
            placeholder="you@example.com"
            class="form-input"
          />
        </div>

        <div class="form-group">
          <label for="password" class="form-label">비밀번호</label>
          <input
            id="password"
            v-model="password"
            type="password"
            required
            placeholder="••••••••"
            class="form-input"
          />
        </div>

        <button type="submit" class="submit-button">
          이메일로 로그인
        </button>
      </form>

      <div class="divider">
        <span>또는</span>
      </div>

      <button type="button" @click="onNaverLogin" class="naver-button">
        네이버 로그인
      </button>

      <p class="signup-link">
        아직 회원이 아니신가요?
        <button type="button" class="link-button">회원가입</button>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';

const email = ref('');
const password = ref('');

const onSubmit = () => {
  console.log('email:', email.value, 'password:', password.value);
};

const onNaverLogin = () => {
  window.location.href = 'http://localhost:8010/oauth2/authorization/naver';
  console.log('네이버 로그인 시도');
};
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

.login-form {
  margin-bottom: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  margin-bottom: 8px;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  font-size: 15px;
  color: #1f2937;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  transition: border-color 0.15s ease;
}

.form-input:focus {
  outline: none;
  border-color: #2563eb;
}

.form-input::placeholder {
  color: #9ca3af;
}

.submit-button {
  width: 100%;
  padding: 12px;
  font-size: 15px;
  font-weight: 500;
  color: white;
  background: #2563eb;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.15s ease;
  margin-top: 8px;
}

.submit-button:hover {
  background: #1d4ed8;
}

.divider {
  display: flex;
  align-items: center;
  margin: 24px 0;
  text-align: center;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e5e7eb;
}

.divider span {
  padding: 0 16px;
  font-size: 13px;
  color: #9ca3af;
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

.signup-link {
  margin-top: 24px;
  text-align: center;
  font-size: 14px;
  color: #6b7280;
}

.link-button {
  background: none;
  border: none;
  color: #2563eb;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  padding: 0;
  margin-left: 4px;
  transition: color 0.15s ease;
}

.link-button:hover {
  color: #1d4ed8;
}

@media (max-width: 640px) {
  .login-card {
    padding: 32px 24px;
  }
}
</style>
