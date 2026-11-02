<template>
  <div class="home">
    <AppHeader />
    
    <!-- Hero 区域 -->
    <section class="hero">
      <div class="hero-icon">E</div>
      <h1>EchoBBS</h1>
      <div class="hero-tags">
        <span class="hero-tag">自由讨论</span>
        <span class="hero-tag">知识分享</span>
        <span class="hero-tag">兴趣社区</span>
      </div>
      <p class="hero-sub">一个安静的角落，聊聊你想聊的。在这里发现感兴趣的话题，与同好交流。</p>
      <form class="search" @submit.prevent="handleSearch">
        <input 
          v-model="searchQuery" 
          type="text" 
          placeholder="搜索帖子、话题或板块…" 
          autocomplete="off"
        />
        <button type="submit">搜索</button>
      </form>
    </section>

    <div class="container">
      <main>
        <div class="section-head">
          <h2>最新发布</h2>
          <router-link to="/boards">全部板块</router-link>
        </div>

        <div v-if="loading" class="loading">加载中...</div>
        <div v-else-if="posts.length === 0" class="empty-state">暂无帖子</div>
        <template v-else>
          <div
            v-for="post in posts"
            :key="post.id"
            class="post-item"
            @click="router.push(`/posts/${post.id}`)"
          >
            <div class="post-main">
              <div class="post-tags">
                <a 
                  @click.stop 
                  :href="`/boards/${post.boardSlug || post.boardId}`" 
                  class="board-tag"
                >
                  {{ post.boardName }}
                </a>
              </div>
              <div>
                <span v-if="post.isPinned" class="post-badge badge-pinned">置顶</span>
                <span v-if="post.isLocked" class="post-badge badge-locked">锁定</span>
                <a class="post-title">{{ post.title }}</a>
              </div>
              <div class="post-desc">
                <span>{{ post.nickname || post.username }}</span>
                <span class="sep">·</span>
                <span>{{ formatTime(post.createdAt) }}</span>
              </div>
            </div>
            <div class="post-stats">
              <div>
                <div class="stat-value">{{ post.viewCount }}</div>
                <div class="stat-label">浏览</div>
              </div>
              <div>
                <div class="stat-value">{{ post.likeCount }}</div>
                <div class="stat-label">点赞</div>
              </div>
            </div>
          </div>
        </template>
      </main>

      <aside>
        <div class="section-head">
          <h2>热门话题</h2>
        </div>

        <div v-if="hotPosts.length === 0" class="empty-state">暂无热门</div>
        <template v-else>
          <div
            v-for="post in hotPosts"
            :key="post.id"
            class="res-card"
            @click="router.push(`/posts/${post.id}`)"
          >
            <img
              v-if="post.avatarPath"
              :src="post.avatarPath"
              :alt="post.username"
              class="res-avatar"
            />
            <div v-else class="res-avatar">
              {{ (post.nickname || post.username).charAt(0).toUpperCase() }}
            </div>
            <div class="res-body">
              <a class="res-title">{{ post.title }}</a>
              <div class="res-meta">
                <b>{{ post.viewCount }}</b> 浏览
                <span class="sep">·</span>
                <b>{{ post.likeCount }}</b> 点赞
              </div>
            </div>
          </div>
        </template>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import { useUserStore } from '@/stores/user'
import { api } from '@/api'

const router = useRouter()
const userStore = useUserStore()

const posts = ref<any[]>([])
const hotPosts = ref<any[]>([])
const loading = ref(true)
const searchQuery = ref('')

const formatTime = (dateString: string) => {
  const date = new Date(dateString)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes}`
}

const handleSearch = () => {
  if (searchQuery.value.trim()) {
    router.push(`/search?q=${encodeURIComponent(searchQuery.value)}`)
  }
}

const fetchPosts = async () => {
  try {
    loading.value = true
    const [latestRes, hotRes] = await Promise.all([
      api.get('/api/posts/latest?limit=20'),
      api.get('/api/posts/hot?limit=5')
    ])
    posts.value = latestRes.data.data || []
    hotPosts.value = hotRes.data.data || []
  } catch (error) {
    console.error('获取帖子失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchPosts()
})
</script>

<style scoped>
.home {
  min-height: 100vh;
  background-color: var(--bg);
}

/* Hero 区域 */
.hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 80px 24px 64px;
  background: radial-gradient(60% 80% at 50% 0%, rgba(63, 185, 80, 0.12), transparent 70%);
}

.hero-icon {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  font-weight: normal;
  color: #06140b;
  background: linear-gradient(135deg, #3fb950, #2ea043);
  box-shadow: 0 8px 24px rgba(63, 185, 80, 0.35);
  margin-bottom: 24px;
}

.hero h1 {
  font-size: 42px;
  font-weight: normal;
  letter-spacing: -1px;
  margin-bottom: 18px;
}

.hero-tags {
  display: flex;
  gap: 12px;
  margin-bottom: 18px;
  flex-wrap: wrap;
  justify-content: center;
}

.hero-tag {
  font-size: 13px;
  font-weight: normal;
  color: var(--text-dim);
  padding: 5px 14px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background-color: var(--bg-soft);
}

.hero-sub {
  font-size: 16px;
  color: var(--text-dim);
  margin-bottom: 32px;
  max-width: 560px;
}

.search {
  display: flex;
  width: 100%;
  max-width: 760px;
  background-color: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  overflow: hidden;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.search:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
}

.search input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  color: var(--text);
  font-size: 16px;
  padding: 18px 22px;
}

.search input::placeholder {
  color: var(--text-dim);
}

.search button {
  border: none;
  cursor: pointer;
  background-color: var(--accent);
  color: #06140b;
  font-size: 16px;
  font-weight: 500;
  padding: 0 32px;
  transition: background-color 0.15s;
}

.search button:hover {
  background-color: var(--accent-hover);
}

/* 主容器 */
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px;
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 28px;
  align-items: start;
}

main {
  min-width: 0;
}

aside {
  min-width: 0;
}

/* 分区标题 */
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 16px;
}

.section-head h2 {
  font-size: 18px;
  font-weight: normal;
}

.section-head a {
  font-size: 14px;
  color: var(--text-dim);
  transition: color 0.15s;
}

.section-head a:hover {
  color: var(--accent);
}

/* 帖子列表 */
.post-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 18px;
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 12px;
  margin-bottom: 12px;
  transition: border-color 0.15s, transform 0.15s, background-color 0.15s;
  cursor: pointer;
}

.post-item:hover {
  background-color: var(--bg-card);
  border-color: #3d4856;
  transform: translateY(-2px);
}

.post-main {
  flex: 1;
  min-width: 0;
}

.post-tags {
  margin-bottom: 6px;
}

.board-tag {
  font-size: 11px;
  font-weight: normal;
  padding: 2px 9px;
  border-radius: 6px;
  background-color: var(--accent-soft);
  color: var(--accent-hover);
  transition: background-color 0.15s;
}

.board-tag:hover {
  background-color: rgba(63, 185, 80, 0.25);
}

.post-badge {
  font-size: 11px;
  padding: 2px 7px;
  border-radius: 6px;
  margin-right: 6px;
  font-weight: normal;
}

.badge-pinned {
  background-color: rgba(210, 153, 34, 0.2);
  color: #e3b341;
}

.badge-locked {
  background-color: rgba(248, 81, 73, 0.15);
  color: #f85149;
}

.post-title {
  font-size: 16px;
  font-weight: normal;
  color: var(--text);
  transition: color 0.15s;
  cursor: pointer;
}

.post-item:hover .post-title {
  color: var(--accent-hover);
}

.post-desc {
  font-size: 13px;
  color: var(--text-dim);
  margin-top: 6px;
}

.sep {
  margin: 0 7px;
  color: #3d4856;
}

.post-stats {
  display: flex;
  gap: 22px;
  flex-shrink: 0;
  white-space: nowrap;
  text-align: center;
}

.stat-value {
  font-size: 18px;
  font-weight: normal;
  color: var(--text);
}

.stat-label {
  font-size: 11px;
  color: var(--text-dim);
  margin-top: 2px;
}

/* 热门资源卡片 */
.res-card {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 14px 16px;
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 12px;
  margin-bottom: 12px;
  transition: border-color 0.15s, background-color 0.15s;
  cursor: pointer;
}

.res-card:hover {
  background-color: var(--bg-card);
  border-color: #3d4856;
}

.res-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.res-avatar:not(img) {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--accent);
  color: #000;
  font-weight: 600;
  font-size: 18px;
}

.res-body {
  flex: 1;
  min-width: 0;
}

.res-title {
  font-size: 14px;
  font-weight: normal;
  color: var(--text);
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.15s;
  cursor: pointer;
}

.res-card:hover .res-title {
  color: var(--accent-hover);
}

.res-meta {
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 3px;
}

.res-meta b {
  color: #adbac7;
  font-weight: normal;
}

/* 加载和空状态 */
.loading,
.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-dim);
  font-size: 14px;
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 12px;
}

/* 响应式 */
@media (max-width: 860px) {
  .container {
    grid-template-columns: 1fr;
  }

  .hero h1 {
    font-size: 32px;
  }

  .post-stats {
    gap: 14px;
  }
}
</style>
