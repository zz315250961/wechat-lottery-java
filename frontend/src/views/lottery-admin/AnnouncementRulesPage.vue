<template>
  <section class="lottery-config-page">
    <LotteryPageHeader title="公告与活动规则" subtitle="公告显示在转盘上方；规则显示在“活动规则”弹窗中。次数和奖品数据仍由系统自动生成。">
      <template #actions><el-button type="primary" :loading="saving" @click="save">保存公告与规则</el-button></template>
    </LotteryPageHeader>
    <div class="notice-card" @input.capture="markDirty" @change.capture="markDirty">
      <div class="notice-switch"><div><h2>首页公告</h2><p>控制用户端转盘上方的公告提示条。</p></div><el-switch v-model="activity.noticeEnabled" active-text="显示公告" inactive-text="关闭公告" /></div>
      <label>公告内容（最多 500 字）<el-input v-model="activity.noticeText" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="例如：本周五 18:00 前可到指定门店核销" /></label>
    </div>
    <div class="rules-title"><div><h2>自定义规则</h2><p>最多添加 20 条，关闭的规则保留内容但不在用户端展示。</p></div><el-button :disabled="rules.length >= 20" @click="add">添加规则</el-button></div>
    <div class="rules-card" @input.capture="markDirty" @change.capture="markDirty">
      <div v-for="(rule, index) in rules" :key="rule._key || rule.id" class="rule-row">
        <el-switch v-model="rule.enabled" :aria-label="`显示规则 ${index + 1}`" /><el-input v-model="rule.content" type="textarea" :rows="2" maxlength="300" :aria-label="`规则内容 ${index + 1}`" placeholder="请输入活动规则" /><div class="rule-actions"><el-button link @click="move(index, -1)" :disabled="index === 0">上移</el-button><el-button link @click="move(index, 1)" :disabled="index === rules.length - 1">下移</el-button><el-button type="danger" link @click="remove(index)">删除</el-button></div>
      </div>
      <p v-if="!rules.length" class="empty">暂无自定义规则</p>
    </div>
    <p v-if="feedback" class="save-feedback" role="status">{{ feedback }}</p>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import LotteryPageHeader from '@/components/lottery/LotteryPageHeader.vue'
import { saveActivity, saveRules } from '@/api/lottery'
import { toRulePayload } from '@/composables/lotteryConfigurationPersistence'

const props = defineProps({ activity: { type: Object, required: true }, rules: { type: Array, required: true } })
const emit = defineEmits(['saved', 'dirty-change'])
const saving = ref(false)
const feedback = ref('')
const markDirty = () => emit('dirty-change', true)
function add() { if (props.rules.length < 20) { props.rules.push({ content: '新活动规则', enabled: true, _key: `rule-${Date.now()}-${Math.random()}` }); markDirty() } }
function move(index, offset) { const target = index + offset; if (target < 0 || target >= props.rules.length) return; const [rule] = props.rules.splice(index, 1); props.rules.splice(target, 0, rule); markDirty() }
function remove(index) { props.rules.splice(index, 1); markDirty() }
async function save() { saving.value = true; try { await Promise.all([saveActivity(props.activity), saveRules(toRulePayload(props.rules))]); feedback.value = '公告与规则已保存'; ElMessage.success(feedback.value); emit('saved') } finally { saving.value = false } }
</script>

<style scoped>
.lottery-config-page{display:grid;gap:var(--lottery-space-3)}.notice-card,.rules-card{display:grid;gap:18px;padding:var(--lottery-space-3);border:1px solid var(--lottery-border);border-radius:var(--lottery-radius-lg);background:var(--lottery-paper);box-shadow:var(--lottery-shadow)}.notice-switch,.rules-title{display:flex;align-items:center;justify-content:space-between;gap:20px}.notice-card h2,.notice-card p,.rules-title h2,.rules-title p{margin:0}.notice-card p,.rules-title p{margin-top:5px;color:var(--lottery-muted);font-size:13px}.notice-card label{display:grid;gap:8px;color:var(--lottery-ink);font-weight:650}.rule-row{display:grid;grid-template-columns:auto minmax(0,1fr) auto;align-items:center;gap:14px;padding-bottom:14px;border-bottom:1px solid var(--lottery-border)}.rule-actions{display:flex}.rule-actions :deep(.el-button){margin-left:0}.empty{text-align:center;color:var(--lottery-muted)}.save-feedback{margin:0;color:var(--lottery-orange);font-size:13px}@media(max-width:600px){.notice-card,.rules-card{padding:var(--lottery-space-2)}.notice-switch,.rules-title{align-items:flex-start;flex-direction:column}.rule-row{grid-template-columns:auto minmax(0,1fr)}.rule-actions{grid-column:1/-1;justify-content:flex-end}}
</style>
