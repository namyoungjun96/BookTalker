<template>
  <AppHeader v-if="route.name !== 'login'" />
  <RouterView />
</template>

<script setup>
import { useRoute } from 'vue-router';
import AppHeader from './components/AppHeader.vue';

const route = useRoute();
</script>
