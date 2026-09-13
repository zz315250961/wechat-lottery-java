<template>
  <LoginPage v-if="!authenticated" @logged-in="authenticated = true" />
  <LotteryAdminLayout v-else />
</template>
<script setup>
import { ref } from 'vue'
import { getToken } from '@/utils/auth'
import LoginPage from '@/views/login.vue'
import LotteryAdminLayout from './LotteryAdminLayout.vue'
const authenticated = ref(Boolean(getToken()))
</script>
