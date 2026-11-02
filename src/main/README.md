# EchoBBS 后端服务

基于 Spring Boot 4.0 构建的社区论坛 REST API 服务。

## 技术栈

- **框架**：Spring Boot 4.0.6
- **安全**：Spring Security（Session 认证 + CSRF + 密码加密）
- **数据层**：Spring Data JPA + PostgreSQL
- **数据库迁移**：Flyway
- **缓存**：Caffeine
- **校验**：Spring Validation
- **Markdown**：commonmark + jsoup（HTML 消毒）
- **限流**：bucket4j
- **图片处理**：thumbnailator（头像压缩）

## 环境要求

- JDK 21+
- PostgreSQL 14+
- Gradle（项目自带 wrapper，无需单独安装）

## 快速开始

### 1. 配置数据库

确保 PostgreSQL 已启动，并创建数据库 `echobbs`：

```bash
psql -U postgres -c "CREATE DATABASE echobbs;"
```

如需修改连接配置，通过环境变量覆盖（或直接编辑 `src/main/resources/application.yml`）：

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=echobbs
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
```

### 2. 运行

**Windows（PowerShell）**：
```bash
.\gradlew.bat bootRun
```

**Linux / macOS**：
```bash
./gradlew bootRun
```

服务启动后访问 http://localhost:8081。

首次启动时，Flyway 会自动执行 `src/main/resources/db/migration/` 下的迁移脚本创建表结构。

### 3. 构建可执行 JAR

```bash
.\gradlew.bat build
java -jar build/libs/echobbs-1.0-SNAPSHOT.jar
```

## 模块结构

```
src/main/java/com/restond/echobbs/
├── common/                 # 通用层
│   ├── GlobalExceptionHandler.java   # 全局异常处理 + 安全审计日志
│   ├── Result.java                  # 统一响应封装
│   ├── NotFoundException.java
│   ├── ForbiddenException.java
│   └── ConflictException.java
├── config/                 # 配置层
│   ├── SecurityConfig.java           # Spring Security 配置
│   ├── SecurityHeadersFilter.java    # HTTP 安全响应头
│   ├── CorsConfig.java               # CORS 跨域配置
│   ├── RateLimitInterceptor.java     # API 限流拦截器
│   ├── BannedUserInterceptor.java    # 封禁用户拦截器
│   ├── WebConfig.java                # 拦截器与静态资源注册
│   ├── CacheConfig.java              # Caffeine 缓存配置
│   └── FlywayConfig.java             # Flyway 配置
├── module/                 # 业务模块
│   ├── board/             # 板块模块
│   ├── post/              # 帖子模块
│   ├── user/              # 用户模块
│   ├── comment/           # 评论模块
│   ├── like/              # 点赞模块
│   └── notification/      # 通知模块
└── util/
    └── MarkdownUtil.java              # Markdown 渲染 + jsoup 消毒
```

每个业务模块遵循分层结构：
```
module/<name>/
├── controller/
│   └── api/          # REST API 控制器
├── service/          # 业务逻辑
├── repository/       # 数据访问层
├── model/            # 实体
│   └── dto/          # 数据传输对象
└── (config/)         # 模块专属配置（如需要）
```

## API 概览

所有 API 以 `/api` 为前缀，响应统一为 `Result<T>` 格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 公开接口（无需认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/register` | 用户注册 |
| POST | `/api/login` | 用户登录 |
| GET | `/api/posts` | 帖子列表（支持分页、排序、搜索） |
| GET | `/api/posts/{id}` | 帖子详情 |
| GET | `/api/posts/{id}/comments` | 帖子评论列表 |
| GET | `/api/posts/latest` | 最新帖子 |
| GET | `/api/posts/hot` | 热门帖子 |
| GET | `/api/boards` | 板块列表 |
| GET | `/api/search` | 全文搜索 |
| GET | `/api/users/me` | 当前登录态探测（游客返回 null） |
| GET | `/api/users/{username}` | 用户主页信息 |

### 认证接口（需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/logout` | 登出 |
| GET | `/api/users/me/posts` | 我的帖子 |
| GET | `/api/users/me/comments` | 我的评论 |
| PUT | `/api/users/me` | 更新个人资料 |
| POST | `/api/users/me/avatar` | 上传头像 |
| POST | `/api/posts` | 发布帖子 |
| PUT | `/api/posts/{id}` | 编辑帖子 |
| DELETE | `/api/posts/{id}` | 删除帖子 |
| POST | `/api/posts/{id}/like` | 帖子点赞切换 |
| POST | `/api/posts/{postId}/comments` | 发表评论 |
| POST | `/api/comments/{commentId}/reply` | 回复评论（楼中楼） |
| DELETE | `/api/comments/{commentId}` | 删除评论 |
| POST | `/api/comments/{commentId}/like` | 评论点赞切换 |
| GET | `/api/notifications` | 通知列表 |
| GET | `/api/notifications/unread-count` | 未读通知数 |
| PUT | `/api/notifications/{id}/read` | 标记通知已读 |

### 管理接口（需 ADMIN 或 MODERATOR 角色）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/users` | 用户列表（分页） |
| POST | `/api/users/{id}/ban` | 封禁 / 解封用户 |
| POST | `/api/admin/boards` | 创建板块 |
| PUT | `/api/admin/boards/{id}` | 编辑板块 |
| DELETE | `/api/admin/boards/{id}` | 删除板块 |
| POST | `/api/admin/posts/{id}/pin` | 帖子置顶切换 |
| POST | `/api/admin/posts/{id}/lock` | 帖子锁定切换 |

## 数据库迁移

迁移脚本位于 `src/main/resources/db/migration/`，采用 Flyway 版本化管理：

```
V1__init.sql                  # 初始化
V2__create_user_table.sql     # 用户表
V3__create_forum_tables.sql   # 论坛核心表（板块、帖子、评论）
V4__add_last_replied_at.sql   # 帖子最后回复时间
V5__create_user_like_table.sql # 点赞表
V6__create_notification_table.sql # 通知表
V7__add_fulltext_search.sql   # 全文搜索索引
```

应用启动时 Flyway 自动检测并执行未应用的迁移。

## 安全机制

| 机制 | 实现位置 |
|------|----------|
| 密码加密（BCrypt） | `SecurityConfig.passwordEncoder()` |
| Session 认证 | `SecurityConfig` + `UserService.login()` |
| CSRF 防护 | `SecurityConfig`（API 路径禁用，页面路由启用） |
| 会话固定防护 | `SecurityConfig`（`migrateSession`） |
| HTTP 安全头 | `SecurityHeadersFilter` + `SecurityConfig.headers()` |
| CORS 跨域 | `CorsConfig` |
| API 限流 | `RateLimitInterceptor`（登录注册 5/min，写操作 30/min） |
| 封禁用户拦截 | `BannedUserInterceptor` |
| XSS 防护 | `MarkdownUtil`（jsoup 消毒） |
| 输入校验 | `@Valid` + `@NotBlank` + `@Size` + `@Pattern` |
| 文件上传安全 | `UserService.uploadAvatar()` |
| 全局异常处理 | `GlobalExceptionHandler`（不泄露堆栈） |
| 安全审计日志 | `GlobalExceptionHandler.logSecurityEvent()` |

## 配置说明

`application.yml` 关键配置：

```yaml
server:
  port: 8081
  servlet:
    session:
      timeout: 7d              # Session 超时

spring:
  jpa:
    open-in-view: false        # 关闭 OSIV，防懒加载问题
  servlet:
    multipart:
      max-file-size: 5MB       # 上传文件大小限制
      max-request-size: 5MB

app:
  upload:
    avatar-dir: uploads/avatars
  cors:
    allowed-origins: http://localhost:3000
```

## 开发说明

### 热重载

开发时可使用 Spring DevTools 实现热重载（如需启用，在 `build.gradle` 添加依赖）：
```gradle
developmentOnly 'org.springframework.boot:spring-boot-devtools'
```

### 日志

日志输出到控制台和 `logs/echobbs.log`，按天滚动，保留 30 天，单文件最大 10MB。

安全相关事件（越权访问、注册冲突、文件过大、CSRF 失败等）通过 `GlobalExceptionHandler.logSecurityEvent()` 记录为 WARN 级别。
