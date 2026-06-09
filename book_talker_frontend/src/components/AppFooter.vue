<template>
  <footer class="app-footer">
    <div class="footer-content">
      <span class="footer-brand">BookTalker</span>
      <a href="mailto:fuswns96@naver.com" class="footer-email">
        문의 · fuswns96@naver.com
      </a>
    </div>
    <div class="footer-copy">
      © 2025 BookTalker. All rights reserved.
    </div>
  </footer>
</template>

<style scoped>
.app-footer {
  background: white;
  border-top: 1px solid #e5e7eb;
  padding: 24px 0 20px;
}

.footer-content {
  max-width: 720px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.footer-brand {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.footer-email {
  font-size: 13px;
  color: #6b7280;
  text-decoration: none;
  transition: color 0.15s ease;
}

.footer-email:hover {
  color: #2563eb;
}

.footer-copy {
  max-width: 720px;
  margin: 0 auto;
  padding: 0 24px;
  font-size: 12px;
  color: #9ca3af;
}

@media (max-width: 640px) {
  .footer-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }
}
</style>
