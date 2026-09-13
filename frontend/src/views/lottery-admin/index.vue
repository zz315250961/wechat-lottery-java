<template>
  <div class="admin-shell">
    <LotteryConfirm ref="confirmBox" />
    <main>
      <section v-if="loadState === 'loading'" class="load-state" aria-live="polite">正在加载真实活动配置…</section>
      <section v-else-if="loadState === 'error'" class="load-state load-error" role="alert"><p>{{ loadError }}</p><el-button type="primary" @click="load">重新加载</el-button></section>
      <component v-else-if="currentPage" :is="currentPage" :activity="configuration.activity" :prizes="configuration.prizes" :merchants="data.merchants" :rules="configuration.rules" :users="data.users" :tickets="data.tickets" :directives="data.directives" :has-unsaved-changes="hasUnsavedChanges" @saved="handleSaved" @refresh="loadTab" @dirty-change="pageDirty = $event" />
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute } from 'vue-router'
import LotteryConfirm from '@/components/LotteryConfirm.vue'
import PrizePoolPage from './PrizePoolPage.vue'
import ActivitySettingsPage from './ActivitySettingsPage.vue'
import AppearancePage from './AppearancePage.vue'
import AnnouncementRulesPage from './AnnouncementRulesPage.vue'
import MerchantManagementPage from './MerchantManagementPage.vue'
import ParticipantUsersPage from './ParticipantUsersPage.vue'
import TicketRecordsPage from './TicketRecordsPage.vue'
import DirectedWinsPage from './DirectedWinsPage.vue'
import { adminOverview, adminTickets, adminUsers, getDirectives } from '@/api/lottery'

const route = useRoute()
const tab = ref(route.meta.lotteryTab || 'prizes')
const configuration = reactive({ activity: {}, prizes: [], rules: [] })
const data = reactive({ merchants: [], users: [], tickets: [], directives: [] })
const confirmBox = ref()
const pageDirty = ref(false)
const loadState = ref('loading')
const loadError = ref('')
let loading = false
const pageByTab = { prizes: PrizePoolPage, activity: ActivitySettingsPage, appearance: AppearancePage, rules: AnnouncementRulesPage, merchants: MerchantManagementPage, users: ParticipantUsersPage, tickets: TicketRecordsPage, directives: DirectedWinsPage }
const currentPage = computed(() => pageByTab[tab.value] || PrizePoolPage)
const hasUnsavedChanges = computed(() => loadState.value === 'ready' && pageDirty.value)
function normalizePrize(prize) { const enabled = prize.enabled === true || prize.enabled === 1; return { ...prize, merchantId: prize.merchantId == null && prize.merchant_id == null ? null : String(prize.merchantId ?? prize.merchant_id), merchantName: prize.merchantName ?? prize.merchant_name ?? '', imageUrl: prize.imageUrl ?? prize.image_url ?? '', perUserLimit: prize.perUserLimit ?? prize.per_user_limit ?? 1, validDays: 30, redeemEndAt: prize.redeemEndAt ?? prize.redeem_end_at ?? null, wonCount: prize.wonCount ?? prize.won_count ?? 0, redeemedCount: prize.redeemedCount ?? prize.redeemed_count ?? 0, enabled, _originalEnabled: enabled, _originalProbability: Number(prize.probability || 0), _key: prize.id || `draft-${Date.now()}-${Math.random()}` } }
async function load() { loading = true; loadState.value = 'loading'; loadError.value = ''; try { const response = await adminOverview(); Object.assign(data, { merchants: [], users: [], tickets: [], directives: [], ...response.data }); configuration.activity = response.data.activity || {}; configuration.prizes = (response.data.prizes || []).map(normalizePrize); configuration.rules = (response.data.rules || []).map(rule => ({ ...rule, enabled: rule.enabled === true || rule.enabled === 1, _key: rule.id || `rule-${Math.random()}` })); await loadTab(); await nextTick(); pageDirty.value = false; loadState.value = 'ready' } catch (error) { Object.assign(data, { merchants: [], users: [], tickets: [], directives: [] }); Object.assign(configuration, { activity: {}, prizes: [], rules: [] }); pageDirty.value = false; loadError.value = error?.message || '活动配置加载失败，请重试'; loadState.value = 'error' } finally { loading = false } }
async function loadTab() { if (tab.value === 'users') data.users = (await adminUsers('')).data || []; if (tab.value === 'tickets') data.tickets = (await adminTickets('')).data || []; if (tab.value === 'directives') { const [users, directives] = await Promise.all([adminUsers(''), getDirectives()]); data.users = users.data || []; data.directives = directives.data || [] } }
function handleSaved() { if (tab.value !== 'merchants') load() }
const warnUnload = (event) => { if (!loading && hasUnsavedChanges.value) { event.preventDefault(); event.returnValue = '' } }
window.addEventListener('beforeunload', warnUnload)
onBeforeUnmount(() => window.removeEventListener('beforeunload', warnUnload))
onBeforeRouteLeave(async () => { if (loading || !hasUnsavedChanges.value) return true; const accepted = await confirmBox.value.open({ title: '尚未保存', message: '当前设置尚未保存，确定离开此页面吗？', confirmText: '离开' }); if (accepted) pageDirty.value = false; return accepted })
onMounted(load)
watch(() => route.meta.lotteryTab, async (value) => { if (!value) return; tab.value = value; if (loadState.value === 'ready') await loadTab() })
</script>

<style scoped>
.admin-shell{min-height:100vh;background:var(--lottery-paper);color:var(--lottery-ink)}main{max-width:1540px;margin:auto;padding:var(--lottery-space-4) var(--lottery-space-5) 60px}.load-state{display:grid;gap:var(--lottery-space-2);padding:var(--lottery-space-3);border:1px solid var(--lottery-border);border-radius:var(--lottery-radius-lg);background:var(--lottery-paper);box-shadow:var(--lottery-shadow)}.load-error{color:var(--lottery-ink)}.load-error p{margin:0}.admin-shell :deep(.el-button--primary){--el-button-bg-color:var(--lottery-orange);--el-button-border-color:var(--lottery-orange)}@media(max-width:760px){main{padding:var(--lottery-space-3) 14px 40px}.load-state{padding:var(--lottery-space-2)}}
</style>
<style>
.lottery-data-page{display:grid;gap:var(--lottery-space-2)}.lottery-data-page .summary{display:flex;flex-wrap:wrap;gap:var(--lottery-space-4);padding:18px var(--lottery-space-3);border-radius:var(--lottery-radius-md);background:var(--lottery-orange-soft);color:var(--lottery-muted)}.lottery-data-page .summary strong{margin-left:5px;color:var(--lottery-orange);font-size:21px;font-variant-numeric:tabular-nums}.lottery-data-page .toolbar :is(.el-input,.el-select){max-width:520px}.lottery-data-page .data-table{overflow-x:auto;border:1px solid var(--lottery-border);border-radius:var(--lottery-radius-lg);background:var(--lottery-paper)}.lottery-data-page .empty{padding:28px;text-align:center;color:var(--lottery-muted)}@media(max-width:600px){.lottery-data-page .summary{gap:12px;justify-content:space-between;padding:14px}.lottery-data-page .toolbar .el-input__wrapper,.lottery-data-page .el-select__wrapper,.lottery-data-page .el-input-number,.lottery-data-page .el-button{min-height:44px}.lottery-data-page .el-input-number{width:100%}}
</style>
