<template>
  <div class="search-page">
    <AppHeader />

    <!-- 页头 -->
    <div class="page-head">
      <div class="breadcrumb">
        <router-link to="/">首页</router-link>
        <span class="sep">/</span>
        <span>搜索</span>
      </div>
      <h1 class="page-title">搜索资源</h1>

      <!-- 搜索框 -->
      <form class="search-box" @submit.prevent="doSearch">
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索帖子、话题或作者…"
          autocomplete="off"
        />
        <button type="submit">搜索</button>
      </form>

      <div v-if="searched" class="result-count">
        找到 <b>{{ total }}</b> 条与「<b>{{ queriedKeyword }}</b>」相关的结果
      </div>
    </div>

    <!-- 结果区 -->
    <div class="container">
      <div v-if="loading" class="state-box">搜索中...</div>

      <template v-else-if="results.length > 0">
        <div
          v-for="post in results"
          :key="post.id"
          class="result-item"
          @click="router.push(`/posts/${post.id}`)"
        >
          <img
            v-if="post.avatarPath"
            :src="post.avatarPath"
            :alt="post.username"
            class="result-avatar"
          />
          <div v-else class="result-avatar result-avatar-placeholder">
            {{ (post.nickname || post.username).charAt(0).toUpperCase() }}
          </div>

          <div class="result-body">
            <div class="result-tags">
              <span class="board-tag">{{ post.boardName }}</span>
            </div>
            <a class="result-title">{{ post.title }}</a>
            <div class="result-excerpt">{{ excerpt(post.content) }}</div>
            <div class="result-meta">
              <span>{{ post.nickname || post.username }}</span>
              <span class="sep">·</span>
              <span>{{ formatTime(post.createdAt) }}</span>
              <span class="sep">·</span>
              <span>{{ post.viewCount }} 浏览</span>
              <span class="sep">·</span>
              <span>{{ post.likeCount }} 点赞</span>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="totalPages > 1" class="pager">
          <span @click="prevPage" :class="{ disabled: currentPage <= 1 }">‹</span>
          <span
            v-for="p in visiblePages"
            :key="p"
            :class="{ current: p === currentPage }"
            @click="goToPage(p)"
          >{{ p }}</span>
          <span @click="nextPage" :class="{ disabled: currentPage >= totalPages }">›</span>
        </div>
      </template>

      <div v-else-if="searched" class="state-box">
        没有找到与「{{ queriedKeyword }}」相关的结果，换个关键词试试吧
      </div>

      <div v-else class="state-box">
        输入关键词开始搜索社区资源
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import { api } from '@/api'
import type { Post } from '@/types'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const queriedKeyword = ref('')
const results = ref<Post[]>([])
const loading = ref(false)
const searched = ref(false)
const currentPage = ref(1)
const totalPages = ref(1)
const total = ref(0)
const pageSize = 20

const visiblePages = computed(() => {
  const pages: number[] = []
  const start = Math.max(1, currentPage.value - 2)
  const end = Math.min(totalPages.value, start + 4)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

const excerpt = (content: string) => {
  if (!content) return ''
  return content.length > 200 ? content.slice(0, 200) + '…' : content
}

const formatTime = (dateStr: string) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const doSearch = async () => {
  const q = keyword.value.trim()
  if (!q) return
  // 同步到 URL query，便于分享/刷新
  router.replace({ query: { q } })
  currentPage.value = 1
  await fetchResults()
}

const fetchResults = async () => {
  const q = queriedKeyword.value
  if (!q) return
  loading.value = true
  searched.value = true
  try {
    const res = await api.get('/api/posts', {
      params: {
        page: currentPage.value - 1,
        size: pageSize,
        sort: 'latest',
        q
      }
    })
    const data = res.data.data || {}
    results.value = data.content || []
    total.value = data.page?.totalElements ?? data.totalElements ?? results.value.length
    totalPages.value = data.page?.totalPages ?? data.totalPages ?? 1
  } catch {
    results.value = []
    total.value = 0
    totalPages.value = 1
  } finally {
    loading.value = false
  }
}

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    fetchResults()
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    fetchResults()
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

const goToPage = (p: number) => {
  if (p >= 1 && p <= totalPages.value && p !== currentPage.value) {
    currentPage.value = p
    fetchResults()
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

// 监听 URL query 变化（如从导航栏搜索跳转过来）
watch(() => route.query.q, (q) => {
  if (q && typeof q === 'string' && q !== queriedKeyword.value) {
    keyword.value = q
    queriedKeyword.value = q
    currentPage.value = 1
    fetchResults()
  }
})

onMounted(() => {
  const q = route.query.q
  if (q && typeof q === 'string') {
    keyword.value = q
    queriedKeyword.value = q
    fetchResults()
  }
})
</script>

<style scoped>
.search-page {
  min-height: 100vh;
  background-color: var(--bg);
}

/* 页头 */
.page-head {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px 24px 0;
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
  margin-bottom: 20px;
}

/* 搜索框 */
.search-box {
  display: flex;
  background-color: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  overflow: hidden;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.search-box:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
}

.search-box input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  color: var(--text);
  font-size: 16px;
  padding: 16px 22px;
}

.search-box input::placeholder {
  color: var(--text-dim);
}

.search-box button {
  border: none;
  cursor: pointer;
  background-color: var(--accent);
  color: #06140b;
  font-size: 15px;
  font-weight: 600;
  padding: 0 36px;
  transition: background-color 0.15s;
}

.search-box button:hover {
  background-color: var(--accent-hover);
}

.result-count {
  font-size: 14px;
  color: var(--text-dim);
  margin-top: 20px;
}

.result-count b {
  color: var(--text);
  font-weight: 600;
}

/* 结果区 */
.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 24px 80px;
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

.result-item {
  display: flex;
  gap: 14px;
  padding: 18px;
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 12px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: border-color 0.15s, transform 0.15s, background-color 0.15s;
}

.result-item:hover {
  background-color: var(--bg-card);
  border-color: #3d4856;
  transform: translateY(-2px);
}

.result-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.result-avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #3fb950, #2ea043);
  color: #06140b;
  font-weight: 600;
  font-size: 18px;
}

.result-body {
  flex: 1;
  min-width: 0;
}

.result-tags {
  margin-bottom: 6px;
}

.board-tag {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 9px;
  border-radius: 6px;
  background-color: var(--accent-soft);
  color: var(--accent-hover);
}

.result-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
  display: block;
  margin-bottom: 6px;
  transition: color 0.15s;
}

.result-item:hover .result-title {
  color: var(--accent-hover);
}

.result-excerpt {
  font-size: 13px;
  color: var(--text-dim);
  line-height: 1.6;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.result-meta {
  font-size: 12px;
  color: var(--text-dim);
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.result-meta .sep {
  color: #3d4856;
}

/* 分页 */
.pager {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  margin-top: 24px;
}

.pager span {
  min-width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  border: 1px solid var(--border);
  background-color: var(--bg-soft);
  color: var(--text-dim);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s;
}

.pager span:hover:not(.disabled):not(.current) {
  border-color: var(--accent);
  color: var(--accent-hover);
}

.pager span.current {
  background-color: var(--accent);
  border-color: var(--accent);
  color: #06140b;
  font-weight: 600;
}

.pager span.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
