<template>
  <section class="monitor-page">
    <LotteryPageHeader title="操作日志" subtitle="查询系统操作、执行结果与详细审计记录。"><template #actions><button data-action="refresh" :disabled="loading || busy" @click="load">刷新</button></template></LotteryPageHeader>
    <form class="filter-bar" @submit.prevent="search">
      <label>操作人员<input data-field="operName" :value="filters.operName" @input="filters.operName = $event.target.value" /></label>
      <label>操作地址<input data-field="operIp" :value="filters.operIp" @input="filters.operIp = $event.target.value" /></label>
      <label>系统模块<input data-field="title" :value="filters.title" @input="filters.title = $event.target.value" /></label>
      <label>操作类型<select data-field="businessType" :disabled="dictLoading" :value="filters.businessType" @change="filters.businessType = $event.target.value"><option value="">全部类型</option><option v-for="item in types" :key="item.dictValue" :value="item.dictValue">{{ item.dictLabel }}</option></select></label>
      <label>操作状态<select data-field="status" :value="filters.status" @change="filters.status = $event.target.value"><option value="">全部状态</option><option value="0">成功</option><option value="1">失败</option></select></label>
      <label>开始日期<input data-field="beginTime" type="date" :value="filters.beginTime" @input="filters.beginTime = $event.target.value" /></label>
      <label>结束日期<input data-field="endTime" type="date" :value="filters.endTime" @input="filters.endTime = $event.target.value" /></label>
      <label>排序字段<select data-field="orderByColumn" :value="filters.orderByColumn" @change="filters.orderByColumn = $event.target.value"><option value="operTime">操作时间</option><option value="operName">操作人员</option><option value="costTime">消耗时间</option></select></label>
      <label>排序方向<select :value="filters.isAsc" @change="filters.isAsc = $event.target.value"><option value="descending">降序</option><option value="ascending">升序</option></select></label>
      <button class="primary" :disabled="loading || busy">查询</button><button type="button" :disabled="loading || busy" @click="reset">重置</button>
    </form>
    <p v-if="dictError" class="feedback error" role="alert">{{ dictError }} <button @click="loadTypes">重试操作类型</button></p>
    <div class="action-bar">
      <button v-hasPermi="['monitor:operlog:remove']" data-action="delete-selected" class="danger" :disabled="busy || loading || !selected.length" @click="mutate('delete')">删除所选</button>
      <button v-hasPermi="['monitor:operlog:remove']" data-action="clean" class="danger" :disabled="busy || loading" @click="mutate('clean')">清空日志</button>
      <button v-hasPermi="['monitor:operlog:export']" data-action="export" :disabled="busy || loading" @click="exportLogs">导出结果</button>
    </div>
    <p v-if="feedback" role="status" class="feedback" :class="{ error: actionError }">{{ feedback }}</p>
    <LotteryDataTable v-model:selected="selected" selectable :rows="rows" :columns="columns" row-key="operId" label="操作日志" :loading="loading" :error="error" :total="total" :page-num="pageNum" :page-size="pageSize" @retry="load" @page-change="changePage">
      <template #actions="{ row }"><button v-hasPermi="['monitor:operlog:query']" :data-action="`detail-${row.operId}`" @click="openDetail(row, $event)">详细</button></template>
    </LotteryDataTable>
    <div v-if="detail" class="detail-mask" @click.self="closeDetail">
      <section ref="detailDialog" class="detail-card" role="dialog" aria-modal="true" aria-label="操作日志详细" tabindex="-1" @keydown.esc.prevent="closeDetail" @keydown.tab="trapFocus">
        <header><h2>操作日志详细 · {{ detail.operId }}</h2><button data-action="close-detail" @click="closeDetail">关闭</button></header>
        <dl><div v-for="column in detailColumns" :key="column.key"><dt>{{ column.label }}</dt><dd>{{ column.format ? column.format(detail[column.key]) : detail[column.key] ?? '—' }}</dd></div></dl>
        <h3>请求参数</h3><button @click="copy(detail.operParam)">复制请求参数</button><pre>{{ formatJson(detail.operParam) }}</pre>
        <h3>返回参数</h3><button @click="copy(detail.jsonResult)">复制返回参数</button><pre>{{ formatJson(detail.jsonResult) }}</pre>
        <template v-if="String(detail.status) === '1'"><h3>异常信息</h3><pre class="error">{{ detail.errorMsg || '（无异常详情）' }}</pre></template>
        <p v-if="copyFeedback" role="status">{{ copyFeedback }}</p>
      </section>
    </div>
    <LotteryConfirmDialog ref="confirmBox" />
  </section>
</template>
<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { list, delOperlog, cleanOperlog } from '@/api/monitor/operlog'
import { getDicts } from '@/api/system/dict/data'
import { parseTime, tansParams } from '@/utils/ruoyi'
import request from '@/utils/request'
import FileSaver from 'file-saver'
import LotteryPageHeader from '@/components/lottery/LotteryPageHeader.vue'
import LotteryDataTable from '@/components/lottery/LotteryDataTable.vue'
import LotteryConfirmDialog from '@/components/lottery/LotteryConfirmDialog.vue'
const rows = ref([]), total = ref(0), loading = ref(false), error = ref(''), selected = ref([]), pageNum = ref(1), pageSize = ref(10)
const filters = ref(emptyFilters()), busy = ref(false), feedback = ref(''), actionError = ref(false), confirmBox = ref()
function emptyFilters() { return { title: '', businessType: '', operName: '', operIp: '', status: '', beginTime: '', endTime: '', orderByColumn: 'operTime', isAsc: 'descending' } }
const types = ref([]), dictLoading = ref(false), dictError = ref(''), detail = ref(null), detailDialog = ref(), copyFeedback = ref('')
let detailTrigger
const typeLabel = value => types.value.find(item => String(item.dictValue) === String(value))?.dictLabel || (value == null ? '—' : String(value))
const statusLabel = value => String(value) === '0' ? '成功' : String(value) === '1' ? '失败' : '—'
const columns = [{ key: 'operId', label: '日志编号' }, { key: 'title', label: '系统模块' }, { key: 'businessType', label: '操作类型', format: typeLabel }, { key: 'operName', label: '操作人员' }, { key: 'operIp', label: '操作地址' }, { key: 'status', label: '操作状态', format: statusLabel }, { key: 'operTime', label: '操作时间', format: value => parseTime(value) || '—' }, { key: 'costTime', label: '消耗时间', format: value => value == null ? '—' : `${value} 毫秒` }]
const detailColumns = [...columns, { key: 'deptName', label: '所属部门' }, { key: 'operLocation', label: '操作地点' }, { key: 'requestMethod', label: '请求方式' }, { key: 'operUrl', label: '请求地址' }, { key: 'method', label: '操作方法' }]
async function loadTypes() {
  dictLoading.value = true; dictError.value = ''
  try { types.value = (await getDicts('sys_oper_type')).data || [] }
  catch (cause) { dictError.value = cause?.message || '操作类型读取失败'; types.value = [] }
  finally { dictLoading.value = false }
}
async function openDetail(row, event) { detailTrigger = event?.target; detail.value = row; copyFeedback.value = ''; await nextTick(); detailDialog.value?.focus?.() }
function closeDetail() { detail.value = null; detailTrigger?.focus?.() }
function trapFocus(event) {
  const controls = [...(detailDialog.value?.querySelectorAll?.('button') || [])]
  if (!controls.length) return
  const index = controls.indexOf(event.target)
  if (event.shiftKey && index <= 0) { event.preventDefault(); controls.at(-1).focus() }
  else if (!event.shiftKey && (index === controls.length - 1 || index < 0)) { event.preventDefault(); controls[0].focus() }
}
function formatJson(value) { if (value == null || value === '') return '（无数据）'; try { return JSON.stringify(JSON.parse(value), null, 2) } catch { return String(value) } }
async function copy(value) {
  try { await navigator.clipboard.writeText(formatJson(value)); copyFeedback.value = '已复制' }
  catch { copyFeedback.value = '复制失败，请手动选择内容复制' }
}
function query() {
  const { beginTime, endTime, ...values } = filters.value
  if (beginTime && endTime && beginTime > endTime) throw new Error('开始日期不能晚于结束日期')
  const params = {}
  if (beginTime) params.beginTime = `${beginTime} 00:00:00`
  if (endTime) params.endTime = `${endTime} 23:59:59`
  return { ...values, operName: values.operName.trim(), operIp: values.operIp.trim(), pageNum: pageNum.value, pageSize: pageSize.value, params }
}
async function load() {
  loading.value = true; error.value = ''; selected.value = []
  try {
    const response = await list(query()); rows.value = response.rows || []; total.value = response.total || 0
    const lastPage = Math.max(1, Math.ceil(total.value / pageSize.value))
    if (pageNum.value > lastPage && !rows.value.length) { pageNum.value = lastPage; return await load() }
  } catch (cause) { rows.value = []; total.value = 0; error.value = cause?.message || '操作日志加载失败，请重试' }
  finally { loading.value = false }
}
function search() { pageNum.value = 1; return load() }
function reset() { filters.value = emptyFilters(); return search() }
function changePage(page) { pageNum.value = page.pageNum; pageSize.value = page.pageSize; return load() }
async function mutate(kind) {
  if (busy.value) return
  const ids = [...selected.value]
  if (kind === 'delete' && !ids.length) return
  busy.value = true
  try {
    const message = kind === 'clean' ? '确认永久清空全部操作日志？' : `确认删除所选 ${ids.length} 条操作日志？`
    if (!await confirmBox.value.open({ title: '删除操作日志', message, confirmText: '确认操作', danger: true })) return
    if (kind === 'clean') await cleanOperlog()
    else await delOperlog(ids)
    feedback.value = '日志已删除'; actionError.value = false; await load()
  } catch (cause) { feedback.value = cause?.message || '操作失败，请重试'; actionError.value = true }
  finally { busy.value = false }
}
async function exportLogs() {
  if (busy.value) return
  busy.value = true; feedback.value = ''
  try {
    const data = await request({ url: '/monitor/operlog/export', method: 'post', data: query(), transformRequest: [tansParams], headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, responseType: 'blob' })
    if (data.type?.includes('json')) { const result = JSON.parse(await data.text()); throw new Error(result.msg || '导出失败') }
    FileSaver.saveAs(data, `operlog_${Date.now()}.xlsx`); feedback.value = '导出文件已生成'; actionError.value = false
  } catch (cause) { feedback.value = cause?.message || '导出失败，请重试'; actionError.value = true }
  finally { busy.value = false }
}
onMounted(() => { load(); loadTypes() })
</script>
<style src="./monitor-pages.css"></style>
