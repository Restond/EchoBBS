# EchoBBS 前端

基于 Vue 3 + TypeScript + Vite 构建的单页应用（SPA），为 EchoBBS 社区论坛提供用户界面。

## 技术栈

- **框架**：Vue 3.5（Composition API + `<script setup>`）
- **路由**：Vue Router 4
- **状态管理**：Pinia
- **构建工具**：Vite 8
- **类型系统**：TypeScript
- **HTTP 客户端**：Axios
- **Markdown 渲染**：marked
- **XSS 防护**：DOMPurify

## 环境要求

- Node.js 18+
- npm（或 pnpm / yarn）

## 快速开始

### 1. 安装依赖

```bash
cd frontend
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

开发服务器启动在 http://localhost:3000，API 请求自动代理到后端 `http://localhost:8081`（见 `vite.config.ts`）。

### 3. 构建生产版本

```bash
npm run build
```

构建产物输出到 `dist/`，可直接由 Nginx 或其他静态服务器托管。

### 4. 预览构建产物

```bash
npm run preview
```

## 目录结构

```
src/
├── api/
│   └── index.ts              # Axios 实例与拦截器
├── components/
│   └── AppHeader.vue          # 顶部导航栏（含通知、用户菜单、管理后台入口）
├── router/
│   └── index.ts              # 路由配置 + 全局守卫（认证、权限）
├── stores/
│   └── user.ts               # 用户状态管理（Pinia）
├── types/
│   └── index.ts              # 全局 TypeScript 类型定义
├── views/
│   ├── Home.vue              # 首页
│   ├── Boards.vue            # 资源中心
│   ├── Login.vue             # 登录
│   ├── Register.vue          # 注册
│   ├── UserProfile.vue       # 用户主页
│   ├── ProfileSettings.vue   # 资料设置
│   ├── NewPost.vue           # 发布资源
│   ├── PostDetail.vue        # 帖子详情（评论、点赞、编辑、Markdown）
│   ├── Search.vue            # 搜索
│   ├── Notifications.vue     # 通知中心
│   ├── AdminBoards.vue       # 管理后台-板块管理
│   └── AdminUsers.vue        # 管理后台-用户管理
├── App.vue                   # 根组件
└── main.ts                   # 应用入口
```

## 页面与路由

| 路由 | 页面 | 权限 |
|------|------|------|
| `/` | 首页 | 公开 |
| `/boards` | 资源中心 | 公开 |
| `/posts/:id` | 帖子详情 | 公开 |
| `/search` | 搜索 | 公开 |
| `/user/:username` | 用户主页 | 公开 |
| `/login` | 登录 | 游客 |
| `/register` | 注册 | 游客 |
| `/posts/new` | 发布资源 | 需登录 |
| `/settings/profile` | 资料设置 | 需登录 |
| `/notifications` | 通知中心 | 需登录 |
| `/admin/boards` | 板块管理 | 需 ADMIN/MODERATOR |
| `/admin/users` | 用户管理 | 需 ADMIN/MODERATOR |

### 路由守卫

`router/index.ts` 中定义全局守卫：

- `requiresAuth`：未登录跳转到登录页（携带 redirect 参数）
- `requiresAdmin`：非 ADMIN 跳转首页
- `requiresStaff`：非 ADMIN/MODERATOR 跳转首页

## 状态管理

`stores/user.ts`（Pinia）管理用户状态：

```typescript
const userStore = useUserStore()
userStore.currentUser    // 当前用户信息
userStore.isLoggedIn     // 是否已登录（computed）
userStore.isAdmin        // 是否管理员（computed）
userStore.isStaff        // 是否管理员或版主（computed）
userStore.fetchCurrentUser()  // 拉取当前用户
userStore.login(username, password, remember)
userStore.logout()
userStore.updateProfile(data)
userStore.uploadAvatar(file)
```

应用启动时（`App.vue`）自动调用 `fetchCurrentUser()` 探测登录态。

## API 交互

`api/index.ts` 封装了 Axios 实例：

- `baseURL`：通过 `VITE_API_BASE_URL` 环境变量配置，默认为空（开发时走 Vite 代理）
- `withCredentials: true`：携带 Cookie/Session
- 响应拦截器：不做全局跳转（`/api/users/me` 对游客返回 401 属正常），由路由守卫和页面组件自行处理

调用示例：
```typescript
import { api } from '@/api'

const res = await api.get('/api/posts')
const posts = res.data.data  // Result<Post[]>
```

## 安全机制

| 机制 | 说明 |
|------|------|
| XSS 防护 | `marked` 渲染后经 `DOMPurify.sanitize()` 消毒，白名单允许标签和属性 |
| 认证守卫 | 路由 `requiresAuth` / `requiresAdmin` / `requiresStaff` 拦截未授权访问 |
| 密码校验 | 注册页前端校验密码强度（8-64 位，需含字母和数字），与后端一致 |
| 凭证携带 | Axios `withCredentials: true` 携带 Session Cookie |
| 登录失败信息 | 统一提示"登录失败，请检查用户名和密码"，防用户名枚举 |

### Markdown 渲染与 XSS 防护

帖子正文和评论内容均通过 `renderMarkdown()` 渲染：

```typescript
const rawHtml = marked.parse(content) as string
return DOMPurify.sanitize(rawHtml, {
  ALLOWED_TAGS: ['h1','h2','h3','p','a','code','pre','img','table', ...],
  ALLOWED_ATTR: ['href','src','alt','title','class'],
  ALLOW_DATA_ATTR: false
})
```

配合后端 jsoup 消毒，形成三层 XSS 防御。

## 配置说明

### 环境变量

在项目根目录创建 `.env` 文件（可选）：

```bash
# 后端 API 地址（生产环境部署时配置）
VITE_API_BASE_URL=https://api.example.com
```

开发环境默认通过 Vite 代理（`vite.config.ts`）转发到 `http://localhost:8081`，无需配置。

### Vite 代理

```typescript
server: {
  port: 3000,
  proxy: {
    '/api': { target: 'http://localhost:8081', changeOrigin: true },
    '/uploads': { target: 'http://localhost:8081', changeOrigin: true }
  }
}
```

## 开发说明

### 类型定义

全局类型定义在 `src/types/index.ts`：

```typescript
interface User { id, username, email, nickname?, avatarPath?, role, status?, createdAt }
interface Board { id, name, slug, description, postCount?, sortOrder?, createdAt? }
interface Post { id, title, content, boardId, boardName, boardSlug, ... }
interface Reply { id, content, postId, parentId?, rootId?, authorNickname, ... }
interface Notification { id, type, content, targetUrl, isRead, createdAt }
```

### 样式约定

项目使用 CSS 变量实现主题色，定义在全局样式中：

- `--bg`：背景色
- `--bg-soft`：卡片背景
- `--bg-card`：输入框背景
- `--text` / `--text-dim`：文字颜色
- `--accent` / `--accent-hover` / `--accent-soft`：主色调
- `--border`：边框色

组件内样式使用 `<style scoped>` 隔离。

### 生产部署

1. 执行 `npm run build` 生成 `dist/`
2. 将 `dist/` 部署到静态服务器（Nginx 推荐）
3. 配置 SPA history 模式回退（Nginx 示例）：

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

4. 配置 API 反向代理指向后端服务：

```nginx
location /api/ {
    proxy_pass http://backend:8081;
}
location /uploads/ {
    proxy_pass http://backend:8081;
}
```
