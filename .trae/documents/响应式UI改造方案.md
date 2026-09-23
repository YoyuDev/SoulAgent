# SoulAgent 前端 UI 美化 + 响应式改造方案

## Context（为什么做这件事）

当前前端完全没有响应式能力——整个 `frontend/src` 下**零个 `@media` 查询**，所有布局都是为宽屏写死的：

- 顶层 `.app { display: flex; height: 100vh }` 固定并列「侧边栏 260px + 聊天区」
- 四个弹窗宽度全部写死（520px / 460px / 480px / 480px），在手机 375px 下必然横向溢出
- 麦克风、发送、朗读、角色「更多」等按钮触控区域仅 18×18 左右，移动端点不准
- Element Plus 主色仍是默认蓝色，与 `theme.css` 里定义的绿色 accent `#10a37f` 冲突（界面上会出现蓝色按钮配绿色主题）

目标：在**不引入任何新依赖**的前提下（该机器 esbuild 构建时已多次内存不足），用纯 CSS 媒体查询 + 现有 Element Plus 实现手机 / 平板 / PC 三档适配，并统一主题色、打磨视觉细节。

## 已确认的决策

1. **手机端侧边栏 → 抽屉浮层 + 汉堡按钮**：选中角色或点遮罩后自动收起，聊天区占满全宽
2. **三档断点**：`< 768px` 手机 / `768px – 1024px` 平板 / `> 1024px` PC
3. **美化力度**：主题统一 + 细节打磨（不做渐变 / 毛玻璃式大改版）

## 现状关键位置

| 位置 | 现状 |
| --- | --- |
| [App.vue](file:///e:/SoulAgent/frontend/src/App.vue#L2-L33) | 顶层并列渲染 `<Sidebar>` + `<ChatView>`；[L341-L352](file:///e:/SoulAgent/frontend/src/App.vue#L341-L352) `.app` flex + 100vh |
| [theme.css](file:///e:/SoulAgent/frontend/src/theme.css) | `:root/[data-theme="dark"]` 与 `:root[data-theme="light"]` 两套令牌；[L67-L105](file:///e:/SoulAgent/frontend/src/theme.css#L67-L105) 全局 `el-` 覆盖（大量 `!important`） |
| [useTheme.js](file:///e:/SoulAgent/frontend/src/composables/useTheme.js) | 通过 `document.documentElement` 的 `data-theme` 属性切主题 |
| [Sidebar.vue](file:///e:/SoulAgent/frontend/src/components/Sidebar.vue#L120-L135) | 宽 260px / 折叠 60px，`transition: width 0.3s` |
| [Sidebar.vue](file:///e:/SoulAgent/frontend/src/components/Sidebar.vue#L269-L279) | `.more-btn` 触控区域过小（仅 `padding: 2px`） |
| [ChatView.vue](file:///e:/SoulAgent/frontend/src/components/ChatView.vue#L103-L124) | 底部输入区横向 flex，麦克风 / 发送按钮图标 18px |
| [ChatView.vue](file:///e:/SoulAgent/frontend/src/components/ChatView.vue#L52-L63) | 朗读按钮 `.speak-btn` 18×18 |
| [ChatView.vue](file:///e:/SoulAgent/frontend/src/components/ChatView.vue#L554-L558) | 消息气泡 `max-width: 65%` |
| 四个 Dialog | [CreateCharacterDialog](file:///e:/SoulAgent/frontend/src/components/CreateCharacterDialog.vue#L2-L8) 520px、[CharacterInfoDialog](file:///e:/SoulAgent/frontend/src/components/CharacterInfoDialog.vue#L2-L8) 460px、[SettingsDialog](file:///e:/SoulAgent/frontend/src/components/SettingsDialog.vue#L2-L8) 480px、[EventHistoryDialog](file:///e:/SoulAgent/frontend/src/components/EventHistoryDialog.vue#L2-L8) 480px |
| [EventHistoryDialog.vue](file:///e:/SoulAgent/frontend/src/components/EventHistoryDialog.vue#L113-L117) | 事件列表 `max-height: 420px` |

## 实施方案

### 1. 断点与规则落点

CSS 变量不能用在媒体查询条件里，因此断点值直接字面写在 `@media` 中，**统一使用这三个值**：`767px`（手机上限）、`768px–1024px`（平板）、`1024px`（PC 下限）。

规则分两层，避免散落：

- **跨组件、需要全局生效的**（弹窗宽度兜底、Element Plus 主色、滚动条、触控目标统一放大）→ 集中写在 [theme.css](file:///e:/SoulAgent/frontend/src/theme.css)
- **单个组件自身的布局变化**（侧边栏抽屉化、聊天区头部与输入区、消息气泡宽度）→ 写在该组件的 `<style scoped>` 里

### 2. 手机端抽屉

- **用 `el-drawer`**：Element Plus 已全量注册可直接使用，自带遮罩、`append-to-body`、点遮罩关闭、焦点锁定与动画，无需自己处理 z-index 与滚动穿透
- **汉堡按钮放在 ChatView 自身头部的最左侧**（移动档才显示），而不是在 App.vue 新加一条顶栏——避免多出一行挤占本就紧张的垂直空间；ChatView 头部已有头像与名字区
- **新增状态与事件**：
  - `App.vue` 新增 `mobileDrawerOpen = ref(false)`
  - `Sidebar` 新增 `inDrawer` prop（抽屉内不显示折叠按钮、宽度改为 100%）
  - `ChatView` 新增 `showMenuButton` prop，emit `toggleMenu`
  - `Sidebar` 原有 `select` 事件在 App.vue 里处理时顺带 `mobileDrawerOpen.value = false`；删除 / 清空历史等操作也可一并收起
- **平板档（768–1024px）**：不走抽屉，侧边栏**自动折叠为 60px 窄条**（复用已有 `sidebarCollapsed`）。实现上用 JS 监听 `matchMedia('(min-width: 768px) and (max-width: 1024px)')`，命中时置 `sidebarCollapsed = true`
- **判断档位**：只在「需要 JS 分支行为」处用 JS。新增 `src/composables/useBreakpoint.js`，内部用 `window.matchMedia` 暴露 `isMobile` / `isTablet`，并在卸载时移除监听。**纯样式一律走 CSS，不进 JS**

### 3. 弹窗响应式

Element Plus 的 `width` 会被写成**内联 style**，优先级高于普通 CSS 类。因此：

- 把四个弹窗的 `width="480px"` 等改为 `width="520px"` 不变，**改为在 theme.css 里统一兜底**：
  
  由于内联样式无法被普通选择器覆盖，稳妥做法是**保留各弹窗原有 `width` 作为 PC 值**，同时在 theme.css 加一条移动档规则用 `!important` 覆盖宽度与最大宽度：

  ```css
  @media (max-width: 767px) {
    .el-dialog { width: 92vw !important; max-width: 92vw !important; margin-top: 8vh !important; }
  }
  ```

  这与文件里已有的 `!important` 覆盖风格一致，且**一处改动覆盖全部弹窗**，无需逐个改组件
- 手机端弹窗内容超高时，给 `.el-dialog__body` 加 `max-height: 62vh; overflow-y: auto`
- 手机端不强制全屏化，保持 92vw 浮层即可，视觉更轻

### 4. 触控友好度（≥44px）

**只在移动档媒体查询里放大**，PC 端观感不变。目标元素：

| 元素 | 文件位置 |
| --- | --- |
| 麦克风按钮、发送按钮 | [ChatView.vue](file:///e:/SoulAgent/frontend/src/components/ChatView.vue#L112-L123) |
| 朗读按钮 `.speak-btn` | [ChatView.vue](file:///e:/SoulAgent/frontend/src/components/ChatView.vue#L52-L63) |
| 角色「更多」按钮 `.more-btn` | [Sidebar.vue](file:///e:/SoulAgent/frontend/src/components/Sidebar.vue#L269-L279) |
| 角色列表项 | [Sidebar.vue](file:///e:/SoulAgent/frontend/src/components/Sidebar.vue#L236-L240) |

优先用「**伪元素扩大点击热区**」而不是直接把图标做大（图标变大反而破坏 PC/移动视觉一致性）：`::after { content:''; position:absolute; inset:-12px; }` 配合元素本身 `position: relative`。

### 5. 主题统一

在 [theme.css](file:///e:/SoulAgent/frontend/src/theme.css) 两个主题块中各自补上 Element Plus 主色衍生变量（Element Plus 依赖 light-3/5/7/8/9 与 dark-2 生成 hover / active / disabled / 描边等状态）：

```css
--el-color-primary: #10a37f;
--el-color-primary-light-3: ...;
--el-color-primary-light-5: ...;
--el-color-primary-light-7: ...;
--el-color-primary-light-8: ...;
--el-color-primary-light-9: ...;
--el-color-primary-dark-2: ...;
```

暗色与亮色主题分别取值（亮色下 light-N 需更浅、dark-2 更深）。

**细节打磨项（6 项）**：

1. 统一圆角体系：按钮 10px / 弹窗 14px（已有）/ 气泡 12px / 输入框 12px，写进 theme.css
2. 统一阴影层级：弹窗、下拉浮层、卡片三档阴影变量
3. 补 `:focus-visible` 反馈：按钮与输入框聚焦时用 `--accent` 描边，提升键盘可用性
4. 滚动条美化：`::-webkit-scrollbar` 细条 + 透明轨道，用 `--border-secondary` 作滑块色
5. 空状态打磨：角色列表为空、事件记录为空、消息为空的文案与图标居中留白
6. 统一过渡时长：`--transition-fast: .15s` / `--transition-base: .3s`，替换散落的硬编码

### 6. 分步实施顺序（每步可独立验证）

| 阶段 | 内容 | 验收 |
| --- | --- | --- |
| 1 | theme.css 加 Element Plus 主色变量 + 圆角/阴影/过渡令牌 | 刷新后所有按钮变绿色，主题切换正常 |
| 2 | theme.css 加弹窗移动档兜底、滚动条、`focus-visible` | 375px 下打开任意弹窗不溢出 |
| 3 | 新增 `composables/useBreakpoint.js` | 缩放到 375/800/1440 时 `isMobile/isTablet` 正确变化 |
| 4 | App.vue 加 `mobileDrawerOpen` + `el-drawer` 包裹 Sidebar；Sidebar 加 `inDrawer` | 手机档点汉堡出抽屉、选角色自动收起 |
| 5 | ChatView 加汉堡按钮 + 移动档头部/输入区样式 | 375px 下聊天区满宽、输入区不换行错位 |
| 6 | 触控热区、消息气泡宽度（移动档 82%）、空状态 | DevTools 触摸模拟下能顺畅点到各按钮 |
| 7 | 平板档自动折叠侧边栏 | 800px 下侧栏为 60px 窄条，1024px 以上恢复 260px |

每阶段结束后跑 `npm run build`（若报 esbuild OOM 属环境内存问题，重试即可；连续失败则改用 `@vue/compiler-sfc` 单独校验改动文件），并在 DevTools 的 375px / 768px / 1440px 三档宽度下目视检查。

## 风险与规避

1. **内联 style 覆盖冲突**：弹窗 `width` 是内联样式，必须用 `!important` 才能在移动档压过它——与 theme.css 现有风格一致，可接受
2. **`100vh` 移动端地址栏遮挡**：`.app` 的 `height: 100vh` 在 iOS Safari 上会被地址栏吃掉一截，改用 `100dvh` 并保留 `100vh` 兜底
3. **el-dropdown 在抽屉内定位异常**：`el-drawer` 默认 `append-to-body`，下拉浮层挂在 body 上一般正常；若出现错位，给 dropdown 显式加 `:teleported="true"` 并检查 `popper-append-to-body`
4. **z-index 冲突**：`el-drawer` 与 `el-dialog` 默认层级由 Element Plus 统一管理，抽屉内**不要**打开弹窗；如需（例如从抽屉里点「设置」），先关闭抽屉再开弹窗
5. **`!important` 叠加**：theme.css 已大量使用 `!important`，新增规则要放在文件末尾，避免被同优先级规则覆盖时难以排查
6. **构建内存 OOM**：不新增依赖；若 OOM 持续，用 `@vue/compiler-sfc` + esbuild 单独校验改动文件作为替代验证

## 影响范围

改动的文件：`theme.css`、`App.vue`、`Sidebar.vue`、`ChatView.vue`，新增 `composables/useBreakpoint.js`。四个 Dialog 组件**无需改动**（响应式由 theme.css 统一兜底）。后端不涉及。