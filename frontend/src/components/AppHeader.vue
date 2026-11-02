<template>
  <header class="header">
    <div class="header-content">
      <router-link to="/" class="logo">Echo<span>BBS</span></router-link>
      <nav class="nav">
        <router-link to="/">首页</router-link>
        <router-link to="/boards">资源</router-link>
        <router-link to="/search">搜索</router-link>
      </nav>
      <div v-if="!userStore.isLoggedIn" class="auth-area">
        <router-link to="/login" class="btn-login">登录</router-link>
        <router-link to="/register" class="btn-register">注册</router-link>
      </div>
      <div v-else class="user-menu">
        <router-link to="/notifications" class="notification-icon">
          <span class="icon">✉</span>
          <span v-if="unreadCount > 0" class="badge">{{ unreadCount }}</span>
        </router-link>
        <div class="dropdown">
          <button class="user-trigger">
            <img
              v-if="userStore.currentUser?.avatarPath"
              :src="userStore.currentUser.avatarPath"
              :alt="userStore.currentUser.nickname || userStore.currentUser.username"
              class="user-avatar"
            />
            <div v-else class="user-avatar-placeholder">
              {{ (userStore.currentUser?.nickname || userStore.currentUser?.username || 'U').charAt(0).toUpperCase() }}
            </div>
          </button>
          <div class="dropdown-menu">
            <!-- 顶部用户信息区 -->
            <div class="dropdown-user-info">
              <img
                v-if="userStore.currentUser?.avatarPath"
                :src="userStore.currentUser.avatarPath"
                :alt="userStore.currentUser.nickname || userStore.currentUser.username"
                class="dropdown-user-avatar"
              />
              <div v-else class="dropdown-user-avatar-placeholder">
                {{ (userStore.currentUser?.nickname || userStore.currentUser?.username || 'U').charAt(0).toUpperCase() }}
              </div>
              <div class="dropdown-user-name">
                <span class="dropdown-user-name-text">
                  {{ userStore.currentUser?.nickname || userStore.currentUser?.username }}
                </span>
                <span class="dropdown-user-username">@{{ userStore.currentUser?.username }}</span>
              </div>
            </div>
            <!-- 菜单项列表 -->
            <div class="dropdown-menu-list">
              <router-link :to="`/user/${userStore.currentUser?.username}`" class="dropdown-item">
                <span class="dropdown-label">个人资料</span>
              </router-link>
              <router-link to="/settings/profile" class="dropdown-item">
                <span class="dropdown-label">设置</span>
              </router-link>
              <router-link :to="`/user/${userStore.currentUser?.username}`" class="dropdown-item">
                <span class="dropdown-label">我的资源</span>
              </router-link>
              <template v-if="userStore.isStaff">
                <div class="dropdown-divider"></div>
                <div class="dropdown-group-label">管理后台</div>
                <router-link to="/admin/boards" class="dropdown-item">
                  <span class="dropdown-label">板块管理</span>
                </router-link>
                <router-link to="/admin/users" class="dropdown-item">
                  <span class="dropdown-label">用户管理</span>
                </router-link>
              </template>
              <div class="dropdown-divider"></div>
              <a @click="handleLogout" class="dropdown-item logout-btn">
                <span class="dropdown-label">登出</span>
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
    import { ref, onMounted } from 'vue'
    import { useUserStore } from '../stores/user'
    import { useRouter } from 'vue-router'
    import { api } from '../api'

    const userStore = useUserStore()
    const router = useRouter()
    const unreadCount = ref(0)

    async function fetchUnreadCount() {
      if (userStore.isLoggedIn) {
        try {
          const response = await api.get('/api/notifications/unread-count')
          unreadCount.value = response.data.data
        } catch (error) {
          console.error('获取未读通知数失败:', error)
        }
      }
    }

    async function handleLogout() {
      try {
        await userStore.logout()
        router.push('/login')
      } catch (error) {
        alert('登出失败，请重试')
      }
    }

    onMounted(() => {
      fetchUnreadCount()
    })
</script>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background-color: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border);
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 14px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  font-size: 22px;
  font-weight: normal;
  color: #fff;
  letter-spacing: -0.5px;
  flex-shrink: 0;
}

.logo span { color: #fff; }

.nav {
  display: flex;
  gap: 28px;
}

.nav a {
  color: var(--text-dim);
  font-size: 14px;
  font-weight: normal;
  transition: color 0.15s;
}

.nav a:hover { color: var(--text); }

.auth-area {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.btn-login,
.btn-register {
  font-size: 14px;
  font-weight: normal;
  padding: 7px 16px;
  border-radius: 8px;
  border: 1px solid var(--accent);
  background-color: transparent;
  color: #fff;
  transition: all 0.15s;
}

.btn-login { color: var(--text-dim); }
.btn-login:hover,
.btn-register:hover {
  background-color: var(--accent-soft);
  color: #fff;
}

.user-menu {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
}

.notification-icon {
  position: relative;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: transparent;
  border: 1px solid var(--border);
  color: var(--text-dim);
  transition: all 0.15s;
  text-decoration: none;
}

.notification-icon:hover {
  background-color: var(--bg-soft);
  color: var(--text);
  border-color: #484f58;
}

.notification-icon .icon {
  font-size: 18px;
}

.badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background-color: #f85149;
  color: #fff;
  font-size: 11px;
  font-weight: normal;
  min-width: 18px;
  height: 18px;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
  box-shadow: 0 0 0 2px var(--bg);
}

.dropdown {
  position: relative;
}

.user-trigger {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  display: inline-block;
}

.user-avatar,
.user-avatar-placeholder {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 2px solid var(--border);
  object-fit: cover;
  cursor: pointer;
  transition: border-color 0.15s, transform 0.15s;
}

.user-avatar:hover,
.user-avatar-placeholder:hover {
  border-color: var(--accent);
  transform: scale(1.05);
}

.user-avatar-placeholder {
  background: linear-gradient(135deg, #3fb950, #2ea043);
  color: #06140b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: normal;
}

/* 下拉菜单：紧贴头像(top:100%)，用 ::before 透明桥接消除间隙 */
.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  width: 240px;
  background-color: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.6);
  opacity: 0;
  visibility: hidden;
  transform: translateY(-8px);
  transition: opacity 0.2s, transform 0.2s, visibility 0.2s;
  z-index: 1000;
  overflow: hidden;
  margin-top: 8px;
}

/* 透明桥接：覆盖头像与菜单之间的间隙，保证鼠标移动时不丢失 hover */
.dropdown-menu::before {
  content: '';
  position: absolute;
  top: -8px;
  left: 0;
  right: 0;
  height: 8px;
}

.dropdown:hover .dropdown-menu {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

.dropdown-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background-color: var(--bg-soft);
  border-bottom: 1px solid var(--border);
}

.dropdown-user-avatar,
.dropdown-user-avatar-placeholder {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--border);
  flex-shrink: 0;
}

.dropdown-user-avatar-placeholder {
  background: linear-gradient(135deg, #3fb950, #2ea043);
  color: #06140b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
}

.dropdown-user-name {
  flex: 1;
  min-width: 0;
}

.dropdown-user-name-text {
  font-size: 15px;
  font-weight: 500;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
}

.dropdown-user-username {
  font-size: 13px;
  color: var(--text-dim);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
  margin-top: 2px;
}

.dropdown-menu-list {
  padding: 8px 0;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  color: var(--text);
  font-size: 14px;
  transition: background-color 0.15s;
  cursor: pointer;
  text-decoration: none;
}

.dropdown-item:hover {
  background-color: var(--bg-soft);
}

.dropdown-label {
  flex: 1;
  font-weight: 400;
}

.dropdown-divider {
  height: 1px;
  background-color: var(--border);
  margin: 8px 0;
}

.dropdown-group-label {
  padding: 6px 16px 4px;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-dim);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.logout-btn {
  color: #f85149;
}

.logout-btn:hover {
  background-color: rgba(248, 81, 73, 0.1);
}
</style>
