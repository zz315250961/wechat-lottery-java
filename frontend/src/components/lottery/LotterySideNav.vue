<template>
  <div v-if="mobileOpen" class="nav-mask" aria-hidden="true" @click="closeMobile" />
  <aside class="lottery-side-nav" :class="{ 'is-collapsed': collapsed, 'is-mobile-open': mobileOpen }" :aria-hidden="drawerHidden ? 'true' : undefined" :inert="drawerHidden ? true : undefined">
    <router-link class="brand" to="/admin/prizes" aria-label="幸运抽奖活动管理后台" @click="onBrandClick">
      <span class="brand-icon"><Gift :size="25" /></span>
      <span class="brand-copy"><b>幸运抽奖</b><small>活动管理后台</small></span>
    </router-link>

    <nav aria-label="抽奖后台导航">
      <template v-for="item in items" :key="item.path">
        <p v-if="item.group">{{ item.group }}</p>
        <router-link :to="item.path" :title="collapsed ? item.label : undefined" @click="closeMobile">
          <component :is="icons[item.icon]" :size="19" /><span>{{ item.label }}</span>
        </router-link>
      </template>
    </nav>

    <footer>
      <a href="/" target="_blank" rel="noopener"><ExternalLink :size="18" /><span>打开抽奖页面</span></a>
      <button type="button" @click="emit('logout')"><LogOut :size="18" /><span>退出登录</span></button>
      <button class="collapse-button" type="button" :aria-label="collapsed ? '展开菜单' : '收起菜单'" @click="emit('toggle-collapse')">
        <component :is="collapsed ? ChevronRight : ChevronLeft" :size="18" /><span>{{ collapsed ? '展开菜单' : '收起菜单' }}</span>
      </button>
    </footer>
  </aside>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Activity, Bell, ChevronLeft, ChevronRight, CircleUserRound, Database, ExternalLink, FileClock, Gift, ListChecks, LogOut, Palette, Server, ShieldCheck, SlidersHorizontal, Store, TicketCheck, UsersRound } from 'lucide-vue-next'

const props = defineProps({
  items: { type: Array, required: true },
  collapsed: { type: Boolean, default: false },
  mobileOpen: { type: Boolean, default: false }
})

const emit = defineEmits(['update:mobileOpen', 'brand-click', 'logout', 'toggle-collapse'])
const icons = { Activity, Bell, CircleUserRound, Database, FileClock, Gift, ListChecks, Palette, Server, ShieldCheck, SlidersHorizontal, Store, TicketCheck, UsersRound }
const isMobile = ref(false)
let mediaQuery

function syncViewport() {
  isMobile.value = mediaQuery?.matches ?? false
}

onMounted(() => {
  mediaQuery = window.matchMedia('(max-width: 767px)')
  syncViewport()
  mediaQuery.addEventListener?.('change', syncViewport)
})

onBeforeUnmount(() => mediaQuery?.removeEventListener?.('change', syncViewport))

const drawerHidden = computed(() => isMobile.value && !props.mobileOpen)

function closeMobile() {
  emit('update:mobileOpen', false)
}

function onBrandClick() {
  emit('brand-click')
  closeMobile()
}
</script>

<style scoped>
.lottery-side-nav { position: fixed; z-index: 20; inset: 0 auto 0 0; display: flex; width: var(--nav-width); box-sizing: border-box; flex-direction: column; padding: 22px 14px 16px; background: var(--lottery-paper); border-right: 1px solid var(--lottery-border); transition: width .2s ease, transform .22s ease; }
.brand { display: flex; min-height: 44px; align-items: center; gap: 12px; padding: 6px 4px 16px; color: var(--lottery-ink); text-decoration: none; }
.brand-icon { display: grid; width: 46px; height: 46px; flex: none; border-radius: var(--lottery-radius-md); background: var(--lottery-orange-soft); color: var(--lottery-orange); place-items: center; }
.brand b, .brand small { display: block; }
.brand b { font-size: 18px; }
.brand small { margin-top: 4px; color: var(--lottery-muted); font-size: 12px; }
nav { min-height: 0; overflow-y: auto; padding-right: 3px; }
nav p { margin: 14px 12px 5px; color: var(--lottery-muted); font-size: 11px; letter-spacing: .08em; }
nav a { display: flex; min-height: 44px; box-sizing: border-box; align-items: center; gap: 12px; padding: 0 13px; border-radius: var(--lottery-radius-sm); color: var(--lottery-muted); text-decoration: none; }
nav a:hover { background: #fff8f1; color: var(--lottery-orange); }
nav a.router-link-active { background: var(--lottery-orange-soft); color: var(--lottery-orange); font-weight: 600; }
footer { display: grid; gap: 2px; margin-top: auto; padding-top: 12px; border-top: 1px solid var(--lottery-border); }
footer a, footer button { display: flex; min-height: 44px; align-items: center; gap: 10px; padding: 0 11px; border: 0; background: transparent; color: var(--lottery-muted); text-decoration: none; font: inherit; cursor: pointer; }
.collapse-button { border-top: 1px solid var(--lottery-border) !important; }
.is-collapsed { padding-inline: 10px; }
.is-collapsed .brand-copy, .is-collapsed nav span, .is-collapsed nav p, .is-collapsed footer span { display: none; }
.is-collapsed .brand { justify-content: center; }
.is-collapsed nav a, .is-collapsed footer a, .is-collapsed footer button { justify-content: center; padding-inline: 0; }
.is-collapsed nav { overflow-x: hidden; }
.nav-mask { display: none; }
@media (max-width: 767px) {
  .lottery-side-nav { width: min(86vw, 300px); transform: translateX(-105%); box-shadow: var(--lottery-shadow); }
  .lottery-side-nav.is-mobile-open { transform: translateX(0); }
  .lottery-side-nav.is-collapsed { width: min(86vw, 300px); padding-inline: 14px; }
  .lottery-side-nav.is-collapsed .brand-copy, .lottery-side-nav.is-collapsed nav span, .lottery-side-nav.is-collapsed nav p, .lottery-side-nav.is-collapsed footer span { display: initial; }
  .lottery-side-nav.is-collapsed .brand { justify-content: flex-start; }
  .lottery-side-nav.is-collapsed nav a, .lottery-side-nav.is-collapsed footer a, .lottery-side-nav.is-collapsed footer button { justify-content: flex-start; padding-inline: 13px; }
  .nav-mask { position: fixed; z-index: 19; inset: 0; display: block; background: rgb(46 33 27 / 45%); }
}
</style>
