<template>
  <div class="app-layout">
    <AppHeader v-if="route.name !== 'login'" />
    <main class="app-main">
      <RouterView />
    </main>
    <AppFooter v-if="route.name !== 'login'" />
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router';
import AppHeader from './components/AppHeader.vue';
import AppFooter from './components/AppFooter.vue';

const route = useRoute();
</script>

<style>
.app-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.app-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f9fafb;
}
</style>
