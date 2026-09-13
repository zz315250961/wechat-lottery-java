<template>
  <div class="lottery-layout" :class="{ collapsed }">
    <LotterySideNav :items="navItems" :collapsed="collapsed" :mobile-open="mobileOpen" @update:mobile-open="mobileOpen = $event" @brand-click="recordBrandClick" @logout="logout" @toggle-collapse="toggleCollapse" />
    <section class="workspace">
      <header class="mobile-header">
        <button type="button" aria-label="打开菜单" @click="mobileOpen = true"><Menu :size="22" /></button>
        <b>{{ route.meta.title }}</b>
      </header>
      <router-view />
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Menu } from 'lucide-vue-next'
import useUserStore from '@/store/modules/user'
import { usePortalTitle } from '@/composables/usePortalTitle'
import LotterySideNav from '@/components/lottery/LotterySideNav.vue'

usePortalTitle('admin')

const route = useRoute()
const router = useRouter()
const mobileOpen = ref(false)
const collapsed = ref(localStorage.getItem('lottery-admin-collapsed') === '1')
const brandClicks = ref([])
const navItems = [
  { group: '活动运营', path: '/admin/prizes', label: '奖池管理', icon: 'Gift' },
  { path: '/admin/activity', label: '活动设置', icon: 'SlidersHorizontal' },
  { path: '/admin/appearance', label: '外观主题', icon: 'Palette' },
  { path: '/admin/rules', label: '公告规则', icon: 'Bell' },
  { path: '/admin/merchants', label: '商户管理', icon: 'Store' },
  { path: '/admin/users', label: '参与用户', icon: 'UsersRound' },
  { path: '/admin/tickets', label: '中奖与核销', icon: 'TicketCheck' },
  { group: '系统管理', path: '/admin/accounts', label: '后台用户管理', icon: 'CircleUserRound' },
  { path: '/admin/roles', label: '角色权限', icon: 'ShieldCheck' },
  { path: '/admin/online', label: '在线用户', icon: 'Activity' },
  { path: '/admin/server', label: '服务器监控', icon: 'Server' },
  { path: '/admin/cache', label: 'Redis 缓存', icon: 'Database' },
  { path: '/admin/login-log', label: '登录日志', icon: 'FileClock' },
  { path: '/admin/operation-log', label: '操作日志', icon: 'ListChecks' }
]

async function logout() {
  await useUserStore().logOut()
  router.replace('/admin/accounts')
}

function toggleCollapse() {
  collapsed.value = !collapsed.value
  localStorage.setItem('lottery-admin-collapsed', collapsed.value ? '1' : '0')
}

function recordBrandClick() {
  const now = Date.now()
  brandClicks.value = [...brandClicks.value.filter(v => now - v < 3000), now]
  if (brandClicks.value.length >= 5) {
    brandClicks.value = []
    router.push('/admin/directives')
  }
}
</script>

<style scoped>
.lottery-layout { --nav-width: 244px; min-height: 100vh; background: var(--lottery-paper); color: var(--lottery-ink); }
.workspace { min-height: 100vh; margin-left: var(--nav-width); }
.workspace :deep(.app-container) { max-width: 1540px; box-sizing: border-box; margin-inline: auto; padding: var(--lottery-space-4) var(--lottery-space-5) 60px; }
.workspace :deep(.el-button--primary) { --el-button-bg-color: var(--lottery-orange); --el-button-border-color: var(--lottery-orange); --el-button-hover-bg-color: #dc5a12; --el-button-hover-border-color: #dc5a12; }
.workspace :deep(.el-input__wrapper), .workspace :deep(.el-select__wrapper), .workspace :deep(.el-textarea__inner) { border-radius: var(--lottery-radius-sm); }
.workspace :deep(.el-table) { --el-table-header-bg-color: #faf8f5; --el-table-row-hover-bg-color: #fff9f1; --el-table-border-color: var(--lottery-border); }
.workspace :deep(.pagination-container .el-pagination.is-background .el-pager li.is-active) { background: var(--lottery-orange); }
.mobile-header { display: none; }
.collapsed { --nav-width: 78px; }
@media (max-width: 767px) {
  .workspace { margin-left: 0; padding-top: 58px; }
  .workspace :deep(.app-container) { padding: var(--lottery-space-3) 14px 40px; }
  .mobile-header { position: fixed; z-index: 15; inset: 0 0 auto 0; display: flex; height: 58px; align-items: center; gap: var(--lottery-space-2); padding: 0 max(16px, env(safe-area-inset-left)); background: var(--lottery-paper); border-bottom: 1px solid var(--lottery-border); }
  .mobile-header button { display: grid; width: 44px; height: 44px; border: 0; background: transparent; color: var(--lottery-ink); place-items: center; }
}
</style>
