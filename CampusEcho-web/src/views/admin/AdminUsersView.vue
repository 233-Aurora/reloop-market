<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { RefreshRight, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchAdminUser,
  fetchAdminUsers,
  updateAdminUserRole,
  updateAdminUserStatus,
} from '../../api/admin'
import type { AdminUserResponse, UserRole, UserStatus } from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import {
  canChangeUserRole,
  canChangeUserStatus,
  roleLabel,
  statusLabel,
} from '../../utils/admin'
import { getErrorMessage } from '../../utils/error'
import { formatDateTime, resolveAssetUrl } from '../../utils/format'
import EmptyState from '../../components/EmptyState.vue'
import ProblemAlert from '../../components/ProblemAlert.vue'

const PAGE_SIZE = 20
const auth = useAuthStore()
const router = useRouter()

const keywordInput = ref('')
const keyword = ref('')
const roleFilter = ref<'ALL' | UserRole>('ALL')
const statusFilter = ref<'ALL' | UserStatus>('ALL')
const page = ref(1)

const users = ref<AdminUserResponse[]>([])
const totalElements = ref(0)
const totalPages = ref(0)
const loading = ref(true)
const errorMessage = ref('')
const actingId = ref<number | null>(null)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detailUser = ref<AdminUserResponse | null>(null)

const actorId = computed(() => auth.user?.id ?? 0)

const summary = computed(() => {
  if (loading.value) return '加载中…'
  if (errorMessage.value) return '加载失败'
  return `共 ${totalElements.value} 位用户`
})

async function loadUsers() {
  if (!auth.isAdmin) {
    loading.value = false
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    const result = await fetchAdminUsers({
      keyword: keyword.value || undefined,
      role: roleFilter.value === 'ALL' ? undefined : roleFilter.value,
      status: statusFilter.value === 'ALL' ? undefined : statusFilter.value,
      page: page.value,
      size: PAGE_SIZE,
    })
    users.value = result.content
    totalElements.value = result.totalElements
    totalPages.value = result.totalPages
    if (result.totalPages > 0 && page.value > result.totalPages) {
      page.value = result.totalPages
    }
  } catch (error) {
    users.value = []
    totalElements.value = 0
    totalPages.value = 0
    errorMessage.value = getErrorMessage(error, '无法加载用户列表')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  keyword.value = keywordInput.value.trim()
  page.value = 1
  void loadUsers()
}

function handlePageChange(next: number) {
  page.value = next
  void loadUsers()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function replaceUser(updated: AdminUserResponse) {
  users.value = users.value.map((row) => (row.id === updated.id ? updated : row))
  if (detailUser.value?.id === updated.id) {
    detailUser.value = updated
  }
}

async function openDetail(userId: number) {
  detailVisible.value = true
  detailLoading.value = true
  detailError.value = ''
  detailUser.value = null
  try {
    detailUser.value = await fetchAdminUser(userId)
  } catch (error) {
    detailError.value = getErrorMessage(error, '无法加载用户详情')
  } finally {
    detailLoading.value = false
  }
}

async function changeStatus(user: AdminUserResponse, status: UserStatus) {
  const blocked = canChangeUserStatus(actorId.value, user, status)
  if (blocked) {
    ElMessage.warning(blocked)
    return
  }

  const actionText = status === 'SUSPENDED' ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定${actionText}用户「${user.displayName}（@${user.username}）」吗？${
        status === 'SUSPENDED' ? '停用后其进行中的交易可能被系统取消。' : ''
      }`,
      `${actionText}账户`,
      {
        type: 'warning',
        confirmButtonText: actionText,
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }

  actingId.value = user.id
  try {
    const updated = await updateAdminUserStatus(user.id, status)
    replaceUser(updated)
    ElMessage.success(`已${actionText}该用户`)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, `${actionText}失败`))
  } finally {
    actingId.value = null
  }
}

async function changeRole(user: AdminUserResponse, role: UserRole) {
  const blocked = canChangeUserRole(actorId.value, user, role)
  if (blocked) {
    ElMessage.warning(blocked)
    return
  }

  const actionText = role === 'ADMIN' ? '设为管理员' : '降为普通用户'
  try {
    await ElMessageBox.confirm(
      `确定将「${user.displayName}（@${user.username}）」${actionText}吗？角色变更对旧令牌立即生效。`,
      '调整角色',
      {
        type: 'warning',
        confirmButtonText: '确认',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }

  actingId.value = user.id
  try {
    const updated = await updateAdminUserRole(user.id, role)
    replaceUser(updated)
    ElMessage.success('角色已更新')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '调整角色失败'))
  } finally {
    actingId.value = null
  }
}

watch([roleFilter, statusFilter], () => {
  page.value = 1
  void loadUsers()
})

onMounted(() => {
  void loadUsers()
})
</script>

<template>
  <section class="ce-page">
    <header class="ce-page-header">
      <div>
        <p class="ce-eyebrow">管理后台</p>
        <h1>用户管理</h1>
        <p class="ce-page-desc">
          检索用户、启停账户与调整角色。不能停用或降级自己；系统至少保留一名活跃管理员。
        </p>
      </div>
      <el-button @click="router.push({ name: 'admin-dashboard' })">管理概览</el-button>
    </header>

    <EmptyState
      v-if="!auth.isAuthenticated"
      title="请先登录"
      description="用户管理需要管理员账户访问。"
    >
      <el-button
        type="primary"
        @click="router.push({ name: 'login', query: { redirect: '/admin/users' } })"
      >
        去登录
      </el-button>
    </EmptyState>

    <EmptyState
      v-else-if="!auth.isAdmin"
      title="权限不足"
      description="当前账户不是管理员，无法管理用户。"
    >
      <el-button type="primary" @click="router.push({ name: 'home' })">返回市场</el-button>
    </EmptyState>

    <template v-else>
      <div class="toolbar">
        <el-input
          v-model="keywordInput"
          clearable
          placeholder="搜索用户名、邮箱或昵称"
          :prefix-icon="Search"
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
          </template>
        </el-input>

        <el-select v-model="roleFilter" placeholder="角色" style="width: 140px">
          <el-option label="全部角色" value="ALL" />
          <el-option label="普通用户" value="USER" />
          <el-option label="管理员" value="ADMIN" />
        </el-select>

        <el-select v-model="statusFilter" placeholder="状态" style="width: 140px">
          <el-option label="全部状态" value="ALL" />
          <el-option label="正常" value="ACTIVE" />
          <el-option label="已停用" value="SUSPENDED" />
        </el-select>
      </div>

      <div class="ce-result-bar">
        <p>{{ summary }}</p>
        <el-button text :icon="RefreshRight" :loading="loading" @click="loadUsers">
          刷新
        </el-button>
      </div>

      <ProblemAlert
        v-if="errorMessage"
        title="加载失败"
        :message="errorMessage"
        @close="errorMessage = ''"
      />

      <div v-if="loading" class="ce-panel loading-box">加载用户列表…</div>

      <EmptyState
        v-else-if="users.length === 0 && !errorMessage"
        title="没有匹配的用户"
        description="试试调整关键词或筛选条件。"
      >
        <el-button
          @click="
            keywordInput = '';
            keyword = '';
            roleFilter = 'ALL';
            statusFilter = 'ALL';
            page = 1;
            loadUsers()
          "
        >
          清除筛选
        </el-button>
      </EmptyState>

      <div v-else-if="users.length > 0" class="table-wrap">
        <table class="user-table">
          <thead>
            <tr>
              <th>用户</th>
              <th>联系方式</th>
              <th>角色</th>
              <th>状态</th>
              <th>信誉</th>
              <th>注册时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id">
              <td>
                <div class="user-cell">
                  <span class="avatar" aria-hidden="true">
                    <img
                      v-if="resolveAssetUrl(user.avatarUrl)"
                      :src="resolveAssetUrl(user.avatarUrl)!"
                      :alt="user.displayName"
                    />
                    <span v-else>{{ user.displayName.slice(0, 1) }}</span>
                  </span>
                  <div>
                    <strong>{{ user.displayName }}</strong>
                    <p>@{{ user.username }} · #{{ user.id }}</p>
                  </div>
                </div>
              </td>
              <td>
                <div class="contact">
                  <span>{{ user.email }}</span>
                  <span class="muted">{{ user.phone || '无手机号' }}</span>
                </div>
              </td>
              <td>
                <span class="tag" :class="user.role === 'ADMIN' ? 'tag-admin' : 'tag-user'">
                  {{ roleLabel(user.role) }}
                </span>
              </td>
              <td>
                <span
                  class="tag"
                  :class="user.status === 'ACTIVE' ? 'tag-active' : 'tag-suspended'"
                >
                  {{ statusLabel(user.status) }}
                </span>
              </td>
              <td>{{ user.reputationScore }}</td>
              <td :title="formatDateTime(user.createdAt)">
                {{ formatDateTime(user.createdAt) }}
              </td>
              <td>
                <div class="row-actions">
                  <el-button size="small" @click="openDetail(user.id)">详情</el-button>
                  <el-button
                    v-if="user.status === 'ACTIVE'"
                    size="small"
                    type="danger"
                    plain
                    :loading="actingId === user.id"
                    :disabled="user.id === actorId"
                    @click="changeStatus(user, 'SUSPENDED')"
                  >
                    停用
                  </el-button>
                  <el-button
                    v-else
                    size="small"
                    type="success"
                    plain
                    :loading="actingId === user.id"
                    @click="changeStatus(user, 'ACTIVE')"
                  >
                    启用
                  </el-button>
                  <el-button
                    v-if="user.role === 'USER'"
                    size="small"
                    type="primary"
                    plain
                    :loading="actingId === user.id"
                    @click="changeRole(user, 'ADMIN')"
                  >
                    升权
                  </el-button>
                  <el-button
                    v-else
                    size="small"
                    plain
                    :loading="actingId === user.id"
                    :disabled="user.id === actorId"
                    @click="changeRole(user, 'USER')"
                  >
                    降权
                  </el-button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="totalPages > 1" class="ce-pagination">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="page"
          :page-size="PAGE_SIZE"
          :total="totalElements"
          @current-change="handlePageChange"
        />
      </div>
    </template>

    <el-drawer v-model="detailVisible" title="用户详情" size="min(420px, 92vw)" destroy-on-close>
      <div v-if="detailLoading" class="detail-loading">加载中…</div>
      <ProblemAlert
        v-else-if="detailError"
        title="加载失败"
        :message="detailError"
        @close="detailError = ''"
      />
      <template v-else-if="detailUser">
        <div class="detail-head">
          <span class="avatar large" aria-hidden="true">
            <img
              v-if="resolveAssetUrl(detailUser.avatarUrl)"
              :src="resolveAssetUrl(detailUser.avatarUrl)!"
              :alt="detailUser.displayName"
            />
            <span v-else>{{ detailUser.displayName.slice(0, 1) }}</span>
          </span>
          <div>
            <h3>{{ detailUser.displayName }}</h3>
            <p>@{{ detailUser.username }}</p>
          </div>
        </div>
        <dl class="detail-meta">
          <div>
            <dt>用户 ID</dt>
            <dd>{{ detailUser.id }}</dd>
          </div>
          <div>
            <dt>邮箱</dt>
            <dd>{{ detailUser.email }}</dd>
          </div>
          <div>
            <dt>手机号</dt>
            <dd>{{ detailUser.phone || '未填写' }}</dd>
          </div>
          <div>
            <dt>校区</dt>
            <dd>{{ detailUser.campus || '未填写' }}</dd>
          </div>
          <div>
            <dt>角色</dt>
            <dd>{{ roleLabel(detailUser.role) }}</dd>
          </div>
          <div>
            <dt>状态</dt>
            <dd>{{ statusLabel(detailUser.status) }}</dd>
          </div>
          <div>
            <dt>信誉分</dt>
            <dd>{{ detailUser.reputationScore }}</dd>
          </div>
          <div>
            <dt>注册时间</dt>
            <dd>{{ formatDateTime(detailUser.createdAt) }}</dd>
          </div>
        </dl>
        <p class="privacy-note">管理详情可查看邮箱与手机号，请勿外泄。界面不展示密码。</p>
        <div class="detail-actions">
          <el-button
            v-if="detailUser.status === 'ACTIVE'"
            type="danger"
            plain
            :disabled="detailUser.id === actorId"
            :loading="actingId === detailUser.id"
            @click="changeStatus(detailUser, 'SUSPENDED')"
          >
            停用账户
          </el-button>
          <el-button
            v-else
            type="success"
            plain
            :loading="actingId === detailUser.id"
            @click="changeStatus(detailUser, 'ACTIVE')"
          >
            启用账户
          </el-button>
          <el-button
            v-if="detailUser.role === 'USER'"
            type="primary"
            plain
            :loading="actingId === detailUser.id"
            @click="changeRole(detailUser, 'ADMIN')"
          >
            设为管理员
          </el-button>
          <el-button
            v-else
            plain
            :disabled="detailUser.id === actorId"
            :loading="actingId === detailUser.id"
            @click="changeRole(detailUser, 'USER')"
          >
            降为普通用户
          </el-button>
        </div>
      </template>
    </el-drawer>
  </section>
</template>

<style scoped>
.toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 140px 140px;
  gap: 10px;
}

.loading-box {
  text-align: center;
  color: var(--ce-color-muted);
  padding: 32px;
}

.table-wrap {
  overflow-x: auto;
  background: var(--ce-color-surface);
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius);
  box-shadow: var(--ce-shadow-sm);
}

.user-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 960px;
}

.user-table th,
.user-table td {
  padding: 12px 14px;
  text-align: left;
  border-bottom: 1px solid var(--ce-color-border);
  font-size: 13px;
  vertical-align: middle;
}

.user-table th {
  background: #f9fafb;
  color: var(--ce-color-muted);
  font-size: 12px;
  font-weight: 600;
}

.user-table tr:last-child td {
  border-bottom: 0;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--ce-color-primary-soft);
  color: var(--ce-color-primary);
  font-weight: 700;
  flex-shrink: 0;
}

.avatar.large {
  width: 56px;
  height: 56px;
  font-size: 20px;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-cell strong {
  display: block;
  font-size: 14px;
}

.user-cell p,
.contact .muted,
.detail-head p {
  margin: 2px 0 0;
  color: var(--ce-color-muted);
  font-size: 12px;
}

.contact {
  display: grid;
  gap: 2px;
}

.tag {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.tag-admin {
  background: #eff8ff;
  color: #175cd3;
}

.tag-user {
  background: #f2f4f7;
  color: #344054;
}

.tag-active {
  background: #e6f2ef;
  color: #0f574c;
}

.tag-suspended {
  background: #fef3f2;
  color: #b42318;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.detail-loading {
  color: var(--ce-color-muted);
  padding: 24px 0;
  text-align: center;
}

.detail-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.detail-head h3 {
  margin: 0;
  font-size: 18px;
}

.detail-meta {
  margin: 0;
  display: grid;
  gap: 12px;
}

.detail-meta > div {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 8px;
  font-size: 14px;
}

.detail-meta dt {
  color: var(--ce-color-muted);
}

.detail-meta dd {
  margin: 0;
  word-break: break-word;
}

.privacy-note {
  margin: 16px 0;
  padding-top: 14px;
  border-top: 1px solid var(--ce-color-border);
  color: var(--ce-color-muted);
  font-size: 12px;
  line-height: 1.5;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 960px) {
  .toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
