<template>
  <teleport to="body">
    <div v-if="visible" class="lottery-confirm-mask" @click.self="cancel">
      <section role="dialog" aria-modal="true" :aria-label="title">
        <span class="dialog-icon" :class="{ danger }">!</span>
        <h3>{{ title }}</h3>
        <p>{{ message }}</p>
        <footer>
          <button class="cancel" type="button" @click="cancel">取消</button>
          <button class="confirm" :class="{ danger }" type="button" @click="accept">{{ confirmText }}</button>
        </footer>
      </section>
    </div>
  </teleport>
</template>

<script setup>
import { ref } from 'vue'

const visible = ref(false)
const title = ref('请确认')
const message = ref('')
const confirmText = ref('确定')
const danger = ref(false)
let resolvePending

function open({ title: nextTitle = '请确认', message: nextMessage = '', confirmText: nextConfirmText = '确定', danger: nextDanger = false } = {}) {
  title.value = nextTitle
  message.value = nextMessage
  confirmText.value = nextConfirmText
  danger.value = nextDanger
  visible.value = true
  return new Promise((resolve) => { resolvePending = resolve })
}

function finish(accepted) {
  visible.value = false
  resolvePending?.(accepted)
  resolvePending = undefined
}

function accept() { finish(true) }
function cancel() { finish(false) }

defineExpose({ open })
</script>

<style scoped>
.lottery-confirm-mask { position: fixed; z-index: 10000; inset: 0; display: grid; place-items: center; padding: var(--lottery-space-3); background: rgb(45 33 27 / 40%); backdrop-filter: blur(5px); }
section { width: min(100%, 390px); padding: 26px; border: 1px solid var(--lottery-border); border-radius: var(--lottery-radius-xl); background: var(--lottery-paper); color: var(--lottery-ink); box-shadow: var(--lottery-shadow); text-align: center; }
.dialog-icon { display: grid; width: 46px; height: 46px; margin: auto; border-radius: 50%; background: var(--lottery-orange-soft); color: var(--lottery-orange); font-size: 24px; font-weight: 800; place-items: center; }
.dialog-icon.danger { background: #fff0ec; color: #cf3e2f; }
h3 { margin: 14px 0 8px; font-size: 21px; }
p { margin: 0; color: var(--lottery-muted); line-height: 1.7; }
footer { display: flex; gap: 10px; margin-top: 22px; }
button { min-height: 44px; flex: 1; border-radius: 99px; font-weight: 700; cursor: pointer; }
.cancel { border: 1px solid var(--lottery-border); background: var(--lottery-paper); color: var(--lottery-muted); }
.confirm { border: 0; background: var(--lottery-orange); color: #fff; }
.confirm.danger { background: #cf3e2f; }
</style>
