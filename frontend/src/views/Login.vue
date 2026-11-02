<template>
  <div>
    <AppHeader />

    <div class="main-wrapper">
      <div class="main">
        <!-- 左侧：登录表单 -->
        <div class="form-area">
          <div class="form-icon">E</div>
          <h1 class="form-title">欢迎回来</h1>
          <p class="form-subtitle">登录你的 EchoBBS 账户</p>

          <div v-if="successMessage" class="success-msg">{{ successMessage }}</div>
          <div v-if="errorMessage" class="error-msg">{{ errorMessage }}</div>

          <form class="login-form" @submit.prevent="handleLogin">
            <div class="form-group">
              <label class="form-label" for="username">用户名</label>
              <input
                id="username"
                v-model="formData.username"
                type="text"
                class="form-input"
                placeholder="输入用户名"
                required
                autocomplete="username"
              />
            </div>

            <div class="form-group">
              <label class="form-label" for="password">密码</label>
              <div class="password-wrapper">
                <input
                  id="password"
                  v-model="formData.password"
                  :type="showPassword ? 'text' : 'password'"
                  class="form-input"
                  placeholder="输入密码"
                  required
                  autocomplete="current-password"
                />
                <button type="button" class="password-toggle" @click="showPassword = !showPassword">
                  {{ showPassword ? '隐藏' : '显示' }}
                </button>
              </div>
            </div>

            <div class="form-row">
              <label class="checkbox-wrap">
                <input v-model="formData.remember" type="checkbox" />
                <span>保持登录</span>
              </label>
              <a href="#" class="forgot-link">忘记密码？</a>
            </div>

            <button type="submit" class="btn-submit" :disabled="loading">
              {{ loading ? '登录中...' : '登录' }}
            </button>

            <div class="form-footer">
              还没有账户？<router-link to="/register">立即注册</router-link>
            </div>
          </form>
        </div>

        <!-- 右侧：推广区 -->
        <div class="promo-area">
          <div class="promo-header">
            <h2>加入 <span class="highlight">EchoBBS</span> 社区</h2>
          </div>

          <div class="promo-showcase">
            <div class="showcase-card">
              <div class="showcase-title">自由讨论</div>
            </div>
            <div class="showcase-card">
              <div class="showcase-title">热门话题</div>
            </div>
            <div class="showcase-card">
              <div class="showcase-title">知识分享</div>
            </div>
            <div class="showcase-card">
              <div class="showcase-title">兴趣圈子</div>
            </div>
          </div>

          <div class="promo-cta">
            <router-link to="/boards">浏览所有板块 →</router-link>
          </div>

          <div class="feature-list">
            <div class="feature-item">
              <div class="feature-icon">✓</div>
              <div class="feature-body">
                <h3>实时通知</h3>
                <p>第一时间收到回复和互动提醒，不错过任何重要消息。</p>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-icon">✓</div>
              <div class="feature-body">
                <h3>个性化首页</h3>
                <p>根据你的兴趣推荐内容，打造专属的浏览体验。</p>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-icon">✓</div>
              <div class="feature-body">
                <h3>收藏与关注</h3>
                <p>收藏喜欢的帖子，关注感兴趣的板块和用户。</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import AppHeader from '@/components/AppHeader.vue';
import { useUserStore } from '@/stores/user';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const formData = ref({
  username: '',
  password: '',
  remember: false
});

const showPassword = ref(false);
const loading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');

onMounted(() => {
  // 检查是否是注册成功后跳转过来
  if (route.query.registered === 'true') {
    successMessage.value = '注册成功！请登录你的账户';
  }
  
  // 如果已登录，跳转到首页
  if (userStore.isLoggedIn) {
    const redirect = route.query.redirect as string || '/'
    router.push(redirect)
  }
});

const handleLogin = async () => {
  errorMessage.value = '';
  successMessage.value = '';
  loading.value = true;

  try {
    await userStore.login(formData.value.username, formData.value.password, formData.value.remember);
    const redirect = route.query.redirect as string || '/'
    router.push(redirect);
  } catch (error: any) {
    errorMessage.value = error.response?.data?.message || '登录失败，请检查用户名和密码';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.main-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 60px);
  padding: 40px 24px;
}

.main {
  display: grid;
  grid-template-columns: 500px 540px;
  width: 100%;
  max-width: 1040px;
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
}

.form-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 56px 48px;
  background-color: var(--bg);
}

.form-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  font-weight: normal;
  color: #06140b;
  background: linear-gradient(135deg, #3fb950, #2ea043);
  box-shadow: 0 6px 20px rgba(63, 185, 80, 0.3);
  margin-bottom: 20px;
}

.form-title {
  font-size: 28px;
  font-weight: normal;
  margin-bottom: 8px;
}

.form-subtitle {
  font-size: 14px;
  color: var(--text-dim);
  margin-bottom: 32px;
}

.login-form {
  width: 100%;
  max-width: 380px;
}

.form-group {
  margin-bottom: 18px;
}

.form-label {
  display: block;
  font-size: 13px;
  font-weight: normal;
  margin-bottom: 7px;
  color: var(--text);
}

.form-input {
  width: 100%;
  padding: 11px 14px;
  font-size: 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background-color: var(--bg-soft);
  color: var(--text);
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.form-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
}

.form-input::placeholder {
  color: var(--text-dim);
}

.password-wrapper {
  position: relative;
}

.password-toggle {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: var(--text-dim);
  cursor: pointer;
  font-size: 13px;
  padding: 4px 6px;
  transition: color 0.15s;
}

.password-toggle:hover {
  color: var(--text);
}

.form-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.checkbox-wrap {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  color: var(--text-dim);
}

.checkbox-wrap input {
  cursor: pointer;
}

.forgot-link {
  font-size: 13px;
  color: var(--accent);
  transition: color 0.15s;
}

.forgot-link:hover {
  color: var(--accent-hover);
}

.btn-submit {
  width: 100%;
  padding: 12px;
  font-size: 15px;
  font-weight: normal;
  border: 1px solid var(--accent);
  border-radius: 8px;
  background-color: transparent;
  color: #fff;
  cursor: pointer;
  transition: background-color 0.15s, transform 0.15s;
}

.btn-submit:hover:not(:disabled) {
  background-color: var(--accent-soft);
  transform: translateY(-1px);
}

.btn-submit:active {
  transform: translateY(0);
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--text-dim);
}

.form-footer a {
  color: var(--accent);
  font-weight: normal;
  transition: color 0.15s;
}

.form-footer a:hover {
  color: var(--accent-hover);
}

.error-msg {
  width: 100%;
  max-width: 380px;
  padding: 12px 16px;
  background-color: rgba(248, 81, 73, 0.1);
  border: 1px solid rgba(248, 81, 73, 0.3);
  border-radius: 8px;
  color: #f85149;
  font-size: 14px;
  margin-bottom: 20px;
}

.success-msg {
  width: 100%;
  max-width: 380px;
  padding: 12px 16px;
  background-color: rgba(63, 185, 80, 0.1);
  border: 1px solid rgba(63, 185, 80, 0.3);
  border-radius: 8px;
  color: var(--accent);
  font-size: 14px;
  margin-bottom: 20px;
}

.promo-area {
  background: linear-gradient(135deg, #0d1117 0%, #000000 100%);
  padding: 56px 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.promo-header h2 {
  font-size: 32px;
  font-weight: normal;
  line-height: 1.3;
  margin-bottom: 16px;
}

.promo-header .highlight {
  color: var(--accent);
}

.promo-showcase {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin: 28px 0;
}

.showcase-card {
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: transform 0.15s, border-color 0.15s;
}

.showcase-card:hover {
  transform: translateX(3px);
  border-color: #484f58;
}

.showcase-icon {
  font-size: 28px;
  flex-shrink: 0;
}

.showcase-title {
  font-size: 16px;
  font-weight: normal;
  color: var(--text);
}

.promo-cta {
  text-align: center;
  margin-bottom: 28px;
}

.promo-cta a {
  font-size: 14px;
  color: var(--accent);
  font-weight: normal;
  transition: color 0.15s;
}

.promo-cta a:hover {
  color: var(--accent-hover);
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.feature-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--accent-soft);
  color: var(--accent);
  font-size: 16px;
}

.feature-body h3 {
  font-size: 15px;
  font-weight: normal;
  margin-bottom: 4px;
}

.feature-body p {
  font-size: 13px;
  color: var(--text-dim);
  line-height: 1.5;
}

@media (max-width: 960px) {
  .main {
    grid-template-columns: 1fr;
    max-width: 480px;
  }
  .promo-area {
    display: none;
  }
}
</style>
