export const PORTAL_TITLES = Object.freeze({
  consumer: '幸运抽奖',
  admin: '幸运抽奖｜活动管理后台',
  merchant: '幸运抽奖｜商户核销工作台'
})

export function usePortalTitle(portal) {
  if (!Object.hasOwn(PORTAL_TITLES, portal)) throw new Error(`Unknown portal: ${portal}`)
  document.title = PORTAL_TITLES[portal]
}

export function resolvePortalForRoute(to) {
  const portal = to?.matched?.find((route) => Object.hasOwn(PORTAL_TITLES, route.meta?.portal))?.meta?.portal
  return portal || null
}

export function attachPortalTitle(router) {
  router.afterEach((to) => {
    const portal = resolvePortalForRoute(to)
    if (portal) usePortalTitle(portal)
  })
}
