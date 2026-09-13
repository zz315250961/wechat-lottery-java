<template>
  <section class="lottery-data-page">
    <LotteryConfirm ref="confirmBox" />
    <LotteryPageHeader title="参与用户" subtitle="查看真实参与、抽奖与获券统计"><template #actions><el-button @click="$emit('refresh')">刷新数据</el-button></template></LotteryPageHeader>
    <div class="summary"><span>参与用户 <strong>{{ users.length }}</strong></span><span>有奖券用户 <strong>{{ ticketUsers }}</strong></span><span>累计抽奖 <strong>{{ drawTotal }}</strong></span></div>
    <div class="toolbar"><el-input v-model="keyword" clearable placeholder="搜索昵称、手机号或 OpenID" aria-label="搜索参与用户" /></div>
    <div class="data-table" role="region" aria-label="参与用户列表"><table><thead><tr><th>用户</th><th>手机号</th><th>OpenID</th><th>抽奖次数</th><th>奖券数</th><th>账号状态</th></tr></thead><tbody><tr v-for="user in filteredUsers" :key="user.id"><td>{{ user.nickname || '微信用户' }}</td><td>{{ user.phone || '—' }}</td><td class="code">{{ user.openid || '—' }}</td><td>{{ user.drawCount }}</td><td>{{ user.ticketCount }}</td><td><el-switch :model-value="user.enabled" :aria-label="`${user.nickname || '用户'}账号状态`" @change="toggle(user, $event)" /></td></tr></tbody></table><div v-if="!filteredUsers.length" class="empty">{{ keyword ? '没有匹配的参与用户' : '暂时没有参与用户' }}</div></div>
    <div class="mobile-cards"><article v-for="user in filteredUsers" :key="user.id" class="data-card"><div><strong>{{ user.nickname || '微信用户' }}</strong><span>{{ user.phone || '未绑定手机号' }}</span></div><dl><dt>OpenID</dt><dd class="code">{{ user.openid || '—' }}</dd><dt>抽奖次数</dt><dd>{{ user.drawCount }}</dd><dt>奖券数</dt><dd>{{ user.ticketCount }}</dd><dt>账号状态</dt><dd><el-switch :model-value="user.enabled" :aria-label="`${user.nickname || '用户'}账号状态`" @change="toggle(user, $event)" /></dd></dl></article></div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import LotteryConfirm from '@/components/LotteryConfirm.vue'
import LotteryPageHeader from '@/components/lottery/LotteryPageHeader.vue'
import { setLotteryUserEnabled } from '@/api/lottery'
const props = defineProps({ users: { type: Array, default: () => [] } })
const emit = defineEmits(['refresh'])
const keyword = ref('')
const confirmBox = ref()
const normalizedUsers = computed(() => props.users.map(user => ({ ...user, drawCount: Number(user.drawCount ?? user.draw_count ?? 0), ticketCount: Number(user.ticketCount ?? user.ticket_count ?? 0), enabled: user.enabled === true || user.enabled === 1 })))
const filteredUsers = computed(() => { const term = keyword.value.trim().toLowerCase(); return term ? normalizedUsers.value.filter(user => `${user.nickname || ''} ${user.phone || ''} ${user.openid || ''}`.toLowerCase().includes(term)) : normalizedUsers.value })
const ticketUsers = computed(() => normalizedUsers.value.filter(user => user.ticketCount > 0).length)
const drawTotal = computed(() => normalizedUsers.value.reduce((total, user) => total + user.drawCount, 0))
async function toggle(user, enabled) { if (!enabled) { const accepted = await confirmBox.value.open({ title: '停用用户', message: `停用“${user.nickname || user.phone || '该用户'}”后将无法继续参与活动。`, confirmText: '确认停用', danger: true }); if (!accepted) return } await setLotteryUserEnabled(user.id, enabled); user.enabled = enabled; ElMessage.success(enabled ? '用户已启用' : '用户已停用'); emit('refresh') }
</script>

<style scoped>
table{width:100%;min-width:850px;border-collapse:collapse;text-align:left}th,td{padding:15px 16px;border-bottom:1px solid var(--lottery-border);vertical-align:middle}th{background:#faf8f5;color:var(--lottery-muted);font-size:13px;font-weight:700}td{color:var(--lottery-ink)}tbody tr:last-child td{border-bottom:0}.code{font-family:ui-monospace,monospace;font-size:12px;overflow-wrap:anywhere}.mobile-cards{display:none}@media(max-width:600px){.data-table{display:none}.mobile-cards{display:grid;gap:12px}.data-card{padding:16px;border:1px solid var(--lottery-border);border-radius:var(--lottery-radius-lg);background:var(--lottery-paper);box-shadow:var(--lottery-shadow)}.data-card>div{display:grid;gap:4px}.data-card span{color:var(--lottery-muted);font-size:13px}.data-card dl{display:grid;grid-template-columns:90px minmax(0,1fr);gap:12px;margin:16px 0 0}.data-card dt{color:var(--lottery-muted)}.data-card dd{margin:0;text-align:right}.data-card :deep(.el-switch){min-height:44px}}
</style>
