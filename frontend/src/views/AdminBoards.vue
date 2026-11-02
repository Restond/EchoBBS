<template>
  <div>
    <AppHeader />

    <!-- 页面标题区 -->
    <div class="page-head">
      <div class="breadcrumb">
        <router-link to="/">首页</router-link>
        <span class="sep">/</span>
        <span>管理后台</span>
        <span class="sep">/</span>
        <span>板块管理</span>
      </div>
      <div class="title-row">
        <h1 class="page-title">板块管理</h1>
        <button class="btn-primary" @click="openCreate">+ 新建板块</button>
      </div>
    </div>

    <!-- 板块列表 -->
    <div class="content-wrap">
      <div v-if="loading" class="state-box">加载中...</div>
      <div v-else-if="boards.length === 0" class="state-box">暂无板块</div>
      <table v-else class="data-table">
        <thead>
          <tr>
            <th class="col-id">ID</th>
            <th class="col-name">名称</th>
            <th class="col-slug">标识</th>
            <th class="col-desc">描述</th>
            <th class="col-sort">排序</th>
            <th class="col-actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="b in boards" :key="b.id">
            <td>{{ b.id }}</td>
            <td>{{ b.name }}</td>
            <td><code>{{ b.slug }}</code></td>
            <td class="cell-desc">{{ b.description || '-' }}</td>
            <td>{{ b.sortOrder ?? 0 }}</td>
            <td>
              <button class="btn-link" @click="openEdit(b)">编辑</button>
              <button class="btn-link btn-danger" @click="remove(b)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 编辑/新建 弹窗 -->
    <div v-if="modalOpen" class="modal-mask" @click.self="closeModal">
      <div class="modal">
        <div class="modal-head">
          <h2 class="modal-title">{{ editing ? '编辑板块' : '新建板块' }}</h2>
          <button class="modal-close" @click="closeModal">×</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <label class="form-label"><span class="required">*</span>名称</label>
            <input
              v-model="form.name"
              type="text"
              class="form-input"
              maxlength="64"
              placeholder="板块名称"
            />
          </div>
          <div class="form-row">
            <label class="form-label"><span class="required">*</span>标识</label>
            <input
              v-model="form.slug"
              type="text"
              class="form-input"
              maxlength="64"
              placeholder="小写字母、数字、连字符"
            />
            <div class="form-hint">用于 URL，如：tech-news</div>
          </div>
          <div class="form-row">
            <label class="form-label">描述</label>
            <textarea
              v-model="form.description"
              class="form-input form-textarea"
              rows="3"
              maxlength="512"
              placeholder="板块描述（可选）"
            ></textarea>
          </div>
          <div class="form-row">
            <label class="form-label">排序</label>
            <input
              v-model.number="form.sortOrder"
              type="number"
              class="form-input"
              placeholder="0"
            />
            <div class="form-hint">数字越小越靠前</div>
          </div>
          <div v-if="modalMessage" class="inline-msg" :class="modalMessageType">{{ modalMessage }}</div>
        </div>
        <div class="modal-foot">
          <button class="btn-cancel" @click="closeModal">取消</button>
          <button class="btn-primary" :disabled="saving" @click="save">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import AppHeader from '@/components/AppHeader.vue'
import { api } from '@/api'
import type { Board } from '@/types'

const boards = ref<Board[]>([])
const loading = ref(true)

const modalOpen = ref(false)
const editing = ref(false)
const saving = ref(false)
const modalMessage = ref('')
const modalMessageType = ref<'success' | 'error'>('success')

const form = reactive({
  id: 0,
  name: '',
  slug: '',
  description: '',
  sortOrder: 0
})

const fetchBoards = async () => {
  loading.value = true
  try {
    const res = await api.get('/api/boards')
    boards.value = res.data.data || []
  } catch {
    boards.value = []
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.id = 0
  form.name = ''
  form.slug = ''
  form.description = ''
  form.sortOrder = 0
  modalMessage.value = ''
}

const openCreate = () => {
  editing.value = false
  resetForm()
  modalOpen.value = true
}

const openEdit = (b: Board) => {
  editing.value = true
  form.id = b.id
  form.name = b.name
  form.slug = b.slug
  form.description = b.description || ''
  form.sortOrder = b.sortOrder ?? 0
  modalMessage.value = ''
  modalOpen.value = true
}

const closeModal = () => {
  modalOpen.value = false
}

const save = async () => {
  if (!form.name.trim() || !form.slug.trim()) {
    modalMessage.value = '名称和标识不能为空'
    modalMessageType.value = 'error'
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      slug: form.slug.trim(),
      description: form.description.trim(),
      sortOrder: form.sortOrder ?? 0
    }
    if (editing.value) {
      await api.put(`/api/admin/boards/${form.id}`, payload)
    } else {
      await api.post('/api/admin/boards', payload)
    }
    modalOpen.value = false
    await fetchBoards()
  } catch (err: any) {
    modalMessage.value = err.response?.data?.message || '保存失败'
    modalMessageType.value = 'error'
  } finally {
    saving.value = false
  }
}

const remove = async (b: Board) => {
  if (!confirm(`确定删除板块「${b.name}」吗？该操作不可撤销。`)) return
  try {
    await api.delete(`/api/admin/boards/${b.id}`)
    await fetchBoards()
  } catch (err: any) {
    alert(err.response?.data?.message || '删除失败')
  }
}

onMounted(() => {
  fetchBoards()
})
</script>

<style scoped>
.page-head {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 24px 16px;
}

.breadcrumb {
  font-size: 13px;
  color: var(--text-dim);
  margin-bottom: 12px;
}

.breadcrumb a:hover {
  color: var(--accent);
}

.breadcrumb .sep {
  margin: 0 8px;
  color: #3d4856;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  letter-spacing: -0.5px;
  color: var(--text);
}

.content-wrap {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 64px;
}

.state-box {
  text-align: center;
  padding: 48px 20px;
  color: var(--text-dim);
  font-size: 15px;
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 12px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
}

.data-table th,
.data-table td {
  padding: 12px 16px;
  text-align: left;
  font-size: 14px;
  border-bottom: 1px solid var(--border);
}

.data-table th {
  background-color: rgba(110, 118, 129, 0.15);
  color: var(--text-dim);
  font-weight: 500;
  font-size: 13px;
}

.data-table tr:last-child td {
  border-bottom: none;
}

.data-table td {
  color: var(--text);
}

.col-id { width: 60px; }
.col-slug { width: 160px; }
.col-sort { width: 80px; }
.col-actions { width: 140px; }

.cell-desc {
  color: var(--text-dim);
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

code {
  background-color: rgba(110, 118, 129, 0.2);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  font-family: 'SFMono-Regular', Consolas, monospace;
}

.btn-link {
  background: none;
  border: none;
  color: var(--accent-hover);
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
  margin-right: 4px;
}

.btn-link:hover {
  text-decoration: underline;
}

.btn-link.btn-danger {
  color: #f85149;
}

.btn-primary {
  padding: 8px 24px;
  background-color: var(--accent);
  color: #06140b;
  font-size: 14px;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.btn-primary:hover:not(:disabled) {
  background-color: var(--accent-hover);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-cancel {
  padding: 8px 24px;
  background-color: transparent;
  color: var(--text-dim);
  font-size: 14px;
  font-weight: 500;
  border: 1px solid var(--border);
  border-radius: 8px;
  cursor: pointer;
}

.btn-cancel:hover {
  color: var(--text);
  border-color: #484f58;
}

/* 弹窗 */
.modal-mask {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  width: 480px;
  max-width: 90vw;
  background-color: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.6);
}

.modal-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid var(--border);
}

.modal-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--text);
}

.modal-close {
  background: none;
  border: none;
  color: var(--text-dim);
  font-size: 22px;
  cursor: pointer;
  line-height: 1;
  padding: 0 4px;
}

.modal-close:hover {
  color: var(--text);
}

.modal-body {
  padding: 20px 24px;
}

.modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--border);
}

.form-row {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
  margin-bottom: 6px;
}

.required {
  color: #f85149;
  margin-right: 4px;
}

.form-input {
  width: 100%;
  padding: 9px 12px;
  background-color: var(--bg);
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--text);
  font-size: 14px;
  font-family: inherit;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.15s;
}

.form-input:focus {
  border-color: var(--accent);
}

.form-textarea {
  resize: vertical;
  line-height: 1.6;
}

.form-hint {
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 4px;
}

.inline-msg {
  font-size: 13px;
  margin-top: 8px;
}

.inline-msg.success { color: var(--accent-hover); }
.inline-msg.error { color: #f85149; }
</style>
