<template>
  <section class="monitor-page">
    <LotteryPageHeader title="服务器监控" subtitle="服务器与 Java 运行环境的当前状态。"><template #actions><button data-action="refresh" :disabled="loading" @click="load">刷新</button></template></LotteryPageHeader>
    <p v-if="loading" class="feedback" role="status">正在读取服务器状态…</p>
    <div v-else-if="error" class="feedback error" role="alert">{{ error }} <button data-action="retry" @click="load">重试</button></div>
    <p v-else-if="!server" class="feedback" role="status">暂无服务器数据 <button data-action="retry" @click="load">重试</button></p>
    <div v-else class="summary-grid">
      <article v-for="card in cards" :key="card.title" class="summary-card"><h2>{{ card.title }}</h2><dl><div v-for="[label, value] in card.values" :key="label"><dt>{{ label }}</dt><dd>{{ value }}</dd></div></dl></article>
      <article class="summary-card wide"><h2>磁盘状态</h2><LotteryDataTable :rows="server.sysFiles || []" :columns="diskColumns" row-key="dirName" label="磁盘状态" empty-text="暂无磁盘数据" /></article>
    </div>
  </section>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { getServer } from '@/api/monitor/server'
import LotteryPageHeader from '@/components/lottery/LotteryPageHeader.vue'
import LotteryDataTable from '@/components/lottery/LotteryDataTable.vue'
const server = ref(null), loading = ref(false), error = ref('')
const measure = (value, unit = '') => value == null ? '—' : `${value}${unit}`
const cards = computed(() => {
  const { cpu = {}, mem = {}, jvm = {}, sys = {} } = server.value || {}
  return [
    { title: 'CPU', values: [['核心数', measure(cpu.cpuNum)], ['用户使用率', measure(cpu.used, '%')], ['系统使用率', measure(cpu.sys, '%')], ['当前空闲率', measure(cpu.free, '%')]] },
    { title: '内存', values: [['总内存', measure(mem.total, ' GB')], ['已用内存', measure(mem.used, ' GB')], ['剩余内存', measure(mem.free, ' GB')], ['使用率', measure(mem.usage, '%')]] },
    { title: '服务器信息', values: [['服务器名称', measure(sys.computerName)], ['服务器 IP', measure(sys.computerIp)], ['操作系统', measure(sys.osName)], ['系统架构', measure(sys.osArch)], ['项目路径', measure(sys.userDir)]] },
    { title: 'Java 虚拟机', values: [['名称', measure(jvm.name)], ['版本', measure(jvm.version)], ['总内存', measure(jvm.total, ' MB')], ['已用内存', measure(jvm.used, ' MB')], ['剩余内存', measure(jvm.free, ' MB')], ['使用率', measure(jvm.usage, '%')], ['启动时间', measure(jvm.startTime)], ['运行时长', measure(jvm.runTime)], ['安装路径', measure(jvm.home)], ['运行参数', measure(jvm.inputArgs)]] }
  ]
})
const diskColumns = [{ key: 'dirName', label: '盘符路径' }, { key: 'sysTypeName', label: '文件系统' }, { key: 'typeName', label: '盘符类型' }, { key: 'total', label: '总大小' }, { key: 'free', label: '可用大小' }, { key: 'used', label: '已用大小' }, { key: 'usage', label: '使用率', format: value => measure(value, '%') }]
async function load() {
  loading.value = true; error.value = ''; server.value = null
  try { const response = await getServer(); server.value = response.data && Object.keys(response.data).length ? response.data : null }
  catch (cause) { error.value = cause?.message || '服务器状态加载失败，请重试' }
  finally { loading.value = false }
}
onMounted(load)
</script>
<style src="./monitor-pages.css"></style>
