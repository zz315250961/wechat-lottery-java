<template>
  <LotteryConfirmDialog ref="dialog" />
</template>

<script setup>
import { ref } from 'vue'
import LotteryConfirmDialog from '@/components/lottery/LotteryConfirmDialog.vue'

const dialog = ref()
const open = (options) => dialog.value.open(options)

defineExpose({ open })
</script>
