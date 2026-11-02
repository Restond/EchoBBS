<template>
  <div>
    <AppHeader />

    <!-- 主体双栏布局 -->
    <div class="main-container">
      <!-- 左侧边栏 -->
      <aside class="left-sidebar">
        <!-- 横幅头像区 -->
        <div class="profile-banner">
          <img
            v-if="user?.avatarPath"
            :src="user.avatarPath"
            class="profile-avatar-large"
            alt="头像"
          />
          <div v-else class="profile-avatar-placeholder">
            {{ (user?.nickname || user?.username)?.charAt(0).toUpperCase() || 'U' }}
          </div>
        </div>

        <!-- 基础信息区 -->
        <div class="profile-info-card">
          <div class="profile-username">{{ user?.nickname || user?.username || '未知用户' }}</div>
          <div class="profile-role">
            <span v-if="user?.role === 'ADMIN'">管理员</span>
            <span v-else-if="user?.role === 'MODERATOR'">版主</span>
            <span v-else>正式成员</span>
          </div>

          <div class="profile-meta">
            <div class="meta-item">
              <span>加入于 {{ formatDate(user?.createdAt) }}</span>
            </div>
            <div class="meta-item">
              <span>ID: #{{ user?.id }}</span>
            </div>
            <div class="meta-item">
              <span>{{ userStatus }}</span>
            </div>
          </div>
        </div>

        <div class="bio-card">
          <div class="bio-title">个人简介</div>
          <div class="bio-content">
            {{ user?.bio || '暂无个人简介' }}
          </div>
        </div>

        <!-- 数据统计卡片 -->
        <div class="stats-card">
          <div class="stat-item">
            <div class="stat-label">帖子</div>
            <div class="stat-value">{{ stats.postCount }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">被点赞</div>
            <div class="stat-value">{{ stats.likeCount }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">评论</div>
            <div class="stat-value">{{ stats.replyCount }}</div>
          </div>
        </div>

        <!-- 成就模块 -->
        <div class="achievements-card">
          <div class="card-title">
            <span>成就</span>
          </div>
          <div class="empty-state">
            <div class="empty-text">没有任何成就</div>
          </div>
        </div>
      </aside>

      <!-- 右侧主内容区 -->
      <main class="right-content">
        <!-- 标签导航卡 -->
        <div class="tab-bar">
          <div
            class="tab-item"
            :class="{ active: activeTab === 'overview' }"
            @click="activeTab = 'overview'"
          >
            个人状况
          </div>
          <div
            class="tab-item"
            :class="{ active: activeTab === 'posts' }"
            @click="activeTab = 'posts'"
          >
            帖子
          </div>
        </div>

        <!-- 个人状况面板 -->
        <div v-show="activeTab === 'overview'" class="tab-panel">
          <!-- 活动/列表双栏卡片 -->
          <div class="content-card dual-column-card">
            <div class="column">
              <div class="column-title">最近活动</div>
              <div class="empty-state">
                <div class="empty-text">暂无活动</div>
              </div>
            </div>
            <div class="column">
              <div class="column-title">收藏列表</div>
              <div class="empty-state">
                <div class="empty-text">暂无列表</div>
              </div>
            </div>
          </div>

          <!-- 小组卡片 -->
          <div class="content-card groups-card">
            <div class="card-title">
              <span>👥</span>
              <span>用户组</span>
            </div>
            <div class="tags">
              <span v-if="user?.role === 'ADMIN'" class="tag blue">管理员</span>
              <span v-else-if="user?.role === 'MODERATOR'" class="tag blue">版主</span>
              <span v-else class="tag blue">正式成员</span>
            </div>
          </div>

          <!-- 库存卡片 -->
          <div class="content-card inventory-card">
            <div class="card-title">
              <span>库存</span>
            </div>
            <div class="empty-state">
              <div class="empty-text">库存为空</div>
            </div>
          </div>
        </div>

        <!-- 帖子面板 -->
        <div v-show="activeTab === 'posts'" class="tab-panel">
          <div class="content-card">
            <div class="card-title">
              <span>用户帖子</span>
              <span style="margin-left: auto; font-size: 13px; color: var(--text-dim)">
                共 {{ stats.postCount }} 篇
              </span>
            </div>

            <!-- 帖子列表 -->
            <div v-if="posts.length > 0" class="posts-list">
              <div v-for="post in posts" :key="post.id" class="post-item">
                <router-link :to="`/posts/${post.id}`" class="post-link">
                  <div class="post-header">
                    <h3 class="post-title">{{ post.title }}</h3>
                    <div class="post-meta">
                      <span>{{ formatDateTime(post.createdAt) }}</span>
                      <span>·</span>
                      <span>{{ post.boardName }}</span>
                    </div>
                  </div>
                  <div class="post-stats">
                    <span>浏览 {{ post.viewCount }}</span>
                    <span>点赞 {{ post.likeCount }}</span>
                  </div>
                </router-link>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-else class="empty-state">
              <div class="empty-text">暂无帖子</div>
            </div>
          </div>
        </div>
      </main>
    </div>

    <!-- 返回顶部按钮 -->
    <button
      class="back-to-top"
      :class="{ visible: showBackToTop }"
      @click="scrollToTop"
    >
      ↑
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import { api } from '@/api'
import type { User, Post } from '@/types'

const route = useRoute()
const username = computed(() => route.params.username as string)

const user = ref<User | null>(null)
const posts = ref<Post[]>([])
const stats = ref({
  postCount: 0,
  likeCount: 0,
  replyCount: 0
})
const activeTab = ref<'overview' | 'posts'>('overview')
const showBackToTop = ref(false)
const loading = ref(false)
const error = ref('')

// 获取用户信息
const fetchUserProfile = async () => {
  loading.value = true
  error.value = ''
  
  try {
    const response = await api.get(`/api/users/${username.value}`)
    user.value = response.data.data
  } catch (err: any) {
    error.value = err.response?.data?.message || '获取用户信息失败'
    console.error('获取用户信息失败:', err)
  } finally {
    loading.value = false
  }
}

// 获取用户统计数据
const fetchUserStats = async () => {
  try {
    const response = await api.get(`/api/users/${username.value}/stats`)
    stats.value = response.data.data
  } catch (err) {
    console.error('获取用户统计失败:', err)
  }
}

// 获取用户帖子
const fetchUserPosts = async () => {
  try {
    const response = await api.get(`/api/users/${username.value}/posts`, {
      params: { page: 0, size: 20 }
    })
    const responseData = response.data.data
    posts.value = responseData.content || []
    // VIA_DTO 模式下 totalElements 位于 page 节点下，兼容两种结构
    stats.value.postCount = responseData.page?.totalElements
      ?? responseData.totalElements
      ?? posts.value.length
  } catch (err) {
    console.error('获取用户帖子失败:', err)
  }
}

// 格式化日期
const formatDate = (dateString?: string) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

// 格式化日期时间
const formatDateTime = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 用户状态
const userStatus = computed(() => {
  // 可以根据实际状态字段来判断
  return '活跃'
})

// 滚动处理
const handleScroll = () => {
  showBackToTop.value = window.scrollY > 300
}

// 返回顶部
const scrollToTop = () => {
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
}

onMounted(() => {
  fetchUserProfile()
  fetchUserStats()
  fetchUserPosts()
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
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
  --border: #21262d;
  --text: #e6edf3;
  --text-dim: #8b949e;
  --text-muted: #6e7681;
  --accent: #3fb950;
  --accent-hover: #56d364;
  --accent-soft: rgba(63, 185, 80, 0.15);
  --blue: #58a6ff;
  --blue-soft: rgba(88, 166, 255, 0.15);
  --gray-tag: #484f58;
}

/* 主体双栏布局 */
.main-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  display: grid;
  grid-template-columns: 28% 1fr;
  gap: 24px;
  align-items: start;
}

/* 左侧边栏 */
.left-sidebar {
  position: sticky;
  top: 80px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 横幅头像区 */
.profile-banner {
  background: transparent;
  border: none;
  padding: 20px 20px 16px;
  text-align: center;
}

.profile-avatar-large {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid var(--border);
  margin-bottom: 16px;
}

.profile-avatar-placeholder {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent), #2ea043);
  color: #06140b;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  font-weight: 700;
  border: 3px solid var(--border);
  margin-bottom: 16px;
}

/* 基础信息区 */
.profile-info-card {
  background-color: transparent;
  border: none;
  padding: 0 20px 20px;
  text-align: center;
}

.profile-username {
  font-size: 20px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 6px;
}

.profile-role {
  font-size: 14px;
  color: var(--text-dim);
  margin-bottom: 12px;
}

.profile-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
}

/* 简介卡片 */
.bio-card {
  background-color: #000000;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px 20px;
}

.bio-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 12px;
}

.bio-content {
  font-size: 14px;
  color: var(--text-dim);
  line-height: 1.6;
  word-wrap: break-word;
  white-space: pre-wrap;
}

/* 数据统计卡片 */
.stats-card {
  background-color: #000000;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  flex-direction: row;
  gap: 24px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-label {
  font-size: 14px;
  color: var(--text-dim);
  text-align: left;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
  text-align: right;
}

/* 成就模块 */
.achievements-card {
  background-color: #000000;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 20px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 16px;
  color: var(--text);
}

.empty-state {
  text-align: center;
  padding: 32px 16px;
}

.empty-text {
  font-size: 13px;
  color: var(--text-muted);
}

/* 右侧主内容区 */
.right-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 标签导航卡 */
.tab-panel {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.tab-bar {
  display: flex;
  gap: 12px;
}

.tab-item {
  flex: 1;
  padding: 4px 20px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-dim);
  cursor: pointer;
  transition: all 0.15s;
  text-align: center;
  border: 1px solid var(--border);
  background-color: transparent;
}

.tab-item.active {
  background-color: var(--accent);
  color: #06140b;
  border-color: var(--accent);
}

.tab-item:hover:not(.active) {
  background-color: var(--bg-soft);
  color: var(--text);
  border-color: var(--text-dim);
}

/* 数据展示卡片组 */
.content-card {
  background-color: #000000;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 20px;
}

/* 活动/列表双栏卡片 */
.dual-column-card {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.column {
  text-align: center;
  padding: 24px 16px;
}

.column-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 16px;
}

/* 小组卡片 */
.groups-card .tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
}

.tag.blue {
  background-color: var(--blue-soft);
  color: var(--blue);
}

/* 帖子列表 */
.posts-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-item {
  background-color: #1a1f26;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 8px;
  transition: all 0.15s;
}

.post-item:hover {
  background-color: var(--bg-soft);
  border-color: var(--accent);
}

.post-link {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  color: inherit;
  text-decoration: none;
}

.post-header {
  flex: 1;
}

.post-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--text);
  margin-bottom: 6px;
}

.post-meta {
  font-size: 12px;
  color: var(--text-muted);
  display: flex;
  gap: 8px;
}

.post-stats {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: var(--text-dim);
  flex-shrink: 0;
}

/* 返回顶部按钮 */
.back-to-top {
  position: fixed;
  bottom: 32px;
  right: 32px;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background-color: var(--bg-card);
  border: 1px solid var(--border);
  color: var(--text-dim);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  pointer-events: none;
  transition: all 0.2s;
  font-size: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.back-to-top.visible {
  opacity: 1;
  pointer-events: auto;
}

.back-to-top:hover {
  background-color: var(--accent);
  color: #06140b;
  transform: translateY(-2px);
}

/* 响应式 */
@media (max-width: 1024px) {
  .main-container {
    grid-template-columns: 1fr;
  }
  .left-sidebar {
    position: static;
  }
  .dual-column-card {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-card {
    flex-direction: column;
    gap: 16px;
  }
}
</style>
