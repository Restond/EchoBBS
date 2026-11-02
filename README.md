# EchoBBS

一个基于 Spring Boot + Vue 3 的社区论坛系统，支持资源分享、话题讨论、评论互动、通知提醒等核心论坛功能。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 4.0 · Spring Security · Spring Data JPA · PostgreSQL · Flyway |
| 前端 | Vue 3.5 · Vue Router · Pinia · TypeScript · Vite |
| 缓存 | Caffeine |
| 其他 | jsoup（HTML 消毒）· commonmark（Markdown）· DOMPurify（前端 XSS 防护）· bucket4j（限流） |

## 功能特性

### 核心功能
- 用户注册 / 登录 / 登出（BCrypt 加密 + Remember-Me）
- 资源（帖子）发布 / 编辑 / 删除（Markdown 渲染）
- 评论与楼中楼回复（层级嵌套）
- 帖子 / 评论点赞
- 通知系统（回复、点赞提醒 + 未读数）
- 全文搜索
- 用户主页（资料、帖子、统计数据）

### 管理后台
- 板块管理（增删改查）
- 用户管理（封禁 / 解封）
- 帖子管理（置顶、锁定、删除）

### 安全机制
- HTTP 安全响应头（CSP、X-Frame-Options、HSTS 等）
- API 限流（登录注册 5 次/分，写操作 30 次/分）
- CSRF 防护 + 会话固定攻击防护
- 三层 XSS 防御（后端 jsoup + marked 转义 + 前端 DOMPurify）
- 密码强度策略（8-64 位，需含字母和数字）
- 文件上传安全（MIME / 大小 / 路径穿越防护）
- 封禁用户拦截器
- CORS 跨域配置
- 全局异常处理 + 安全审计日志

## 项目结构

```
EchoBBS/
├── src/                    # 后端 Spring Boot 项目
│   └── main/
│       ├── java/com/restond/echobbs/
│       │   ├── common/     # 通用组件（异常处理、Result 封装）
│       │   ├── config/     # 配置类（安全、CORS、限流、拦截器）
│       │   ├── module/     # 业务模块（board/post/user/comment/like/notification）
│       │   └── util/       # 工具类（Markdown 渲染）
│       └── resources/
│           ├── db/migration/  # Flyway 数据库迁移脚本
│           └── application.yml
├── frontend/               # 前端 Vue 3 项目
│   └── src/
│       ├── api/            # Axios 实例
│       ├── components/     # 公共组件（AppHeader）
│       ├── router/         # 路由配置
│       ├── stores/         # Pinia 状态管理
│       ├── types/          # TypeScript 类型定义
│       └── views/          # 页面组件
├── uploads/                # 用户上传文件（头像）
└── build.gradle            # 后端构建配置
```

## 快速开始

### 环境要求
- JDK 21+
- Node.js 18+
- PostgreSQL 14+
- Gradle（项目自带 wrapper）

### 1. 准备数据库

```bash
# 登录 PostgreSQL，创建数据库
createdb echobbs
```

数据库迁移由 Flyway 自动执行，无需手动建表。

### 2. 启动后端

详见 [后端文档](src/README.md)。

### 3. 启动前端

详见 [前端文档](frontend/README.md)。

### 4. 访问应用

- 前端：http://localhost:3000
- 后端 API：http://localhost:8081

## 配置说明

后端配置位于 `src/main/resources/application.yml`，支持环境变量覆盖：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `DB_HOST` | localhost | 数据库地址 |
| `DB_PORT` | 5432 | 数据库端口 |
| `DB_NAME` | echobbs | 数据库名 |
| `DB_USERNAME` | postgres | 数据库用户名 |
| `DB_PASSWORD` | postgres | 数据库密码 |
| `app.cors.allowed-origins` | http://localhost:3000 | 允许的前端域名 |

## 默认账号

首次启动后，可通过注册页面创建账号。如需管理员权限，需在数据库中手动将用户角色改为 `ADMIN`：

```sql
UPDATE users SET role = 'ADMIN' WHERE username = 'your_username';
```

## 许可证

本项目仅用于学习交流。
