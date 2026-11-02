<template>
  <div>
    <AppHeader />

    <!-- 面包屑 -->
    <div class="page-head">
      <div class="breadcrumb">
        <router-link to="/">首页</router-link>
        <span class="sep">/</span>
        <span>通知中心</span>
      </div>
      <h1 class="page-title">通知中心</h1>
    </div>

    <!-- 主体双栏 -->
    <div class="main-layout">
      <!-- 左侧边栏 -->
      <aside class="sidebar">
        <div class="sidebar-section">
          <div class="sidebar-section-title">账户</div>
          <div class="sidebar-menu">
            <router-link
              v-if="userStore.currentUser?.username"
              :to="`/user/${userStore.currentUser.username}`"
              class="sidebar-item"
            >
              <span>个人资料</span>
            </router-link>
            <router-link to="/notifications" class="sidebar-item active">
              <span>通知</span>
            </router-link>
          </div>
        </div>

        <div class="sidebar-section">
          <div class="sidebar-section-title">设置</div>
          <div class="sidebar-menu">
            <router-link to="/settings/profile" class="sidebar-item">
              <span>账户详情</span>
            </router-link>
            <a class="sidebar-item"><span>密码和安全性</span></a>
            <a class="sidebar-item"><span>隐私设置</span></a>
            <a class="sidebar-item"><span>提醒偏好</span></a>
            <a class="sidebar-item"><span>账户升级</span></a>
            <a class="sidebar-item"><span>签名设置</span></a>
          </div>
        </div>

        <div class="sidebar-logout">
          <a class="sidebar-item" @click="handleLogout">
            <span>退出登录</span>
          </a>
        </div>
      </aside>

      <!-- 右侧通知区 -->
      <main class="notification-container">
        <div class="notification-header">
          <h2 class="notification-title">我的通知</h2>
          <button class="btn-mark-read" :disabled="marking" @click="markAllRead">
            {{ marking ? '处理中...' : '一键已读' }}
          </button>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="empty-state">
          <div class="empty-text">加载中...</div>
        </div>

        <!-- 通知列表 -->
        <div v-else-if="notifications.length > 0" class="notification-list">
          <a
            v-for="n in notifications"
            :key="n.id"
            :href="n.targetUrl"
            class="notification-item"
            :class="{ unread: !n.isRead }"
          >
            <div class="notification-icon" :class="n.type === 'REPLY' ? 'reply' : 'like'">
              <span v-if="n.type === 'REPLY'">💬</span>
              <span v-else>❤️</span>
            </div>

            <div class="notification-body">
              <div class="notification-content">{{ n.content }}</div>
              <div class="notification-time">{{ formatTime(n.createdAt) }}</div>
            </div>

            <div v-if="!n.isRead" class="notification-badge"></div>
          </a>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-state">
          <div class="empty-text">暂无通知</div>
        </div>

        <!-- 分页 -->
        <div v-if="!loading && notifications.length > 0" class="pagination">
          <a v-if="currentPage > 0" @click="changePage(currentPage - 1)">← 上一页</a>
          <a v-if="currentPage < totalPages - 1" @click="changePage(currentPage + 1)">下一页 →</a>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import { useUserStore } from '@/stores/user'
import { api } from '@/api'
import type { Notification } from '@/types'

const router = useRouter()
const userStore = useUserStore()

const notifications = ref<Notification[]>([])
const currentPage = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const marking = ref(false)

const fetchNotifications = async (page = 0) => {
  loading.value = true
  try {
    const response = await api.get('/api/notifications', { params: { page } })
    const data = response.data.data
    notifications.value = data.content || []
    // VIA_DTO 模式：分页信息位于 page 节点
    currentPage.value = data.page?.number ?? page
    totalPages.value = data.page?.totalPages ?? 1
  } catch (err) {
    console.error('获取通知失败:', err)
  } finally {
    loading.value = false
  }
}

const markAllRead = async () => {
  marking.value = true
  try {
    await api.post('/api/notifications/read-all')
    notifications.value = notifications.value.map(n => ({ ...n, isRead: true }))
  } catch (err) {
    console.error('标记已读失败:', err)
  } finally {
    marking.value = false
  }
}

const changePage = (page: number) => {
  fetchNotifications(page)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const formatTime = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const handleLogout = async () => {
  try {
    await userStore.logout()
    router.push('/login')
  } catch {
    alert('登出失败，请重试')
  }
}

onMounted(() => {
  fetchNotifications()
})
</script>

<style scoped>
* {
  box-sizing: border-box;
}

:root {
  --bg: #000000;
  --bg-soft: #0d1117;
  --bg-card: #161b22;
  --border: #30363d;
  --text: #e6edf3;
  --text-dim: #8b949e;
  --accent: #3fb950;
  --accent-hover: #56d364;
  --accent-soft: rgba(63, 185, 80, 0.15);
  --blue: #58a6ff;
}

/* 面包屑 */
.page-head {
  max-width: 1400px;
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

/* 主体双栏布局 */
.main-layout {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px 64px;
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 32px;
  align-items: start;
}

/* 左侧边栏 */
.sidebar {
  position: sticky;
  top: 80px;
}

.sidebar-section {
  margin-bottom: 24px;
}

.sidebar-section-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-dim);
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 0 12px 8px;
}

.sidebar-menu {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-dim);
  transition: all 0.15s;
  cursor: pointer;
}

.sidebar-item:hover {
  background-color: var(--bg-soft);
  color: var(--text);
}

.sidebar-item.active {
  background-color: var(--accent-soft);
  color: var(--accent-hover);
  font-weight: 500;
}

.sidebar-logout {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

.sidebar-logout .sidebar-item {
  color: #f85149;
}

.sidebar-logout .sidebar-item:hover {
  background-color: rgba(248, 81, 73, 0.1);
}

/* 右侧通知区 */
.notification-container {
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 24px;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border);
}

.notification-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text);
}

.btn-mark-read {
  padding: 8px 20px;
  background-color: var(--accent);
  color: #06140b;
  font-size: 14px;
  font-weight: 500;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.btn-mark-read:hover:not(:disabled) {
  background-color: var(--accent-hover);
}

.btn-mark-read:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 通知列表 */
.notification-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  background-color: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  transition: all 0.15s;
}

.notification-item:hover {
  transform: translateY(-1px);
  border-color: var(--accent);
}

.notification-item.unread {
  background-color: rgba(88, 166, 255, 0.08);
}

.notification-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  background-color: var(--bg-soft);
}

.notification-body {
  flex: 1;
  min-width: 0;
}

.notification-content {
  font-size: 15px;
  color: var(--text);
  line-height: 1.5;
  margin-bottom: 8px;
}

.notification-time {
  font-size: 13px;
  color: var(--text-dim);
}

.notification-badge {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--blue);
  flex-shrink: 0;
  margin-top: 8px;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 64px 20px;
  color: var(--text-dim);
}

.empty-text {
  font-size: 15px;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
}

.pagination a {
  padding: 8px 16px;
  background-color: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s;
}

.pagination a:hover {
  background-color: var(--accent);
  color: #06140b;
  border-color: var(--accent);
}

/* 响应式 */
@media (max-width: 1024px) {
  .main-layout {
    grid-template-columns: 1fr;
  }
  .sidebar {
    position: static;
  }
}
</style>
