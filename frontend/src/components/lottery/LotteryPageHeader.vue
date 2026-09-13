<template>
  <header class="lottery-page-header">
    <div>
      <h1>{{ title }}</h1>
      <p v-if="subtitle">{{ subtitle }}</p>
    </div>
    <div v-if="$slots.actions" class="lottery-page-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' }
})
</script>

<style scoped>
.lottery-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--lottery-space-2);
  padding-bottom: var(--lottery-space-3);
  border-bottom: 1px solid var(--lottery-border);
  color: var(--lottery-ink);
}

h1, p { margin: 0; }
h1 { font-size: clamp(24px, 3vw, 32px); letter-spacing: -.03em; }
p { margin-top: var(--lottery-space-1); color: var(--lottery-muted); line-height: 1.6; }

.lottery-page-header__actions {
  display: flex;
  min-height: 44px;
  flex: none;
  align-items: center;
  gap: var(--lottery-space-1);
}

@media (max-width: 600px) {
  .lottery-page-header { align-items: stretch; flex-direction: column; }
  .lottery-page-header__actions { width: 100%; }
  .lottery-page-header__actions :deep(button), .lottery-page-header__actions :deep(.el-button) {
    min-width: 44px;
    min-height: 44px;
  }
}
</style>
