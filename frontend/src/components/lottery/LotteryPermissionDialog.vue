<template>
  <teleport to="body">
    <div v-if="visible" class="permission-mask" @click.self="cancel">
      <section class="permission-dialog" role="dialog" aria-modal="true" :aria-label="title">
        <header><div><p class="eyebrow">角色授权</p><h2>{{ title }}</h2></div><button type="button" aria-label="关闭" @click="cancel">×</button></header>
        <p v-if="error" class="scope-warning">{{ error }}</p>
        <template v-if="mode === 'menu'">
          <p class="hint">勾选该角色可进入的真实菜单。保存后会提交给若依角色菜单接口。</p>
          <div class="menu-list"><label v-for="menu in flatMenus" :key="menu.id" class="check-row" :style="{ paddingLeft: `${10 + menu.depth * 20}px` }"><input type="checkbox" :value="menu.id" :checked="isChecked(menu, selectedMenus)" :indeterminate="isPartial(menu, selectedMenus)" :disabled="loading || !loaded" @change="toggleNode(menu, $event.target.checked, 'menu')"><span>{{ menu.label }}</span></label></div>
        </template>
        <template v-else>
          <p class="hint">选择若依已有的数据范围；此页面不臆造组织树或部门。</p>
          <div v-if="selectedScope === '2'" class="menu-list"><label v-for="dept in flatMenus" :key="dept.id" class="check-row" :style="{ paddingLeft: `${10 + dept.depth * 20}px` }"><input type="checkbox" :value="dept.id" :checked="isChecked(dept, selectedDepts)" :indeterminate="isPartial(dept, selectedDepts)" :disabled="loading || !loaded" @change="toggleNode(dept, $event.target.checked, 'scope')"><span>{{ dept.label }}</span></label></div>
          <label class="scope-option" v-for="option in scopeOptions" :key="option.value"><input v-model="selectedScope" type="radio" name="data-scope" :value="option.value"><span><b>{{ option.label }}</b><small>{{ option.description }}</small></span></label>
        </template>
        <footer><button type="button" class="cancel" @click="cancel">取消</button><button type="button" class="confirm" :disabled="loading || !loaded" @click="save">保存权限</button></footer>
      </section>
    </div>
  </teleport>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
const props = defineProps({ visible: Boolean, title: { type: String, default: '配置权限' }, mode: { type: String, default: 'menu' }, menus: { type: Array, default: () => [] }, checkedMenuIds: { type: Array, default: () => [] }, checkedDeptIds: { type: Array, default: () => [] }, menuCheckStrictly: Boolean, deptCheckStrictly: Boolean, dataScope: { type: String, default: '1' }, loading: Boolean, loaded: Boolean, error: { type: String, default: '' } })
const emit = defineEmits(['update:visible', 'save'])
const selectedMenus = ref([])
const selectedDepts = ref([])
const selectedScope = ref('1')
const scopeOptions = [{ value: '1', label: '全部数据权限', description: '可查看全部授权数据' }, { value: '2', label: '自定数据权限', description: '从真实部门树选择' }, { value: '3', label: '本部门数据权限', description: '只查看本部门数据' }, { value: '4', label: '本部门及以下数据权限', description: '查看本部门和下级数据' }, { value: '5', label: '仅本人数据权限', description: '仅查看本人创建的数据' }]
const flatten = (items, ancestors = []) => (items || []).flatMap(item => [{ ...item, ancestors, depth: ancestors.length }, ...flatten(item.children, [...ancestors, item.id])])
const flatMenus = computed(() => flatten(props.menus))
// RuoYi's *CheckStrictly fields mean parent/child linkage (the inverse of ElTree.checkStrictly).
const linked = computed(() => props.mode === 'menu' ? props.menuCheckStrictly : props.deptCheckStrictly)
const descendants = node => flatten(node.children).map(child => child.id)
function isChecked(node, selected) { const children = descendants(node); return selected.includes(node.id) || (linked.value && children.length > 0 && children.every(id => selected.includes(id))) }
function isPartial(node, selected) { return linked.value && !isChecked(node, selected) && descendants(node).some(id => selected.includes(id)) }
function toggleNode(node, checked, mode) {
  if (!props.loaded || props.loading) return
  const target = mode === 'menu' ? selectedMenus : selectedDepts
  const branch = linked.value ? [node.id, ...descendants(node)] : [node.id]
  target.value = checked ? [...new Set([...target.value, ...branch])] : target.value.filter(id => !branch.includes(id) && (!linked.value || !node.ancestors.includes(id)))
}
function withAncestors(selected) { if (!linked.value) return [...selected]; const result = new Set(selected); flatMenus.value.forEach(node => { if (selected.includes(node.id)) node.ancestors.forEach(id => result.add(id)) }); return [...result] }
watch(() => [props.visible, props.checkedMenuIds, props.checkedDeptIds, props.dataScope], () => { if (props.visible) { selectedMenus.value = [...props.checkedMenuIds]; selectedDepts.value=[...props.checkedDeptIds]; selectedScope.value = String(props.dataScope || '1') } }, { immediate: true, deep: true })
function cancel() { emit('update:visible', false) }
function save() { if (!props.visible || !props.loaded || props.loading) return; emit('save', props.mode === 'menu' ? { menuIds: withAncestors(selectedMenus.value) } : { dataScope: selectedScope.value, deptIds: selectedScope.value === '2' ? withAncestors(selectedDepts.value) : [] }) }
</script>

<style scoped>
.permission-mask{position:fixed;z-index:10020;inset:0;display:grid;place-items:center;padding:16px;background:rgb(45 33 27 / 42%);backdrop-filter:blur(5px)}.permission-dialog{width:min(100%,620px);max-height:min(760px,calc(100vh - 32px));overflow:auto;display:grid;gap:16px;padding:24px;border:1px solid var(--lottery-border);border-radius:var(--lottery-radius-xl);background:var(--lottery-paper);box-shadow:var(--lottery-shadow);color:var(--lottery-ink)}header{display:flex;justify-content:space-between;gap:16px}h2,p{margin:0}.eyebrow{color:var(--lottery-orange);font-size:13px;font-weight:800}.hint{color:var(--lottery-muted);line-height:1.65}.scope-warning{padding:10px 12px;border-radius:10px;background:#fff5e8;color:#985315;line-height:1.6}header button{min-width:44px;min-height:44px;border:0;background:transparent;color:var(--lottery-muted);font-size:28px;cursor:pointer}.menu-list{display:grid;gap:8px;padding:8px;border:1px solid var(--lottery-border);border-radius:14px}.check-row,.scope-option{display:flex;min-height:44px;align-items:center;gap:11px;padding:8px 10px;border-radius:10px;cursor:pointer}.check-row:hover,.scope-option:hover{background:var(--lottery-orange-soft)}input{accent-color:var(--lottery-orange)}.scope-option{align-items:flex-start;border:1px solid var(--lottery-border)}.scope-option span{display:grid;gap:3px}.scope-option small{color:var(--lottery-muted)}footer{display:flex;justify-content:flex-end;gap:10px}footer button{min-height:44px;border-radius:12px;padding:0 18px;font-weight:700;cursor:pointer}.cancel{border:1px solid var(--lottery-border);background:var(--lottery-paper);color:var(--lottery-ink)}.confirm{border:0;background:var(--lottery-orange);color:#fff}.confirm:disabled{cursor:not-allowed;opacity:.5}@media(max-width:600px){.permission-dialog{padding:20px}.permission-dialog footer button{flex:1}}
</style>
