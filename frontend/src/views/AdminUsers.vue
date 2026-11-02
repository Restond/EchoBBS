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
        <span>用户管理</span>
      </div>
      <h1 class="page-title">用户管理</h1>
    </div>

    <!-- 用户列表 -->
    <div class="content-wrap">
      <div v-if="loading" class="state-box">加载中...</div>
      <div v-else-if="users.length === 0" class="state-box">暂无用户</div>
      <table v-else class="data-table">
        <thead>
          <tr>
            <th class="col-id">ID</th>
            <th class="col-user">用户</th>
            <th class="col-email">邮箱</th>
            <th class="col-role">角色</th>
            <th class="col-status">状态</th>
            <th class="col-time">注册时间</th>
            <th class="col-actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td>{{ u.id }}</td>
            <td>
              <div class="user-cell">
                <img
                  v-if="u.avatarPath"
                  :src="u.avatarPath"
                  class="user-avatar"
                  :alt="u.username"
                />
                <div v-else class="user-avatar-placeholder">
                  {{ (u.nickname || u.username || 'U').charAt(0).toUpperCase() }}
                </div>
                <div class="user-info">
                  <div class="user-nickname">{{ u.nickname || u.username }}</div>
                  <div class="user-username">@{{ u.username }}</div>
                </div>
              </div>
            </td>
            <td class="cell-email">{{ u.email }}</td>
            <td>
              <span class="role-badge" :class="roleClass(u.role)">{{ roleLabel(u.role) }}</span>
            </td>
            <td>
              <span class="status-badge" :class="statusClass(u.status)">{{ statusLabel(u.status) }}</span>
            </td>
            <td class="cell-time">{{ formatTime(u.createdAt) }}</td>
            <td>
              <button
                v-if="canBan(u)"
                class="btn-link"
                :class="{ 'btn-danger': u.status !== 'BANNED' }"
                @click="toggleBan(u)"
              >
                {{ u.status === 'BANNED' ? '解封' : '封禁' }}
              </button>
              <span v-else class="cell-disabled">-</span>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="pagination">
        <button class="page-btn" :disabled="page === 0" @click="goPage(page - 1)">上一页</button>
        <span class="page-info">{{ page + 1 }} / {{ totalPages }}</span>
        <button class="page-btn" :disabled="page >= totalPages - 1" @click="goPage(page + 1)">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppHeader from '@/components/AppHeader.vue'
import { api } from '@/api'
import { useUserStore } from '@/stores/user'
import type { User } from '@/types'

const userStore = useUserStore()
const users = ref<User[]>([])
const loading = ref(true)
const page = ref(0)
const total = ref(0)
const pageSize = 20

const totalPages = computed(() => Math.ceil(total.value / pageSize) || 1)

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await api.get('/api/admin/users', {
      params: { page: page.value, size: pageSize }
    })
    users.value = res.data.data?.content || []
    total.value = res.data.data?.totalElements || 0
  } catch {
    users.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const goPage = (p: number) => {
  if (p < 0 || p >= totalPages.value) return
  page.value = p
  fetchUsers()
}

const canBan = (u: User): boolean => {
  if (u.role === 'ADMIN') return false
  if (u.id === userStore.currentUser?.id) return false
  return userStore.isStaff
}

const toggleBan = async (u: User) => {
  const action = u.status === 'BANNED' ? '解封' : '封禁'
  if (!confirm(`确定${action}用户「${u.nickname || u.username}」吗？`)) return
  try {
    const res = await api.post(`/api/users/${u.id}/ban`)
    const idx = users.value.findIndex(x => x.id === u.id)
    if (idx >= 0) {
      users.value[idx] = res.data.data
    }
  } catch (err: any) {
    alert(err.response?.data?.message || `${action}失败`)
  }
}

const roleLabel = (role: string): string => {
  switch (role) {
    case 'ADMIN': return '管理员'
    case 'MODERATOR': return '版主'
    default: return '用户'
  }
}

const roleClass = (role: string): string => {
  switch (role) {
    case 'ADMIN': return 'role-admin'
    case 'MODERATOR': return 'role-moderator'
    default: return 'role-user'
  }
}

const statusLabel = (status?: string): string => {
  switch (status) {
    case 'BANNED': return '已封禁'
    case 'INACTIVE': return '未激活'
    default: return '正常'
  }
}

const statusClass = (status?: string): string => {
  switch (status) {
    case 'BANNED': return 'status-banned'
    case 'INACTIVE': return 'status-inactive'
    default: return 'status-active'
  }
}

const formatTime = (dateStr: string) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

onMounted(() => {
  fetchUsers()
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
.col-role { width: 90px; }
.col-status { width: 90px; }
.col-time { width: 120px; }
.col-actions { width: 90px; }

/* 用户单元格 */
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar,
.user-avatar-placeholder {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  flex-shrink: 0;
  object-fit: cover;
}

.user-avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: #06140b;
  background: linear-gradient(135deg, #3fb950, #2ea043);
}

.user-info {
  min-width: 0;
}

.user-nickname {
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
}

.user-username {
  font-size: 12px;
  color: var(--text-dim);
}

.cell-email,
.cell-time {
  color: var(--text-dim);
  font-size: 13px;
}

.cell-disabled {
  color: var(--text-dim);
}

/* 角色徽章 */
.role-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.role-admin {
  background-color: rgba(210, 153, 34, 0.2);
  color: #e3b341;
}

.role-moderator {
  background-color: rgba(63, 185, 80, 0.15);
  color: #3fb950;
}

.role-user {
  background-color: rgba(110, 118, 129, 0.2);
  color: var(--text-dim);
}

/* 状态徽章 */
.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.status-active {
  background-color: rgba(63, 185, 80, 0.15);
  color: #3fb950;
}

.status-banned {
  background-color: rgba(248, 81, 73, 0.15);
  color: #f85149;
}

.status-inactive {
  background-color: rgba(110, 118, 129, 0.2);
  color: var(--text-dim);
}

/* 操作按钮 */
.btn-link {
  background: none;
  border: none;
  color: var(--accent-hover);
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
}

.btn-link:hover {
  text-decoration: underline;
}

.btn-link.btn-danger {
  color: #f85149;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
}

.page-btn {
  padding: 8px 20px;
  background-color: var(--bg-soft);
  color: var(--text);
  font-size: 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.page-btn:hover:not(:disabled) {
  border-color: var(--accent);
  color: var(--accent-hover);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: var(--text-dim);
}
</style>
