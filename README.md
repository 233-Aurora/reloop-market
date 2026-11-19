# ReLoop Market — Campus Secondhand Marketplace

ReLoop Market is a campus-focused secondhand marketplace for listings, trade requests, reviews, messaging, reports, and dispute resolution.

This is a learning continuation of the upstream [CampusEcho project](https://github.com/bestxiangest/Goods-Trading-Center). ReLoop Market is the local product name; retain the upstream attribution and usage terms when sharing or distributing the code.

当前仓库为完整重构版：

- 后端：`CampusEcho/` — Spring Boot 4 + Java 21 + PostgreSQL + Flyway + JWT
- 前端：`CampusEcho-web/` — Vue 3 + TypeScript + Vite + Pinia + Element Plus

API 统一前缀：`/api/v1`。

---

## 功能概览

### 用户与认证

- 注册 / 登录（用户名或邮箱）
- JWT 无状态鉴权，密码仅存 BCrypt 哈希
- 当前用户资料修改（昵称、手机号、校区）
- 头像上传 / 替换 / 删除（JPEG、PNG、WebP，≤ 2 MiB）
- 公开用户资料与信誉分展示（不暴露手机号、邮箱）

### 分类与物品

- 公开分类树浏览
- 管理员新增 / 修改 / 安全删除分类
- 物品发布、编辑、上架、下架
- 公开分页列表（分类、关键字筛选）
- 物品图片上传、主图设置、删除与公开访问
  - 单图 ≤ 5 MiB，每件最多 5 张
  - 服务端随机文件名，事务失败时清理文件
- 物品交易状态与审核状态分离
  - 交易状态：`DRAFT` / `ON_SALE` / `RESERVED` / `SOLD` / `OFF_SHELF`
  - 审核状态：`NORMAL` / `BLOCKED`

### 交易与评价

- 买家发起交易请求，卖家接受 / 拒绝，买家取消
- 接受后物品变为「已预订」；同物品其他待处理请求自动拒绝
- 双方确认完成后交易结束，物品变为「已售出」
- 已完成交易双方各可评价一次（1–5 分）
- 信誉分按收到评分的平均值计算，保留两位小数

### 站内消息

- 系统在交易状态变化、举报处理、治理处罚、争议仲裁时自动写入通知
- 消息列表、仅未读、类型筛选、详情、单条已读、全部已读
- 前端顶栏未读角标（>99 显示 99+），登录后轮询刷新

### 举报与内容治理

- 用户可举报物品或用户，并查看自己的处理结果
- 管理员筛选、处理、驳回举报
- 处罚动作：不处罚 / 屏蔽物品 / 停用用户 / 组合处罚
- 屏蔽物品：隐藏公开展示与图片，取消相关未完成交易
- 停用用户：隐藏其公开物品，取消其未完成交易，旧 JWT 立即失效
- 管理员可解除物品屏蔽（恢复后仍保持下架，需卖家重新上架）

### 交易争议与管理员仲裁

与普通举报职责分离：

| 场景 | 使用模块 |
| --- | --- |
| 未交付、货不对板、付款纠纷、爽约 | 交易争议 |
| 违禁品、虚假信息、骚扰、冒充 | 普通举报 |

- 仅 `ACCEPTED` 交易可发起争议，交易进入 `DISPUTED` 并冻结确认完成
- 管理员可裁定：完成交易 / 取消交易 / 驳回（恢复为已接受）
- 屏蔽物品或停用账户时，相关开放争议自动关闭

### 管理端

- 数据概览（用户、物品、交易、评价、举报、争议）
- 用户检索、详情、账户启停、角色调整
- 分类管理、举报处理、争议仲裁
- 权限同时校验 JWT 与数据库实时角色 / 状态
- 禁止管理员停用或降级自己；系统至少保留一名活跃管理员

---

## 技术架构

```text
Vue 3 SPA (CampusEcho-web)
        │  JSON / Bearer JWT
        ▼
Spring Boot API (/api/v1)
  ├── controller      HTTP 映射与参数校验
  ├── application     用例编排与事务
  ├── domain          实体、枚举、业务规则
  ├── infrastructure  JPA / 本地文件存储
  └── common/security 统一响应、异常、JWT
        │
        ▼
PostgreSQL + Flyway 版本化迁移
```

### 后端技术

| 项 | 说明 |
| --- | --- |
| 语言 / 运行时 | Java 21 |
| 框架 | Spring Boot 4.1、Spring Web、Spring Data JPA、Spring Security |
| 数据库 | PostgreSQL |
| 迁移 | Flyway（`V1`–`V8`） |
| 认证 | JWT（HS256，密钥至少 32 字符） |
| 构建 | Maven |

### 前端技术

| 项 | 说明 |
| --- | --- |
| 框架 | Vue 3.5（Composition API） |
| 语言 | TypeScript |
| 构建 | Vite 8 |
| 状态 / 路由 | Pinia 4、Vue Router 5 |
| UI | Element Plus 2 |
| HTTP | Axios（统一解包与问题详情错误） |

---

## 目录结构

```text
.
├── CampusEcho/                 # 后端
│   ├── src/main/java/...       # 业务模块（user/item/trade/review/report/message/dispute/admin）
│   ├── src/main/resources/
│   │   ├── application.yaml
│   │   ├── application-local.yaml.example
│   │   └── db/migration/       # Flyway SQL
│   └── scripts/sql/            # 本地管理员脚本等
├── CampusEcho-web/             # 前端
│   └── src/
│       ├── api/                # 接口封装
│       ├── components/
│       ├── layouts/
│       ├── router/
│       ├── stores/
│       ├── utils/
│       └── views/
├── API文档.md
├── 重构文档.md
├── 项目进度.md
└── README.md
```

---

## 本地启动

### 环境要求

- JDK 21+
- Maven 3.9+（或使用仓库内 `./mvnw`）
- Node.js 20+
- PostgreSQL 14+（本地已在 PostgreSQL 18 验证）

### 1. 准备数据库

```bash
createdb -h localhost -U <你的数据库用户> campusecho
```

### 2. 配置后端

复制本地配置示例：

```bash
cp CampusEcho/src/main/resources/application-local.yaml.example \
   CampusEcho/src/main/resources/application-local.yaml
```

编辑 `application-local.yaml`，填写数据库账号密码与至少 32 字符的 `app.jwt.secret`。

默认激活 profile 为 `local`，也可通过环境变量覆盖：

| 变量 | 说明 | 默认 |
| --- | --- | --- |
| `DB_HOST` | 主机 | `localhost` |
| `DB_PORT` | 端口 | `5432` |
| `DB_NAME` | 库名 | `campusecho` |
| `DB_USERNAME` | 用户名 | 无 |
| `DB_PASSWORD` | 密码 | 无 |
| `JWT_SECRET` | JWT 签名密钥（≥32 字符） | 无 |
| `ITEM_IMAGE_DIRECTORY` | 物品图片目录 | `./data/item-images` |
| `AVATAR_DIRECTORY` | 头像目录 | `./data/avatars` |

> 不要将真实密码、JWT 密钥或 `application-local.yaml` 提交到仓库。

### 3. 启动后端

```bash
cd CampusEcho
./mvnw spring-boot:run
```

健康检查：`http://127.0.0.1:8080/api/v1/health`

### 4. 启动前端

```bash
cd CampusEcho-web
npm install
npm run dev
```

开发地址：`http://127.0.0.1:5173`  
Vite 已将 `/api` 代理到 `http://localhost:8080`。

### 5. 本地管理员（可选）

仓库提供显式 SQL 脚本创建/重置本地管理员，**不属于** Flyway 自动迁移：

```bash
psql -h localhost -U <用户> -d campusecho -f CampusEcho/scripts/sql/create_local_admin.sql
```

生产环境请使用独立、强随机凭据，勿使用开发脚本中的固定口令。

---

## API 约定

- 成功：`{"code":0,"message":"ok","data":...}`
- 失败：`application/problem+json`（`status` / `detail` / `code`）
- 认证：`Authorization: Bearer <accessToken>`
- 分页：`page` 从 1 开始，`size` 为 1–100

完整接口说明见 [API文档.md](./API文档.md)。

主要模块：

| 模块 | 路径前缀 |
| --- | --- |
| 认证 / 用户 | `/api/v1/auth`、`/api/v1/users` |
| 分类 | `/api/v1/categories` |
| 物品 / 图片 | `/api/v1/items`、`/api/v1/item-images` |
| 交易 | `/api/v1/trade-requests` |
| 评价 | `/api/v1/trade-requests/{id}/reviews`、`/api/v1/users/{id}/reviews` |
| 消息 | `/api/v1/messages` |
| 举报 | `/api/v1/reports`、`/api/v1/admin/reports` |
| 争议 | `/api/v1/disputes`、`/api/v1/admin/disputes` |
| 管理 | `/api/v1/admin` |

---

## 前端页面

| 区域 | 页面 |
| --- | --- |
| 公开 | 物品市场、物品详情、登录 / 注册、公开用户资料 |
| 用户 | 发布 / 编辑物品、我的物品、我的交易、交易详情、消息中心、我的争议、我的举报、个人资料 |
| 管理 | 管理概览、用户管理、分类管理、举报处理、争议仲裁 |

---

## 数据库迁移

结构变更一律通过 Flyway 管理，已执行迁移不可修改。

| 版本 | 说明 |
| --- | --- |
| V1 | 初始 schema（用户、分类、物品、交易、评价、消息） |
| V2 | 默认根分类种子 |
| V3 | 物品 / 交易乐观锁版本列 |
| V4–V5 | 本地管理员种子历史与弱口令清理 |
| V6 | 举报表与物品审核状态 |
| V7 | 消息通知字段扩展 |
| V8 | 交易争议表与 `DISPUTED` 状态 |

---

## 测试

后端集成测试（需本地 PostgreSQL 与可用凭据）：

```bash
cd CampusEcho
export DB_USERNAME=...
export DB_PASSWORD=...
export JWT_SECRET=test-only-signing-key-with-at-least-32-characters
./mvnw test
```

当前覆盖：Flyway、认证与 JWT、分类权限、物品与图片、交易与评价、信誉分、举报治理、消息通知、交易争议仲裁等。

前端构建：

```bash
cd CampusEcho-web
npm run build
```

---

## 安全说明

- 密码只存强哈希，从不回传
- JWT 密钥不入库、不写示例配置中的真实值
- 文件上传校验类型与大小，文件名由服务端生成
- 交易 / 争议 / 举报状态迁移由服务层集中校验
- 角色与账户状态以数据库实时结果为准，旧令牌权限可立即失效
- CORS 与生产密钥需按部署环境单独配置

---

## 相关文档

| 文档 | 说明 |
| --- | --- |
| [API文档.md](./API文档.md) | 接口约定与请求示例 |

后端数据库迁移说明见 `CampusEcho/src/main/resources/db/migration/` 目录中的 SQL 文件。

---

## 许可证

本项目用于课程 / 学习与个人展示。使用与二次分发请遵守仓库声明及所属学校相关要求。
