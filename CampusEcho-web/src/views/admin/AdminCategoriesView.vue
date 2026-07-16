<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import CategoryTreeEditor from '../../components/CategoryTreeEditor.vue'
import EmptyState from '../../components/EmptyState.vue'

const auth = useAuthStore()
const router = useRouter()
</script>

<template>
  <section class="ce-page">
    <header class="ce-page-header">
      <div>
        <p class="ce-eyebrow">管理后台</p>
        <h1>分类管理</h1>
        <p class="ce-page-desc">仅管理员可维护分类树。写操作会再次校验服务端角色与账户状态。</p>
      </div>
    </header>

    <EmptyState
      v-if="!auth.isAuthenticated"
      title="请先登录"
      description="分类管理需要管理员账户登录后访问。"
    >
      <el-button type="primary" @click="router.push({ name: 'login', query: { redirect: '/admin/categories' } })">
        去登录
      </el-button>
    </EmptyState>

    <EmptyState
      v-else-if="!auth.isAdmin"
      title="权限不足"
      description="当前账户不是管理员，无法访问分类管理。后端仍会拒绝非管理员写操作。"
    >
      <el-button type="primary" @click="router.push({ name: 'home' })">返回市场</el-button>
    </EmptyState>

    <div v-else class="ce-panel">
      <CategoryTreeEditor />
    </div>
  </section>
</template>
