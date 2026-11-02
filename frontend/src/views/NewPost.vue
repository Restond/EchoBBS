<template>
  <div>
    <AppHeader />

    <!-- 面包屑 + 主标题 -->
    <div class="page-head">
      <div class="breadcrumb">
        <router-link to="/">首页</router-link>
        <span class="sep">/</span>
        <router-link to="/boards">资源</router-link>
        <span class="sep">/</span>
        <span>发布新资源</span>
      </div>
      <h1 class="page-title">发布新资源</h1>
    </div>

    <!-- 表单容器 -->
    <div class="form-wrap">
      <div class="form-container">
        <form @submit.prevent="handleSubmit">
          <!-- 板块选择 -->
          <div class="form-row">
            <label class="form-label">
              <span class="required">*</span>板块
            </label>
            <div class="form-content">
              <select
                v-model="form.boardId"
                class="form-select"
                :class="{ 'is-error': errors.boardId }"
              >
                <option :value="null" disabled>请选择板块</option>
                <option v-for="b in boards" :key="b.id" :value="b.id">
                  {{ b.name }}
                </option>
              </select>
              <div v-if="errors.boardId" class="form-error">{{ errors.boardId }}</div>
              <div v-else class="form-hint">选择资源所属的板块</div>
            </div>
          </div>

          <!-- 标题 -->
          <div class="form-row">
            <label class="form-label">
              <span class="required">*</span>标题
            </label>
            <div class="form-content">
              <input
                v-model="form.title"
                type="text"
                class="form-input"
                :class="{ 'is-error': errors.title }"
                placeholder="一句话描述你的资源"
                maxlength="256"
              />
              <div v-if="errors.title" class="form-error">{{ errors.title }}</div>
              <div v-else class="form-hint">最多 256 个字符</div>
            </div>
          </div>

          <!-- 内容 -->
          <div class="form-row">
            <label class="form-label">
              <span class="required">*</span>内容
            </label>
            <div class="form-content">
              <textarea
                v-model="form.content"
                class="form-input form-textarea"
                :class="{ 'is-error': errors.content }"
                rows="14"
                placeholder="详细介绍资源内容，支持纯文本…"
                maxlength="50000"
              ></textarea>
              <div class="textarea-footer">
                <span v-if="errors.content" class="form-error">{{ errors.content }}</span>
                <span class="char-count">{{ form.content.length }} / 50000</span>
              </div>
            </div>
          </div>

          <!-- 全局消息 -->
          <div v-if="message" class="form-message" :class="messageType">
            {{ message }}
          </div>

          <!-- 底部操作栏 -->
          <div class="form-actions">
            <button type="button" class="btn-cancel" @click="goBack">取消</button>
            <button type="submit" class="btn-submit" :disabled="submitting">
              {{ submitting ? '发布中...' : '发布资源' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import { api } from '@/api'
import { useUserStore } from '@/stores/user'
import type { Board } from '@/types'

const router = useRouter()
const userStore = useUserStore()

const boards = ref<Board[]>([])
const submitting = ref(false)
const message = ref('')
const messageType = ref<'success' | 'error'>('success')

const form = reactive({
  boardId: null as number | null,
  title: '',
  content: ''
})

const errors = reactive({
  boardId: '',
  title: '',
  content: ''
})

// 加载板块列表
const loadBoards = async () => {
  try {
    const res = await api.get('/api/boards')
    boards.value = res.data.data || []
    // 若通过 query 指定了板块，自动选中
    const preselect = router.currentRoute.value.query.boardId
    if (preselect) {
      const id = Number(preselect)
      if (boards.value.some(b => b.id === id)) {
        form.boardId = id
      }
    }
  } catch (e) {
    showMessage('板块列表加载失败', 'error')
  }
}

const validate = (): boolean => {
  errors.boardId = form.boardId ? '' : '请选择板块'
  errors.title = form.title.trim() ? '' : '请输入标题'
  errors.content = form.content.trim() ? '' : '请输入内容'
  return !errors.boardId && !errors.title && !errors.content
}

const handleSubmit = async () => {
  if (!validate()) return
  submitting.value = true
  try {
    const res = await api.post('/api/posts', {
      boardId: form.boardId,
      title: form.title.trim(),
      content: form.content.trim()
    })
    const newId = res.data.data?.id
    showMessage('发布成功，正在跳转...', 'success')
    // 帖子详情页尚未迁移，暂跳回资源页
    setTimeout(() => {
      router.push(newId ? `/posts/${newId}` : '/boards')
    }, 800)
  } catch (err: any) {
    showMessage(err.response?.data?.message || '发布失败，请重试', 'error')
  } finally {
    submitting.value = false
  }
}

const showMessage = (text: string, type: 'success' | 'error') => {
  message.value = text
  messageType.value = type
}

const goBack = () => {
  router.back()
}

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    await userStore.fetchCurrentUser()
  }
  if (!userStore.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: '/posts/new' } })
    return
  }
  loadBoards()
})
</script>

<style scoped>
* {
  box-sizing: border-box;
}

/* 面包屑 + 标题 */
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

.page-title {
  font-size: 28px;
  font-weight: 600;
  letter-spacing: -0.5px;
  color: var(--text);
}

/* 表单容器 */
.form-wrap {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 64px;
}

.form-container {
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 32px;
}

/* 表单行 */
.form-row {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
  align-items: flex-start;
}

.form-label {
  width: 140px;
  flex-shrink: 0;
  padding-top: 10px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
}

.required {
  color: #f85149;
  margin-right: 4px;
}

.form-content {
  flex: 1;
  min-width: 0;
}

.form-input,
.form-select {
  width: 100%;
  padding: 10px 14px;
  background-color: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--text);
  font-size: 14px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
}

.form-input:focus,
.form-select:focus {
  border-color: var(--accent);
}

.form-input.is-error,
.form-select.is-error {
  border-color: #f85149;
}

.form-textarea {
  resize: vertical;
  min-height: 320px;
  line-height: 1.7;
}

.form-select {
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='%238b949e' stroke-width='2'><polyline points='6 9 12 15 18 9'/></svg>");
  background-repeat: no-repeat;
  background-position: right 14px center;
  padding-right: 40px;
}

.form-hint {
  font-size: 13px;
  color: var(--text-dim);
  margin-top: 6px;
}

.form-error {
  font-size: 13px;
  color: #f85149;
  margin-top: 6px;
}

.textarea-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
}

.char-count {
  font-size: 12px;
  color: var(--text-dim);
  margin-left: auto;
}

/* 消息提示 */
.form-message {
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 16px;
}

.form-message.success {
  background-color: var(--accent-soft);
  color: var(--accent-hover);
  border: 1px solid var(--accent);
}

.form-message.error {
  background-color: rgba(248, 81, 73, 0.1);
  color: #f85149;
  border: 1px solid #f85149;
}

/* 底部操作栏 */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 24px;
  border-top: 1px solid var(--border);
  margin-top: 8px;
}

.btn-cancel {
  padding: 10px 28px;
  background-color: transparent;
  color: var(--text-dim);
  font-size: 14px;
  font-weight: 500;
  border: 1px solid var(--border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-cancel:hover {
  color: var(--text);
  border-color: #484f58;
}

.btn-submit {
  padding: 10px 40px;
  background-color: var(--accent);
  color: #06140b;
  font-size: 14px;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.btn-submit:hover:not(:disabled) {
  background-color: var(--accent-hover);
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 响应式 */
@media (max-width: 768px) {
  .form-row {
    flex-direction: column;
    gap: 8px;
  }
  .form-label {
    width: 100%;
    padding-top: 0;
  }
  .form-actions {
    flex-direction: column-reverse;
  }
  .btn-cancel,
  .btn-submit {
    width: 100%;
    text-align: center;
  }
}
</style>
