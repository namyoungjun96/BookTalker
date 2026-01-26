import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // 환경 변수 로드
  const env = loadEnv(mode, process.cwd(), '')
  const apiBaseUrl = env.VITE_API_BASE_URL || 'http://localhost:8010'
  const appUrl = env.VITE_APP_URL || 'http://localhost:5173'

  return {
    plugins: [
      vue(),
      VitePWA({
        registerType: 'autoUpdate',
        includeAssets: ['favicon.ico', 'apple-touch-icon.png', 'mask-icon.svg'],
        devOptions: {
          enabled: true,  // 개발 모드에서도 PWA 활성화
          type: 'module'
        },
        manifest: {
          name: 'BookTalker',
          short_name: 'BookTalker',
          description: '나만의 독후감 작성 어플리케이션',
          start_url: appUrl,  // 프론트엔드 접속 주소
          scope: '/',
          theme_color: '#3B82F6',
          background_color: '#ffffff',
          display: 'standalone',
          icons: [
            {
              src: 'pwa-192x192.png',
              sizes: '192x192',
              type: 'image/png'
            },
            {
              src: 'pwa-512x512.png',
              sizes: '512x512',
              type: 'image/png'
            },
            {
              src: 'pwa-512x512.png',
              sizes: '512x512',
              type: 'image/png',
              purpose: 'any maskable'
            }
          ]
        },
        workbox: {
          // Service Worker 캐싱 전략
          runtimeCaching: [
            {
              urlPattern: /^https:\/\/cover\.aladin\.co\.kr\/.*/i,
              handler: 'CacheFirst',
              options: {
                cacheName: 'aladin-images-cache',
                expiration: {
                  maxEntries: 50,
                  maxAgeSeconds: 60 * 60 * 24 * 30 // 30일
                },
                cacheableResponse: {
                  statuses: [0, 200]
                }
              }
            }
          ]
        }
      })
    ],
  }
})
