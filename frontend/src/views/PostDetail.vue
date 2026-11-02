<template>
  <div class="post-detail">
    <AppHeader />

    <!-- 主体双栏 -->
    <div class="main-layout">
      <!-- 左侧主内容栏 -->
      <main class="main-col">
        <!-- 面包屑 -->
        <div class="breadcrumb">
          <router-link to="/">首页</router-link>
          <span class="sep">/</span>
          <router-link to="/boards">资源</router-link>
          <template v-if="post">
            <span class="sep">/</span>
            <router-link :to="`/boards/${post.boardSlug}`">{{ post.boardName }}</router-link>
          </template>
        </div>

        <!-- 加载/错误/空状态 -->
        <div v-if="loading" class="state-box">加载中...</div>
        <div v-else-if="!post" class="state-box">帖子不存在或已被删除</div>

        <template v-else>
          <!-- 编辑模式 -->
          <div v-if="editing" class="edit-wrap">
            <div class="edit-head">
              <h2 class="edit-title">编辑帖子</h2>
            </div>
            <div class="edit-form">
              <div class="edit-row">
                <label class="edit-label">标题</label>
                <input
                  v-model="editForm.title"
                  type="text"
                  class="edit-input"
                  maxlength="256"
                  placeholder="帖子标题"
                />
              </div>
              <div class="edit-row">
                <label class="edit-label">内容</label>
                <textarea
                  v-model="editForm.content"
                  class="edit-input edit-textarea"
                  rows="14"
                  placeholder="支持 Markdown 格式"
                  maxlength="50000"
                ></textarea>
              </div>
              <div v-if="editMessage" class="inline-msg" :class="editMessageType">{{ editMessage }}</div>
              <div class="edit-actions">
                <button class="btn-cancel" @click="cancelEdit">取消</button>
                <button class="btn-primary" :disabled="savingEdit" @click="saveEdit">
                  {{ savingEdit ? '保存中...' : '保存修改' }}
                </button>
              </div>
            </div>
          </div>

          <!-- 查看模式 -->
          <template v-else>
            <!-- 资源头部 -->
            <div class="resource-header">
              <div class="resource-icon">{{ post.boardName?.charAt(0) || 'E' }}</div>
              <div class="resource-info">
                <div class="resource-title-row">
                  <span class="board-prefix" :href="`/boards/${post.boardSlug}`">{{ post.boardName }}</span>
                  <h1 class="resource-title">{{ post.title }}</h1>
                </div>
                <div class="resource-sub">
                  <span>由 <b>{{ post.nickname || post.username }}</b> 发布于 {{ formatTime(post.createdAt) }}</span>
                  <span class="sep">·</span>
                  <span v-if="post.isPinned" class="badge badge-pinned">置顶</span>
                  <span v-if="post.isLocked" class="badge badge-locked">已锁定</span>
                </div>
              </div>
            </div>

            <!-- 管理操作（作者/管理员可见） -->
            <div v-if="canManage" class="manage-bar">
              <button
                v-if="isOwner"
                class="mg-btn"
                @click="startEdit"
              >
                编辑
              </button>
              <button
                v-if="userStore.isStaff"
                class="mg-btn"
                :class="{ active: post.isPinned }"
                @click="togglePin"
              >
                {{ post.isPinned ? '取消置顶' : '置顶' }}
              </button>
              <button
                v-if="userStore.isStaff"
                class="mg-btn"
                :class="{ active: post.isLocked }"
                @click="toggleLock"
              >
                {{ post.isLocked ? '解锁' : '锁定' }}
              </button>
              <button class="mg-btn mg-danger" @click="deletePost">
                删除帖子
              </button>
            </div>

            <!-- 通知横幅 -->
            <div class="notices">
              <div class="notice notice-info">
                <span class="notice-icon">i</span>
                <span>这是一个社区资源分享，请遵守社区规范，文明讨论。</span>
              </div>
              <div v-if="post.isLocked" class="notice notice-warn">
                <span class="notice-icon">!</span>
                <span>该帖子已被锁定，无法回复。</span>
              </div>
            </div>

            <!-- 正文（Markdown 渲染） -->
            <article class="article-body markdown-body" v-html="renderedContent"></article>

            <!-- 标签页导航：详情 / 评论 -->
            <div class="tab-nav">
              <button class="tab" :class="{ active: tab === 'comments' }" @click="tab = 'comments'">
                <span>评论 {{ totalCommentCount }}</span>
              </button>
              <button class="tab" :class="{ active: tab === 'info' }" @click="tab = 'info'">
                <span>详情</span>
              </button>
            </div>

            <!-- 评论列表 -->
            <div v-show="tab === 'comments'" class="comments-section">
              <!-- 发评论 -->
              <div v-if="!post.isLocked" class="comment-form">
                <textarea
                  v-model="commentText"
                  class="comment-input"
                  rows="3"
                  placeholder="写下你的评论..."
                  :disabled="!userStore.isLoggedIn"
                ></textarea>
                <div class="comment-form-actions">
                  <span v-if="!userStore.isLoggedIn" class="login-tip">
                    <router-link to="/login">登录</router-link> 后评论
                  </span>
                  <button
                    class="btn-primary"
                    :disabled="!userStore.isLoggedIn || !commentText.trim() || submittingComment"
                    @click="submitComment"
                  >
                    {{ submittingComment ? '发送中...' : '发表评论' }}
                  </button>
                </div>
                <div v-if="commentMessage" class="inline-msg" :class="commentMessageType">{{ commentMessage }}</div>
              </div>

              <div v-if="topLevelComments.length === 0" class="state-box small">暂无评论，快来抢沙发吧</div>
              <div v-else class="comment-list">
                <div v-for="tc in topLevelComments" :key="tc.id" class="comment-thread">
                  <!-- 顶层评论 -->
                  <div class="comment-item">
                    <div class="comment-avatar">
                      {{ (tc.authorNickname || tc.authorUsername || 'U').charAt(0).toUpperCase() }}
                    </div>
                    <div class="comment-main">
                      <div class="comment-head">
                        <span class="comment-author">{{ tc.authorNickname || tc.authorUsername }}</span>
                        <span class="comment-time">{{ formatTime(tc.createdAt) }}</span>
                      </div>
                      <div class="comment-content markdown-body" v-html="renderMarkdown(tc.content)"></div>
                      <div class="comment-actions">
                        <button
                          class="action-link"
                          :class="{ liked: likedComments.has(tc.id) }"
                          @click="toggleCommentLike(tc.id)"
                        >
                          ❤ {{ tc.likeCount }}
                        </button>
                        <button
                          v-if="userStore.isLoggedIn && !post.isLocked"
                          class="action-link"
                          @click="openReply(tc.id)"
                        >
                          回复
                        </button>
                        <button
                          v-if="canDeleteComment(tc)"
                          class="action-link action-danger"
                          @click="deleteComment(tc.id)"
                        >
                          删除
                        </button>
                      </div>
                      <!-- 内联回复框 -->
                      <div v-if="replyingTo === tc.id" class="reply-box">
                        <textarea
                          v-model="replyText"
                          class="reply-input"
                          rows="2"
                          :placeholder="`回复 @${tc.authorNickname || tc.authorUsername}...`"
                        ></textarea>
                        <div class="reply-actions">
                          <button class="btn-cancel-sm" @click="cancelReply">取消</button>
                          <button
                            class="btn-primary-sm"
                            :disabled="!replyText.trim() || submittingReply"
                            @click="submitReply(tc.id)"
                          >
                            {{ submittingReply ? '发送中...' : '回复' }}
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- 子回复列表 -->
                  <div v-if="childrenMap[tc.id]?.length" class="reply-list">
                    <div v-for="rc in childrenMap[tc.id]" :key="rc.id" class="comment-item reply-item">
                      <div class="comment-avatar reply-avatar">
                        {{ (rc.authorNickname || rc.authorUsername || 'U').charAt(0).toUpperCase() }}
                      </div>
                      <div class="comment-main">
                        <div class="comment-head">
                          <span class="comment-author">{{ rc.authorNickname || rc.authorUsername }}</span>
                          <template v-if="replyToName(rc)">
                            <span class="reply-arrow">回复</span>
                            <span class="comment-author reply-target">@{{ replyToName(rc) }}</span>
                          </template>
                          <span class="comment-time">{{ formatTime(rc.createdAt) }}</span>
                        </div>
                        <div class="comment-content markdown-body" v-html="renderMarkdown(rc.content)"></div>
                        <div class="comment-actions">
                          <button
                            class="action-link"
                            :class="{ liked: likedComments.has(rc.id) }"
                            @click="toggleCommentLike(rc.id)"
                          >
                            ❤ {{ rc.likeCount }}
                          </button>
                          <button
                            v-if="userStore.isLoggedIn && !post.isLocked"
                            class="action-link"
                            @click="openReply(tc.id, rc.id)"
                          >
                            回复
                          </button>
                          <button
                            v-if="canDeleteComment(rc)"
                            class="action-link action-danger"
                            @click="deleteComment(rc.id)"
                          >
                            删除
                          </button>
                        </div>
                        <!-- 子回复的回复框 -->
                        <div v-if="replyingTo === tc.id && replyingToChild === rc.id" class="reply-box">
                          <textarea
                            v-model="replyText"
                            class="reply-input"
                            rows="2"
                            :placeholder="`回复 @${rc.authorNickname || rc.authorUsername}...`"
                          ></textarea>
                          <div class="reply-actions">
                            <button class="btn-cancel-sm" @click="cancelReply">取消</button>
                            <button
                              class="btn-primary-sm"
                              :disabled="!replyText.trim() || submittingReply"
                              @click="submitReply(rc.id)"
                            >
                              {{ submittingReply ? '发送中...' : '回复' }}
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 详情信息 -->
            <div v-show="tab === 'info'" class="info-section">
              <div class="info-row"><span>帖子 ID</span><span>{{ post.id }}</span></div>
              <div class="info-row"><span>所属板块</span><span>{{ post.boardName }}</span></div>
              <div class="info-row"><span>作者</span><span>{{ post.nickname || post.username }}</span></div>
              <div class="info-row"><span>创建时间</span><span>{{ formatFullTime(post.createdAt) }}</span></div>
              <div class="info-row"><span>最后回复</span><span>{{ formatFullTime(post.lastRepliedAt) }}</span></div>
              <div class="info-row"><span>更新时间</span><span>{{ formatFullTime(post.updatedAt) }}</span></div>
            </div>
          </template>
        </template>
      </main>

      <!-- 右侧侧边栏 -->
      <aside v-if="post && !editing" class="sidebar">
        <!-- 互动操作按钮 -->
        <button
          class="action-btn action-like"
          :class="{ active: isLiked }"
          @click="toggleLike"
        >
          <span class="action-icon">{{ isLiked ? '❤️' : '🤍' }}</span>
          <span>{{ isLiked ? '已点赞' : '点赞' }}</span>
        </button>

        <!-- 辅助功能链接 -->
        <div class="card secondary-actions">
          <a class="sec-link">疑问</a>
          <span class="sep">·</span>
          <a class="sec-link">举报</a>
          <span class="sep">·</span>
          <a class="sec-link">分享</a>
        </div>

        <!-- 作者卡片 -->
        <div class="card author-card">
          <div class="author-head">
            <img
              v-if="post.avatarPath"
              :src="post.avatarPath"
              class="author-avatar"
              :alt="post.username"
            />
            <div v-else class="author-avatar author-avatar-placeholder">
              {{ (post.nickname || post.username).charAt(0).toUpperCase() }}
            </div>
            <div class="author-meta">
              <router-link :to="`/user/${post.username}`" class="author-name">
                {{ post.nickname || post.username }}
              </router-link>
              <span class="author-role">@{{ post.username }}</span>
            </div>
          </div>
          <div class="author-stats">
            <div class="stat-row">
              <span class="stat-label">浏览</span>
              <span class="stat-num">{{ post.viewCount }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">点赞</span>
              <span class="stat-num">{{ post.likeCount }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">评论</span>
              <span class="stat-num">{{ totalCommentCount }}</span>
            </div>
          </div>
        </div>

        <!-- 时间信息 -->
        <div class="card time-card">
          <div class="time-row">
            <span>发布于</span>
            <span>{{ formatFullTime(post.createdAt) }}</span>
          </div>
          <div v-if="post.lastRepliedAt" class="time-row">
            <span>最后回复</span>
            <span>{{ formatFullTime(post.lastRepliedAt) }}</span>
          </div>
        </div>
      </aside>
    </div>

    <!-- 返回顶部 -->
    <button v-show="showBackToTop" class="back-top" @click="scrollTop">↑</button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import AppHeader from '@/components/AppHeader.vue'
import { api } from '@/api'
import { useUserStore } from '@/stores/user'
import type { Post, Reply } from '@/types'

// 配置 marked：启用 GFM 和换行
marked.setOptions({
  breaks: true,
  gfm: true
})

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const post = ref<Post | null>(null)
const comments = ref<Reply[]>([])
const loading = ref(true)
const tab = ref<'comments' | 'info'>('comments')
const isLiked = ref(false)
const likedComments = ref<Set<number>>(new Set())

const commentText = ref('')
const submittingComment = ref(false)
const commentMessage = ref('')
const commentMessageType = ref<'success' | 'error'>('success')
const showBackToTop = ref(false)

// 回复相关状态
const replyingTo = ref<number | null>(null) // 顶层评论 id
const replyingToChild = ref<number | null>(null) // 当回复子评论时，记录子评论 id
const replyText = ref('')
const submittingReply = ref(false)

// 编辑帖子相关状态
const editing = ref(false)
const savingEdit = ref(false)
const editForm = ref({ title: '', content: '' })
const editMessage = ref('')
const editMessageType = ref<'success' | 'error'>('success')

const postId = computed(() => Number(route.params.id))

// Markdown 渲染（marked 解析 + DOMPurify 消毒，防御 XSS）
// 形成三层 XSS 防御：后端 jsoup 消毒 → marked 默认转义 → 前端 DOMPurify 白名单过滤
const renderMarkdown = (content: string): string => {
  if (!content) return ''
  try {
    const rawHtml = marked.parse(content) as string
    return DOMPurify.sanitize(rawHtml, {
      ALLOWED_TAGS: [
        'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'p', 'br', 'hr',
        'ul', 'ol', 'li', 'blockquote', 'code', 'pre',
        'a', 'strong', 'em', 'del', 'img',
        'table', 'thead', 'tbody', 'tr', 'th', 'td',
        'span', 'div'
      ],
      ALLOWED_ATTR: ['href', 'src', 'alt', 'title', 'class'],
      ALLOW_DATA_ATTR: false
    })
  } catch {
    return content
  }
}

const renderedContent = computed(() => {
  return renderMarkdown(post.value?.content || '')
})

// 评论分组：后端返回扁平列表，前端按 parentId 组装层级结构
// topLevelComments - 顶层评论（parentId 为 null）
// childrenMap - 子回复映射 { parentId: Reply[] }
const topLevelComments = computed(() => {
  return comments.value.filter(c => !c.parentId)
})

const childrenMap = computed(() => {
  const map: Record<number, Reply[]> = {}
  for (const c of comments.value) {
    if (c.parentId) {
      if (!map[c.parentId]) map[c.parentId] = []
      map[c.parentId].push(c)
    }
  }
  return map
})

const totalCommentCount = computed(() => comments.value.length)

// 获取子回复的回复目标用户名（即 parent 评论的作者）
const replyToName = (reply: Reply): string => {
  if (!reply.parentId) return ''
  const parent = comments.value.find(c => c.id === reply.parentId)
  return parent ? (parent.authorNickname || parent.authorUsername) : ''
}

// 管理权限：作者可编辑/删除，管理员/版主可置顶/锁定/删除
const isOwner = computed(() => {
  if (!post.value || !userStore.isLoggedIn) return false
  return userStore.currentUser?.id === post.value.userId
})

const canManage = computed(() => {
  if (!post.value || !userStore.isLoggedIn) return false
  return isOwner.value || userStore.isStaff
})

const canDeleteComment = (c: Reply): boolean => {
  if (!userStore.isLoggedIn) return false
  const isCommentOwner = userStore.currentUser?.username === c.authorUsername
  return isCommentOwner || userStore.isStaff
}

const fetchPost = async () => {
  try {
    const res = await api.get(`/api/posts/${postId.value}`)
    post.value = res.data.data
  } catch {
    post.value = null
  }
}

const fetchComments = async () => {
  try {
    const res = await api.get(`/api/posts/${postId.value}/comments`)
    comments.value = res.data.data || []
  } catch {
    comments.value = []
  }
}

const togglePin = async () => {
  try {
    const res = await api.post(`/api/admin/posts/${postId.value}/pin`)
    post.value = res.data.data
  } catch (err: any) {
    showCommentMsg(err.response?.data?.message || '操作失败', 'error')
  }
}

const toggleLock = async () => {
  try {
    const res = await api.post(`/api/admin/posts/${postId.value}/lock`)
    post.value = res.data.data
  } catch (err: any) {
    showCommentMsg(err.response?.data?.message || '操作失败', 'error')
  }
}

const deletePost = async () => {
  if (!confirm('确定要删除这个帖子吗？此操作不可撤销。')) return
  try {
    await api.delete(`/api/posts/${postId.value}`)
    showCommentMsg('删除成功，正在跳转...', 'success')
    setTimeout(() => router.push('/boards'), 800)
  } catch (err: any) {
    showCommentMsg(err.response?.data?.message || '删除失败', 'error')
  }
}

// ===== 编辑帖子 =====
const startEdit = () => {
  if (!post.value) return
  editForm.value = {
    title: post.value.title,
    content: post.value.content
  }
  editing.value = true
  editMessage.value = ''
}

const cancelEdit = () => {
  editing.value = false
  editMessage.value = ''
}

const saveEdit = async () => {
  if (!editForm.value.title.trim() || !editForm.value.content.trim()) {
    editMessage.value = '标题和内容不能为空'
    editMessageType.value = 'error'
    return
  }
  savingEdit.value = true
  try {
    const res = await api.put(`/api/posts/${postId.value}`, {
      title: editForm.value.title.trim(),
      content: editForm.value.content.trim()
    })
    post.value = res.data.data
    editing.value = false
    showCommentMsg('修改已保存', 'success')
  } catch (err: any) {
    editMessage.value = err.response?.data?.message || '保存失败'
    editMessageType.value = 'error'
  } finally {
    savingEdit.value = false
  }
}

const toggleLike = async () => {
  if (!userStore.isLoggedIn) {
    showCommentMsg('请先登录', 'error')
    return
  }
  try {
    const res = await api.post(`/api/posts/${postId.value}/like`)
    isLiked.value = res.data.data
    if (post.value) {
      post.value.likeCount += isLiked.value ? 1 : -1
    }
  } catch (err: any) {
    showCommentMsg(err.response?.data?.message || '操作失败', 'error')
  }
}

const submitComment = async () => {
  if (!commentText.value.trim()) return
  submittingComment.value = true
  try {
    await api.post(`/api/posts/${postId.value}/comments`, { content: commentText.value.trim() })
    commentText.value = ''
    await fetchComments()
    showCommentMsg('评论成功', 'success')
  } catch (err: any) {
    showCommentMsg(err.response?.data?.message || '评论失败', 'error')
  } finally {
    submittingComment.value = false
  }
}

// ===== 回复评论 =====
// 回复评论（楼中楼）：记录当前回复目标（顶层评论 ID + 可选子评论 ID）
// 回复顶层评论时 replyingToChild 为 null，回复子评论时记录子评论 ID
const openReply = (topId: number, childId?: number) => {
  replyingTo.value = topId
  replyingToChild.value = childId ?? null
  replyText.value = ''
}

const cancelReply = () => {
  replyingTo.value = null
  replyingToChild.value = null
  replyText.value = ''
}

const submitReply = async (commentId: number) => {
  if (!replyText.value.trim()) return
  submittingReply.value = true
  try {
    await api.post(`/api/comments/${commentId}/reply`, { content: replyText.value.trim() })
    replyText.value = ''
    replyingTo.value = null
    replyingToChild.value = null
    await fetchComments()
    showCommentMsg('回复成功', 'success')
  } catch (err: any) {
    showCommentMsg(err.response?.data?.message || '回复失败', 'error')
  } finally {
    submittingReply.value = false
  }
}

const deleteComment = async (commentId: number) => {
  if (!confirm('确定要删除这条评论吗？')) return
  try {
    await api.delete(`/api/comments/${commentId}`)
    await fetchComments()
    showCommentMsg('评论已删除', 'success')
  } catch (err: any) {
    showCommentMsg(err.response?.data?.message || '删除失败', 'error')
  }
}

const toggleCommentLike = async (commentId: number) => {
  if (!userStore.isLoggedIn) {
    showCommentMsg('请先登录', 'error')
    return
  }
  try {
    const res = await api.post(`/api/comments/${commentId}/like`)
    const liked = res.data.data
    const c = comments.value.find(x => x.id === commentId)
    if (c) {
      c.likeCount += liked ? 1 : -1
    }
    if (liked) likedComments.value.add(commentId)
    else likedComments.value.delete(commentId)
  } catch {
    // ignore
  }
}

const showCommentMsg = (text: string, type: 'success' | 'error') => {
  commentMessage.value = text
  commentMessageType.value = type
  setTimeout(() => { commentMessage.value = '' }, 3000)
}

const formatTime = (dateStr: string) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}-${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const formatFullTime = (dateStr?: string) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const handleScroll = () => {
  showBackToTop.value = window.scrollY > 400
}

const scrollTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(async () => {
  await Promise.all([fetchPost(), fetchComments()])
  loading.value = false
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.post-detail {
  min-height: 100vh;
  background-color: var(--bg);
}

.main-layout {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 24px 80px;
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 28px;
  align-items: start;
}

.main-col {
  min-width: 0;
}

.sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 36px;
}

/* 面包屑 */
.breadcrumb {
  font-size: 13px;
  color: var(--text-dim);
  margin-bottom: 16px;
}

.breadcrumb a:hover {
  color: var(--accent);
}

.breadcrumb .sep {
  margin: 0 8px;
  color: #3d4856;
}

/* 状态框 */
.state-box {
  text-align: center;
  padding: 48px 20px;
  color: var(--text-dim);
  font-size: 15px;
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 12px;
}

.state-box.small {
  padding: 32px 20px;
}

/* 资源头部 */
.resource-header {
  display: flex;
  gap: 16px;
  padding: 24px;
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 12px;
  margin-bottom: 16px;
}

.resource-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 600;
  color: #06140b;
  background: linear-gradient(135deg, #3fb950, #2ea043);
}

.resource-info {
  flex: 1;
  min-width: 0;
}

.resource-title-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.board-prefix {
  font-size: 12px;
  font-weight: 500;
  padding: 2px 9px;
  border-radius: 6px;
  background-color: var(--accent-soft);
  color: var(--accent-hover);
  flex-shrink: 0;
}

.resource-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text);
  line-height: 1.3;
}

.resource-sub {
  font-size: 13px;
  color: var(--text-dim);
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.resource-sub .sep {
  color: #3d4856;
}

.badge {
  font-size: 11px;
  padding: 2px 7px;
  border-radius: 6px;
}

.badge-pinned {
  background-color: rgba(210, 153, 34, 0.2);
  color: #e3b341;
}

.badge-locked {
  background-color: rgba(248, 81, 73, 0.15);
  color: #f85149;
}

/* 管理操作栏 */
.manage-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.mg-btn {
  padding: 7px 18px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background-color: var(--bg-soft);
  color: var(--text-dim);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}

.mg-btn:hover {
  border-color: var(--accent);
  color: var(--accent-hover);
}

.mg-btn.active {
  background-color: rgba(210, 153, 34, 0.15);
  border-color: rgba(210, 153, 34, 0.5);
  color: #e3b341;
}

.mg-danger:hover {
  border-color: #f85149;
  color: #f85149;
  background-color: rgba(248, 81, 73, 0.08);
}

/* 通知横幅 */
.notices {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}

.notice {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 13px;
  border-left: 3px solid;
}

.notice-icon {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.notice-info {
  background-color: var(--accent-soft);
  color: var(--accent-hover);
  border-left-color: var(--accent);
}

.notice-info .notice-icon {
  background: var(--accent);
  color: #06140b;
}

.notice-warn {
  background-color: rgba(248, 81, 73, 0.08);
  color: #f85149;
  border-left-color: #f85149;
}

.notice-warn .notice-icon {
  background: #f85149;
  color: #fff;
}

/* 正文 */
.article-body {
  padding: 24px;
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 12px;
  margin-bottom: 20px;
  font-size: 15px;
  line-height: 1.8;
  color: var(--text);
  word-break: break-word;
}

/* Markdown 渲染样式（不 scoped，因为 v-html 内容不受 scoped 影响） */
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 16px 0 8px;
  font-weight: 600;
  line-height: 1.3;
}

.markdown-body :deep(h1) { font-size: 1.6em; }
.markdown-body :deep(h2) { font-size: 1.4em; }
.markdown-body :deep(h3) { font-size: 1.2em; }

.markdown-body :deep(p) {
  margin-bottom: 12px;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 8px 0 12px;
  padding-left: 24px;
}

.markdown-body :deep(li) {
  margin-bottom: 4px;
}

.markdown-body :deep(code) {
  background-color: rgba(110, 118, 129, 0.2);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.9em;
  font-family: 'SFMono-Regular', Consolas, monospace;
}

.markdown-body :deep(pre) {
  background-color: rgba(0, 0, 0, 0.4);
  padding: 12px 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0 12px;
}

.markdown-body :deep(pre code) {
  background: none;
  padding: 0;
}

.markdown-body :deep(blockquote) {
  border-left: 3px solid var(--accent);
  padding: 4px 16px;
  margin: 8px 0;
  color: var(--text-dim);
}

.markdown-body :deep(a) {
  color: var(--accent-hover);
  text-decoration: none;
}

.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

.markdown-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0 12px;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid var(--border);
  padding: 8px 12px;
  text-align: left;
}

.markdown-body :deep(th) {
  background-color: rgba(110, 118, 129, 0.15);
}

/* 编辑模式 */
.edit-wrap {
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 24px;
}

.edit-head {
  margin-bottom: 20px;
}

.edit-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text);
}

.edit-row {
  margin-bottom: 20px;
}

.edit-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
  margin-bottom: 8px;
}

.edit-input {
  width: 100%;
  padding: 10px 14px;
  background-color: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--text);
  font-size: 14px;
  font-family: inherit;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.15s;
}

.edit-input:focus {
  border-color: var(--accent);
}

.edit-textarea {
  resize: vertical;
  min-height: 320px;
  line-height: 1.7;
}

.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
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

/* 标签页 */
.tab-nav {
  display: flex;
  gap: 4px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--border);
}

.tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  color: var(--text-dim);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s;
  margin-bottom: -1px;
}

.tab:hover {
  color: var(--text);
}

.tab.active {
  color: var(--accent-hover);
  border-bottom-color: var(--accent);
}

/* 评论 */
.comment-form {
  padding: 16px;
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 12px;
  margin-bottom: 16px;
}

.comment-input {
  width: 100%;
  padding: 12px 14px;
  background-color: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--text);
  font-size: 14px;
  font-family: inherit;
  outline: none;
  resize: vertical;
  box-sizing: border-box;
  transition: border-color 0.15s;
}

.comment-input:focus {
  border-color: var(--accent);
}

.comment-form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.login-tip {
  font-size: 13px;
  color: var(--text-dim);
}

.login-tip a {
  color: var(--accent-hover);
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

.inline-msg {
  font-size: 13px;
  margin-top: 10px;
}

.inline-msg.success { color: var(--accent-hover); }
.inline-msg.error { color: #f85149; }

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-thread {
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px;
}

.comment-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 600;
  color: #06140b;
  background: linear-gradient(135deg, #3fb950, #2ea043);
}

.reply-avatar {
  width: 30px;
  height: 30px;
  font-size: 13px;
}

.comment-main {
  flex: 1;
  min-width: 0;
}

.comment-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.comment-author {
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
}

.reply-arrow {
  font-size: 12px;
  color: var(--text-dim);
}

.reply-target {
  font-weight: 400;
  color: var(--accent-hover);
}

.comment-time {
  font-size: 12px;
  color: var(--text-dim);
}

.comment-content {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text);
  word-break: break-word;
}

.comment-actions {
  margin-top: 8px;
  display: flex;
  gap: 16px;
  align-items: center;
}

.action-link {
  background: none;
  border: none;
  color: var(--text-dim);
  font-size: 13px;
  cursor: pointer;
  transition: color 0.15s;
}

.action-link:hover,
.action-link.liked {
  color: #f85149;
}

.action-danger:hover {
  color: #f85149;
}

/* 回复框 */
.reply-box {
  margin-top: 10px;
  padding: 12px;
  background-color: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
}

.reply-input {
  width: 100%;
  padding: 8px 12px;
  background-color: var(--bg);
  border: 1px solid var(--border);
  border-radius: 6px;
  color: var(--text);
  font-size: 13px;
  font-family: inherit;
  outline: none;
  resize: vertical;
  box-sizing: border-box;
}

.reply-input:focus {
  border-color: var(--accent);
}

.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.btn-cancel-sm {
  padding: 5px 14px;
  background-color: transparent;
  color: var(--text-dim);
  font-size: 13px;
  border: 1px solid var(--border);
  border-radius: 6px;
  cursor: pointer;
}

.btn-cancel-sm:hover {
  color: var(--text);
}

.btn-primary-sm {
  padding: 5px 16px;
  background-color: var(--accent);
  color: #06140b;
  font-size: 13px;
  font-weight: 500;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.btn-primary-sm:hover:not(:disabled) {
  background-color: var(--accent-hover);
}

.btn-primary-sm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 子回复列表 */
.reply-list {
  padding: 0 16px 12px 56px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  border-top: 1px solid var(--border);
  margin-left: 56px;
  background-color: rgba(0, 0, 0, 0.15);
}

.reply-item {
  padding: 12px 16px;
  background-color: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
}

/* 详情信息 */
.info-section {
  padding: 24px;
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 12px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  font-size: 14px;
  border-bottom: 1px solid var(--border);
}

.info-row:last-child {
  border-bottom: none;
}

.info-row span:first-child {
  color: var(--text-dim);
}

.info-row span:last-child {
  color: var(--text);
}

/* 侧边栏卡片 */
.card {
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
}

/* 互动按钮 */
.action-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 24px;
  border-radius: 999px;
  border: 1px solid var(--border);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  background-color: var(--bg-soft);
  color: var(--text-dim);
}

.action-btn:hover {
  border-color: #484f58;
  color: var(--text);
}

.action-btn.active {
  background-color: rgba(248, 81, 73, 0.12);
  border-color: rgba(248, 81, 73, 0.5);
  color: #f85149;
}

.action-icon {
  font-size: 15px;
}

/* 辅助链接 */
.secondary-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 13px;
}

.sec-link {
  color: var(--text-dim);
  cursor: pointer;
  transition: color 0.15s;
}

.sec-link:hover {
  color: var(--accent-hover);
}

.secondary-actions .sep {
  color: #3d4856;
}

/* 作者卡片 */
.author-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 14px;
}

.author-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.author-avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  color: #06140b;
  background: linear-gradient(135deg, #3fb950, #2ea043);
}

.author-meta {
  flex: 1;
  min-width: 0;
}

.author-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  display: block;
}

.author-name:hover {
  color: var(--accent-hover);
}

.author-role {
  font-size: 12px;
  color: var(--text-dim);
}

.author-stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  padding: 7px 18px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background-color: var(--bg-card);
}

.stat-label {
  color: var(--text-dim);
}

.stat-num {
  color: var(--text);
  font-weight: 500;
}

/* 时间卡片 */
.time-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.time-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.time-row span:first-child {
  color: var(--text-dim);
}

.time-row span:last-child {
  color: var(--text);
}

/* 返回顶部 */
.back-top {
  position: fixed;
  right: 28px;
  bottom: 28px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 1px solid var(--border);
  background-color: var(--bg-soft);
  color: var(--text);
  font-size: 18px;
  cursor: pointer;
  z-index: 50;
  transition: all 0.15s;
}

.back-top:hover {
  background-color: var(--accent-soft);
  transform: translateY(-2px);
}

/* 响应式 */
@media (max-width: 900px) {
  .main-layout {
    grid-template-columns: 1fr;
  }
  .sidebar {
    position: static;
  }
  .reply-list {
    margin-left: 0;
    padding-left: 16px;
  }
}
</style>
