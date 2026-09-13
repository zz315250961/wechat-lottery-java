<template>
  <section class="monitor-page">
    <LotteryPageHeader title="登录日志" subtitle="查询账号登录结果与访问记录。"><template #actions><button data-action="refresh" :disabled="loading || busy" @click="load">刷新</button></template></LotteryPageHeader>
    <form class="filter-bar" @submit.prevent="search">
      <label>用户名称<input data-field="userName" :value="filters.userName" @input="filters.userName = $event.target.value" /></label>
      <label>登录地址<input data-field="ipaddr" :value="filters.ipaddr" @input="filters.ipaddr = $event.target.value" /></label>
      <label>登录状态<select data-field="status" :value="filters.status" @change="filters.status = $event.target.value"><option value="">全部状态</option><option value="0">成功</option><option value="1">失败</option></select></label>
      <label>开始日期<input data-field="beginTime" type="date" :value="filters.beginTime" @input="filters.beginTime = $event.target.value" /></label>
      <label>结束日期<input data-field="endTime" type="date" :value="filters.endTime" @input="filters.endTime = $event.target.value" /></label>
      <label>排序字段<select data-field="orderByColumn" :value="filters.orderByColumn" @change="filters.orderByColumn = $event.target.value"><option value="loginTime">访问时间</option><option value="userName">用户名称</option></select></label>
      <label>排序方向<select :value="filters.isAsc" @change="filters.isAsc = $event.target.value"><option value="descending">降序</option><option value="ascending">升序</option></select></label>
      <button class="primary" :disabled="loading || busy">查询</button><button type="button" :disabled="loading || busy" @click="reset">重置</button>
    </form>
    <div class="action-bar">
      <button v-hasPermi="['monitor:logininfor:remove']" data-action="delete-selected" class="danger" :disabled="busy || loading || !selected.length" @click="mutate('delete')">删除所选</button>
      <button v-hasPermi="['monitor:logininfor:remove']" data-action="clean" class="danger" :disabled="busy || loading" @click="mutate('clean')">清空日志</button>
      <button v-hasPermi="['monitor:logininfor:unlock']" data-action="unlock" :disabled="busy || loading || selected.length !== 1" @click="mutate('unlock')">解锁账号</button>
      <button v-hasPermi="['monitor:logininfor:export']" data-action="export" :disabled="busy || loading" @click="exportLogs">导出结果</button>
    </div>
    <p v-if="feedback" role="status" class="feedback" :class="{ error: actionError }">{{ feedback }}</p>
    <LotteryDataTable v-model:selected="selected" selectable :rows="rows" :columns="columns" row-key="infoId" label="登录日志" :loading="loading" :error="error" :total="total" :page-num="pageNum" :page-size="pageSize" @retry="load" @page-change="changePage" />
    <LotteryConfirmDialog ref="confirmBox" />
  </section>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { list, delLogininfor, cleanLogininfor, unlockLogininfor } from '@/api/monitor/logininfor'
import { parseTime, tansParams } from '@/utils/ruoyi'
import request from '@/utils/request'
import FileSaver from 'file-saver'
import LotteryPageHeader from '@/components/lottery/LotteryPageHeader.vue'
import LotteryDataTable from '@/components/lottery/LotteryDataTable.vue'
import LotteryConfirmDialog from '@/components/lottery/LotteryConfirmDialog.vue'
const rows = ref([]), total = ref(0), loading = ref(false), error = ref(''), selected = ref([]), pageNum = ref(1), pageSize = ref(10)
const filters = ref(emptyFilters()), busy = ref(false), feedback = ref(''), actionError = ref(false), confirmBox = ref()
function emptyFilters() { return { userName: '', ipaddr: '', status: '', beginTime: '', endTime: '', orderByColumn: 'loginTime', isAsc: 'descending' } }
const columns = [{ key: 'infoId', label: '访问编号' }, { key: 'userName', label: '用户名称' }, { key: 'ipaddr', label: '登录地址' }, { key: 'loginLocation', label: '登录地点' }, { key: 'os', label: '操作系统' }, { key: 'browser', label: '浏览器' }, { key: 'status', label: '登录状态', format: value => String(value) === '0' ? '成功' : String(value) === '1' ? '失败' : '—' }, { key: 'msg', label: '描述' }, { key: 'loginTime', label: '访问时间', format: value => parseTime(value) || '—' }]
function query() {
  const { beginTime, endTime, ...values } = filters.value
  if (beginTime && endTime && beginTime > endTime) throw new Error('开始日期不能晚于结束日期')
  const params = {}
  if (beginTime) params.beginTime = `${beginTime} 00:00:00`
  if (endTime) params.endTime = `${endTime} 23:59:59`
  return { ...values, userName: values.userName.trim(), ipaddr: values.ipaddr.trim(), pageNum: pageNum.value, pageSize: pageSize.value, params }
}
async function load() {
  loading.value = true; error.value = ''; selected.value = []
  try {
    const response = await list(query()); rows.value = response.rows || []; total.value = response.total || 0
    const lastPage = Math.max(1, Math.ceil(total.value / pageSize.value))
    if (pageNum.value > lastPage && !rows.value.length) { pageNum.value = lastPage; return await load() }
  } catch (cause) { rows.value = []; total.value = 0; error.value = cause?.message || '登录日志加载失败，请重试' }
  finally { loading.value = false }
}
function search() { pageNum.value = 1; return load() }
function reset() { filters.value = emptyFilters(); return search() }
function changePage(page) { pageNum.value = page.pageNum; pageSize.value = page.pageSize; return load() }
async function mutate(kind) {
  if (busy.value) return
  const ids = [...selected.value], user = rows.value.find(row => row.infoId === ids[0])?.userName
  if (kind === 'delete' && !ids.length || kind === 'unlock' && (ids.length !== 1 || !user)) return
  busy.value = true
  try {
    const message = kind === 'clean' ? '确认永久清空全部登录日志？' : kind === 'unlock' ? `确认解锁账号“${user}”的登录限制？` : `确认删除所选 ${ids.length} 条登录日志？`
    if (!await confirmBox.value.open({ title: kind === 'unlock' ? '解锁账号' : '删除登录日志', message, confirmText: '确认操作', danger: kind !== 'unlock' })) return
    if (kind === 'clean') await cleanLogininfor()
    else if (kind === 'unlock') await unlockLogininfor(user)
    else await delLogininfor(ids)
    feedback.value = kind === 'unlock' ? '账号已解锁' : '日志已删除'; actionError.value = false
    if (kind !== 'unlock') await load()
  } catch (cause) { feedback.value = cause?.message || '操作失败，请重试'; actionError.value = true }
  finally { busy.value = false }
}
async function exportLogs() {
  if (busy.value) return
  busy.value = true; feedback.value = ''
  try {
    const data = await request({ url: '/monitor/logininfor/export', method: 'post', data: query(), transformRequest: [tansParams], headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, responseType: 'blob' })
    if (data.type?.includes('json')) { const result = JSON.parse(await data.text()); throw new Error(result.msg || '导出失败') }
    FileSaver.saveAs(data, `logininfor_${Date.now()}.xlsx`); feedback.value = '导出文件已生成'; actionError.value = false
  } catch (cause) { feedback.value = cause?.message || '导出失败，请重试'; actionError.value = true }
  finally { busy.value = false }
}
onMounted(load)
</script>
<style src="./monitor-pages.css"></style>
