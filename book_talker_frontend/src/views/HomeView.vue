<template>
  <div class="app-container">
    <main class="main-content">
      <div class="content-wrapper">
        <div class="hero-section">
          <h2 class="hero-title">장르별 평점 TOP 10</h2>
          <p class="hero-subtitle">독자들이 가장 높게 평가한 책을 장르별로 만나보세요</p>
        </div>

        <!-- 로딩 -->
        <div v-if="isLoading" class="loading-container">
          <div class="spinner"></div>
        </div>

        <!-- 데이터 있음 -->
        <template v-else-if="genres.length > 0">
          <div class="genre-tabs">
            <button
              v-for="genre in genres"
              :key="genre"
              class="genre-tab"
              :class="{ active: selectedGenre === genre }"
              @click="selectedGenre = genre"
            >
              {{ genre }}
            </button>
          </div>

          <ul class="rank-list">
            <li
              v-for="(book, index) in currentBooks"
              :key="book.isbn13"
              class="rank-item"
            >
              <span class="rank-number" :class="{ top3: index < 3 }">{{ index + 1 }}</span>
              <img
                v-if="book.cover"
                :src="book.cover"
                :alt="book.title"
                class="book-cover"
              />
              <div v-else class="book-cover-placeholder">📖</div>
              <div class="book-info">
                <p class="book-title">{{ book.title }}</p>
                <div class="book-meta">
                  <span class="rating">★ {{ book.avgRating.toFixed(1) }}</span>
                  <span class="review-count">리뷰 {{ book.reviewCount }}개</span>
                </div>
              </div>
            </li>
          </ul>
        </template>

        <!-- 데이터 없음 -->
        <div v-else class="empty-state">
          <div class="empty-icon">📚</div>
          <p class="empty-title">아직 랭킹 데이터가 없습니다</p>
          <p class="empty-desc">리뷰가 쌓이면 장르별 TOP 10이 표시됩니다.<br>먼저 책을 검색하고 리뷰를 남겨보세요!</p>
          <router-link v-if="isLoggedIn" to="/book-search" class="action-btn">
            책 검색하러 가기
          </router-link>
          <router-link v-else to="/login" class="action-btn">
            로그인하고 시작하기
          </router-link>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useToast } from 'vue-toastification';
import apiClient, { API_BASE_URL } from '../api/client';
import axios from 'axios';

const router = useRouter();
const toast = useToast();

const isLoggedIn = ref(false);
const isLoading = ref(true);
const rankData = ref({});
const selectedGenre = ref(null);

const genres = computed(() => Object.keys(rankData.value));
const currentBooks = computed(() => rankData.value[selectedGenre.value] ?? []);

const fetchRank = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/api/review/rank/genres`, {
      withCredentials: true,
    });
    rankData.value = response.data;
    if (genres.value.length > 0) {
      selectedGenre.value = genres.value[0];
    }
  } catch {
    rankData.value = {};
  } finally {
    isLoading.value = false;
  }
};

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

onMounted(async () => {
  const loginAttempt = sessionStorage.getItem('login-attempt');
  await Promise.all([checkLoginStatus(), fetchRank()]);
  if (loginAttempt === 'true' && isLoggedIn.value) {
    sessionStorage.removeItem('login-attempt');
    toast.success('로그인되었습니다!');
  }
});
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background-color: #f9fafb;
}

.main-content {
  padding: 48px 24px;
}

.content-wrapper {
  max-width: 720px;
  margin: 0 auto;
}

.hero-section {
  margin-bottom: 32px;
}

.hero-title {
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 8px 0;
  line-height: 1.3;
}

.hero-subtitle {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
}

/* 로딩 */
.loading-container {
  display: flex;
  justify-content: center;
  padding: 80px 0;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e5e7eb;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* 장르 탭 */
.genre-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

.genre-tab {
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.genre-tab:hover { border-color: #2563eb; color: #2563eb; }
.genre-tab.active { background: #2563eb; border-color: #2563eb; color: white; }

/* 랭킹 리스트 */
.rank-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 16px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 14px 16px;
  transition: box-shadow 0.15s ease;
}

.rank-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.07);
}

.rank-number {
  font-size: 18px;
  font-weight: 700;
  color: #9ca3af;
  width: 28px;
  text-align: center;
  flex-shrink: 0;
}

.rank-number.top3 {
  color: #2563eb;
}

.book-cover {
  width: 52px;
  height: 72px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}

.book-cover-placeholder {
  width: 52px;
  height: 72px;
  background: #f3f4f6;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.book-info {
  flex: 1;
  min-width: 0;
}

.book-title {
  font-size: 15px;
  font-weight: 500;
  color: #1f2937;
  margin: 0 0 6px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.book-meta {
  display: flex;
  gap: 12px;
  align-items: center;
}

.rating {
  font-size: 14px;
  font-weight: 600;
  color: #f59e0b;
}

.review-count {
  font-size: 13px;
  color: #9ca3af;
}

/* 빈 상태 */
.empty-state {
  text-align: center;
  padding: 80px 24px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 20px;
}

.empty-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 12px 0;
}

.empty-desc {
  font-size: 15px;
  color: #6b7280;
  line-height: 1.7;
  margin: 0 0 32px 0;
}

.action-btn {
  display: inline-block;
  padding: 12px 24px;
  font-size: 15px;
  font-weight: 500;
  color: white;
  background: #2563eb;
  border-radius: 8px;
  text-decoration: none;
  transition: background-color 0.15s ease;
}

.action-btn:hover { background: #1d4ed8; }

@media (max-width: 640px) {
  .main-content { padding: 32px 16px; }
  .hero-title { font-size: 24px; }
  .genre-tabs { gap: 6px; }
  .genre-tab { padding: 6px 12px; font-size: 13px; }
}
</style>
