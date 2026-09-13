<template>
  <section class="lottery-data-table" :aria-busy="loading" :aria-label="label">
    <p v-if="loading" class="table-state" role="status">正在加载…</p>
    <div v-else-if="error" class="table-state table-error" role="alert">{{ error }} <button type="button" data-action="retry" @click="$emit('retry')">重试</button></div>
    <p v-else-if="!rows.length" class="table-state" role="status">{{ emptyText }}</p>
    <template v-else>
      <div class="desktop-table">
        <table><thead><tr>
          <th v-if="selectable"><label class="selection"><input type="checkbox" aria-label="选择本页全部记录" :checked="rows.every(row => selected.includes(row[rowKey]))" @change="selectPage($event.target.checked)" /></label></th>
          <th v-for="column in columns" :key="column.key" scope="col">{{ column.label }}</th>
          <th v-if="$slots.actions" scope="col">操作</th>
        </tr></thead><tbody><tr v-for="row in rows" :key="row[rowKey]">
          <td v-if="selectable"><label class="selection"><input type="checkbox" :aria-label="`选择记录 ${row[rowKey]}`" :data-action="`select-${row[rowKey]}`" :checked="selected.includes(row[rowKey])" @change="selectRow(row, $event.target.checked)" /></label></td>
          <td v-for="column in columns" :key="column.key"><slot :name="`cell-${column.key}`" :row="row">{{ display(row, column) }}</slot></td>
          <td v-if="$slots.actions"><div class="table-actions"><slot name="actions" :row="row" /></div></td>
        </tr></tbody></table>
      </div>
      <div class="mobile-cards">
        <article v-for="row in rows" :key="row[rowKey]" class="mobile-record">
          <label v-if="selectable" class="selection"><input type="checkbox" :aria-label="`选择记录 ${row[rowKey]}`" :data-action="`mobile-select-${row[rowKey]}`" :checked="selected.includes(row[rowKey])" @change="selectRow(row, $event.target.checked)" />选择此记录</label>
          <slot name="mobile-card" :row="row"><dl><div v-for="column in columns" :key="column.key"><dt>{{ column.label }}</dt><dd><slot :name="`cell-${column.key}`" :row="row">{{ display(row, column) }}</slot></dd></div></dl></slot>
          <div v-if="$slots.actions" class="table-actions"><slot name="actions" :row="row" /></div>
        </article>
      </div>
    </template>
    <nav v-if="!loading && !error && total > 0" class="table-pagination" aria-label="记录分页">
      <span>共 {{ total }} 条 · 第 {{ pageNum }} / {{ Math.max(1, Math.ceil(total / pageSize)) }} 页</span>
      <label>每页 <select aria-label="每页条数" :value="pageSize" @change="$emit('page-change', { pageNum: 1, pageSize: Number($event.target.value) })"><option v-for="size in [10, 20, 50]" :key="size" :value="size">{{ size }}</option></select></label>
      <button type="button" data-action="previous-page" :disabled="pageNum <= 1" @click="$emit('page-change', { pageNum: pageNum - 1, pageSize })">上一页</button>
      <button type="button" data-action="next-page" :disabled="pageNum * pageSize >= total" @click="$emit('page-change', { pageNum: pageNum + 1, pageSize })">下一页</button>
    </nav>
  </section>
</template>

<script setup>
const props = defineProps({
  rows: { type: Array, default: () => [] }, columns: { type: Array, default: () => [] }, rowKey: { type: String, default: 'id' },
  loading: Boolean, error: { type: String, default: '' }, emptyText: { type: String, default: '暂无记录' }, label: { type: String, default: '数据记录' },
  total: { type: Number, default: 0 }, pageNum: { type: Number, default: 1 }, pageSize: { type: Number, default: 10 },
  selectable: Boolean, selected: { type: Array, default: () => [] }
})
const emit = defineEmits(['retry', 'page-change', 'update:selected'])
function display(row, column) { return column.format ? column.format(row[column.key], row) : row[column.key] ?? '—' }
function selectRow(row, checked) {
  const ids = props.selected.filter(id => id !== row[props.rowKey])
  if (checked) ids.push(row[props.rowKey])
  emit('update:selected', ids)
}
function selectPage(checked) { emit('update:selected', checked ? props.rows.map(row => row[props.rowKey]) : []) }
</script>

<style scoped>
.lottery-data-table { min-width: 0; border: 1px solid var(--lottery-border); border-radius: var(--lottery-radius-lg); background: var(--lottery-paper); overflow: hidden; color: var(--lottery-ink); }
.desktop-table { overflow-x: auto; } table { width: 100%; border-collapse: collapse; font-size: 14px; text-align: left; } th, td { padding: 14px 16px; border-bottom: 1px solid var(--lottery-border); overflow-wrap: anywhere; min-width: 100px; } th { background: var(--lottery-orange-soft); color: var(--lottery-muted); font-weight: 600; } .selection { display: flex; align-items: center; gap: 8px; min-width: 44px; min-height: 44px; cursor: pointer; } input { accent-color: var(--lottery-orange); } .table-state { margin: 0; padding: 32px 20px; color: var(--lottery-muted); text-align: center; } .table-error { color: #a94235; } .table-actions { display: flex; flex-wrap: wrap; gap: 8px; } .table-pagination { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; padding: 16px; color: var(--lottery-muted); font-size: 13px; } button, select { min-height: 44px; padding: 0 14px; border: 1px solid var(--lottery-border); border-radius: var(--lottery-radius-md); background: var(--lottery-paper); color: var(--lottery-ink); cursor: pointer; } button:disabled { opacity: .45; cursor: not-allowed; } .mobile-cards { display: none; }
@media (max-width: 767px) { .desktop-table { display: none; } .mobile-cards { display: grid; } .mobile-record { min-width: 0; padding: 16px; border-bottom: 1px solid var(--lottery-border); } dl { display: grid; gap: 10px; margin: 0 0 12px; } dl > div { display: grid; grid-template-columns: 88px minmax(0, 1fr); gap: 12px; font-size: 14px; } dt { color: var(--lottery-muted); } dd { margin: 0; overflow-wrap: anywhere; white-space: pre-wrap; } .table-actions :deep(button) { flex: 1; min-height: 44px; } .table-pagination > span { width: 100%; } }
</style>
