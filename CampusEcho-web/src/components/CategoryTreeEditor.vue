<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Delete, EditPen, Plus, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createCategory,
  deleteCategory,
  fetchCategoryTree,
  updateCategory,
} from '../api/categories'
import type { CategoryTreeResponse, CategoryWriteRequest } from '../api/types'
import { getErrorMessage, getFieldErrors } from '../utils/error'
import {
  flattenCategoryTree,
  toParentOptions,
  type FlatCategory,
} from '../utils/category'
import EmptyState from './EmptyState.vue'
import ProblemAlert from './ProblemAlert.vue'

const tree = ref<CategoryTreeResponse[]>([])
const loading = ref(true)
const saving = ref(false)
const errorMessage = ref('')
const formError = ref('')
const fieldErrors = ref<Record<string, string>>({})
/** 树接口不含 sortOrder，用写接口响应缓存 */
const sortOrderCache = ref<Record<number, number>>({})

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)

const form = reactive({
  name: '',
  parentId: null as number | null,
  sortOrder: 0,
})

const flatRows = computed(() => flattenCategoryTree(tree.value))

const parentOptions = computed(() =>
  toParentOptions(tree.value, dialogMode.value === 'edit' ? editingId.value : null),
)

const dialogTitle = computed(() =>
  dialogMode.value === 'create' ? '新增分类' : '编辑分类',
)

function clearFormErrors() {
  formError.value = ''
  fieldErrors.value = {}
}

function resetForm() {
  form.name = ''
  form.parentId = null
  form.sortOrder = 0
  editingId.value = null
  clearFormErrors()
}

async function loadTree() {
  loading.value = true
  errorMessage.value = ''
  try {
    tree.value = await fetchCategoryTree()
  } catch (error) {
    tree.value = []
    errorMessage.value = getErrorMessage(error, '无法加载分类树')
  } finally {
    loading.value = false
  }
}

function openCreate(parentId: number | null = null) {
  dialogMode.value = 'create'
  resetForm()
  form.parentId = parentId
  form.sortOrder = 0
  dialogVisible.value = true
}

function openEdit(row: FlatCategory) {
  dialogMode.value = 'edit'
  resetForm()
  editingId.value = row.id
  form.name = row.name
  form.parentId = row.parentId
  form.sortOrder = sortOrderCache.value[row.id] ?? 0
  dialogVisible.value = true
}

function buildPayload(): CategoryWriteRequest {
  return {
    name: form.name.trim(),
    parentId: form.parentId,
    sortOrder: Number.isFinite(form.sortOrder) ? Math.max(0, Math.floor(form.sortOrder)) : 0,
  }
}

async function handleSubmit() {
  if (saving.value) return
  clearFormErrors()

  if (!form.name.trim()) {
    fieldErrors.value = { name: '分类名称不能为空' }
    return
  }
  if (form.name.trim().length > 64) {
    fieldErrors.value = { name: '分类名称不能超过 64 个字符' }
    return
  }
  if (form.sortOrder < 0) {
    fieldErrors.value = { sortOrder: '排序值不能小于 0' }
    return
  }

  const payload = buildPayload()
  saving.value = true
  try {
    if (dialogMode.value === 'create') {
      const created = await createCategory(payload)
      sortOrderCache.value = { ...sortOrderCache.value, [created.id]: created.sortOrder }
      ElMessage.success('分类已创建')
    } else if (editingId.value != null) {
      const updated = await updateCategory(editingId.value, payload)
      sortOrderCache.value = { ...sortOrderCache.value, [updated.id]: updated.sortOrder }
      ElMessage.success('分类已更新')
    }
    dialogVisible.value = false
    await loadTree()
  } catch (error) {
    fieldErrors.value = getFieldErrors(error)
    formError.value = getErrorMessage(error, '保存分类失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: FlatCategory) {
  try {
    await ElMessageBox.confirm(
      `确定删除分类「${row.name}」吗？若仍有子分类或被物品使用将无法删除。`,
      '删除分类',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }

  try {
    await deleteCategory(row.id)
    ElMessage.success('分类已删除')
    await loadTree()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '删除失败'))
  }
}

watch(dialogVisible, (open) => {
  if (!open) {
    clearFormErrors()
  }
})

onMounted(() => {
  void loadTree()
})

defineExpose({ loadTree })
</script>

<template>
  <section class="editor">
    <header class="toolbar">
      <div>
        <h2>分类树</h2>
        <p>维护物品分类层级。删除前请确保无子分类且未被物品引用。</p>
      </div>
      <div class="toolbar-actions">
        <el-button :icon="RefreshRight" :loading="loading" @click="loadTree">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate(null)">新增根分类</el-button>
      </div>
    </header>

    <div v-if="loading" class="loading-box">加载分类中…</div>

    <template v-else-if="errorMessage">
      <ProblemAlert title="加载失败" :message="errorMessage" @close="errorMessage = ''" />
      <EmptyState title="无法加载分类" :description="errorMessage">
        <el-button type="primary" @click="loadTree">重试</el-button>
      </EmptyState>
    </template>

    <EmptyState
      v-else-if="flatRows.length === 0"
      title="还没有分类"
      description="先创建根分类，再按需添加子分类。"
    >
      <el-button type="primary" @click="openCreate(null)">新增根分类</el-button>
    </EmptyState>

    <div v-else class="table-wrap">
      <table class="cat-table">
        <thead>
          <tr>
            <th>名称</th>
            <th>路径</th>
            <th>子分类</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in flatRows" :key="row.id">
            <td>
              <span class="name-cell" :style="{ paddingLeft: `${row.depth * 18}px` }">
                <span v-if="row.depth > 0" class="branch" aria-hidden="true">└</span>
                <strong>{{ row.name }}</strong>
                <span class="id-tag">#{{ row.id }}</span>
              </span>
            </td>
            <td class="path-cell" :title="row.path">{{ row.path }}</td>
            <td>{{ row.childrenCount }}</td>
            <td>
              <div class="row-actions">
                <el-button size="small" :icon="Plus" @click="openCreate(row.id)">子级</el-button>
                <el-button size="small" :icon="EditPen" @click="openEdit(row)">编辑</el-button>
                <el-button
                  size="small"
                  type="danger"
                  plain
                  :icon="Delete"
                  @click="handleDelete(row)"
                >
                  删除
                </el-button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="min(480px, 92vw)"
      destroy-on-close
      @closed="resetForm"
    >
      <ProblemAlert
        v-if="formError"
        title="保存失败"
        :message="formError"
        @close="clearFormErrors"
      />

      <el-form label-position="top" size="large" @submit.prevent="handleSubmit">
        <el-form-item label="分类名称" required :error="fieldErrors.name">
          <el-input
            v-model="form.name"
            maxlength="64"
            show-word-limit
            placeholder="例如：教材"
            @input="clearFormErrors"
          />
        </el-form-item>

        <el-form-item label="父分类（可选）" :error="fieldErrors.parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            check-strictly
            clearable
            filterable
            placeholder="不选则为根分类"
            style="width: 100%"
            :props="{ label: 'label', value: 'value', children: 'children' }"
            @change="clearFormErrors"
          />
          <p class="field-hint">编辑时不能选择自身或其子分类作为父级。</p>
        </el-form-item>

        <el-form-item label="排序值" :error="fieldErrors.sortOrder">
          <el-input-number
            v-model="form.sortOrder"
            :min="0"
            :step="1"
            controls-position="right"
            style="width: 100%"
            @change="clearFormErrors"
          />
          <p class="field-hint">同级按排序值升序展示，值越小越靠前。</p>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button :disabled="saving" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.editor {
  display: grid;
  gap: 16px;
}

.toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.toolbar h2 {
  margin: 0 0 6px;
  font-size: 16px;
}

.toolbar p {
  margin: 0;
  color: var(--ce-color-muted);
  font-size: 13px;
  line-height: 1.5;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex-shrink: 0;
}

.loading-box {
  padding: 28px;
  text-align: center;
  color: var(--ce-color-muted);
  background: var(--ce-color-surface);
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius);
}

.table-wrap {
  overflow-x: auto;
  background: var(--ce-color-surface);
  border: 1px solid var(--ce-color-border);
  border-radius: var(--ce-radius);
  box-shadow: var(--ce-shadow-sm);
}

.cat-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 640px;
}

.cat-table th,
.cat-table td {
  padding: 12px 14px;
  text-align: left;
  border-bottom: 1px solid var(--ce-color-border);
  font-size: 14px;
  vertical-align: middle;
}

.cat-table th {
  background: #f9fafb;
  color: var(--ce-color-muted);
  font-weight: 600;
  font-size: 12px;
}

.cat-table tr:last-child td {
  border-bottom: 0;
}

.name-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.branch {
  color: var(--ce-color-muted);
  font-family: monospace;
}

.id-tag {
  color: var(--ce-color-muted);
  font-size: 12px;
  font-weight: 400;
}

.path-cell {
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ce-color-muted);
  font-size: 13px;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.field-hint {
  margin: 6px 0 0;
  color: var(--ce-color-muted);
  font-size: 12px;
  line-height: 1.4;
}

@media (max-width: 720px) {
  .toolbar {
    flex-direction: column;
  }

  .toolbar-actions {
    width: 100%;
  }

  .toolbar-actions .el-button {
    flex: 1;
  }
}
</style>
