<template>
  <section class="monitor-page">
    <LotteryPageHeader title="Redis 缓存" subtitle="缓存服务状态、命令统计与缓存内容。"><template #actions><button data-action="refresh" :disabled="loading || busy" @click="refresh">刷新</button></template></LotteryPageHeader>
    <p v-if="loading" role="status" class="feedback">正在读取 Redis 状态…</p>
    <div v-else-if="error" role="alert" class="feedback error">{{ error }} <button data-action="retry" @click="load">重试</button></div>
    <p v-else-if="!cache" role="status" class="feedback">暂无 Redis 数据 <button data-action="retry" @click="load">重试</button></p>
    <div v-else class="summary-grid">
      <article v-for="card in cards" :key="card.title" class="summary-card"><h2>{{ card.title }}</h2><dl><div v-for="[label, value] in card.values" :key="label"><dt>{{ label }}</dt><dd>{{ value }}</dd></div></dl></article>
      <article class="summary-card wide"><h2>命令统计</h2><LotteryDataTable :rows="cache.commandStats || []" :columns="commandColumns" row-key="name" label="Redis 命令统计" empty-text="暂无命令统计" /></article>
    </div>
    <p v-if="feedback" role="status" class="feedback" :class="{ error: actionError }">{{ feedback }}</p>
    <div class="action-bar"><h2>缓存管理</h2><button v-hasPermi="['monitor:cache:list']" data-action="clear-all" class="danger" :disabled="busy" @click="clear('all')">清理全部缓存</button></div>
    <div class="summary-grid">
      <article class="summary-card"><h2>缓存名称</h2>
        <p v-if="namesLoading" role="status">正在加载缓存名称…</p>
        <div v-else-if="namesError" role="alert" class="error">{{ namesError }} <button @click="loadNames">重试名称列表</button></div>
        <p v-else-if="!names.length" class="muted">暂无缓存名称</p>
        <ul v-else class="cache-list"><li v-for="item in names" :key="item.cacheName"><button :class="{ active: selectedName === item.cacheName }" :data-action="`cache-name-${item.cacheName}`" :disabled="busy" @click="loadKeys(item.cacheName)">{{ item.remark || item.cacheName }} · {{ item.cacheName }}</button><button v-hasPermi="['monitor:cache:list']" class="danger" :data-action="`clear-name-${item.cacheName}`" :disabled="busy" @click="clear('name', item.cacheName)">清理</button></li></ul>
      </article>
      <article class="summary-card"><h2>缓存键名</h2>
        <p v-if="keysLoading" role="status">正在加载键名…</p>
        <div v-else-if="keysError" role="alert" class="error">{{ keysError }} <button @click="loadKeys(selectedName)">重试键名列表</button></div>
        <p v-else-if="!selectedName" class="muted">请先选择缓存名称</p>
        <p v-else-if="!keys.length" class="muted">此名称下暂无缓存</p>
        <ul v-else class="cache-list"><li v-for="key in keys" :key="key"><button :data-action="`cache-key-${key}`" :class="{ active: selectedKey === key }" :disabled="busy" @click="loadValue(key)">{{ key }}</button><button v-hasPermi="['monitor:cache:list']" class="danger" :data-action="`clear-key-${key}`" :disabled="busy" @click="clear('key', key)">清理</button></li></ul>
      </article>
      <article class="summary-card wide"><h2>缓存内容</h2>
        <p v-if="valueLoading" role="status">正在读取缓存内容…</p>
        <div v-else-if="valueError" role="alert" class="error">{{ valueError }} <button @click="loadValue(selectedKey)">重试缓存内容</button></div>
        <template v-else-if="value"><p class="muted">{{ value.cacheName }} / {{ value.cacheKey }}</p><pre>{{ value.cacheValue ?? '（无内容）' }}</pre></template>
        <p v-else class="muted">选择键名查看缓存内容</p>
      </article>
    </div>
    <LotteryConfirmDialog ref="confirmBox" />
  </section>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { getCache, listCacheName, listCacheKey, getCacheValue, clearCacheName, clearCacheKey, clearCacheAll } from '@/api/monitor/cache'
import LotteryPageHeader from '@/components/lottery/LotteryPageHeader.vue'
import LotteryDataTable from '@/components/lottery/LotteryDataTable.vue'
import LotteryConfirmDialog from '@/components/lottery/LotteryConfirmDialog.vue'
const cache = ref(null), loading = ref(false), error = ref(''), busy = ref(false), confirmBox = ref(), feedback = ref(''), actionError = ref(false)
const names = ref([]), keys = ref([]), value = ref(null), selectedName = ref(''), selectedKey = ref('')
const namesLoading = ref(false), keysLoading = ref(false), valueLoading = ref(false), namesError = ref(''), keysError = ref(''), valueError = ref('')
let keyRequest = 0, valueRequest = 0
const measure = (value, unit = '') => value == null ? '—' : `${value}${unit}`
const cards = computed(() => {
  const info = cache.value?.info || {}
  return [
    { title: '运行状态', values: [['Redis 版本', measure(info.redis_version)], ['运行模式', info.redis_mode === 'standalone' ? '单机' : measure(info.redis_mode)], ['端口', measure(info.tcp_port)], ['客户端数', measure(info.connected_clients)], ['运行时间', measure(info.uptime_in_days, ' 天')], ['Key 数量', measure(cache.value?.dbSize)]] },
    { title: '内存与持久化', values: [['已用内存', measure(info.used_memory_human)], ['内存配置', measure(info.maxmemory_human)], ['CPU 使用', measure(info.used_cpu_user_children)], ['AOF', info.aof_enabled == null ? '—' : String(info.aof_enabled) === '0' ? '未开启' : '已开启'], ['RDB 保存状态', measure(info.rdb_last_bgsave_status)], ['网络入口', measure(info.instantaneous_input_kbps, ' KB/s')], ['网络出口', measure(info.instantaneous_output_kbps, ' KB/s')]] }
  ]
})
const commandColumns = [{ key: 'name', label: '命令' }, { key: 'value', label: '调用次数' }]
function refresh() { return Promise.all([load(), loadNames(), selectedName.value ? loadKeys(selectedName.value) : Promise.resolve()]) }
async function load() {
  loading.value = true; error.value = ''; cache.value = null
  try { const response = await getCache(); cache.value = response.data && Object.keys(response.data).length ? response.data : null }
  catch (cause) { error.value = cause?.message || 'Redis 连接失败，请重试' }
  finally { loading.value = false }
}
async function loadNames() {
  namesLoading.value = true; namesError.value = ''
  try { names.value = (await listCacheName()).data || [] }
  catch (cause) { names.value = []; namesError.value = cause?.message || '缓存名称加载失败' }
  finally { namesLoading.value = false }
}
async function loadKeys(name) {
  if (!name) return
  const requestId = ++keyRequest; ++valueRequest
  selectedName.value = name; selectedKey.value = ''; value.value = null; valueError.value = ''; valueLoading.value = false
  keys.value = []; keysLoading.value = true; keysError.value = ''
  try { const response = await listCacheKey(name); if (requestId === keyRequest) keys.value = response.data || [] }
  catch (cause) { if (requestId === keyRequest) keysError.value = cause?.message || '缓存键名加载失败' }
  finally { if (requestId === keyRequest) keysLoading.value = false }
}
async function loadValue(key) {
  const requestId = ++valueRequest
  selectedKey.value = key; value.value = null; valueLoading.value = true; valueError.value = ''
  try { const response = await getCacheValue(selectedName.value, key); if (requestId === valueRequest) value.value = response.data }
  catch (cause) { if (requestId === valueRequest) valueError.value = cause?.message || '缓存内容加载失败' }
  finally { if (requestId === valueRequest) valueLoading.value = false }
}
async function clear(scope, key) {
  if (busy.value) return
  busy.value = true
  try {
    const target = scope === 'all' ? '全部缓存（包括登录会话，可能需要重新登录）' : key
    if (!await confirmBox.value.open({ title: '清理缓存', message: `确认清理 ${target}？`, confirmText: '确认清理', danger: true })) return
    if (scope === 'all') await clearCacheAll()
    else if (scope === 'name') await clearCacheName(key)
    else await clearCacheKey(key)
    value.value = null; selectedKey.value = ''; ++valueRequest
    feedback.value = '缓存已清理'; actionError.value = false
    await Promise.all([load(), loadNames(), selectedName.value ? loadKeys(selectedName.value) : Promise.resolve()])
  } catch (cause) { feedback.value = cause?.message || '缓存清理失败，请重试'; actionError.value = true }
  finally { busy.value = false }
}
onMounted(() => { load(); loadNames() })
</script>
<style src="./monitor-pages.css"></style>
