<template>
  <div class="app-container">
    <main class="main-content">
      <div class="content-wrapper">
        <!-- 시작 안내: 로그인 여부 확인 후 표시 (확인 전 비로그인 배너가 깜빡이지 않게) -->
        <section v-if="isAuthChecked" class="start-banner">
          <div class="start-banner-body">
            <div class="start-banner-icon">
              <img :src="isLoggedIn ? bannerSearchIcon : bannerBookIcon" alt="" />
            </div>
            <div>
              <p class="start-banner-title">{{ isLoggedIn ? '다음 독서가 기다리고 있어요' : '나만의 독서 기록을 시작해 보세요' }}</p>
              <p class="start-banner-desc">
                {{ isLoggedIn ? '읽고 싶은 책을 검색하고 새로운 독후감을 남겨보세요.' : '좋은 책을 발견하고 읽은 순간의 생각을 차곡차곡 기록해 보세요.' }}
              </p>
            </div>
          </div>
          <router-link v-if="isLoggedIn" to="/book-search" class="start-banner-btn">
            <img :src="bannerSearchWhiteIcon" alt="" />
            책 검색하러 가기
          </router-link>
          <router-link v-else to="/login" class="start-banner-btn">로그인하고 시작하기</router-link>
        </section>

        <div class="rank-header">
          <h2 class="rank-heading">장르별 도서 랭킹</h2>
          <p class="rank-subtitle">북토커들의 평점과 평가 수를 반영한 순위예요.</p>
        </div>

        <div class="rank-card">
          <!-- 장르 탭 -->
          <div v-if="genres.length > 0" class="genre-tabs" role="tablist">
            <button
              v-for="genre in genres"
              :key="genre"
              class="genre-tab"
              :class="{ active: selectedGenre === genre }"
              role="tab"
              :aria-selected="selectedGenre === genre"
              @click="selectedGenre = genre"
            >
              {{ genre }}
            </button>
          </div>

          <!-- 로딩 -->
          <div v-if="isLoading" class="rank-state">
            <div class="spinner"></div>
          </div>

          <!-- 랭킹 목록 -->
          <ul v-else-if="currentBooks.length > 0" class="rank-list">
            <li v-for="(book, index) in currentBooks" :key="book.isbn13">
              <button class="rank-item" :aria-label="`${book.title} 상세 보기`" @click="goToBookReviews(book)">
                <span class="rank-number" :class="{ top3: index < 3 }">{{ index + 1 }}</span>
                <span class="rank-cover">
                  <img v-if="book.cover && !brokenCovers.has(book.isbn13)" :src="book.cover" :alt="book.title" @error="markBroken(book.isbn13)" />
                  <span v-else class="rank-cover-fallback" :style="{ background: fallbackColor(index) }">{{ book.title }}</span>
                </span>
                <span class="rank-info">
                  <span class="rank-title">{{ book.title }}</span>
                  <span class="rank-meta">
                    <img :src="starIcon" alt="" class="rank-star" />
                    <span class="rank-rating">{{ book.avgRating.toFixed(2) }}</span>
                    <span class="rank-count">평가 {{ book.reviewCount.toLocaleString() }}개</span>
                  </span>
                </span>
                <span class="rank-arrow" aria-hidden="true">→</span>
              </button>
            </li>
          </ul>

          <!-- 랭킹 없음 -->
          <div v-else class="rank-state">
            <div class="empty-icon"><img :src="emptyBookIcon" alt="" /></div>
            <p class="empty-title">아직 집계된 랭킹이 없어요</p>
            <p class="empty-desc">
              평가가 쌓이면 {{ selectedGenre ? `${selectedGenre} 장르의` : '장르별' }} 도서 랭킹이 공개됩니다.
            </p>
          </div>
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
import starIcon from '../assets/icons/rank-star.svg';
import emptyBookIcon from '../assets/icons/rank-empty-book.svg';
import bannerBookIcon from '../assets/icons/home-banner-book.svg';
import bannerSearchIcon from '../assets/icons/home-banner-search.svg';
import bannerSearchWhiteIcon from '../assets/icons/home-banner-search-white.svg';

const router = useRouter();
const toast = useToast();

const isLoggedIn = ref(false);
const isAuthChecked = ref(false);
const isLoading = ref(true);
const rankData = ref({});
const selectedGenre = ref(null);

const genres = computed(() => Object.keys(rankData.value));
const currentBooks = computed(() => rankData.value[selectedGenre.value] ?? []);

// 표지가 없거나 깨진 책은 색 블록 + 제목으로 대신 표시
const brokenCovers = ref(new Set());
const markBroken = (isbn13) => { brokenCovers.value = new Set(brokenCovers.value).add(isbn13); };
const FALLBACK_COLORS = ['#33415c', '#c66357', '#4d705f', '#65549b', '#a77737'];
const fallbackColor = (index) => FALLBACK_COLORS[index % FALLBACK_COLORS.length];

const goToBookReviews = (book) => {
  router.push({
    name: 'book-reviews',
    params: { isbn13: book.isbn13 },
    query: { title: book.title, cover: book.cover },
  });
};

const fetchRank = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/rank/genres`, {
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
    const response = await axios.get(`${API_BASE_URL}/auth/session`, {
      withCredentials: true,
    });
    isLoggedIn.value = response.status === 200;
  } catch {
    isLoggedIn.value = false;
  } finally {
    isAuthChecked.value = true;
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
.app-container { min-height: 100%; }
.main-content { padding: 42px 24px 90px; }
.content-wrapper { max-width: 720px; margin: 0 auto; }

/* 시작 안내 배너 */
.start-banner { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 20px 22px; border: 1px solid #dbeafe; border-radius: 6px; background: linear-gradient(166.28deg, #f4f8ff 8.49%, #fbfdff 91.51%); }
.start-banner-body { display: flex; align-items: center; gap: 13px; min-width: 0; }
.start-banner-icon { flex-shrink: 0; display: grid; place-items: center; width: 38px; height: 38px; border-radius: 19px; background: white; filter: drop-shadow(0 4px 6px rgba(37, 99, 235, 0.09)); }
.start-banner-icon img { width: 17px; height: 17px; }
.start-banner-title { margin: 0; font-size: 13px; font-weight: 700; line-height: 19.5px; color: #111827; }
.start-banner-desc { margin: 0; padding-top: 5px; font-size: 10px; line-height: 15px; color: #6b7280; }
.start-banner-btn { flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; gap: 7px; min-width: 142px; min-height: 38px; padding: 9px 15px; box-sizing: border-box; border-radius: 4px; background: #2563eb; font-size: 13px; font-weight: 700; line-height: 19.5px; color: white; text-decoration: none; white-space: nowrap; }
.start-banner-btn:hover { background: #1d4ed8; }
.start-banner-btn img { width: 16px; height: 16px; }

/* 제목 */
.start-banner + .rank-header { margin-top: 34px; }
.rank-header { margin-bottom: 12px; }
.rank-heading { margin: 0; padding-top: 7px; font-size: 16px; font-weight: 400; line-height: 24px; color: #111827; }
.rank-subtitle { margin: 0; padding-top: 5px; font-size: 11px; line-height: 16.5px; color: #6b7280; }

/* 카드 */
.rank-card { background: white; border: 1px solid #e5e7eb; border-radius: 6px; overflow: hidden; }

/* 장르 탭 */
.genre-tabs { display: flex; gap: 23px; height: 47px; box-sizing: border-box; padding: 0 22px; background: #fbfdff; border-bottom: 1px solid #e5e7eb; overflow-x: auto; scrollbar-width: none; }
.genre-tabs::-webkit-scrollbar { display: none; }
.genre-tab { position: relative; flex-shrink: 0; padding: 15px 1px 13px; background: none; border: none; font-size: 12px; line-height: 18px; color: #6b7280; white-space: nowrap; cursor: pointer; }
.genre-tab:hover { color: #2563eb; }
.genre-tab.active { font-weight: 700; color: #2563eb; }
.genre-tab.active::after { content: ''; position: absolute; left: 0; right: 0; bottom: 0; height: 2px; background: #2563eb; }

/* 랭킹 목록 */
.rank-list { list-style: none; margin: 0; padding: 0 18px; }
.rank-list li + li .rank-item { border-top: 1px solid #f0f1f3; }
.rank-item { display: flex; align-items: center; gap: 12px; width: 100%; min-height: 84px; padding: 10px 4px; background: white; border: none; text-align: left; cursor: pointer; }
.rank-item:hover .rank-title,
.rank-item:hover .rank-arrow { color: #2563eb; }

.rank-number { flex-shrink: 0; width: 20px; text-align: center; font-weight: 700; font-size: 14px; line-height: 21px; color: #9ca3af; }
.rank-number.top3 { font-size: 17px; line-height: 25.5px; color: #2563eb; }

.rank-cover { flex-shrink: 0; width: 38px; height: 55px; overflow: hidden; box-shadow: -2px 2px 0 0 rgba(17, 24, 39, 0.12); }
.rank-cover img { display: block; width: 100%; height: 100%; object-fit: cover; }
.rank-cover-fallback { display: flex; align-items: center; justify-content: center; height: 100%; box-sizing: border-box; padding: 5px 4px; font-family: Georgia, serif; font-size: 7px; font-weight: 700; line-height: 8.75px; color: white; text-align: center; word-break: break-all; overflow: hidden; }

.rank-info { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.rank-title { font-size: 14px; font-weight: 700; line-height: 21px; color: #111827; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rank-meta { display: flex; align-items: center; gap: 4px; padding-top: 7px; }
.rank-star { width: 11px; height: 11px; }
.rank-rating { font-size: 12px; font-weight: 700; line-height: 18px; color: #d97706; }
.rank-count { padding-left: 4px; font-size: 10px; line-height: 15px; color: #9ca3af; }

.rank-arrow { flex-shrink: 0; display: grid; place-items: center; width: 28px; height: 28px; border-radius: 14px; font-size: 13px; color: #9ca3af; }

/* 로딩 · 빈 상태 */
.rank-state { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 360px; padding: 40px 20px; box-sizing: border-box; }
.spinner { width: 32px; height: 32px; border: 3px solid #e5e7eb; border-top-color: #2563eb; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.empty-icon { display: grid; place-items: center; width: 46px; height: 46px; border-radius: 23px; background: #f3f4f6; }
.empty-icon img { width: 20px; height: 20px; }
.empty-title { margin: 0; padding-top: 15px; font-size: 13px; font-weight: 700; line-height: 19.5px; color: #111827; text-align: center; }
.empty-desc { margin: 0; padding-top: 7px; font-size: 10px; line-height: 15px; color: #6b7280; text-align: center; }

@media (max-width: 640px) {
  .main-content { padding: 32px 16px 64px; }
  .start-banner { flex-direction: column; align-items: stretch; padding: 18px 16px; }
  .start-banner-btn { width: 100%; }
  .genre-tabs { gap: 18px; padding: 0 16px; }
  .rank-list { padding: 0 12px; }
}
</style>
