# CampusEcho API 文档

## 基础约定

- Base URL：`http://localhost:8080/api/v1`
- 请求与响应：JSON，UTF-8。
- 成功响应：`{"code":0,"message":"ok","data":...}`。
- 失败响应：`application/problem+json`，重点读取 `status`、`detail`、`code`。
- 认证请求头：`Authorization: Bearer <accessToken>`。
- 分页参数：`page` 从 1 开始，`size` 为 1-100。

## 认证与用户

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| POST | `/auth/register` | 否 | 注册用户 |
| POST | `/auth/login` | 否 | 用户名或邮箱登录，返回 JWT |
| GET | `/users/me` | 是 | 当前用户完整资料 |
| PUT | `/users/me` | 是 | 修改昵称、手机号和校区 |
| POST | `/users/me/avatar` | 是 | 上传或替换头像，使用 `multipart/form-data` |
| DELETE | `/users/me/avatar` | 是 | 删除当前头像 |
| GET | `/avatars/{storageKey}` | 否 | 获取头像二进制内容 |
| GET | `/users/{id}` | 否 | 用户公开资料 |

注册请求：

```json
{
  "username": "student_01",
  "email": "student@example.com",
  "password": "password123",
  "displayName": "学生用户",
  "phone": null,
  "campus": "主校区"
}
```

登录请求：

```json
{
  "login": "student_01",
  "password": "password123"
}
```

修改资料请求：

```json
{
  "displayName": "新昵称",
  "phone": "13800138000",
  "campus": "新校区"
}
```

当前用户响应包含 `phone`，公开用户资料不返回手机号和邮箱。头像上传字段名为 `file`，支持 JPEG、PNG、WebP，大小不超过 2 MiB；响应中的 `avatarUrl` 是可直接访问的站内相对地址。

## 分类

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| GET | `/categories/tree` | 否 | 获取完整分类树 |
| POST | `/categories` | 管理员 | 新增分类 |
| PUT | `/categories/{id}` | 管理员 | 修改分类名称、父级和排序 |
| DELETE | `/categories/{id}` | 管理员 | 删除未被使用且没有子级的分类 |

新增或修改请求：

```json
{
  "name": "教材",
  "parentId": 1,
  "sortOrder": 10
}
```

管理员写接口同时校验 JWT 角色与数据库中的当前账户角色、状态。分类不能移动到自己的子级下；存在子分类或仍被物品使用时不能删除。

## 管理员

所有管理员接口均需 `ADMIN` 角色。权限会同时依据 JWT 和数据库中的实时角色、状态判定；角色调整或账户停用后，旧令牌的权限立即变化。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/dashboard` | 获取用户、物品、交易、评价、举报和争议数量概览 |
| GET | `/admin/users` | 用户分页；支持 `keyword`、`role`、`status`、`page`、`size` |
| GET | `/admin/users/{id}` | 获取包含邮箱、手机号、角色和状态的用户详情 |
| PATCH | `/admin/users/{id}/status` | 设置 `ACTIVE` 或 `SUSPENDED` |
| PATCH | `/admin/users/{id}/role` | 设置 `USER` 或 `ADMIN` |

状态修改请求：

```json
{
  "status": "SUSPENDED"
}
```

角色修改请求：

```json
{
  "role": "ADMIN"
}
```

管理员不能停用或降级自己，系统必须始终保留至少一个活跃管理员。当前校园二手交易场景中，普通 `USER` 同时可以购买和发布物品，不单独设置商家角色。

本地开发管理员通过 `CampusEcho/scripts/sql/create_local_admin.sql` 显式创建。该脚本不属于 Flyway 自动迁移，生产环境不得使用固定开发凭据。

## 举报与内容治理

用户举报接口均需登录，管理员处理接口均需 `ADMIN` 角色。

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| POST | `/reports` | 是 | 举报物品或用户，成功返回 `201` |
| GET | `/reports/mine` | 是 | 我的举报分页；支持 `targetType`、`status`、`page`、`size` |
| GET | `/reports/{id}` | 举报人 | 查看自己的举报和处理结果 |
| GET | `/admin/reports` | 管理员 | 举报分页；支持 `targetType`、`reason`、`status`、`page`、`size` |
| GET | `/admin/reports/{id}` | 管理员 | 查看举报、目标和处理详情 |
| POST | `/admin/reports/{id}/resolve` | 管理员 | 完成举报处理并执行选定处罚 |
| POST | `/admin/reports/{id}/reject` | 管理员 | 驳回举报 |
| POST | `/admin/items/{id}/restore` | 管理员 | 解除违规屏蔽，物品保持下架 |

提交举报请求：

```json
{
  "targetType": "ITEM",
  "targetId": 1,
  "reason": "MISLEADING_INFORMATION",
  "description": "物品描述与实际展示的信息明显不一致。"
}
```

目标类型为 `ITEM` 或 `USER`。物品原因可选 `FRAUD`、`PROHIBITED_ITEM`、`MISLEADING_INFORMATION`、`OTHER`；用户原因可选 `FRAUD`、`HARASSMENT`、`IMPERSONATION`、`OTHER`。说明去除首尾空格后长度为 10-1000 个字符。

管理员处理请求：

```json
{
  "action": "BLOCK_ITEM",
  "resolutionNote": "核查后确认物品描述严重失实。"
}
```

处罚动作为 `NONE`、`BLOCK_ITEM`、`SUSPEND_USER`、`BLOCK_ITEM_AND_SUSPEND_USER`。用户举报只允许 `NONE` 或 `SUSPEND_USER`。驳回请求使用 `resolutionNote`，恢复物品请求使用 `reason`，二者去除首尾空格后长度为 2-1000 个字符。

举报状态为 `PENDING`、`RESOLVED`、`REJECTED`。同一用户对同一目标最多存在一条待处理举报；处理结束后可以再次举报。不能举报自己、自己的物品、已屏蔽物品或已停用用户。

屏蔽物品会取消其 `PENDING`、`ACCEPTED` 交易并将未售出物品转为 `OFF_SHELF`；解除屏蔽后仍需卖家主动重新上架。停用用户会取消其参与的未完成交易，停用卖家的物品不再公开展示。被取消的已接受交易仅在卖家仍活跃且物品审核正常时恢复物品为 `ON_SALE`，否则转为 `OFF_SHELF`。

## 物品

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| POST | `/items` | 是 | 发布物品，初始状态为 `ON_SALE` |
| GET | `/items` | 否 | 在售物品分页；支持 `categoryId`、`keyword`、`page`、`size` |
| GET | `/items/{id}` | 否 | 在售物品详情 |
| GET | `/items/mine` | 是 | 当前用户全部物品 |
| PUT | `/items/{id}` | 是 | 物主编辑物品 |
| POST | `/items/{id}/off-shelf` | 是 | 物主下架物品 |
| POST | `/items/{id}/publish` | 是 | 物主重新上架物品 |
| GET | `/items/{id}/images` | 否 | 获取在售物品图片列表 |
| POST | `/items/{id}/images` | 是 | 物主上传图片，使用 `multipart/form-data` |
| POST | `/items/{id}/images/{imageId}/primary` | 是 | 物主设置主图 |
| DELETE | `/items/{id}/images/{imageId}` | 是 | 物主删除图片 |
| GET | `/item-images/{imageId}/content` | 否 | 获取图片二进制内容 |

发布或编辑请求字段：`categoryId`、`title`、`description`、`price`、`condition`、`locationText`。

物品成色：`NEW`、`LIKE_NEW`、`GOOD`、`FAIR`。

物品状态：`DRAFT`、`ON_SALE`、`RESERVED`、`SOLD`、`OFF_SHELF`。

物品审核状态：`NORMAL`、`BLOCKED`。`BLOCKED` 物品不会出现在公开列表、详情和图片接口中，卖家也不能编辑、上架或修改图片。物品响应增加 `moderationStatus` 和 `moderationReason`；审核正常时 `moderationReason` 为 `null`。

物品响应的 `images` 字段按 `sortOrder` 升序返回，图片项包含 `id`、`url`、`sortOrder`、`primary`、`createdAt`。上传字段名为 `file`，可选查询参数 `primary=true` 可将新图片设为主图。支持 JPEG、PNG、WebP，单图不超过 5 MiB，每件物品最多 5 张；第一张图片自动成为主图。

## 交易请求

所有交易请求接口均需认证。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/trade-requests` | 买家对在售物品发起请求 |
| GET | `/trade-requests` | 当前用户参与的请求分页列表 |
| GET | `/trade-requests/{id}` | 买家或卖家查看详情 |
| POST | `/trade-requests/{id}/accept` | 卖家接受请求，物品变为 `RESERVED` |
| POST | `/trade-requests/{id}/reject` | 卖家拒绝待处理请求 |
| POST | `/trade-requests/{id}/cancel` | 买家取消待处理请求 |
| POST | `/trade-requests/{id}/confirm-completion` | 买家或卖家确认完成；双方确认后物品变为 `SOLD` |

创建请求：

```json
{
  "itemId": 1,
  "message": "周五可以当面交易"
}
```

交易状态：`PENDING`、`ACCEPTED`、`REJECTED`、`CANCELLED`、`COMPLETED`、`DISPUTED`。

交易进入 `DISPUTED` 后，双方不能再确认完成，需等待管理员仲裁。

## 交易争议与管理员仲裁

交易争议用于处理**已接受交易**中的履约问题；普通举报用于内容违规和账户治理。二者职责分离：

| 场景 | 使用 | 不使用 |
| --- | --- | --- |
| 未交付、货不对板、付款纠纷、爽约 | 交易争议 | 普通举报 |
| 违禁品、虚假信息、骚扰、冒充 | 普通举报 | 交易争议 |
| 屏蔽物品、停用账户 | 举报处罚 | 争议仲裁 |

用户争议接口均需登录，管理员仲裁接口均需 `ADMIN` 角色。

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| POST | `/disputes` | 是 | 交易参与者对 `ACCEPTED` 交易发起争议，成功返回 `201` |
| GET | `/disputes/mine` | 是 | 我参与的争议分页；支持 `status`、`reason`、`page`、`size` |
| GET | `/disputes/{id}` | 参与者 | 查看自己参与的争议 |
| GET | `/admin/disputes` | 管理员 | 争议分页；支持 `status`、`reason`、`page`、`size` |
| GET | `/admin/disputes/{id}` | 管理员 | 查看争议详情 |
| POST | `/admin/disputes/{id}/resolve` | 管理员 | 仲裁争议 |
| POST | `/admin/disputes/{id}/reject` | 管理员 | 驳回争议，交易恢复为 `ACCEPTED` |

发起争议请求：

```json
{
  "tradeRequestId": 1,
  "reason": "ITEM_NOT_AS_DESCRIBED",
  "description": "当面核验后发现物品成色与描述明显不符。"
}
```

争议原因：`NON_DELIVERY`、`ITEM_NOT_AS_DESCRIBED`、`PAYMENT_ISSUE`、`NO_SHOW`、`OTHER`。说明去除首尾空格后长度为 10-1000 个字符。

管理员仲裁请求：

```json
{
  "resolution": "CANCEL_TRADE",
  "resolutionNote": "核验后同意取消本次交易。"
}
```

仲裁结果：

| resolution | 效果 |
| --- | --- |
| `COMPLETE_TRADE` | 交易变为 `COMPLETED`，物品变为 `SOLD`，双方可评价 |
| `CANCEL_TRADE` | 交易变为 `CANCELLED`；卖家活跃且物品审核正常时恢复 `ON_SALE`，否则 `OFF_SHELF` |
| `NONE` | 仅用于驳回接口，表示不改变交易结果以外的处罚 |

驳回请求使用 `resolutionNote`，去除首尾空格后长度为 2-1000 个字符。

业务规则：

- 仅 `ACCEPTED` 交易可发起争议；`PENDING`、已完成或已取消交易不可发起。
- 同一交易最多存在一条 `OPEN` 争议。
- 只有买卖双方可发起和查看自己的争议；第三方不可访问。
- 争议打开后交易进入 `DISPUTED`，确认完成被冻结。
- 驳回争议后交易恢复 `ACCEPTED`，双方可继续确认完成。
- 屏蔽物品或停用账户时，相关 `DISPUTED` 交易会被取消，开放争议自动关闭。
- 争议不执行屏蔽物品或停用账户；若需内容治理，应走举报流程。

## 评价与信誉分

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| POST | `/trade-requests/{id}/reviews` | 是 | 当前参与者评价已完成交易的另一方 |
| GET | `/trade-requests/{id}/reviews` | 是 | 交易参与者查看双方评价 |
| GET | `/users/{id}/reviews` | 否 | 分页查看用户收到的评价 |

创建评价请求：

```json
{
  "rating": 5,
  "comment": "沟通顺畅，物品符合描述。"
}
```

评分必须为 `1-5`，评价内容最长 1000 个字符。仅 `COMPLETED` 交易可以评价，每位参与者每笔交易最多评价一次，评价对象由交易关系自动确定。用户信誉分为其收到的全部评分平均值，四舍五入保留两位小数；尚未收到评价时保持初始值 `5.00`。

## 站内消息与通知

所有消息接口均需登录。消息仅接收者本人可查看；系统在交易状态变化与举报处理结果产生时自动写入通知。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/messages` | 当前用户消息分页；支持 `unreadOnly`、`messageType`、`page`、`size` |
| GET | `/messages/unread-count` | 当前用户未读数量 |
| GET | `/messages/{id}` | 查看单条消息详情 |
| POST | `/messages/{id}/read` | 将单条消息标为已读 |
| POST | `/messages/read-all` | 将当前用户全部未读消息标为已读 |

消息响应字段：`id`、`messageType`、`content`、`read`、`senderId`、`senderUsername`、`tradeRequestId`、`reportId`、`itemId`、`createdAt`。系统通知的 `senderId` 与 `senderUsername` 可能为 `null`。

当前 `messageType`：

| 类型 | 触发场景 |
| --- | --- |
| `TRADE_REQUEST_CREATED` | 买家发起交易，通知卖家 |
| `TRADE_REQUEST_ACCEPTED` | 卖家接受交易，通知买家 |
| `TRADE_REQUEST_REJECTED` | 卖家拒绝或因接受其他请求自动拒绝，通知买家 |
| `TRADE_REQUEST_CANCELLED` | 买家取消待处理请求，通知卖家 |
| `TRADE_CONFIRMATION_RECEIVED` | 一方确认完成，通知另一方 |
| `TRADE_COMPLETED` | 双方确认后交易完成，通知买卖双方 |
| `TRADE_CANCELLED_BY_MODERATION` | 屏蔽物品或停用账户导致未完成交易取消 |
| `REPORT_RESOLVED` | 举报处理完成，通知举报人 |
| `REPORT_REJECTED` | 举报被驳回，通知举报人 |
| `ITEM_BLOCKED` | 物品被屏蔽，通知卖家 |
| `ITEM_RESTORED` | 物品解除屏蔽，通知卖家 |
| `ACCOUNT_SUSPENDED` | 账户被停用，通知被停用用户 |
| `DISPUTE_OPENED` | 交易进入争议，通知双方 |
| `DISPUTE_RESOLVED` | 争议仲裁完成，通知双方 |
| `DISPUTE_REJECTED` | 争议被驳回或因治理自动关闭，通知双方 |

消息响应另含 `disputeId`（可空），便于前端跳转争议详情。

## 健康检查

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| GET | `/health` | 否 | 检查应用和 PostgreSQL 连接 |

## 关键代码位置

| 模块 | 位置 |
| --- | --- |
| 安全与 JWT | `CampusEcho/src/main/java/top/sharpcaterpillar/campusecho/security/` |
| 用户 | `CampusEcho/src/main/java/top/sharpcaterpillar/campusecho/user/` |
| 分类 | `CampusEcho/src/main/java/top/sharpcaterpillar/campusecho/category/` |
| 物品 | `CampusEcho/src/main/java/top/sharpcaterpillar/campusecho/item/` |
| 图片存储配置 | `CampusEcho/src/main/resources/application.yaml` |
| 通用图片校验 | `CampusEcho/src/main/java/top/sharpcaterpillar/campusecho/common/storage/` |
| 交易 | `CampusEcho/src/main/java/top/sharpcaterpillar/campusecho/trade/` |
| 评价 | `CampusEcho/src/main/java/top/sharpcaterpillar/campusecho/review/` |
| 管理端 | `CampusEcho/src/main/java/top/sharpcaterpillar/campusecho/admin/` |
| 举报与治理 | `CampusEcho/src/main/java/top/sharpcaterpillar/campusecho/report/` |
| 交易争议 | `CampusEcho/src/main/java/top/sharpcaterpillar/campusecho/dispute/` |
| 站内消息 | `CampusEcho/src/main/java/top/sharpcaterpillar/campusecho/message/` |
| 数据库迁移 | `CampusEcho/src/main/resources/db/migration/` |

## 尚未实现

- 举报证据图片和自动处罚。
