<template>
  <div>
    <AppHeader />

    <div class="main-wrapper">
      <div class="main">
        <!-- 左侧：注册表单 -->
        <div class="form-area">
          <div class="form-icon">E</div>
          <h1 class="form-title">创建账户</h1>
          <p class="form-subtitle">加入 EchoBBS 社区，开始你的讨论之旅</p>

          <div v-if="errorMessage" class="alert-error">{{ errorMessage }}</div>

          <form class="register-form" @submit.prevent="handleRegister">
            <div class="form-group">
              <label class="form-label" for="username">用户名</label>
              <input
                id="username"
                v-model="formData.username"
                type="text"
                class="form-input"
                :class="{ 'input-error': fieldError === 'username' }"
                placeholder="3-20个字符，字母数字下划线"
                required
                autocomplete="username"
              />
              <div v-if="fieldError === 'username'" class="field-error">{{ fieldErrorMsg }}</div>
            </div>

            <div class="form-group">
              <label class="form-label" for="email">邮箱</label>
              <input
                id="email"
                v-model="formData.email"
                type="email"
                class="form-input"
                :class="{ 'input-error': fieldError === 'email' }"
                placeholder="your@email.com"
                required
                autocomplete="email"
              />
              <div v-if="fieldError === 'email'" class="field-error">{{ fieldErrorMsg }}</div>
            </div>

            <div class="form-group">
              <label class="form-label" for="password">密码</label>
              <div class="password-wrapper">
                <input
                  id="password"
                  v-model="formData.password"
                  :type="showPassword ? 'text' : 'password'"
                  class="form-input"
                  :class="{ 'input-error': fieldError === 'password' }"
                  placeholder="8-64个字符，需含字母和数字"
                  required
                  autocomplete="new-password"
                />
                <button type="button" class="password-toggle" @click="showPassword = !showPassword">
                  {{ showPassword ? '隐藏' : '显示' }}
                </button>
              </div>
              <div class="form-hint">8-64 个字符，需包含字母和数字</div>
              <div v-if="fieldError === 'password'" class="field-error">{{ fieldErrorMsg }}</div>
            </div>

            <label class="terms-wrap">
              <input v-model="formData.agree" type="checkbox" />
              <span>我已阅读并同意 <a href="/terms" target="_blank">服务条款</a> 和 <a href="/privacy" target="_blank">隐私政策</a></span>
            </label>

            <button type="submit" class="btn-submit" :disabled="loading">
              {{ loading ? '注册中...' : '注册' }}
            </button>

            <div class="form-footer">
              已有账户？<router-link to="/login">立即登录</router-link>
            </div>
          </form>
        </div>

        <!-- 右侧：推广区 -->
        <div class="promo-area">
          <div class="promo-header">
            <h2>成为 <span class="highlight">EchoBBS</span> 的一员</h2>
            <p>注册即可解锁完整的社区体验，与同好一起讨论你感兴趣的一切。</p>
          </div>

          <div class="feature-list">
            <div class="feature-item">
              <div class="feature-body">
                <h3>自由发帖</h3>
                <p>创建话题、分享观点，在你感兴趣的板块里畅所欲言。</p>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-body">
                <h3>实时通知</h3>
                <p>第一时间收到回复和互动提醒，不错过任何重要消息。</p>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-body">
                <h3>收藏与关注</h3>
                <p>收藏喜欢的帖子，关注感兴趣的板块和用户。</p>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-body">
                <h3>个性化首页</h3>
                <p>根据你的兴趣推荐内容，打造专属的浏览体验。</p>
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
import { useRouter } from 'vue-router';
import AppHeader from '@/components/AppHeader.vue';
import { useUserStore } from '@/stores/user';
import { api } from '@/api';

const router = useRouter();
const userStore = useUserStore();

const formData = ref({
  username: '',
  email: '',
  password: '',
  agree: false
});

const showPassword = ref(false);
const loading = ref(false);
const errorMessage = ref('');
const fieldError = ref<string | null>(null);
const fieldErrorMsg = ref('');

onMounted(() => {
  // 如果已登录，跳转到首页
  if (userStore.isLoggedIn) {
    router.push('/');
  }
});

const showFieldError = (field: string | null, message: string = '') => {
  fieldError.value = field;
  fieldErrorMsg.value = message;
};

const validate = (): boolean => {
  const { username, email, password, agree } = formData.value;

  // 清除之前的错误
  showFieldError(null);
  errorMessage.value = '';

  if (!username.trim()) {
    showFieldError('username', '用户名不能为空');
    return false;
  }
  if (!/^[a-zA-Z0-9_]{3,20}$/.test(username)) {
    showFieldError('username', '用户名需为 3-20 位字母、数字或下划线');
    return false;
  }
  if (!email.trim()) {
    showFieldError('email', '邮箱不能为空');
    return false;
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    showFieldError('email', '请输入有效的邮箱地址');
    return false;
  }
  if (!password) {
    showFieldError('password', '密码不能为空');
    return false;
  }
  if (password.length < 8 || password.length > 64) {
    showFieldError('password', '密码长度需为 8-64 个字符');
    return false;
  }
  if (!/(?=.*[A-Za-z])(?=.*\d)/.test(password)) {
    showFieldError('password', '密码必须包含字母和数字');
    return false;
  }
  if (!agree) {
    errorMessage.value = '请先阅读并同意服务条款和隐私政策';
    return false;
  }

  return true;
};

const handleRegister = async () => {
  errorMessage.value = '';

  if (!validate()) return;

  loading.value = true;

  try {
    await api.post('/api/register', {
      username: formData.value.username,
      email: formData.value.email,
      password: formData.value.password
    });

    // 注册成功，跳转到登录页面
    router.push('/login?registered=true');
  } catch (error: any) {
    const errorData = error.response?.data;
    
    // 根据后端返回的错误信息进行字段级错误提示
    if (errorData?.field) {
      showFieldError(errorData.field, errorData.message);
    } else {
      errorMessage.value = errorData?.message || '注册失败，请稍后重试';
    }
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
* {
  box-sizing: border-box;
}

/* 主容器 */
.main-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 60px);
  padding: 40px 24px;
}

.main {
  display: grid;
  grid-template-columns: 540px 500px;
  width: 100%;
  max-width: 1040px;
  background-color: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
}

/* 左侧表单区 */
.form-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background-color: var(--bg);
  max-height: calc(100vh - 60px);
  overflow-y: auto;
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
  margin-bottom: 18px;
  flex-shrink: 0;
}

.form-title {
  font-size: 26px;
  font-weight: normal;
  margin-bottom: 6px;
}

.form-subtitle {
  font-size: 14px;
  color: var(--text-dim);
  margin-bottom: 26px;
}

.alert-error {
  color: var(--danger);
  background-color: rgba(248, 81, 73, 0.12);
  border: 1px solid rgba(248, 81, 73, 0.3);
  font-size: 13px;
  padding: 10px 14px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.register-form {
  width: 100%;
  max-width: 380px;
}

.form-group {
  margin-bottom: 16px;
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

.form-input.input-error {
  border-color: var(--danger);
}

.field-error {
  font-size: 12px;
  color: var(--danger);
  margin-top: 6px;
}

.form-hint {
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 6px;
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

.terms-wrap {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  color: var(--text-dim);
  line-height: 1.5;
  margin: 4px 0 20px;
  cursor: pointer;
}

.terms-wrap input {
  cursor: pointer;
  margin-top: 2px;
  flex-shrink: 0;
}

.terms-wrap span {
  flex: 1;
}

.terms-wrap a {
  color: var(--accent);
  font-weight: normal;
}

.terms-wrap a:hover {
  color: var(--accent-hover);
}

.btn-submit {
  width: 100%;
  padding: 12px;
  font-size: 15px;
  font-weight: normal;
  border: none;
  border-radius: 8px;
  background-color: var(--accent);
  color: #06140b;
  cursor: pointer;
  transition: background-color 0.15s, transform 0.15s;
}

.btn-submit:hover:not(:disabled) {
  background-color: var(--accent-hover);
  transform: translateY(-1px);
}

.btn-submit:active {
  transform: translateY(0);
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.form-footer {
  text-align: center;
  margin-top: 22px;
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

/* 右侧推广区 */
.promo-area {
  background: linear-gradient(135deg, #0d1117 0%, #000000 100%);
  padding: 56px 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.promo-header h2 {
  font-size: 30px;
  font-weight: normal;
  line-height: 1.3;
  margin-bottom: 12px;
}

.promo-header .highlight {
  color: var(--accent);
}

.promo-header p {
  font-size: 14px;
  color: var(--text-dim);
  line-height: 1.6;
  margin-bottom: 32px;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.feature-item {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.feature-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--accent-soft);
  font-size: 20px;
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
  .form-area {
    max-height: none;
  }
}
</style>
