<template>
  <section class="lottery-data-page">
    <LotteryConfirm ref="confirmBox" />
    <LotteryPageHeader title="定向中奖" subtitle="为已参与活动的用户配置指定奖品"><template #actions><el-button @click="$emit('refresh')">刷新数据</el-button></template></LotteryPageHeader>
    <form class="directive-form" @submit.prevent="create">
      <p>用户需先登录活动才会出现在候选列表。定向中奖仍遵守库存、个人上限与抽奖次数。</p>
      <div class="selector-row"><label>用户 / OpenID<el-select v-model="form.userId" filterable clearable :filter-method="filterUsers" placeholder="输入昵称、手机号或 OpenID 搜索"><el-option v-for="user in filteredUsers" :key="user.id" :label="userLabel(user)" :value="user.id" /></el-select></label><label>奖品<el-select v-model="form.prizeId" filterable clearable :filter-method="filterPrizes" placeholder="输入奖品名称搜索"><el-option v-for="prize in filteredPrizes" :key="prize.id" :label="prize.name" :value="prize.id" /></el-select></label></div>
      <div class="directive-form__footer"><label>指定中奖次数<el-input-number v-model="form.count" :min="1" :max="1000" controls-position="right" /></label><el-button native-type="submit" type="primary" :loading="saving">添加定向配置</el-button></div>
    </form>
    <div class="directive-summary"><span>配置记录 <b>{{ directives.length }}</b></span><span>进行中 <b>{{ activeCount }}</b></span><span>已完成 <b>{{ completedCount }}</b></span></div>
    <div class="directive-filters"><el-radio-group v-model="statusFilter"><el-radio-button value="ALL">全部</el-radio-button><el-radio-button value="ACTIVE">进行中</el-radio-button><el-radio-button value="COMPLETED">已完成</el-radio-button></el-radio-group></div>
    <div class="data-table" role="region" aria-label="定向中奖配置记录"><table><thead><tr><th>状态</th><th>用户</th><th>手机号</th><th>奖品</th><th>配置次数</th><th>已定向中奖</th><th>剩余次数</th><th>配置时间</th><th>最近中奖时间</th><th>操作</th></tr></thead><tbody><tr v-for="item in visibleDirectives" :key="item.id"><td><el-tag :type="itemStatus(item) === 'COMPLETED' ? 'success' : 'warning'">{{ itemStatus(item) === 'COMPLETED' ? '已完成' : '进行中' }}</el-tag></td><td>{{ item.nickname || userName(item.user_id) }}</td><td>{{ item.phone || '—' }}</td><td>{{ item.prize_name || prizeName(item.prize_id) }}</td><td>{{ item.initial_count ?? '历史数据' }}</td><td>{{ item.directed_win_count ?? 0 }}</td><td>{{ item.remaining_count }}</td><td>{{ formatTime(item.create_time) }}</td><td>{{ formatTime(item.last_win_time) }}</td><td><el-button v-if="canDelete(item)" link type="danger" @click="remove(item)">删除</el-button><span v-else class="muted">保留记录</span></td></tr></tbody></table><div v-if="!visibleDirectives.length" class="empty">当前筛选下暂无记录</div></div>
    <div class="mobile-cards"><article v-for="item in visibleDirectives" :key="item.id" class="data-card"><div class="card-title"><strong>{{ item.prize_name || prizeName(item.prize_id) }}</strong><el-tag :type="itemStatus(item) === 'COMPLETED' ? 'success' : 'warning'">{{ itemStatus(item) === 'COMPLETED' ? '已完成' : '进行中' }}</el-tag></div><p>{{ item.nickname || userName(item.user_id) }} · {{ item.phone || '未绑定手机号' }}</p><div><span>配置 / 已中奖 / 剩余</span><b>{{ item.initial_count ?? '历史' }} / {{ item.directed_win_count ?? 0 }} / {{ item.remaining_count }}</b></div><p>配置时间：{{ formatTime(item.create_time) }}</p><p>最近中奖：{{ formatTime(item.last_win_time) }}</p><el-button v-if="canDelete(item)" type="danger" plain @click="remove(item)">删除配置</el-button></article></div>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import LotteryConfirm from '@/components/LotteryConfirm.vue'
import LotteryPageHeader from '@/components/lottery/LotteryPageHeader.vue'
import { addDirective, deleteDirective } from '@/api/lottery'
const props = defineProps({ users: { type: Array, default: () => [] }, prizes: { type: Array, default: () => [] }, directives: { type: Array, default: () => [] } })
const emit = defineEmits(['refresh'])
const form = reactive({ userId: null, prizeId: null, count: 1 })
const saving = ref(false)
const confirmBox = ref()
const userQuery = ref('')
const prizeQuery = ref('')
const statusFilter = ref('ALL')
const enabledPrizes = computed(() => props.prizes.filter(prize => prize.enabled === true || prize.enabled === 1))
const activeCount = computed(() => props.directives.filter(item => itemStatus(item) === 'ACTIVE').length)
const completedCount = computed(() => props.directives.filter(item => itemStatus(item) === 'COMPLETED').length)
const visibleDirectives = computed(() => statusFilter.value === 'ALL' ? props.directives : props.directives.filter(item => itemStatus(item) === statusFilter.value))
const filteredUsers = computed(() => { const query = userQuery.value.trim().toLowerCase(); return query ? props.users.filter(user => `${user.nickname || ''} ${user.phone || ''} ${user.openid || ''}`.toLowerCase().includes(query)) : props.users })
const filteredPrizes = computed(() => { const query = prizeQuery.value.trim().toLowerCase(); return query ? enabledPrizes.value.filter(prize => String(prize.name || '').toLowerCase().includes(query)) : enabledPrizes.value })
function userLabel(user) { return `${user.nickname || '微信用户'} · ${user.phone || user.openid || user.id}` }
function userName(id) { return props.users.find(user => user.id === id)?.nickname || id }
function prizeName(id) { return props.prizes.find(prize => prize.id === id)?.name || '已移除奖品' }
function filterUsers(query) { userQuery.value = query }
function filterPrizes(query) { prizeQuery.value = query }
function itemStatus(item) { return item.status || (Number(item.remaining_count) === 0 ? 'COMPLETED' : 'ACTIVE') }
function canDelete(item) { return itemStatus(item) === 'ACTIVE' && Number(item.directed_win_count || 0) === 0 }
function formatTime(value) { if (!value) return '—'; return String(value).replace('T', ' ').slice(0, 19) }
async function create() { if (!form.userId || !form.prizeId) { ElMessage.warning('请从搜索结果中选择用户和奖品'); return } const accepted = await confirmBox.value.open({ title: '添加定向配置', message: '确认将该奖品的指定中奖次数发放给所选用户？', confirmText: '确认添加' }); if (!accepted) return; saving.value = true; try { await addDirective({ userId: String(form.userId), prizeId: String(form.prizeId), count: form.count }); form.count = 1; ElMessage.success('定向配置已添加'); emit('refresh') } catch(error) { ElMessage.error(error?.message || '添加定向配置失败，请刷新后重试') } finally { saving.value = false } }
async function remove(item) { const accepted = await confirmBox.value.open({ title: '删除定向配置', message: '删除后，该用户尚未使用的定向中奖次数将失效。', confirmText: '确认删除', danger: true }); if (!accepted) return; await deleteDirective(item.id); ElMessage.success('定向配置已删除'); emit('refresh') }
</script>

<style scoped>
.directive-form{display:grid;gap:16px;padding:20px;border:1px solid var(--lottery-border);border-radius:var(--lottery-radius-lg);background:var(--lottery-paper);box-shadow:var(--lottery-shadow)}.directive-form p{margin:0;color:var(--lottery-muted);line-height:1.7}.selector-row{display:grid;grid-template-columns:1fr 1fr;gap:16px}.selector-row label,.directive-form__footer label{display:grid;gap:7px;color:var(--lottery-ink);font-size:14px;font-weight:650}.directive-form__footer{display:flex;align-items:end;gap:16px}.directive-form__footer label{width:180px}.directive-summary{display:flex;gap:28px;padding:16px 20px;border-radius:var(--lottery-radius-lg);background:var(--lottery-soft-orange);color:var(--lottery-muted)}.directive-summary b{margin-left:5px;color:var(--lottery-orange);font-size:20px}.directive-filters{display:flex;justify-content:flex-start}table{width:100%;min-width:1120px;border-collapse:collapse;text-align:left}th,td{padding:15px 16px;border-bottom:1px solid var(--lottery-border);vertical-align:middle;white-space:nowrap}th{background:#faf8f5;color:var(--lottery-muted);font-size:13px;font-weight:700}tbody tr:last-child td{border-bottom:0}.muted{color:var(--lottery-muted);font-size:13px}.mobile-cards{display:none}@media(max-width:600px){.directive-form{padding:16px}.selector-row{grid-template-columns:1fr}.directive-form__footer{align-items:stretch;flex-direction:column}.directive-form__footer label{width:100%}.directive-summary{gap:14px;justify-content:space-between;padding:14px}.directive-summary span{display:grid;gap:3px;font-size:12px}.directive-summary b{margin:0}.data-table{display:none}.mobile-cards{display:grid;gap:12px}.data-card{display:grid;gap:10px;padding:16px;border:1px solid var(--lottery-border);border-radius:var(--lottery-radius-lg);background:var(--lottery-paper);box-shadow:var(--lottery-shadow)}.data-card p{margin:0;color:var(--lottery-muted)}.data-card>div{display:flex;justify-content:space-between}.data-card b{color:var(--lottery-orange);font-size:16px}.card-title{align-items:center}}
</style>
