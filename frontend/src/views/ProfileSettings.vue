<template>
  <div>
    <AppHeader />

    <!-- 面包屑 -->
    <div class="page-head">
      <div class="breadcrumb">
        <router-link to="/">首页</router-link>
        <span class="sep">/</span>
        <span>设置</span>
        <span class="sep">/</span>
        <span>账户详情</span>
      </div>
      <h1 class="page-title">账户设置</h1>
    </div>

    <!-- 主体双栏 -->
    <div class="main-layout">
      <!-- 左侧边栏 -->
      <aside class="sidebar">
        <div class="sidebar-section">
          <div class="sidebar-section-title">账户</div>
          <div class="sidebar-menu">
            <router-link
              v-if="user?.username"
              :to="`/user/${user.username}`"
              class="sidebar-item"
            >
              <span>个人资料</span>
            </router-link>
            <router-link to="/notifications" class="sidebar-item">
              <span>提醒</span>
            </router-link>
          </div>
        </div>

        <div class="sidebar-section">
          <div class="sidebar-section-title">设置</div>
          <div class="sidebar-menu">
            <a class="sidebar-item active">
              <span>账户详情</span>
            </a>
            <a class="sidebar-item">
              <span>密码和安全性</span>
            </a>
            <a class="sidebar-item">
              <span>隐私设置</span>
            </a>
            <a class="sidebar-item">
              <span>提醒偏好</span>
            </a>
            <a class="sidebar-item">
              <span>账户升级</span>
            </a>
            <a class="sidebar-item">
              <span>签名设置</span>
            </a>
          </div>
        </div>

        <div class="sidebar-logout">
          <a class="sidebar-item" @click="handleLogout">
            <span>退出登录</span>
          </a>
        </div>
      </aside>

      <!-- 右侧表单 -->
      <main class="form-container">
        <form @submit.prevent="handleSave">
          <!-- 用户名 -->
          <div class="form-row">
            <label class="form-label" for="username">用户名</label>
            <div class="form-content">
              <input
                id="username"
                type="text"
                class="form-input"
                :value="user?.username"
                disabled
              />
              <div class="form-hint">您的唯一用户名</div>
            </div>
          </div>

          <!-- 邮箱 -->
          <div class="form-row">
            <label class="form-label" for="email">电子邮件</label>
            <div class="form-content">
              <input
                id="email"
                v-model="form.email"
                type="email"
                class="form-input"
              />
              <div class="form-hint">用于接收通知和重置密码</div>
            </div>
          </div>

          <!-- 昵称 -->
          <div class="form-row">
            <label class="form-label" for="nickname">显示昵称</label>
            <div class="form-content">
              <input
                id="nickname"
                v-model="form.nickname"
                type="text"
                class="form-input"
                placeholder="输入昵称"
              />
              <div class="form-hint">公开显示的名称，留空则显示用户名</div>
            </div>
          </div>

          <!-- 头像 -->
          <div class="form-row">
            <label class="form-label">头像</label>
            <div class="form-content">
              <div class="avatar-upload">
                <img
                  v-if="avatarPreview"
                  :src="avatarPreview"
                  class="avatar-preview"
                  alt="头像"
                />
                <div v-else class="avatar-placeholder">
                  {{ (user?.nickname || user?.username || 'U').charAt(0).toUpperCase() }}
                </div>

                <div class="avatar-actions">
                  <label class="upload-btn">
                    选择图片
                    <input
                      type="file"
                      accept="image/*"
                      style="display: none"
                      @change="handleAvatarChange"
                    />
                  </label>
                  <div class="form-hint">
                    推荐尺寸 256x256，支持 JPG、PNG 格式，最大 5MB
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 个人简介 -->
          <div class="form-row">
            <label class="form-label" for="bio">个人简介</label>
            <div class="form-content">
              <textarea
                id="bio"
                v-model="form.bio"
                class="form-input"
                rows="4"
                maxlength="500"
                placeholder="介绍一下自己..."
              ></textarea>
              <div class="form-hint">最多 500 个字符</div>
            </div>
          </div>

          <!-- 消息提示 -->
          <div v-if="message" class="form-message" :class="messageType">
            {{ message }}
          </div>

          <!-- 保存按钮 -->
          <div class="form-actions">
            <button type="submit" class="btn-save" :disabled="saving">
              {{ saving ? '保存中...' : '保存更改' }}
            </button>
          </div>
        </form>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import AppHeader from '@/components/AppHeader.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const { currentUser: user } = storeToRefs(userStore)

const form = reactive({
  email: '',
  nickname: '',
  bio: ''
})

const avatarFile = ref<File | null>(null)
const avatarPreview = ref<string>('')
const saving = ref(false)
const message = ref('')
const messageType = ref<'success' | 'error'>('success')

// 用当前用户数据初始化表单
const initForm = () => {
  if (user.value) {
    form.email = user.value.email || ''
    form.nickname = user.value.nickname || ''
    form.bio = user.value.bio || ''
    avatarPreview.value = user.value.avatarPath || ''
  }
}

// 头像选择 + 本地预览
const handleAvatarChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  if (file.size > 5 * 1024 * 1024) {
    showMessage('图片大小不能超过 5MB', 'error')
    return
  }

  avatarFile.value = file
  const reader = new FileReader()
  reader.onload = (event) => {
    const result = event.target?.result
    if (typeof result === 'string') {
      avatarPreview.value = result
    }
  }
  reader.readAsDataURL(file)
}

const showMessage = (text: string, type: 'success' | 'error') => {
  message.value = text
  messageType.value = type
  setTimeout(() => {
    message.value = ''
  }, 3000)
}

// 保存：先传头像，再更新资料
const handleSave = async () => {
  saving.value = true
  try {
    if (avatarFile.value) {
      await userStore.uploadAvatar(avatarFile.value)
      avatarFile.value = null
    }

    await userStore.updateProfile({
      email: form.email,
      nickname: form.nickname,
      bio: form.bio
    })

    // 同步最新数据到表单与预览
    initForm()
    showMessage('保存成功', 'success')
  } catch (err: any) {
    showMessage(err.response?.data?.message || '保存失败，请重试', 'error')
  } finally {
    saving.value = false
  }
}

const handleLogout = async () => {
  try {
    await userStore.logout()
    router.push('/login')
  } catch {
    showMessage('登出失败，请重试', 'error')
  }
}

onMounted(async () => {
  // 确保有最新的用户数据
  if (!user.value) {
    await userStore.fetchCurrentUser()
  }
  if (!user.value) {
    router.push('/login')
    return
  }
  initForm()
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

/* 右侧表单区 */
.form-container {
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 32px;
}

.form-row {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
  align-items: flex-start;
}

.form-label {
  width: 180px;
  flex-shrink: 0;
  padding-top: 10px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
}

.form-content {
  flex: 1;
  min-width: 0;
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  background-color: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--text);
  font-size: 14px;
  outline: none;
  transition: border-color 0.15s;
  font-family: inherit;
  resize: vertical;
}

.form-input:focus {
  border-color: var(--accent);
}

.form-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-hint {
  font-size: 13px;
  color: var(--text-dim);
  margin-top: 6px;
  line-height: 1.5;
}

/* 头像上传区 */
.avatar-upload {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar-preview {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid var(--border);
}

.avatar-placeholder {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3fb950, #2ea043);
  color: #06140b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  font-weight: 600;
  border: 3px solid var(--border);
}

.avatar-actions {
  flex: 1;
}

.upload-btn {
  display: inline-block;
  padding: 8px 16px;
  background-color: var(--accent);
  color: #06140b;
  font-size: 14px;
  font-weight: 500;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.upload-btn:hover {
  background-color: var(--accent-hover);
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

/* 保存按钮 */
.form-actions {
  display: flex;
  justify-content: center;
  padding-top: 24px;
  border-top: 1px solid var(--border);
  margin-top: 32px;
}

.btn-save {
  padding: 12px 48px;
  background-color: var(--accent);
  color: #06140b;
  font-size: 15px;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.btn-save:hover:not(:disabled) {
  background-color: var(--accent-hover);
}

.btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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

@media (max-width: 768px) {
  .form-row {
    flex-direction: column;
    gap: 8px;
  }
  .form-label {
    width: 100%;
    padding-top: 0;
  }
  .avatar-upload {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
