<template>
  <div>
    <AppHeader />

    <!-- 页面标题区 -->
    <div class="page-head">
      <div class="breadcrumb">
        <router-link to="/">首页</router-link>
        <span class="sep">/</span>
        <span>资源</span>
      </div>
      <h1 class="page-title">资源中心</h1>
    </div>

    <!-- 双栏布局 -->
    <div class="layout">
      <!-- 左侧边栏 -->
      <aside>
        <!-- 分类 -->
        <div class="panel">
          <div class="panel-head">分类</div>
          <div class="panel-body">
            <a
              v-for="board in boards"
              :key="board.id"
              href="#"
              class="cat-item"
              @click.prevent="selectBoard(board.slug)"
            >
              <span>{{ board.name }}</span>
            </a>
            <div v-if="!boards || boards.length === 0" class="cat-item">
              <span>暂无分类</span>
            </div>
          </div>
        </div>

        <!-- 推荐资源 -->
        <div class="panel">
          <div class="panel-head">推荐资源</div>
          <div class="panel-body">
            <a
              v-for="post in recommended"
              :key="post.id"
              href="#"
              class="mini-card"
              @click.prevent="goToPost(post.id)"
            >
              <div class="mini-title">{{ post.title }}</div>
              <div class="mini-meta">
                <span>{{ post.nickname || post.username }}</span>
                <span> · </span>
                <span>{{ post.viewCount }}</span> 浏览
              </div>
            </a>
            <div v-if="!recommended || recommended.length === 0" class="mini-card">
              <div class="mini-meta">暂无推荐</div>
            </div>
          </div>
        </div>
      </aside>

      <!-- 右侧主内容 -->
      <div class="main-col">
        <!-- 顶部横幅 -->
        <div class="banner">
          <div class="banner-text">
            <h2 class="banner-title">欢迎来到 EchoBBS 资源中心</h2>
            <p>浏览社区里的最新分享，找到你需要的内容，也可以发布属于自己的资源。</p>
            <div class="banner-btns">
              <a href="javascript:void(0)" class="banner-btn ghost" @click="handlePublishClick">发布资源</a>
            </div>
          </div>
          <div class="quick-links">
            <router-link to="/" class="quick-link"><span class="qi"></span> 返回首页</router-link>
            <router-link to="/boards" class="quick-link"><span class="qi"></span> 全部分类</router-link>
            <a href="#" class="quick-link"><span class="qi"></span> 热门社区</a>
            <a href="#" class="quick-link"><span class="qi"></span> 帮助中心</a>
          </div>
        </div>

        <!-- 搜索栏 -->
        <form class="search" @submit.prevent="handleSearch">
          <label>
            <input
              v-model="searchQuery"
              type="text"
              placeholder="搜索资源、话题或作者…"
              autocomplete="off"
            />
          </label>
          <button type="submit">搜索</button>
        </form>

        <!-- 工具栏：过滤 + 顶部分页 -->
        <div class="toolbar">
          <select v-model="sortBy" class="filter-select" @change="loadResources">
            <option value="latest">最新发布</option>
            <option value="views">最多浏览</option>
            <option value="likes">最多点赞</option>
          </select>
          <div class="pager">
            <span @click="prevPage">‹</span>
            <span
              v-for="page in visiblePages"
              :key="page"
              :class="{ current: page === currentPage }"
              @click="goToPage(page)"
            >
              {{ page }}
            </span>
            <span @click="nextPage">›</span>
          </div>
        </div>

        <!-- 资源列表 -->
        <div v-if="resources && resources.length > 0">
          <div v-for="post in resources" :key="post.id" class="res-item" @click="goToPost(post.id)">
            <div class="res-main">
              <div class="res-tags">
                <a
                  href="#"
                  class="board-tag"
                  @click.stop.prevent="selectBoard(post.boardSlug)"
                >
                  {{ post.boardName }}
                </a>
              </div>
              <a href="#" class="res-title" @click.stop.prevent="goToPost(post.id)">
                <span v-if="post.isPinned" class="resource-badge badge-pinned">置顶</span>
                <span v-if="post.isLocked" class="resource-badge badge-locked">锁定</span>
                {{ post.title }}
              </a>
              <div class="res-meta">
                <span>{{ post.nickname || post.username }}</span>
                <span class="sep">·</span>
                <span>发布于 </span>
                <span>{{ formatDate(post.createdAt) }}</span>
              </div>
            </div>
            <div class="res-stats">
              <div>
                <div class="stat-value">{{ post.likeCount || 0 }}</div>
                <div class="stat-label">点赞</div>
              </div>
              <div>
                <div class="stat-value">{{ post.viewCount || 0 }}</div>
                <div class="stat-label">浏览</div>
              </div>
            </div>
            <div class="res-update">
              <span class="u-label">更新于</span>
              <span>{{ formatDate(post.updatedAt) }}</span>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          暂无资源
        </div>

        <!-- 底部分页 -->
        <div class="toolbar" style="justify-content: center; margin-top: 20px">
          <div class="pager">
            <span @click="prevPage">‹</span>
            <span
              v-for="page in visiblePages"
              :key="'bottom-' + page"
              :class="{ current: page === currentPage }"
              @click="goToPage(page)"
            >
              {{ page }}
            </span>
            <span @click="nextPage">›</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 返回顶部 -->
    <button
      v-show="showBackToTop"
      class="back-top"
      style="display: flex"
      aria-label="返回顶部"
      @click="scrollToTop"
    >
      ↑
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import AppHeader from '@/components/AppHeader.vue';
import { api } from '@/api';
import { useUserStore } from '@/stores/user';
import type { Board } from '@/types';

const router = useRouter();
const userStore = useUserStore();

// 数据状态
const boards = ref<Board[]>([]);
const recommended = ref([]);
const resources = ref([]);
const searchQuery = ref('');
const sortBy = ref('latest');
const showBackToTop = ref(false);
const currentPage = ref(1);
const totalPages = ref(1);

// 计算可见页码
const visiblePages = computed(() => {
  const pages = [];
  const start = Math.max(1, currentPage.value - 1);
  const end = Math.min(totalPages.value, start + 2);
  for (let i = start; i <= end; i++) {
    pages.push(i);
  }
  return pages;
});

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

// 选择板块
const selectBoard = (slug: string) => {
  console.log('选择板块:', slug);
  // 这里可以添加板块筛选逻辑
};

// 跳转到帖子详情
const goToPost = (id: number) => {
  router.push(`/posts/${id}`);
};

// 搜索
const handleSearch = () => {
  currentPage.value = 1;
  loadResources();
};

// 上一页
const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--;
    loadResources();
    scrollToTop();
  }
};

// 下一页
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++;
    loadResources();
    scrollToTop();
  }
};

// 跳转到指定页
const goToPage = (page: number) => {
  if (page >= 1 && page <= totalPages.value && page !== currentPage.value) {
    currentPage.value = page;
    loadResources();
    scrollToTop();
  }
};

// 点击"发布资源"：跳转到独立发帖页（路由守卫会处理登录态）
const handlePublishClick = () => {
  router.push('/posts/new');
};

// 滚动到顶部
const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

// 加载板块列表
const loadBoards = async () => {
  try {
    const response = await api.get('/api/boards');
    boards.value = response.data.data || [];
  } catch (error) {
    console.error('加载板块失败:', error);
    boards.value = [];
  }
};

// 加载推荐资源
const loadRecommended = async () => {
  try {
    const response = await api.get('/api/posts/recommended', { params: { limit: 5 } });
    recommended.value = response.data.data || [];
  } catch (error) {
    console.error('加载推荐资源失败:', error);
    recommended.value = [];
  }
};

// 加载资源列表
const loadResources = async () => {
  try {
    const params: any = {
      page: currentPage.value - 1,
      size: 20,
      sort: sortBy.value
    };
    
    if (searchQuery.value) {
      params.q = searchQuery.value;
    }

    const response = await api.get('/api/posts', { params });
    const pageData = response.data.data || {};
    resources.value = pageData.content || [];
    // 兼容 VIA_DTO 序列化：分页信息在 page 子对象；兼容旧结构直接取 totalPages
    totalPages.value = pageData.page?.totalPages ?? pageData.totalPages ?? 1;
  } catch (error) {
    console.error('加载资源失败:', error);
    resources.value = [];
  }
};

// 处理滚动事件
const handleScroll = () => {
  showBackToTop.value = window.scrollY > 300;
};

// 生命周期
onMounted(() => {
  loadBoards();
  loadRecommended();
  loadResources();
  window.addEventListener('scroll', handleScroll);
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
});
</script>

<style scoped>
.page-head {
  max-width: 1200px;
  margin: 0 auto;
  padding: 28px 24px 8px;
}
.breadcrumb {
  font-size: 13px;
  color: var(--text-dim);
  margin-bottom: 10px;
}
.breadcrumb a:hover { color: var(--accent); }
.breadcrumb .sep { margin: 0 8px; color: #3d4856; }
.page-title { font-size: 26px; font-weight: normal; letter-spacing: -0.5px; }

.layout {
  max-width: 1200px;
  margin: 0 auto 64px;
  padding: 20px 24px 0;
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 24px;
  align-items: start;
}

.panel {
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 12px;
  margin-bottom: 20px;
  overflow: hidden;
}
.panel-head {
  padding: 14px 16px;
  font-size: 14px;
  font-weight: normal;
  border-bottom: 1px solid var(--border);
}
.panel-body { padding: 8px; }

.cat-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 12px;
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-dim);
  transition: background-color 0.15s, color 0.15s;
  cursor: pointer;
}
.cat-item:hover { background-color: var(--bg-card); color: var(--text); }

.mini-card {
  display: block;
  padding: 10px 12px;
  border-radius: 8px;
  transition: background-color 0.15s;
  cursor: pointer;
}
.mini-card:hover { background-color: var(--bg-card); }
.mini-title {
  font-size: 13px;
  font-weight: normal;
  color: var(--text);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.15s;
}
.mini-card:hover .mini-title { color: var(--accent-hover); }
.mini-meta { font-size: 12px; color: var(--text-dim); margin-top: 5px; }

.main-col { min-width: 0; }

.banner {
  background: radial-gradient(120% 140% at 100% 0%, rgba(63, 185, 80, 0.18) 0%, transparent 55%), #000000;
  border: 1px solid rgba(63, 185, 80, 0.25);
  border-radius: 14px;
  padding: 28px;
  display: flex;
  gap: 24px;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.banner-title {
  font-size: 40px;
  font-weight: normal;
  text-align: center;
  color: var(--accent);
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}
.banner-text { max-width: 460px; text-align: center; }
.banner-text h2 { font-size: 24px; font-weight: normal; margin-bottom: 8px; }
.banner-text p { font-size: 14px; color: rgba(255,255,255,0.85); line-height: 1.6; margin-bottom: 16px; }
.banner-btns { display: flex; gap: 10px; flex-wrap: wrap; }
.banner-btn {
  font-size: 14px;
  font-weight: normal;
  padding: 9px 18px;
  border-radius: 8px;
  transition: all 0.15s;
  cursor: pointer;
}
.banner-btn.ghost { background-color: transparent; border: 1px solid var(--accent); color: #fff; }
.banner-btn.ghost:hover { background-color: var(--accent-soft); }
.quick-links { display: flex; flex-direction: column; gap: 8px; min-width: 200px; }
.quick-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 14px;
  background-color: rgba(255,255,255,0.1);
  border-radius: 8px;
  font-size: 13px;
  font-weight: normal;
  transition: background-color 0.15s;
}
.quick-link:hover { background-color: rgba(255,255,255,0.22); }
.quick-link .qi { font-size: 16px; }

.search {
  display: flex;
  width: 100%;
  background-color: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 16px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.search:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
}
.search label { flex: 1; display: flex; }
.search input {
  flex: 1;
  width: 100%;
  border: none;
  outline: none;
  background: transparent;
  color: var(--text);
  font-size: 14px;
  padding: 12px 16px;
}
.search input::placeholder { color: var(--text-dim); }
.search button {
  border: none;
  cursor: pointer;
  background-color: var(--accent);
  color: #06140b;
  font-size: 14px;
  font-weight: normal;
  padding: 0 24px;
  margin: 5px;
  border-radius: 9px;
  transition: background-color 0.15s;
}
.search button:hover { background-color: var(--accent-hover); }

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
  flex-wrap: wrap;
}
.filter-select {
  font-size: 13px;
  color: var(--text);
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 8px 12px;
  outline: none;
  cursor: pointer;
}
.pager { display: flex; gap: 6px; align-items: center; }
.pager span {
  font-size: 13px;
  min-width: 32px;
  height: 32px;
  padding: 0 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--text-dim);
  background-color: var(--bg-soft);
  transition: all 0.15s;
  cursor: pointer;
}
.pager span:hover { border-color: #484f58; color: var(--text); }
.pager .current { background-color: var(--accent); color: #06140b; border-color: var(--accent); }

.res-item {
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
.res-item:hover {
  background-color: var(--bg-card);
  border-color: #3d4856;
  transform: translateY(-2px);
}
.res-main { flex: 1; min-width: 0; }
.res-tags { margin-bottom: 6px; }
.board-tag {
  font-size: 11px;
  font-weight: normal;
  padding: 2px 9px;
  border-radius: 6px;
  background-color: var(--accent-soft);
  color: var(--accent-hover);
}
.res-title {
  font-size: 16px;
  font-weight: normal;
  color: var(--text);
  transition: color 0.15s;
}
.res-item:hover .res-title { color: var(--accent-hover); }
.res-meta { font-size: 13px; color: var(--text-dim); margin-top: 6px; }
.res-meta .sep { margin: 0 7px; color: #3d4856; }
.res-stats {
  display: flex;
  gap: 22px;
  flex-shrink: 0;
  white-space: nowrap;
  text-align: center;
}
.stat-value { font-size: 18px; font-weight: normal; color: var(--text); }
.stat-label { font-size: 11px; color: var(--text-dim); margin-top: 2px; }
.res-update {
  font-size: 12px;
  color: var(--text-dim);
  flex-shrink: 0;
  white-space: nowrap;
  text-align: right;
  min-width: 96px;
}
.res-update .u-label { display: block; margin-bottom: 2px; }

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-dim);
  font-size: 14px;
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 12px;
}

.back-top {
  position: fixed;
  right: 28px;
  bottom: 28px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background-color: transparent;
  border: 1px solid var(--accent);
  color: #fff;
  cursor: pointer;
  font-size: 20px;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 20px rgba(63, 185, 80, 0.4);
  transition: background-color 0.15s, transform 0.15s;
  z-index: 90;
}
.back-top:hover { background-color: var(--accent-soft); transform: translateY(-2px); }

.board-modal {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  align-items: center;
  justify-content: center;
}
.board-modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(4px);
}
.board-modal-content {
  position: relative;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  background-color: var(--bg);
  border: 1px solid var(--border);
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}
.board-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border);
}
.board-modal-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-dim);
}
.board-modal-close {
  width: 32px;
  height: 32px;
  border: none;
  background-color: transparent;
  color: var(--text-dim);
  font-size: 20px;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.15s;
}
.board-modal-close:hover {
  background-color: var(--bg-soft);
  color: var(--text);
}
.board-modal-body {
  overflow-y: auto;
  padding: 12px;
}
.board-modal.show { display: flex; }

.board-option {
  display: block;
  padding: 16px 20px;
  background-color: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.15s;
}
.board-option:hover {
  background-color: var(--bg-soft);
  border-color: var(--accent);
  transform: translateY(-1px);
}
.board-option-name {
  font-size: 16px;
  font-weight: 500;
  color: var(--text);
  margin-bottom: 6px;
}
.board-option-desc {
  font-size: 14px;
  color: var(--text-dim);
  line-height: 1.5;
}

/* 发帖表单模态框 */
.post-form-modal {
  max-width: 720px;
}

.post-form-row {
  margin-bottom: 18px;
}

.post-form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
  margin-bottom: 8px;
}

.post-form-input {
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

.post-form-input:focus {
  border-color: var(--accent);
}

.post-form-textarea {
  resize: vertical;
  min-height: 200px;
  line-height: 1.6;
}

.post-form-hint {
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 6px;
  text-align: right;
}

.post-form-message {
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
  margin-bottom: 16px;
}

.post-form-message.success {
  background-color: var(--accent-soft);
  color: var(--accent-hover);
  border: 1px solid var(--accent);
}

.post-form-message.error {
  background-color: rgba(248, 81, 73, 0.1);
  color: #f85149;
  border: 1px solid #f85149;
}

.post-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.post-btn-cancel {
  display: inline-flex;
  align-items: center;
  padding: 10px 24px;
  background-color: transparent;
  color: var(--text-dim);
  font-size: 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  text-decoration: none;
}

.post-btn-cancel:hover {
  color: var(--text);
  border-color: #484f58;
}

.post-btn-submit {
  display: inline-flex;
  align-items: center;
  padding: 10px 32px;
  background-color: var(--accent);
  color: #06140b;
  font-size: 14px;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.15s;
  text-decoration: none;
}

.post-btn-submit:hover:not(:disabled) {
  background-color: var(--accent-hover);
}

.post-btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 游客提示 */
.login-tip-modal {
  max-width: 380px;
}

.login-tip-text {
  font-size: 15px;
  color: var(--text-dim);
  text-align: center;
  padding: 16px 0;
}

@media (max-width: 900px) {
  .layout { grid-template-columns: 1fr; }
  .res-stats { gap: 14px; }
  .res-update { display: none; }
}


.back-top {
  position: fixed;
  right: 28px;
  bottom: 28px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background-color: transparent;
  border: 1px solid var(--accent);
  color: #fff;
  cursor: pointer;
  font-size: 20px;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 20px rgba(63, 185, 80, 0.4);
  transition: background-color 0.15s, transform 0.15s;
  z-index: 90;
}
.back-top:hover { background-color: var(--accent-soft); transform: translateY(-2px); }

.board-modal {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  align-items: center;
  justify-content: center;
}
.board-modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(4px);
}
.board-modal-content {
  position: relative;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  background-color: var(--bg);
  border: 1px solid var(--border);
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}
.board-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border);
}
.board-modal-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-dim);
}
.board-modal-close {
  width: 32px;
  height: 32px;
  border: none;
  background-color: transparent;
  color: var(--text-dim);
  font-size: 20px;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.15s;
}
.board-modal-close:hover {
  background-color: var(--bg-soft);
  color: var(--text);
}
.board-modal-body {
  overflow-y: auto;
  padding: 12px;
}
.board-modal.show { display: flex; }

.board-option {
  display: block;
  padding: 16px 20px;
  background-color: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.15s;
}
.board-option:hover {
  background-color: var(--bg-soft);
  border-color: var(--accent);
  transform: translateY(-1px);
}
.board-option-name {
  font-size: 16px;
  font-weight: 500;
  color: var(--text);
  margin-bottom: 6px;
}
.board-option-desc {
  font-size: 14px;
  color: var(--text-dim);
  line-height: 1.5;
}

/* 发帖表单模态框 */
.post-form-modal {
  max-width: 720px;
}

.post-form-row {
  margin-bottom: 18px;
}

.post-form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
  margin-bottom: 8px;
}

.post-form-input {
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

.post-form-input:focus {
  border-color: var(--accent);
}

.post-form-textarea {
  resize: vertical;
  min-height: 200px;
  line-height: 1.6;
}

.post-form-hint {
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 6px;
  text-align: right;
}

.post-form-message {
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
  margin-bottom: 16px;
}

.post-form-message.success {
  background-color: var(--accent-soft);
  color: var(--accent-hover);
  border: 1px solid var(--accent);
}

.post-form-message.error {
  background-color: rgba(248, 81, 73, 0.1);
  color: #f85149;
  border: 1px solid #f85149;
}

.post-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.post-btn-cancel {
  display: inline-flex;
  align-items: center;
  padding: 10px 24px;
  background-color: transparent;
  color: var(--text-dim);
  font-size: 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  text-decoration: none;
}

.post-btn-cancel:hover {
  color: var(--text);
  border-color: #484f58;
}

.post-btn-submit {
  display: inline-flex;
  align-items: center;
  padding: 10px 32px;
  background-color: var(--accent);
  color: #06140b;
  font-size: 14px;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.15s;
  text-decoration: none;
}

.post-btn-submit:hover:not(:disabled) {
  background-color: var(--accent-hover);
}

.post-btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 游客提示 */
.login-tip-modal {
  max-width: 380px;
}

.login-tip-text {
  font-size: 15px;
  color: var(--text-dim);
  text-align: center;
  padding: 16px 0;
}

@media (max-width: 900px) {
  .layout { grid-template-columns: 1fr; }
  .res-stats { gap: 14px; }
  .res-update { display: none; }
}





<style scoped>
.page-container {
  min-height: 100vh;
  background: var(--bg-primary);
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}

/* 页面头部 */
.page-header {
  margin-bottom: 32px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.breadcrumb a {
  color: var(--text-secondary);
  text-decoration: none;
  transition: color 0.2s;
}

.breadcrumb a:hover {
  color: var(--primary);
}

.breadcrumb .separator {
  color: var(--border);
}

.breadcrumb .current {
  color: var(--text-primary);
}

.page-title {
  font-size: 32px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 16px;
  color: var(--text-secondary);
  margin: 0;
}

/* 内容布局 */
.content-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 24px;
  align-items: start;
}

/* 左侧边栏 */
.sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
  position: sticky;
  top: 80px;
}

.panel {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.panel-body {
  padding: 12px;
}

/* 分类列表 */
.category-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  color: var(--text-primary);
  text-decoration: none;
  transition: all 0.2s;
  cursor: pointer;
}

.category-item:hover {
  background: var(--bg-hover);
}

.category-item.active {
  background: var(--primary-light);
  color: var(--primary);
}

.category-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.category-name {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
}

.category-count {
  font-size: 12px;
  color: var(--text-tertiary);
  background: var(--bg-tertiary);
  padding: 2px 8px;
  border-radius: 12px;
}

.back-top {
  position: fixed;
  right: 28px;
  bottom: 28px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background-color: transparent;
  border: 1px solid var(--accent);
  color: #fff;
  cursor: pointer;
  font-size: 20px;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 20px rgba(63, 185, 80, 0.4);
  transition: background-color 0.15s, transform 0.15s;
  z-index: 90;
}
.back-top:hover { background-color: var(--accent-soft); transform: translateY(-2px); }

.board-modal {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  align-items: center;
  justify-content: center;
}
.board-modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(4px);
}
.board-modal-content {
  position: relative;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  background-color: var(--bg);
  border: 1px solid var(--border);
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}
.board-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border);
}
.board-modal-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-dim);
}
.board-modal-close {
  width: 32px;
  height: 32px;
  border: none;
  background-color: transparent;
  color: var(--text-dim);
  font-size: 20px;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.15s;
}
.board-modal-close:hover {
  background-color: var(--bg-soft);
  color: var(--text);
}
.board-modal-body {
  overflow-y: auto;
  padding: 12px;
}
.board-modal.show { display: flex; }

.board-option {
  display: block;
  padding: 16px 20px;
  background-color: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.15s;
}
.board-option:hover {
  background-color: var(--bg-soft);
  border-color: var(--accent);
  transform: translateY(-1px);
}
.board-option-name {
  font-size: 16px;
  font-weight: 500;
  color: var(--text);
  margin-bottom: 6px;
}
.board-option-desc {
  font-size: 14px;
  color: var(--text-dim);
  line-height: 1.5;
}

/* 发帖表单模态框 */
.post-form-modal {
  max-width: 720px;
}

.post-form-row {
  margin-bottom: 18px;
}

.post-form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
  margin-bottom: 8px;
}

.post-form-input {
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

.post-form-input:focus {
  border-color: var(--accent);
}

.post-form-textarea {
  resize: vertical;
  min-height: 200px;
  line-height: 1.6;
}

.post-form-hint {
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 6px;
  text-align: right;
}

.post-form-message {
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
  margin-bottom: 16px;
}

.post-form-message.success {
  background-color: var(--accent-soft);
  color: var(--accent-hover);
  border: 1px solid var(--accent);
}

.post-form-message.error {
  background-color: rgba(248, 81, 73, 0.1);
  color: #f85149;
  border: 1px solid #f85149;
}

.post-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.post-btn-cancel {
  display: inline-flex;
  align-items: center;
  padding: 10px 24px;
  background-color: transparent;
  color: var(--text-dim);
  font-size: 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  text-decoration: none;
}

.post-btn-cancel:hover {
  color: var(--text);
  border-color: #484f58;
}

.post-btn-submit {
  display: inline-flex;
  align-items: center;
  padding: 10px 32px;
  background-color: var(--accent);
  color: #06140b;
  font-size: 14px;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.15s;
  text-decoration: none;
}

.post-btn-submit:hover:not(:disabled) {
  background-color: var(--accent-hover);
}

.post-btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 游客提示 */
.login-tip-modal {
  max-width: 380px;
}

.login-tip-text {
  font-size: 15px;
  color: var(--text-dim);
  text-align: center;
  padding: 16px 0;
}

@media (max-width: 900px) {
  .layout { grid-template-columns: 1fr; }
  .res-stats { gap: 14px; }
  .res-update { display: none; }
}


/* 推荐资源 */
.resource-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.resource-item {
  display: block;
  padding: 12px;
  border-radius: 8px;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  text-decoration: none;
  transition: all 0.2s;
}

.resource-item:hover {
  border-color: var(--primary);
  transform: translateY(-2px);
}

.resource-badge {
  display: inline-block;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  margin-right: 6px;
  font-weight: 500;
  vertical-align: middle;
}

.badge-pinned {
  background-color: rgba(210, 153, 34, 0.2);
  color: #e3b341;
}

.badge-locked {
  background-color: rgba(248, 81, 73, 0.15);
  color: #f85149;
}

.resource-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0 0 8px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.resource-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-tertiary);
}

/* 主内容区 */
.main-area {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 顶部横幅 */
.welcome-banner {
  background: linear-gradient(135deg, var(--primary-light) 0%, var(--bg-secondary) 100%);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 24px 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.banner-content {
  flex: 1;
}

.banner-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.banner-text {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.banner-actions .btn {
  padding: 10px 20px;
  font-size: 14px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
  font-weight: 500;
}

.btn-primary {
  background: var(--primary);
  color: white;
}

.btn-primary:hover {
  background: var(--primary-hover);
  transform: translateY(-2px);
}

.btn-icon {
  font-size: 16px;
}

/* 搜索栏 */
.search-bar {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px 20px;
}

.search-input-wrapper {
  display: flex;
  gap: 12px;
}

.search-input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-size: 14px;
  transition: all 0.2s;
}

.search-input:focus {
  outline: none;
  border-color: var(--primary);
}

.search-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  background: var(--primary);
  color: white;
  cursor: pointer;
  transition: all 0.2s;
}

.search-btn:hover {
  background: var(--primary-hover);
}

.search-icon {
  font-size: 16px;
}

/* 过滤工具栏 */
.filter-toolbar {
  display: flex;
  gap: 20px;
  padding: 16px 20px;
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 12px;
}

.back-top {
  position: fixed;
  right: 28px;
  bottom: 28px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background-color: transparent;
  border: 1px solid var(--accent);
  color: #fff;
  cursor: pointer;
  font-size: 20px;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 20px rgba(63, 185, 80, 0.4);
  transition: background-color 0.15s, transform 0.15s;
  z-index: 90;
}
.back-top:hover { background-color: var(--accent-soft); transform: translateY(-2px); }

.board-modal {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  align-items: center;
  justify-content: center;
}
.board-modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(4px);
}
.board-modal-content {
  position: relative;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  background-color: var(--bg);
  border: 1px solid var(--border);
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}
.board-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border);
}
.board-modal-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-dim);
}
.board-modal-close {
  width: 32px;
  height: 32px;
  border: none;
  background-color: transparent;
  color: var(--text-dim);
  font-size: 20px;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.15s;
}
.board-modal-close:hover {
  background-color: var(--bg-soft);
  color: var(--text);
}
.board-modal-body {
  overflow-y: auto;
  padding: 12px;
}
.board-modal.show { display: flex; }

.board-option {
  display: block;
  padding: 16px 20px;
  background-color: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.15s;
}
.board-option:hover {
  background-color: var(--bg-soft);
  border-color: var(--accent);
  transform: translateY(-1px);
}
.board-option-name {
  font-size: 16px;
  font-weight: 500;
  color: var(--text);
  margin-bottom: 6px;
}
.board-option-desc {
  font-size: 14px;
  color: var(--text-dim);
  line-height: 1.5;
}

/* 发帖表单模态框 */
.post-form-modal {
  max-width: 720px;
}

.post-form-row {
  margin-bottom: 18px;
}

.post-form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
  margin-bottom: 8px;
}

.post-form-input {
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

.post-form-input:focus {
  border-color: var(--accent);
}

.post-form-textarea {
  resize: vertical;
  min-height: 200px;
  line-height: 1.6;
}

.post-form-hint {
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 6px;
  text-align: right;
}

.post-form-message {
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
  margin-bottom: 16px;
}

.post-form-message.success {
  background-color: var(--accent-soft);
  color: var(--accent-hover);
  border: 1px solid var(--accent);
}

.post-form-message.error {
  background-color: rgba(248, 81, 73, 0.1);
  color: #f85149;
  border: 1px solid #f85149;
}

.post-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.post-btn-cancel {
  display: inline-flex;
  align-items: center;
  padding: 10px 24px;
  background-color: transparent;
  color: var(--text-dim);
  font-size: 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  text-decoration: none;
}

.post-btn-cancel:hover {
  color: var(--text);
  border-color: #484f58;
}

.post-btn-submit {
  display: inline-flex;
  align-items: center;
  padding: 10px 32px;
  background-color: var(--accent);
  color: #06140b;
  font-size: 14px;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.15s;
  text-decoration: none;
}

.post-btn-submit:hover:not(:disabled) {
  background-color: var(--accent-hover);
}

.post-btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 游客提示 */
.login-tip-modal {
  max-width: 380px;
}

.login-tip-text {
  font-size: 15px;
  color: var(--text-dim);
  text-align: center;
  padding: 16px 0;
}

@media (max-width: 900px) {
  .layout { grid-template-columns: 1fr; }
  .res-stats { gap: 14px; }
  .res-update { display: none; }
}


.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

.filter-select {
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-select:focus {
  outline: none;
  border-color: var(--primary);
}

.filter-btn {
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
}

.filter-btn:hover {
  border-color: var(--primary);
}

.filter-arrow {
  font-size: 10px;
  color: var(--text-tertiary);
}

/* 资源网格 */
.resource-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.resource-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 20px;
  transition: all 0.2s;
  cursor: pointer;
}

.resource-card:hover {
  border-color: var(--primary);
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.resource-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.resource-type {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 6px;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  font-weight: 500;
}

.resource-featured {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 4px;
  background: var(--primary-light);
  color: var(--primary);
  font-weight: 500;
}

.resource-card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.resource-card-desc {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0 0 16px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.resource-card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}

.meta-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-author {
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 500;
}

.meta-time {
  font-size: 12px;
  color: var(--text-tertiary);
}

.meta-right {
  display: flex;
  gap: 12px;
}

.meta-stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-secondary);
}

.stat-icon {
  font-size: 14px;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  padding: 32px 0;
}

.pagination-btn {
  padding: 8px 16px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.pagination-btn:hover:not(:disabled) {
  border-color: var(--primary);
  color: var(--primary);
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-pages {
  display: flex;
  gap: 4px;
}

.pagination-page {
  min-width: 36px;
  height: 36px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.pagination-page:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.pagination-page.active {
  background: var(--primary);
  border-color: var(--primary);
  color: white;
}

/* 模态框 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  padding: 20px;
}

.modal {
  background: var(--bg-secondary);
  border-radius: 16px;
  max-width: 600px;
  width: 100%;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border);
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.modal-close {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: var(--bg-hover);
  color: var(--text-primary);
  font-size: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.modal-close:hover {
  background: var(--bg-tertiary);
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
}

.board-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.board-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-primary);
  cursor: pointer;
  transition: all 0.2s;
}

.board-item:hover {
  border-color: var(--primary);
  transform: translateY(-2px);
}

.board-item.active {
  border-color: var(--primary);
  background: var(--primary-light);
}

.board-icon {
  font-size: 24px;
}

.board-name {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.board-count {
  font-size: 12px;
  color: var(--text-tertiary);
  background: var(--bg-tertiary);
  padding: 2px 8px;
  border-radius: 10px;
}

/* 返回顶部按钮 */
.back-to-top {
  position: fixed;
  bottom: 40px;
  right: 40px;
  width: 48px;
  height: 48px;
  border: none;
  border-radius: 50%;
  background: var(--primary);
  color: white;
  font-size: 20px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.3s;
  z-index: 100;
}

.back-to-top:hover {
  transform: translateY(-4px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

/* 响应式设计 */
@media (max-width: 968px) {
  .content-layout {
    grid-template-columns: 1fr;
  }
  
  .sidebar {
    position: static;
    order: 2;
  }
  
  .main-area {
    order: 1;
  }
  
  .resource-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  }
}

@media (max-width: 640px) {
  .welcome-banner {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .filter-toolbar {
    flex-direction: column;
    gap: 12px;
  }
  
  .resource-grid {
    grid-template-columns: 1fr;
  }
  
  .board-grid {
    grid-template-columns: 1fr;
  }
  
  .back-to-top {
    bottom: 20px;
    right: 20px;
    width: 40px;
    height: 40px;
  }
}
</style>
