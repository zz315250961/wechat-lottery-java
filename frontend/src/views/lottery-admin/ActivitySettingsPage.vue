<template>
  <section class="lottery-config-page">
    <LotteryPageHeader title="活动设置" subtitle="设置活动周期和用户获取抽奖次数的方式">
      <template #actions><el-button type="primary" :loading="saving" @click="save">保存活动设置</el-button></template>
    </LotteryPageHeader>
    <div class="settings-card form-grid" @input.capture="markDirty" @change.capture="markDirty">
      <label class="wide">活动标题<el-input v-model="activity.title" placeholder="活动标题" /></label>
      <label class="wide">活动说明<el-input v-model="activity.description" type="textarea" :rows="3" placeholder="活动说明" /></label>
      <label>活动开始时间<el-date-picker v-model="activity.startAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="不填则立即开始" /></label>
      <label>活动结束时间<el-date-picker v-model="activity.endAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="不填则长期有效" /></label>
      <label>每天免费次数<el-input-number v-model="activity.dailyFree" :min="0" /></label>
      <label>签到奖励次数<el-input-number v-model="activity.checkinReward" :min="0" /></label>
      <label>每次助力奖励<el-input-number v-model="activity.assistReward" :min="0" /></label>
      <label>每日助力上限<el-input-number v-model="activity.assistDailyLimit" :min="0" /></label>
      <label class="wide">未中奖提示语<el-input v-model="activity.thanksLabel" placeholder="未中奖提示语" /></label>
      <div class="switch-row"><span>活动开关</span><el-switch v-model="activity.enabled" active-text="开启活动" inactive-text="关闭活动" /></div>
    </div>
    <p v-if="feedback" class="save-feedback" role="status">{{ feedback }}</p>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import LotteryPageHeader from '@/components/lottery/LotteryPageHeader.vue'
import { saveActivity } from '@/api/lottery'

const props = defineProps({ activity: { type: Object, required: true } })
const emit = defineEmits(['saved', 'dirty-change'])
const saving = ref(false)
const feedback = ref('')
const markDirty = () => emit('dirty-change', true)

async function save() {
  saving.value = true
  try {
    await saveActivity(props.activity)
    feedback.value = '活动设置已保存'
    ElMessage.success(feedback.value)
    emit('saved')
  } finally { saving.value = false }
}
</script>

<style scoped>
.lottery-config-page{display:grid;gap:var(--lottery-space-3)}.settings-card{padding:var(--lottery-space-3);border:1px solid var(--lottery-border);border-radius:var(--lottery-radius-lg);background:var(--lottery-paper);box-shadow:var(--lottery-shadow)}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--lottery-space-2)}label{display:grid;gap:7px;color:var(--lottery-muted);font-size:13px}.wide{grid-column:1/-1}.form-grid :deep(.el-input-number),.form-grid :deep(.el-date-editor){width:100%}.switch-row{display:flex;grid-column:1/-1;align-items:center;justify-content:space-between;padding:14px 0;border-top:1px solid var(--lottery-border);color:var(--lottery-ink)}.save-feedback{margin:0;color:var(--lottery-orange);font-size:13px}@media(max-width:600px){.form-grid{grid-template-columns:1fr}.settings-card{padding:var(--lottery-space-2)}.wide{grid-column:auto}}
</style>
