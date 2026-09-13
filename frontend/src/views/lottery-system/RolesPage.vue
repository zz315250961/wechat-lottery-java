<template>
  <section class="lottery-system-page">
    <LotteryConfirmDialog ref="confirmBox" />
    <LotteryPageHeader title="角色权限" subtitle="维护真实若依角色、菜单权限与数据范围。"><template #actions><button type="button" class="orange-button" data-action="add-role" @click="openCreate">新增角色</button></template></LotteryPageHeader>
    <form class="toolbar" data-action="search-roles" @submit.prevent="loadRoles"><el-input v-model="keyword" data-field="search-role" aria-label="搜索角色" clearable placeholder="搜索角色名称或标识" /><button type="submit" class="soft-button">查询</button></form>
    <div class="data-table" role="region" aria-label="角色列表"><table><thead><tr><th>角色名称</th><th>权限标识</th><th>排序</th><th>数据权限</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead><tbody><tr v-for="role in roles" :key="role.roleId"><td>{{ role.roleName }}</td><td>{{ role.roleKey }}</td><td>{{ role.roleSort }}</td><td>{{ scopeLabel(role.dataScope) }}</td><td><span class="status" :class="role.status === '0' ? 'on' : 'off'">{{ role.status === '0' ? '已启用' : '已停用' }}</span></td><td>{{ role.createTime || '—' }}</td><td class="row-actions"><button type="button" :data-action="`edit-${role.roleId}`" @click="openEdit(role)">编辑</button><button type="button" :data-action="`menus-${role.roleId}`" @click="openMenus(role)">菜单权限</button><button type="button" :data-action="`scope-${role.roleId}`" @click="openScope(role)">数据权限</button><button type="button" :data-action="`toggle-${role.roleId}`" @click="toggleStatus(role)">{{ role.status === '0' ? '停用' : '启用' }}</button><button type="button" class="danger-link" :data-action="`delete-${role.roleId}`" @click="removeRole(role)">删除</button></td></tr></tbody></table><p v-if="!loading && !roles.length" class="empty">没有符合条件的角色</p></div>
    <div v-if="roleDialogVisible" class="dialog-mask" @click.self="roleDialogVisible = false"><form class="dialog-card" data-action="role-form" @submit.prevent="saveRole"><header><div><p class="eyebrow">{{ editing ? '编辑角色' : '新增角色' }}</p><h2>{{ editing ? '维护角色信息' : '创建角色' }}</h2></div><button type="button" class="close" @click="roleDialogVisible = false">×</button></header><label>角色名称<el-input v-model="draft.roleName" data-field="role-name" required maxlength="30" /></label><label>权限标识<el-input v-model="draft.roleKey" data-field="role-key" required maxlength="100" /></label><label>显示排序<el-input v-model="draft.roleSort" data-field="role-sort" required /></label><footer><button type="button" class="soft-button" @click="roleDialogVisible = false">取消</button><button type="submit" class="orange-button">保存角色</button></footer></form></div>
    <LotteryPermissionDialog :visible="permissionVisible" :title="permissionTitle" :mode="permissionMode" :menus="permissionMenus" :checked-menu-ids="checkedMenuIds" :checked-dept-ids="checkedDeptIds" :menu-check-strictly="!!permissionRole?.menuCheckStrictly" :dept-check-strictly="!!permissionRole?.deptCheckStrictly" :data-scope="permissionRole?.dataScope" :loading="permissionLoading" :loaded="permissionLoaded" :error="permissionError" @update:visible="setPermissionVisible" @save="savePermission" />
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import LotteryPageHeader from '@/components/lottery/LotteryPageHeader.vue'
import LotteryConfirmDialog from '@/components/lottery/LotteryConfirmDialog.vue'
import LotteryPermissionDialog from '@/components/lottery/LotteryPermissionDialog.vue'
import { addRole, changeRoleStatus, dataScope, delRole, deptTreeSelect, getRole, listRole, updateRole } from '@/api/system/role'
import { roleMenuTreeselect } from '@/api/system/menu'

const roles = ref([]), loading = ref(false), keyword = ref(''), confirmBox = ref(), roleDialogVisible = ref(false), editing = ref(false), permissionVisible = ref(false), permissionMode = ref('menu'), permissionMenus = ref([]), checkedMenuIds = ref([]), checkedDeptIds = ref([]), permissionRole = ref(), permissionLoading = ref(false), permissionLoaded = ref(false), permissionError = ref(''), permissionRequest = ref(0)
const draft = ref(emptyDraft())
const scopeNames = { '1': '全部数据', '2': '自定数据', '3': '本部门', '4': '本部门及下级', '5': '仅本人' }
const permissionTitle = computed(() => permissionMode.value === 'menu' ? `配置“${permissionRole.value?.roleName || ''}”的菜单权限` : `配置“${permissionRole.value?.roleName || ''}”的数据权限`)
function emptyDraft() { return { roleId: undefined, roleName: '', roleKey: '', roleSort: 1, status: '0', dataScope: '1', menuIds: [], deptIds: [], menuCheckStrictly: true, deptCheckStrictly: true } }
function rows(response) { return response?.rows ?? response?.data?.rows ?? [] }
function scopeLabel(scope) { return scopeNames[String(scope ?? '1')] || '全部数据' }
async function loadRoles() { loading.value = true; try { const query = { pageNum: 1, pageSize: 100 }; if (keyword.value.trim()) query.roleName = keyword.value.trim(); roles.value = rows(await listRole(query)) } catch (error) { ElMessage.error(error?.message || '角色加载失败') } finally { loading.value = false } }
function openCreate() { editing.value = false; draft.value = emptyDraft(); roleDialogVisible.value = true }
async function openEdit(role) {
  try {
    const [roleResponse, menuResponse] = await Promise.all([getRole(role.roleId), roleMenuTreeselect(role.roleId)])
    const menuData = menuResponse?.data ?? menuResponse
    editing.value = true
    draft.value = { ...emptyDraft(), ...role, ...(roleResponse?.data ?? roleResponse), roleId: role.roleId }
    draft.value.menuIds = selectedWithAncestors(menuData.menus, menuData.checkedKeys, draft.value.menuCheckStrictly)
    roleDialogVisible.value = true
  } catch (error) { ElMessage.error(error?.message || '角色详情加载失败') }
}
function rolePayload() { return { roleId: draft.value.roleId, roleName: draft.value.roleName.trim(), roleKey: draft.value.roleKey.trim(), roleSort: Number(draft.value.roleSort), status: String(draft.value.status ?? '0'), dataScope: String(draft.value.dataScope ?? '1'), menuIds: draft.value.menuIds ?? [], deptIds: draft.value.deptIds ?? [], menuCheckStrictly: !!draft.value.menuCheckStrictly, deptCheckStrictly: !!draft.value.deptCheckStrictly } }
async function saveRole() { try { const payload = rolePayload(); if (editing.value) await updateRole(payload); else { delete payload.roleId; await addRole(payload) }; ElMessage.success(editing.value ? '角色已保存' : '角色已创建'); roleDialogVisible.value = false; await loadRoles() } catch (error) { ElMessage.error(error?.message || '角色保存失败') } }
function selectedWithAncestors(tree, selectedIds, linked) { if (!linked) return [...(selectedIds ?? [])]; const selected = new Set(selectedIds ?? []); const resolved = new Set(selected); const visit = (nodes, ancestors = []) => (nodes ?? []).forEach(node => { const path = [...ancestors, node.id]; if (selected.has(node.id)) ancestors.forEach(id => resolved.add(id)); visit(node.children, path) }); visit(tree); return [...resolved] }
function clearPermission() { permissionMenus.value=[]; checkedMenuIds.value=[]; checkedDeptIds.value=[]; permissionLoaded.value=false; permissionError.value='' }
function setPermissionVisible(visible) { if (visible) { permissionVisible.value=true; return } permissionRequest.value++; permissionVisible.value=false; permissionLoading.value=false; clearPermission(); permissionRole.value=undefined }
function beginPermission(role, mode) { const id=++permissionRequest.value; permissionRole.value={...role}; permissionMode.value=mode; clearPermission(); permissionVisible.value=true; permissionLoading.value=true; return id }
async function openMenus(role) { const id=beginPermission(role,'menu'); try { const [detail,response]=await Promise.all([getRole(role.roleId),roleMenuTreeselect(role.roleId)]); if(id!==permissionRequest.value)return; permissionRole.value={...role,...(detail?.data??detail)}; const data=response?.data??response; permissionMenus.value=data.menus??[]; checkedMenuIds.value=data.checkedKeys??[]; permissionLoaded.value=true } catch(error) { if(id===permissionRequest.value) { permissionError.value=error?.message||'菜单权限加载失败'; ElMessage.error(permissionError.value) } } finally { if(id===permissionRequest.value)permissionLoading.value=false } }
async function openScope(role) { const id=beginPermission(role,'scope'); try { const [detail,response]=await Promise.all([getRole(role.roleId),deptTreeSelect(role.roleId)]); if(id!==permissionRequest.value)return; permissionRole.value={...role,...(detail?.data??detail)}; const data=response?.data??response; permissionMenus.value=data.depts??[]; checkedDeptIds.value=data.checkedKeys??[]; permissionLoaded.value=true } catch(error) { if(id===permissionRequest.value) { permissionError.value=error?.message||'数据权限加载失败'; ElMessage.error(permissionError.value) } } finally { if(id===permissionRequest.value)permissionLoading.value=false } }
async function savePermission(payload) {
  if (!permissionVisible.value || !permissionRole.value || !permissionLoaded.value || permissionLoading.value) return
  const id = permissionRequest.value
  permissionLoading.value = true
  try {
    const role = { ...permissionRole.value, menuCheckStrictly: !!permissionRole.value.menuCheckStrictly, deptCheckStrictly: !!permissionRole.value.deptCheckStrictly }
    if (permissionMode.value === 'menu') {
      await updateRole({ roleId: role.roleId, roleName: role.roleName, roleKey: role.roleKey, roleSort: role.roleSort, status: String(role.status ?? '0'), dataScope: String(role.dataScope ?? '1'), menuIds: selectedWithAncestors(permissionMenus.value, payload.menuIds, role.menuCheckStrictly), menuCheckStrictly: role.menuCheckStrictly, deptCheckStrictly: role.deptCheckStrictly })
    } else {
      const deptIds = payload.dataScope === '2' ? (payload.deptIds ?? checkedDeptIds.value) : []
      await dataScope({ roleId: role.roleId, dataScope: payload.dataScope, deptIds: selectedWithAncestors(permissionMenus.value, deptIds, role.deptCheckStrictly), menuCheckStrictly: role.menuCheckStrictly, deptCheckStrictly: role.deptCheckStrictly })
    }
    if (id !== permissionRequest.value) return
    setPermissionVisible(false)
    await loadRoles()
  } catch (error) {
    if (id === permissionRequest.value) ElMessage.error(error?.message || '权限保存失败')
  } finally {
    if (id === permissionRequest.value) permissionLoading.value = false
  }
}
async function toggleStatus(role) { const next = role.status === '0' ? '1' : '0'; if (next === '1' && !await confirmBox.value.open({ title: '停用角色', message: `停用“${role.roleName}”后，该角色不可继续使用。`, confirmText: '确认停用', danger: true })) return; try { await changeRoleStatus(role.roleId, next); ElMessage.success(next === '0' ? '角色已启用' : '角色已停用'); await loadRoles() } catch (error) { ElMessage.error(error?.message || '状态更新失败') } }
async function removeRole(role) { if (!await confirmBox.value.open({ title: '删除角色', message: `确定删除“${role.roleName}”吗？`, confirmText: '确认删除', danger: true })) return; try { await delRole(role.roleId); ElMessage.success('角色已删除'); await loadRoles() } catch (error) { ElMessage.error(error?.message || '删除角色失败') } }
onMounted(loadRoles)
</script>

<style scoped>
.lottery-system-page{display:grid;gap:20px;max-width:1280px;margin:auto;padding:var(--lottery-space-4) var(--lottery-space-5) 60px}.toolbar{display:flex;flex-wrap:wrap;gap:10px}.toolbar :deep(.el-input){width:min(320px,100%)}.toolbar :deep(.el-input__wrapper),.toolbar :deep(.el-select__wrapper),.dialog-card :deep(.el-input__wrapper),.dialog-card :deep(.el-select__wrapper){min-height:44px}.dialog-card :deep(.el-input),.dialog-card :deep(.el-select){min-height:44px}.orange-button,.soft-button,.row-actions button{min-height:44px;border-radius:12px;padding:0 16px;font-weight:700;cursor:pointer}.orange-button{border:0;background:var(--lottery-orange);color:#fff}.soft-button{border:1px solid var(--lottery-border);background:var(--lottery-paper);color:var(--lottery-ink)}.data-table{overflow:auto;border:1px solid var(--lottery-border);border-radius:var(--lottery-radius-lg);background:var(--lottery-paper);box-shadow:var(--lottery-shadow)}table{width:100%;min-width:1030px;border-collapse:collapse;text-align:left}th,td{padding:15px 16px;border-bottom:1px solid var(--lottery-border)}th{background:#faf8f5;color:var(--lottery-muted);font-size:13px}.row-actions{display:flex;flex-wrap:wrap;gap:8px}.row-actions button{padding:0;border:0;background:transparent;color:var(--lottery-orange)}.row-actions .danger-link{color:#c64735}.status{display:inline-flex;min-height:26px;align-items:center;padding:0 9px;border-radius:99px;font-size:12px;font-weight:700}.status.on{background:#edf8ef;color:#267345}.status.off{background:#fff0ec;color:#bd4938}.empty{padding:32px;text-align:center;color:var(--lottery-muted)}.dialog-mask{position:fixed;z-index:10010;inset:0;display:grid;place-items:center;padding:16px;background:rgb(45 33 27 / 42%);backdrop-filter:blur(4px)}.dialog-card{width:min(100%,500px);max-height:min(700px,calc(100vh - 32px));overflow:auto;display:grid;gap:15px;padding:24px;border:1px solid var(--lottery-border);border-radius:var(--lottery-radius-xl);background:var(--lottery-paper);box-shadow:var(--lottery-shadow)}.dialog-card header{display:flex;justify-content:space-between;gap:12px}.dialog-card h2,.dialog-card p{margin:0}.eyebrow{color:var(--lottery-orange);font-size:13px;font-weight:800}.close{min-width:44px;min-height:44px;border:0;background:transparent;color:var(--lottery-muted);font-size:28px;cursor:pointer}.dialog-card label{display:grid;gap:7px;color:var(--lottery-ink);font-size:14px;font-weight:700}.dialog-card footer{display:flex;justify-content:flex-end;gap:10px}@media(max-width:600px){.lottery-system-page{padding:20px 16px 44px}.toolbar>*{width:100%}.toolbar :deep(.el-input){width:100%}.dialog-card{padding:20px}.dialog-card footer>*{flex:1}.row-actions{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:4px}.row-actions button{width:100%}}
</style>
