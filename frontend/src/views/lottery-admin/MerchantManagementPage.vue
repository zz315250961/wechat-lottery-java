<template>
  <section class="lottery-data-page">
    <LotteryConfirm ref="confirmBox" />
    <LotteryPageHeader title="商户管理" subtitle="维护独立商户账号、门店位置和奖券使用规则"><template #actions><el-button type="primary" @click="addMerchant">新增商户</el-button></template></LotteryPageHeader>
    <p class="page-hint">门店地址和使用规则会随中奖写入奖券，之后的修改不会影响已经发出的券。</p>
    <div class="merchant-list">
      <form v-for="merchant in drafts" :key="merchant.key" class="merchant-card" @submit.prevent="save(merchant)">
        <div class="merchant-card__head"><div><h2>{{ merchant.name || '新商户' }}</h2><p>登录账号：{{ merchant.username || '请填写账号' }}</p></div><el-switch v-model="merchant.enabled" :disabled="savingKeys.has(merchant.key)" active-text="启用商户" @change="confirmDisable(merchant, $event)" /></div>
        <label>商户名称<el-input v-model="merchant.name" :disabled="savingKeys.has(merchant.key)" required maxlength="100" placeholder="例如：校园咖啡" /></label>
        <label>登录账号<el-input v-model="merchant.username" :disabled="savingKeys.has(merchant.key)" required maxlength="64" autocomplete="off" placeholder="仅用于商户核销端登录" /></label>
        <label>登录密码<el-input v-model="merchant.password" :disabled="savingKeys.has(merchant.key)" type="password" show-password :required="!merchant.id" autocomplete="new-password" :placeholder="merchant.id ? '留空则不修改密码' : '至少 8 位初始密码'" /></label>
        <label>门店地址<el-input v-model="merchant.address" :disabled="savingKeys.has(merchant.key)" maxlength="500" placeholder="填写用户到店核销的详细位置" /></label>
        <label>奖券使用规则<el-input v-model="merchant.usageRules" :disabled="savingKeys.has(merchant.key)" type="textarea" :rows="4" maxlength="2000" show-word-limit placeholder="例如：仅限到店使用，不可兑换现金" /></label>
        <p v-if="merchant._error" class="merchant-card__error" role="alert">{{ merchant._error }}</p>
        <div class="merchant-card__actions"><el-button native-type="submit" type="primary" :loading="savingKeys.has(merchant.key)">{{ merchant.id ? '保存门店资料' : '创建商户账号' }}</el-button></div>
      </form>
      <div v-if="!drafts.length" class="empty">暂时没有商户，先创建一个商户核销账号。</div>
    </div>
  </section>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import LotteryConfirm from '@/components/LotteryConfirm.vue'
import LotteryPageHeader from '@/components/lottery/LotteryPageHeader.vue'
import { saveMerchant } from '@/api/lottery'

const props = defineProps({ merchants: { type: Array, default: () => [] } })
const emit = defineEmits(['saved', 'dirty-change'])
const confirmBox = ref()
const drafts = ref([])
const savingKeys = ref(new Set())
let baselines = new Map()
const business = ({ key, _error, ...merchant }) => merchant
const serialise = merchant => JSON.stringify(business(merchant))
const isDirty = merchant => baselines.get(merchant.key) !== serialise(merchant)
const reportDirty = () => emit('dirty-change', drafts.value.some(isDirty))
const toDraft = (merchant = {}) => ({ id: merchant.id == null ? undefined : String(merchant.id), name: merchant.name || '', username: merchant.username || '', password: '', address: merchant.address || '', usageRules: merchant.usageRules ?? merchant.usage_rules ?? '', enabled: merchant.enabled === true || merchant.enabled === 1, _error: '', key: merchant.id == null ? `merchant-${Date.now()}-${Math.random()}` : String(merchant.id) })
watch(() => props.merchants, (value) => { const existing = new Map(drafts.value.map(item => [item.id, item])); const nextBaselines = new Map(); drafts.value = (value || []).map((merchant) => { const local = existing.get(merchant.id); if (local && isDirty(local)) { nextBaselines.set(local.key, baselines.get(local.key)); return local } const draft = toDraft(merchant); nextBaselines.set(draft.key, serialise(draft)); return draft }); baselines = nextBaselines; reportDirty() }, { immediate: true, deep: true })
watch(drafts, reportDirty, { deep: true })
function addMerchant() { drafts.value.unshift(toDraft({ enabled: true })); ElMessage.success('请填写新商户资料后保存') }
async function confirmDisable(merchant, enabled) { if (enabled || !merchant.id) return; const accepted = await confirmBox.value.open({ title: '停用商户', message: `停用“${merchant.name}”后，该账号不能登录商户核销端。`, confirmText: '确认停用', danger: true }); if (!accepted) merchant.enabled = true }
async function save(merchant) {
  const previousKey = merchant.key
  if (savingKeys.value.has(previousKey)) return
  merchant._error = ''
  const submitted = { ...business(merchant) }
  savingKeys.value = new Set([...savingKeys.value, previousKey])
  try {
    const response = await saveMerchant(submitted)
    const id = response.data?.id ?? submitted.id
    if (!id) throw new Error('商户保存结果缺少账号标识，请刷新后核对')
    merchant.id = id
    merchant.key = id
    if (merchant.password === submitted.password) merchant.password = ''
    baselines.delete(previousKey)
    // Only the submitted values become clean, even if code changes a draft during await.
    baselines.set(id, serialise({ ...submitted, id, password: '' }))
    reportDirty()
    ElMessage.success(submitted.id ? '门店资料已保存' : '商户账号已创建')
    emit('saved')
  } catch (error) {
    merchant._error = error?.message || '商户资料保存失败，请刷新后重试'
    ElMessage.error?.(merchant._error)
  } finally {
    const next = new Set(savingKeys.value)
    next.delete(previousKey)
    savingKeys.value = next
  }
}
</script>

<style scoped>
.page-hint{margin:0;padding:14px 16px;border-radius:var(--lottery-radius-md);background:var(--lottery-orange-soft);color:var(--lottery-muted);line-height:1.7}.merchant-list{display:grid;grid-template-columns:repeat(auto-fit,minmax(330px,1fr));gap:var(--lottery-space-2)}.merchant-card{display:grid;gap:14px;padding:20px;border:1px solid var(--lottery-border);border-radius:var(--lottery-radius-lg);background:var(--lottery-paper);box-shadow:var(--lottery-shadow)}.merchant-card__head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px}.merchant-card h2{margin:0;color:var(--lottery-ink);font-size:18px}.merchant-card p{margin:5px 0 0;color:var(--lottery-muted);font-size:13px}.merchant-card label{display:grid;gap:7px;color:var(--lottery-ink);font-size:14px;font-weight:650}.merchant-card .merchant-card__error{margin:0;padding:10px 12px;border-radius:8px;background:#fff1f0;color:#d4380d;font-size:14px;line-height:1.5}.merchant-card__actions{display:flex;justify-content:flex-end}.merchant-card__actions :deep(.el-button){min-height:44px}@media(max-width:600px){.merchant-list{grid-template-columns:1fr}.merchant-card{padding:16px}.merchant-card__head{align-items:center}.merchant-card__head :deep(.el-switch){min-height:44px}}
</style>
