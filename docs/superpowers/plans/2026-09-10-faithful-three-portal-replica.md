# 幸运抽奖三端架构级复刻 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不改变 Vue 3／若依 Spring Boot 技术栈和真实业务能力的前提下，把用户端、管理员端、商户端复刻为旧版幸运抽奖界面，并严格隔离管理员、商户、参与用户三类数据。

**Architecture:** 保留现有 REST API、鉴权、数据库与 Redis 能力，在 Vue 3 内建立一套旧版暖橙视觉基础和三端独立壳层。业务后台页面拆为可独立理解的页面组件；若依系统页面继续调用原接口，但用项目组件重写视图，并在后端查询层明确管理员账号边界。

**Tech Stack:** Java 8、Spring Boot 2.5.15、MyBatis-Plus 3.5.7、MySQL 8、Redis、Vue 3.5、Vite 6、JavaScript、Element Plus、JUnit 5、Maven

**Spec:** `wechat-lottery/docs/superpowers/specs/2026-09-10-admin-merchant-faithful-replica-design.md`

## Global Constraints

- 不复制旧版 React/Node 源码，只把 `F:\Desktop\codex\抽奖` 旧版运行界面和用户提供截图作为视觉、信息结构与交互基准。
- 用户端标题固定为 `幸运抽奖`；管理员端固定为 `幸运抽奖｜活动管理后台`；商户端固定为 `幸运抽奖｜商户核销工作台`。
- 管理员业务页和若依系统能力页统一使用旧版暖橙壳层，不显示若依默认 Layout、TagsView、组织树或演示首页。
- `sys_user` 管理员只在后台用户管理出现；绑定 `lottery_merchant` 或商户角色的账号只在商户管理出现；`lottery_user` 只在参与用户出现。
- 角色、权限、状态、在线会话、服务器、Redis、登录日志和操作日志读取真实接口与数据库，不制造演示数据。
- 桌面端逐页对照旧版；手机端只做响应式重排，不改变视觉语言；触控目标不小于 44px。
- 每个任务只提交该任务列出的文件，不提交 `交付/` 等现有无关未跟踪内容。

---

### Task 1: 固定三端标题与旧版视觉基础

**Files:**
- Create: `wechat-lottery/frontend/src/styles/lottery-tokens.css`
- Create: `wechat-lottery/frontend/src/composables/usePortalTitle.js`
- Create: `wechat-lottery/frontend/src/components/lottery/LotteryPageHeader.vue`
- Modify: `wechat-lottery/frontend/src/main.js`
- Modify: `wechat-lottery/frontend/src/router/index.js`
- Test: `tests/java-rebuild-ui.test.mjs`

**Interfaces:**
- Produces: `PORTAL_TITLES: Readonly<Record<'consumer'|'admin'|'merchant', string>>`
- Produces: `usePortalTitle(portal: 'consumer'|'admin'|'merchant'): void`
- Produces: `<LotteryPageHeader title subtitle actions>`，供所有后台页面复用。

- [ ] **Step 1: 写标题映射和若依视觉泄漏的失败测试**

```js
test('三端使用固定浏览器标题', async () => {
  const source = await read('wechat-lottery/frontend/src/composables/usePortalTitle.js')
  assert.match(source, /consumer:\s*'幸运抽奖'/)
  assert.match(source, /admin:\s*'幸运抽奖｜活动管理后台'/)
  assert.match(source, /merchant:\s*'幸运抽奖｜商户核销工作台'/)
})
```

- [ ] **Step 2: 运行测试并确认因文件不存在而失败**

Run: `node --test tests/java-rebuild-ui.test.mjs`
Expected: FAIL，指出 `usePortalTitle.js` 不存在。

- [ ] **Step 3: 建立固定标题接口和 CSS token**

```js
export const PORTAL_TITLES = Object.freeze({
  consumer: '幸运抽奖',
  admin: '幸运抽奖｜活动管理后台',
  merchant: '幸运抽奖｜商户核销工作台'
})
export function usePortalTitle(portal) {
  if (!PORTAL_TITLES[portal]) throw new Error(`Unknown portal: ${portal}`)
  document.title = PORTAL_TITLES[portal]
}
```

在 `lottery-tokens.css` 定义并只从此处消费 `--lottery-orange`、`--lottery-orange-soft`、`--lottery-paper`、`--lottery-ink`、`--lottery-muted`、`--lottery-border`、`--lottery-shadow`、圆角和 8px 间距阶梯；在三端路由进入时调用对应标题函数。

- [ ] **Step 4: 运行标题测试与生产构建**

Run: `node --test tests/java-rebuild-ui.test.mjs`
Expected: PASS。

Run: `npm run build:prod`
Working directory: `wechat-lottery/frontend`
Expected: Vite 构建成功且无未解析资源。

- [ ] **Step 5: 提交基础层**

```bash
git add tests/java-rebuild-ui.test.mjs wechat-lottery/frontend/src/styles/lottery-tokens.css wechat-lottery/frontend/src/composables/usePortalTitle.js wechat-lottery/frontend/src/components/lottery/LotteryPageHeader.vue wechat-lottery/frontend/src/main.js wechat-lottery/frontend/src/router/index.js
git commit -m "feat: establish faithful lottery portal foundation"
```

### Task 2: 复刻管理员统一壳层和隐藏入口

**Files:**
- Modify: `wechat-lottery/frontend/src/layout/LotteryAdminLayout.vue`
- Create: `wechat-lottery/frontend/src/components/lottery/LotterySideNav.vue`
- Create: `wechat-lottery/frontend/src/components/lottery/LotteryConfirmDialog.vue`
- Modify: `wechat-lottery/frontend/src/components/LotteryConfirm.vue`
- Test: `tests/java-rebuild-ui.test.mjs`

**Interfaces:**
- Consumes: `usePortalTitle('admin')`、Task 1 的 CSS tokens。
- Produces: `<LotterySideNav items collapsed mobileOpen>`；固定 14 个可见菜单和“三击品牌进入 `/admin/directives`”行为。
- Produces: `open({ title, message, confirmText, danger }): Promise<boolean>`。

- [ ] **Step 1: 写导航顺序、隐藏定向入口和移动抽屉失败测试**

测试精确断言七个业务菜单、七个系统菜单的文本顺序，断言 `定向中奖` 不在 `navItems`，并断言壳层存在 `aria-label="抽奖后台导航"` 与移动抽屉状态。

- [ ] **Step 2: 运行测试确认当前壳层未满足完整契约**

Run: `node --test tests/java-rebuild-ui.test.mjs`
Expected: FAIL，至少缺少独立 `LotterySideNav.vue`。

- [ ] **Step 3: 提取侧栏并实现桌面折叠、手机抽屉与三击入口**

```js
function recordBrandClick() {
  const now = Date.now()
  brandClicks.value = [...brandClicks.value.filter(v => now - v < 3000), now]
  if (brandClicks.value.length >= 3) {
    brandClicks.value = []
    router.push('/admin/directives')
  }
}
```

桌面宽度使用 244px／78px 两态；`max-width: 767px` 时使用覆盖式抽屉和遮罩；内容区不得渲染若依 Navbar、TagsView、Breadcrumb 或默认首页。

- [ ] **Step 4: 运行测试、构建并手工验证三个断点**

Run: `node --test tests/java-rebuild-ui.test.mjs && npm run build:prod`
Working directory for npm: `wechat-lottery/frontend`
Expected: 全部 PASS。

Manual: 在 1440×900、390×844、375×667 下验证导航顺序、折叠、抽屉关闭和三击入口。

- [ ] **Step 5: 提交壳层**

```bash
git add tests/java-rebuild-ui.test.mjs wechat-lottery/frontend/src/layout/LotteryAdminLayout.vue wechat-lottery/frontend/src/components/lottery/LotterySideNav.vue wechat-lottery/frontend/src/components/lottery/LotteryConfirmDialog.vue wechat-lottery/frontend/src/components/LotteryConfirm.vue
git commit -m "feat: replicate the legacy admin shell"
```

### Task 3: 拆分并复刻奖池、活动、外观和公告规则

**Files:**
- Create: `wechat-lottery/frontend/src/views/lottery-admin/PrizePoolPage.vue`
- Create: `wechat-lottery/frontend/src/views/lottery-admin/ActivitySettingsPage.vue`
- Create: `wechat-lottery/frontend/src/views/lottery-admin/AppearancePage.vue`
- Create: `wechat-lottery/frontend/src/views/lottery-admin/AnnouncementRulesPage.vue`
- Create: `wechat-lottery/frontend/src/composables/useDirtySnapshot.js`
- Modify: `wechat-lottery/frontend/src/views/lottery-admin/index.vue`
- Modify: `wechat-lottery/frontend/src/api/lottery/index.js`
- Test: `tests/java-rebuild-ui.test.mjs`

**Interfaces:**
- Produces: `useDirtySnapshot(model)` 返回 `{ capture(), isDirty(), reset() }`。
- Each page consumes: `overview()` 的真实 DTO，并只通过 `saveActivity`、`savePrize`、`movePrize`、`deletePrize`、`saveRules`、`uploadImage` 写入。

- [ ] **Step 1: 写四页字段完整性与脏状态失败测试**

奖池测试必须覆盖图片、商户、概率、库存、已抽中、已核销、个人上限、核销时间、启用、排序、删除、统一保存；活动、外观、公告分别断言规范第 4.2～4.4 节全部字段。

- [ ] **Step 2: 运行测试确认巨型单页尚未拆分**

Run: `node --test tests/java-rebuild-ui.test.mjs`
Expected: FAIL，四个页面组件不存在。

- [ ] **Step 3: 逐页迁移现有真实行为并复刻旧版布局**

保持 API 参数字段不变。奖池桌面用行内表格，手机用同字段卡片；活动和外观使用旧版设置卡片；规则列表支持新增、删除、排序、启停。首次 `capture()` 后 `isDirty()` 必须为 false，只有用户修改后才拦截离开。

- [ ] **Step 4: 运行测试、构建并用测试数据完成 CRUD 冒烟**

Run: `node --test tests/java-rebuild-ui.test.mjs`
Expected: PASS。

Run: `npm run build:prod`
Working directory: `wechat-lottery/frontend`
Expected: PASS。

Manual: 使用 `sql/test_data.sql` 数据验证添加草稿置顶、保存、排序、上传预览、启停与未保存提醒。

- [ ] **Step 5: 提交四个业务页面**

```bash
git add tests/java-rebuild-ui.test.mjs wechat-lottery/frontend/src/views/lottery-admin wechat-lottery/frontend/src/composables/useDirtySnapshot.js wechat-lottery/frontend/src/api/lottery/index.js
git commit -m "feat: faithfully rebuild lottery configuration pages"
```

### Task 4: 复刻商户、参与用户、中奖核销和定向中奖页面

**Files:**
- Create: `wechat-lottery/frontend/src/views/lottery-admin/MerchantManagementPage.vue`
- Create: `wechat-lottery/frontend/src/views/lottery-admin/ParticipantUsersPage.vue`
- Create: `wechat-lottery/frontend/src/views/lottery-admin/TicketRecordsPage.vue`
- Create: `wechat-lottery/frontend/src/views/lottery-admin/DirectedWinsPage.vue`
- Modify: `wechat-lottery/frontend/src/views/lottery-admin/index.vue`
- Modify: `wechat-lottery/frontend/src/api/lottery/index.js`
- Test: `tests/java-rebuild-ui.test.mjs`

**Interfaces:**
- Consumes: `/lottery/admin/overview|users|tickets|directives|merchants` 真实响应。
- Produces: 商户卡片独立保存；用户/奖券即时本地过滤；`DirectedWinsPage` 同行用户与奖品模糊选择。

- [ ] **Step 1: 写四页信息结构与数据归属失败测试**

断言商户页包含名称、账号、地址、使用规则、启用和密码；参与用户页包含昵称、手机号、OpenID、抽奖次数、奖券数、状态；奖券页包含奖品、用户、手机号、券码、状态、中奖/核销时间；定向页不出现在侧栏。

- [ ] **Step 2: 运行测试确认页面尚未独立**

Run: `node --test tests/java-rebuild-ui.test.mjs`
Expected: FAIL。

- [ ] **Step 3: 迁移真实交互并使用旧版卡片、工具栏、表格和反馈**

商户不使用若依用户通用弹窗；用户统计与奖券统计只展示接口返回的真实聚合值；所有删除、停用和定向配置操作使用 `LotteryConfirmDialog`。

- [ ] **Step 4: 运行测试、构建和跨页统计核对**

Run: `node --test tests/java-rebuild-ui.test.mjs`
Expected: PASS。

Run: `npm run build:prod`
Working directory: `wechat-lottery/frontend`
Expected: PASS。

Manual: 同一测试数据下核对奖池“已抽中/已核销”、参与用户奖券数、中奖核销记录三处一致。

- [ ] **Step 5: 提交数据业务页**

```bash
git add tests/java-rebuild-ui.test.mjs wechat-lottery/frontend/src/views/lottery-admin wechat-lottery/frontend/src/api/lottery/index.js
git commit -m "feat: rebuild lottery data management pages"
```

### Task 5: 后端强制隔离管理员与商户账号

**Files:**
- Create: `wechat-lottery/backend/ruoyi-system/src/test/java/com/ruoyi/system/service/AdminAccountScopeTest.java`
- Modify: `wechat-lottery/backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/SysUserController.java`
- Modify: `wechat-lottery/backend/ruoyi-system/src/main/java/com/ruoyi/system/mapper/SysUserMapper.java`
- Modify: `wechat-lottery/backend/ruoyi-system/src/main/resources/mapper/system/SysUserMapper.xml`
- Modify: `wechat-lottery/sql/test_data.sql`

**Interfaces:**
- Produces: `SysUserMapper.selectAdminUserList(SysUser filter): List<SysUser>`，只返回拥有管理员类角色且未绑定 `lottery_merchant` 的账号。
- Preserves: 若依用户新增、编辑、启停、重置密码和删除接口响应结构。

- [ ] **Step 1: 写 mapper 集成测试，证明 test1/test2/test3 不属于管理员列表**

```java
@Test
void adminListExcludesMerchantAccounts() {
    List<SysUser> users = mapper.selectAdminUserList(new SysUser());
    assertTrue(users.stream().noneMatch(u -> u.getUserName().matches("test[123]")));
    assertTrue(users.stream().allMatch(u -> u.getRoles().stream()
        .anyMatch(r -> "admin".equals(r.getRoleKey()))));
}
```

- [ ] **Step 2: 运行测试确认当前通用查询会混入商户**

Run: `mvn -pl ruoyi-system -am -Dtest=AdminAccountScopeTest test`
Working directory: `wechat-lottery/backend`
Expected: FAIL，`selectAdminUserList` 尚不存在或结果含商户。

- [ ] **Step 3: 增加专用查询并仅让后台用户页面使用**

SQL 使用 `EXISTS sys_user_role JOIN sys_role` 限定管理员类角色，并用 `NOT EXISTS lottery_merchant WHERE sys_user_id = u.user_id` 排除商户；不修改角色管理和鉴权使用的通用 `selectUserList`。

- [ ] **Step 4: 清理测试数据中的错误部门关联与乱码展示值**

`test_data.sql` 只保留可重复执行的真实项目角色关联；商户账号绑定真实商户记录；管理员账号不绑定商户。不得新增虚构部门树。

- [ ] **Step 5: 运行模块与全量后端测试**

Run: `mvn -pl ruoyi-system -am -Dtest=AdminAccountScopeTest test`
Expected: PASS。

Run: `mvn test`
Working directory: `wechat-lottery/backend`
Expected: 全部测试 PASS。

- [ ] **Step 6: 提交账号隔离**

```bash
git add wechat-lottery/backend/ruoyi-system/src/test/java/com/ruoyi/system/service/AdminAccountScopeTest.java wechat-lottery/backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/SysUserController.java wechat-lottery/backend/ruoyi-system/src/main/java/com/ruoyi/system/mapper/SysUserMapper.java wechat-lottery/backend/ruoyi-system/src/main/resources/mapper/system/SysUserMapper.xml wechat-lottery/sql/test_data.sql
git commit -m "fix: isolate admin and merchant account scopes"
```

### Task 6: 重写后台用户与角色权限视图

**Files:**
- Create: `wechat-lottery/frontend/src/views/lottery-system/AdminUsersPage.vue`
- Create: `wechat-lottery/frontend/src/views/lottery-system/RolesPage.vue`
- Create: `wechat-lottery/frontend/src/components/lottery/LotteryPermissionDialog.vue`
- Modify: `wechat-lottery/frontend/src/router/index.js`
- Modify: `wechat-lottery/frontend/src/api/system/user.js`
- Modify: `wechat-lottery/frontend/src/api/system/role.js`
- Test: `tests/java-rebuild-ui.test.mjs`

**Interfaces:**
- Consumes: 现有 `/system/user`、`/system/role`、角色菜单和数据权限接口。
- Produces: 暖橙后台用户表、角色表和居中权限弹窗；页面不请求部门树接口。

- [ ] **Step 1: 写禁止组织树、管理员字段和权限操作失败测试**

断言 `AdminUsersPage` 不导入 `TreePanel`、不调用 `deptTreeSelect`，并包含账号、昵称、手机号、角色、状态、创建时间；断言角色页保留菜单权限和数据权限操作。

- [ ] **Step 2: 运行测试确认仍指向若依默认页面**

Run: `node --test tests/java-rebuild-ui.test.mjs`
Expected: FAIL。

- [ ] **Step 3: 用专用 Vue 页面复用真实 API**

路由 `/admin/accounts` 和 `/admin/roles` 指向新页面。新增、编辑、启停、重置密码、删除、菜单权限与数据权限保持真实接口；所有表单和弹窗使用 Task 1/2 的项目组件及 token。

- [ ] **Step 4: 运行测试、构建与真实数据库冒烟**

Run: `node --test tests/java-rebuild-ui.test.mjs`
Expected: PASS。

Run: `npm run build:prod`
Working directory: `wechat-lottery/frontend`
Expected: PASS。

Manual: 管理员列表只见管理员；商户管理可见 test1/test2/test3；角色启停与数据库状态一致。

- [ ] **Step 5: 提交系统账号页面**

```bash
git add tests/java-rebuild-ui.test.mjs wechat-lottery/frontend/src/views/lottery-system wechat-lottery/frontend/src/components/lottery/LotteryPermissionDialog.vue wechat-lottery/frontend/src/router/index.js wechat-lottery/frontend/src/api/system/user.js wechat-lottery/frontend/src/api/system/role.js
git commit -m "feat: restyle real user and role administration"
```

### Task 7: 重写监控与日志视图但保留真实数据

**Files:**
- Create: `wechat-lottery/frontend/src/views/lottery-system/OnlineUsersPage.vue`
- Create: `wechat-lottery/frontend/src/views/lottery-system/ServerMonitorPage.vue`
- Create: `wechat-lottery/frontend/src/views/lottery-system/RedisMonitorPage.vue`
- Create: `wechat-lottery/frontend/src/views/lottery-system/LoginLogPage.vue`
- Create: `wechat-lottery/frontend/src/views/lottery-system/OperationLogPage.vue`
- Create: `wechat-lottery/frontend/src/components/lottery/LotteryDataTable.vue`
- Modify: `wechat-lottery/frontend/src/router/index.js`
- Test: `tests/java-rebuild-ui.test.mjs`

**Interfaces:**
- Consumes: 现有 `api/monitor/online.js`、`server.js`、`cache.js`、`logininfor.js`、`operlog.js`，不改变接口。
- Produces: `LotteryDataTable` 统一空态、加载态、错误态、分页与手机卡片插槽。

- [ ] **Step 1: 写五页真实 API 与无若依 Layout 失败测试**

逐页断言导入对应 monitor API，断言不导入若依 Navbar、Breadcrumb、TagsView，并检查日志筛选、分页、清理/强退等原有真实操作仍在。

- [ ] **Step 2: 运行测试确认路由仍使用若依默认视图**

Run: `node --test tests/java-rebuild-ui.test.mjs`
Expected: FAIL。

- [ ] **Step 3: 迁移监控和日志功能到暖橙卡片与表格组件**

服务器与 Redis 采用旧版摘要卡片；在线用户和日志采用筛选栏＋表格；API 失败显示同区域错误态和重试按钮，不用假数据填充。

- [ ] **Step 4: 运行测试、构建及断开 Redis 的错误态测试**

Run: `node --test tests/java-rebuild-ui.test.mjs`
Expected: PASS。

Run: `npm run build:prod`
Working directory: `wechat-lottery/frontend`
Expected: PASS。

Manual: Redis 可用时显示真实指标；不可用时显示明确错误和重试，不出现演示数字。

- [ ] **Step 5: 提交监控日志页面**

```bash
git add tests/java-rebuild-ui.test.mjs wechat-lottery/frontend/src/views/lottery-system wechat-lottery/frontend/src/components/lottery/LotteryDataTable.vue wechat-lottery/frontend/src/router/index.js
git commit -m "feat: restyle real monitoring and audit pages"
```

### Task 8: 一比一复刻商户登录、核销、记录和全屏扫码

**Files:**
- Create: `wechat-lottery/frontend/src/views/lottery/merchant/MerchantLogin.vue`
- Create: `wechat-lottery/frontend/src/views/lottery/merchant/RedeemWorkbench.vue`
- Create: `wechat-lottery/frontend/src/views/lottery/merchant/FullscreenScanner.vue`
- Create: `wechat-lottery/frontend/src/views/lottery/merchant/MerchantTicketRecords.vue`
- Modify: `wechat-lottery/frontend/src/views/lottery/Merchant.vue`
- Modify: `wechat-lottery/frontend/src/api/lottery/index.js`
- Test: `tests/java-rebuild-ui.test.mjs`

**Interfaces:**
- Consumes: 商户独立 7 天 Token、`merchantLogin`、`merchantTickets`、`redeemTicket`。
- Produces: `FullscreenScanner` emits `detected(code: string)` and `close()`；扫码成功只回填券码，不自动核销。

- [ ] **Step 1: 写商户流程和安全边界失败测试**

断言登录卡片、24 位券码校验、查询后奖品图与用户预览、二次确认、核销反馈、本店记录搜索；断言扫码组件使用全屏 fixed 容器并只 emit detected。

- [ ] **Step 2: 运行测试确认单文件实现尚未拆分**

Run: `node --test tests/java-rebuild-ui.test.mjs`
Expected: FAIL。

- [ ] **Step 3: 拆分状态组件并复刻旧版工作台**

品牌区显示真实商户名，浏览器标题仍固定；商户记录隐藏本店商户列；相机占满可视区域，关闭和权限错误按钮均不小于 44px。

- [ ] **Step 4: 运行测试、构建和真实核销冒烟**

Run: `node --test tests/java-rebuild-ui.test.mjs`
Expected: PASS。

Run: `npm run build:prod`
Working directory: `wechat-lottery/frontend`
Expected: PASS。

Manual: 分别验证合法、跨店、过期、重复核销和相机拒绝权限；确认数据库只在二次确认后改变。

- [ ] **Step 5: 提交商户端**

```bash
git add tests/java-rebuild-ui.test.mjs wechat-lottery/frontend/src/views/lottery wechat-lottery/frontend/src/api/lottery/index.js
git commit -m "feat: faithfully rebuild merchant redemption portal"
```

### Task 9: 校准用户端名称、响应式重排和三端视觉回归

**Files:**
- Modify: `wechat-lottery/frontend/src/views/lottery/Consumer.vue`
- Modify: `wechat-lottery/frontend/src/views/login.vue`
- Create: `wechat-lottery/docs/qa/faithful-replica-checklist.md`
- Create: `wechat-lottery/docs/qa/screenshots/.gitkeep`
- Modify: `tests/java-rebuild-ui.test.mjs`
- Modify: `wechat-lottery/README.md`

**Interfaces:**
- Consumes: 全部前述页面和真实测试数据。
- Produces: 逐页桌面／手机验收清单，记录基准状态、视口、账号类型、数据集、结果和偏差等级。

- [ ] **Step 1: 写三端禁用旧标题文案和 44px 触控目标失败测试**

断言源码不再出现 `校园幸运抽奖 · 用户端`、`校园幸运抽奖 · 商户核销端`、`校园幸运抽奖 · 管理端`；为抽屉、确认按钮、扫码关闭按钮增加可机器检查的最小高度规则。

- [ ] **Step 2: 运行测试并确认遗留标题导致失败**

Run: `node --test tests/java-rebuild-ui.test.mjs`
Expected: FAIL，路由或页面仍含旧标题。

- [ ] **Step 3: 修正用户端与管理员登录页品牌，不引入若依默认视觉**

用户端保留旧版转盘、底部导航、奖券和中奖弹窗；管理员登录页改为暖橙独立登录卡片。删除或断路由所有用户可达的若依演示首页。

- [ ] **Step 4: 建立逐页视觉 QA 表并完成截图对比**

清单至少覆盖 7 个业务页、7 个系统页、隐藏定向页、管理员登录、商户登录/工作台/扫码、用户首页/奖券页。每页在 1440×900 与 390×844 下记录：标题、布局、尺寸、颜色、间距、图片、表格/卡片、弹窗、空态、错误态和核心交互；P0/P1 偏差必须修复，P2 记录后修复。

- [ ] **Step 5: 运行全量验证**

Run: `node --test tests/java-rebuild-ui.test.mjs`
Working directory: repository root
Expected: PASS。

Run: `npm run build:prod`
Working directory: `wechat-lottery/frontend`
Expected: PASS。

Run: `mvn test`
Working directory: `wechat-lottery/backend`
Expected: 全部测试 PASS。

Run: `mvn install -DskipTests && mvn -pl ruoyi-admin clean package -DskipTests`
Working directory: `wechat-lottery/backend`
Expected: 聚合安装与最终运行包构建成功，避免嵌入本地仓库旧业务 JAR。

- [ ] **Step 6: 提交最终视觉校准与 QA 记录**

```bash
git add tests/java-rebuild-ui.test.mjs wechat-lottery/frontend/src/views/lottery/Consumer.vue wechat-lottery/frontend/src/views/login.vue wechat-lottery/docs/qa wechat-lottery/README.md
git commit -m "test: complete faithful three-portal visual acceptance"
```

## 完成判定

- 三端固定浏览器标题逐路由验证通过。
- 所有业务页、系统页、登录页和弹窗均无若依默认外观泄漏。
- 后台用户列表只包含管理员；test1/test2/test3 只在商户管理出现；参与用户只来自 `lottery_user`。
- 奖池、参与用户、中奖核销、商户记录统计使用同一真实数据口径。
- 管理员、商户和用户主要流程在桌面与手机断点通过真实接口验证。
- 前端 Node 测试、Vite 生产构建、Maven 全量测试、最终运行包构建全部通过。
- 视觉 QA 清单无未解决 P0/P1/P2 偏差。
