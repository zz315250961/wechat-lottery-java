<template>
  <section class="monitor-page">
    <LotteryPageHeader title="在线用户" subtitle="查看当前登录会话与访问情况。"><template #actions><button data-action="refresh" :disabled="loading || busy" @click="load">刷新</button></template></LotteryPageHeader>
    <form class="filter-bar" @submit.prevent="search">
      <label>用户名称<input data-field="userName" :value="filters.userName" @input="filters.userName = $event.target.value" /></label>
      <label>登录地址<input data-field="ipaddr" :value="filters.ipaddr" @input="filters.ipaddr = $event.target.value" /></label>
      <button class="primary" :disabled="loading">查询</button><button type="button" :disabled="loading" @click="reset">重置</button>
    </form>
    <p v-if="feedback" role="status" class="feedback" :class="{ error: actionError }">{{ feedback }}</p>
    <LotteryDataTable :rows="visibleRows" :columns="columns" row-key="tokenId" label="在线用户" :loading="loading" :error="error" :total="rows.length" :page-num="pageNum" :page-size="pageSize" @retry="load" @page-change="changePage">
      <template #actions="{ row }"><button v-hasPermi="['monitor:online:forceLogout']" class="danger" :data-action="`logout-${row.tokenId}`" :disabled="busy" @click="logout(row)">强退</button></template>
    </LotteryDataTable>
    <LotteryConfirmDialog ref="confirmBox" />
  </section>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { list, forceLogout } from '@/api/monitor/online'
import { parseTime } from '@/utils/ruoyi'
import LotteryPageHeader from '@/components/lottery/LotteryPageHeader.vue'
import LotteryDataTable from '@/components/lottery/LotteryDataTable.vue'
import LotteryConfirmDialog from '@/components/lottery/LotteryConfirmDialog.vue'
const rows = ref([]), loading = ref(false), error = ref(''), busy = ref(false), feedback = ref(''), actionError = ref(false), confirmBox = ref()
const filters = ref({ userName: '', ipaddr: '' }), pageNum = ref(1), pageSize = ref(10)
const visibleRows = computed(() => rows.value.slice((pageNum.value - 1) * pageSize.value, pageNum.value * pageSize.value))
const columns = [
  { key: 'userName', label: '登录名称' }, { key: 'tokenId', label: '会话编号' }, { key: 'ipaddr', label: '登录地址' }, { key: 'loginLocation', label: '登录地点' },
  { key: 'os', label: '操作系统' }, { key: 'browser', label: '浏览器' }, { key: 'loginTime', label: '登录时间', format: value => parseTime(value) || '—' }
]
async function load() {
  loading.value = true; error.value = ''
  try {
    const response = await list({ userName: filters.value.userName.trim(), ipaddr: filters.value.ipaddr.trim() })
    rows.value = response.rows ?? []
    pageNum.value = Math.min(pageNum.value, Math.max(1, Math.ceil(rows.value.length / pageSize.value)))
  } catch (cause) { rows.value = []; error.value = cause?.message || '在线用户加载失败，请重试' }
  finally { loading.value = false }
}
function search() { pageNum.value = 1; return load() }
function reset() { filters.value = { userName: '', ipaddr: '' }; return search() }
function changePage(page) { pageNum.value = page.pageNum; pageSize.value = page.pageSize }
async function logout(row) {
  if (busy.value) return
  busy.value = true
  try {
    if (!await confirmBox.value.open({ title: '强退在线用户', message: `确认结束“${row.userName}”的登录会话？该用户需要重新登录。`, confirmText: '确认强退', danger: true })) return
    await forceLogout(row.tokenId); feedback.value = '已结束该登录会话'; actionError.value = false; await load()
  } catch (cause) { feedback.value = cause?.message || '强退失败，请重试'; actionError.value = true }
  finally { busy.value = false }
}
onMounted(load)
</script>
<style src="./monitor-pages.css"></style>
