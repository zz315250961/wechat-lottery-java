<template>
  <section class="lottery-data-page">
    <LotteryPageHeader title="中奖与核销" subtitle="核对中奖、券码状态和商户核销记录"><template #actions><el-button @click="$emit('refresh')">刷新数据</el-button></template></LotteryPageHeader>
    <div class="summary"><span>中奖记录 <strong>{{ tickets.length }}</strong></span><span>待核销 <strong>{{ pendingCount }}</strong></span><span>已核销 <strong>{{ redeemedCount }}</strong></span><span>已过期 <strong>{{ expiredCount }}</strong></span></div>
    <div class="toolbar"><el-input v-model="keyword" clearable placeholder="搜索用户、手机号、奖品、商户或核销码" aria-label="搜索中奖与核销记录" /><el-select v-model="statusFilter" aria-label="按状态筛选" placeholder="全部状态" clearable><el-option v-for="item in statusFilters" :key="item.value" :label="item.label" :value="item.value" /></el-select><el-select v-model="sourceFilter" aria-label="按中奖来源筛选" placeholder="全部来源" clearable><el-option v-for="item in sourceFilters" :key="item.value" :label="item.label" :value="item.value" /></el-select><el-select v-model="merchantFilter" aria-label="按商户筛选" placeholder="全部商户" clearable><el-option v-for="merchant in merchants" :key="merchant" :label="merchant" :value="merchant" /></el-select></div>
    <div class="data-table" role="region" aria-label="中奖与核销记录"><table><thead><tr><th>奖品</th><th>用户</th><th>手机号</th><th>商户</th><th>中奖来源</th><th>核销码</th><th>状态</th><th>中奖时间</th><th>核销时间</th></tr></thead><tbody><tr v-for="ticket in filteredTickets" :key="ticket.id"><td>{{ ticket.prizeName }}</td><td>{{ ticket.nickname || '微信用户' }}</td><td>{{ ticket.phone || '—' }}</td><td>{{ ticket.merchantName || '—' }}</td><td><el-tag :type="ticket.sourceType === 'DIRECTED' ? 'warning' : 'info'">{{ sourceText(ticket.sourceType) }}</el-tag></td><td class="code">{{ ticket.ticketNo }}</td><td><span class="status" :class="ticket.status.toLowerCase()">{{ statusText(ticket.status) }}</span></td><td>{{ ticket.wonAt || '—' }}</td><td>{{ ticket.redeemedAt || '—' }}</td></tr></tbody></table><div v-if="!filteredTickets.length" class="empty">{{ keyword ? '没有匹配的中奖或核销记录' : '暂时没有奖券记录' }}</div></div>
    <div class="mobile-cards"><article v-for="ticket in filteredTickets" :key="ticket.id" class="data-card"><div class="data-card__head"><strong>{{ ticket.prizeName }}</strong><span class="status" :class="ticket.status.toLowerCase()">{{ statusText(ticket.status) }}</span></div><dl><dt>用户</dt><dd>{{ ticket.nickname || '微信用户' }} {{ ticket.phone || '' }}</dd><dt>商户</dt><dd>{{ ticket.merchantName || '—' }}</dd><dt>中奖来源</dt><dd>{{ sourceText(ticket.sourceType) }}</dd><dt>核销码</dt><dd class="code">{{ ticket.ticketNo }}</dd><dt>中奖时间</dt><dd>{{ ticket.wonAt || '—' }}</dd><dt>核销时间</dt><dd>{{ ticket.redeemedAt || '—' }}</dd></dl></article></div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import LotteryPageHeader from '@/components/lottery/LotteryPageHeader.vue'
const props = defineProps({ tickets: { type: Array, default: () => [] } })
defineEmits(['refresh'])
const keyword = ref('')
const statusFilter = ref('')
const sourceFilter = ref('')
const merchantFilter = ref('')
const statusFilters = [{ value: 'PENDING', label: '待核销' }, { value: 'REDEEMED', label: '已核销' }, { value: 'EXPIRED', label: '已过期' }]
const sourceFilters = [{ value: 'DIRECTED', label: '定向中奖' }, { value: 'RANDOM', label: '随机中奖' }, { value: 'UNKNOWN', label: '历史未标记' }]
const normalizedTickets = computed(() => props.tickets.map(ticket => ({ ...ticket, prizeName: ticket.prizeName ?? ticket.prize_name ?? '', merchantName: ticket.merchantName ?? ticket.merchant_name ?? '', ticketNo: ticket.ticketNo ?? ticket.ticket_no ?? '', wonAt: ticket.wonAt ?? ticket.createTime ?? ticket.create_time ?? '', redeemedAt: ticket.redeemedAt ?? ticket.redeemed_at ?? '', sourceType: String(ticket.sourceType ?? ticket.source_type ?? 'UNKNOWN').toUpperCase(), status: String(ticket.status || 'PENDING').toUpperCase() })))
const merchants = computed(() => [...new Set(normalizedTickets.value.map(ticket => ticket.merchantName).filter(Boolean))])
const filteredTickets = computed(() => { const term = keyword.value.trim().toLowerCase(); return normalizedTickets.value.filter(ticket => (!statusFilter.value || ticket.status === statusFilter.value) && (!sourceFilter.value || ticket.sourceType === sourceFilter.value) && (!merchantFilter.value || ticket.merchantName === merchantFilter.value) && (!term || `${ticket.prizeName} ${ticket.nickname || ''} ${ticket.phone || ''} ${ticket.merchantName} ${ticket.ticketNo} ${statusText(ticket.status)} ${sourceText(ticket.sourceType)}`.toLowerCase().includes(term))) })
const pendingCount = computed(() => normalizedTickets.value.filter(ticket => ticket.status === 'PENDING').length)
const redeemedCount = computed(() => normalizedTickets.value.filter(ticket => ticket.status === 'REDEEMED').length)
const expiredCount = computed(() => normalizedTickets.value.filter(ticket => ticket.status === 'EXPIRED').length)
function statusText(status) { return ({ PENDING: '待核销', REDEEMED: '已核销', EXPIRED: '已过期', USED: '已核销' }[status] || status) }
function sourceText(source) { return ({ DIRECTED: '定向中奖', RANDOM: '随机中奖', UNKNOWN: '历史未标记' }[source] || '历史未标记') }
</script>

<style scoped>
table{width:100%;min-width:1120px;border-collapse:collapse;text-align:left}th,td{padding:15px 16px;border-bottom:1px solid var(--lottery-border);vertical-align:middle;white-space:nowrap}th{background:#faf8f5;color:var(--lottery-muted);font-size:13px;font-weight:700}td{color:var(--lottery-ink)}tbody tr:last-child td{border-bottom:0}.code{font-family:ui-monospace,monospace;font-size:12px}.status{display:inline-flex;min-height:28px;align-items:center;padding:0 10px;border-radius:999px;background:var(--lottery-orange-soft);color:var(--lottery-orange);font-size:13px;font-weight:700}.status.redeemed{background:#eaf7ed;color:#277a43}.status.expired{background:#f3f0ed;color:var(--lottery-muted)}.mobile-cards{display:none}@media(max-width:600px){.data-table{display:none}.mobile-cards{display:grid;gap:12px}.data-card{padding:16px;border:1px solid var(--lottery-border);border-radius:var(--lottery-radius-lg);background:var(--lottery-paper);box-shadow:var(--lottery-shadow)}.data-card__head{display:flex;align-items:center;justify-content:space-between;gap:12px}.data-card dl{display:grid;grid-template-columns:76px minmax(0,1fr);gap:12px;margin:16px 0 0}.data-card dt{color:var(--lottery-muted)}.data-card dd{margin:0;text-align:right;overflow-wrap:anywhere}}
.toolbar{display:flex;flex-wrap:wrap;gap:10px}.toolbar :deep(.el-input){width:min(580px,100%)}.toolbar :deep(.el-select){width:180px}.toolbar :deep(.el-input__wrapper),.toolbar :deep(.el-select__wrapper){min-height:42px}@media(max-width:600px){.toolbar :deep(.el-input),.toolbar :deep(.el-select){width:100%}}
</style>
