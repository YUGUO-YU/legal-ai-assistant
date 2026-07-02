<template>
  <div class="login-container">
    <div class="bg-animation">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
      <div class="circle circle-4"></div>
    </div>

    <div class="login-box">
      <div class="login-header">
        <div class="logo-icon">
          <el-icon :size="48"><Sunny /></el-icon>
        </div>
        <h1>法律AI助手</h1>
        <p>智能法律服务解决方案</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="rules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="captcha">
          <el-input
            v-model="loginForm.captcha"
            placeholder="请输入验证码"
            size="large"
            style="width: 60%"
            :prefix-icon="CircleCheck"
          />
          <div class="captcha-code" @click="refreshCaptcha">
            <span>{{ captchaText }}</span>
          </div>
        </el-form-item>

        <el-form-item>
          <div class="form-options">
            <el-checkbox v-model="rememberMe">记住我</el-checkbox>
            <el-link type="primary" class="forgot-link" @click="$router.push('/forgot-password')">忘记密码？</el-link>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <span>还没有账号？</span>
        <el-link type="primary" @click="$router.push('/register')">立即注册</el-link>
      </div>

      <div class="admin-quick-link">
        <el-link type="info" underline="never" @click="goAdmin">
          <el-icon><Setting /></el-icon>
          管理员登录
        </el-link>
      </div>

      <div class="demo-accounts">
        <el-divider>
          <span class="demo-divider-text">演示账号</span>
        </el-divider>
        <div class="demo-info">
          <div class="demo-item">
            <el-tag size="small" type="info">用户名</el-tag>
            <span class="demo-value">demo</span>
          </div>
          <div class="demo-item">
            <el-tag size="small" type="info">密码</el-tag>
            <span class="demo-value">demo123</span>
          </div>
        </div>
      </div>

      <div class="admin-entry">
        <el-divider>
          <span class="admin-divider-text">快速入口</span>
        </el-divider>
        <div class="entry-buttons">
          <el-button
            v-if="isLoggedIn"
            type="primary"
            size="large"
            class="entry-btn dashboard-btn"
            @click="goDashboard"
          >
            <el-icon><Odometer /></el-icon>
            <span>进入工作台</span>
          </el-button>
          <el-button
            size="large"
            class="entry-btn admin-btn"
            @click="goAdmin"
          >
            <el-icon><Setting /></el-icon>
            <span>后台管理</span>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, CircleCheck, Sunny, Setting, Odometer } from '@element-plus/icons-vue'
import api from '../api'

const router = useRouter()
const loginFormRef = ref(null)
const loading = ref(false)
const rememberMe = ref(false)
const captchaText = ref('')

const isLoggedIn = computed(() => !!localStorage.getItem('token'))

const loginForm = reactive({
  username: '',
  password: '',
  captcha: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

const generateCaptcha = () => {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  let result = ''
  for (let i = 0; i < 4; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return result
}

const refreshCaptcha = () => {
  captchaText.value = generateCaptcha()
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    await loginFormRef.value.validate()
  } catch (e) {
    return
  }

  if (loginForm.captcha.toUpperCase() !== captchaText.value.toUpperCase()) {
    ElMessage.error('验证码错误')
    refreshCaptcha()
    return
  }

  loading.value = true

  try {
    const res = await api.auth.login({
      username: loginForm.username,
      password: loginForm.password
    })

    if (!res?.data?.token) {
      throw new Error('登录响应数据异常')
    }

    localStorage.setItem('token', res.data.token)
    localStorage.setItem('userInfo', JSON.stringify(res.data.userInfo))

    ElMessage.success('登录成功，即将跳转到首页...')

    setTimeout(() => {
      router.push('/dashboard')
    }, 500)
  } catch (e) {
    console.error('登录失败:', e)
    ElMessage.error(e?.message || '登录失败，请检查用户名和密码')
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
})

const goDashboard = () => {
  router.push('/dashboard')
}

const goAdmin = () => {
  router.push('/admin/login')
}

onMounted(() => {
  refreshCaptcha()
  if (localStorage.getItem('darkMode') === 'true') {
    document.documentElement.classList.add('dark')
  }
})
</script>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  position: relative;
  overflow: hidden;
}

.bg-animation {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  pointer-events: none;

  .circle {
    position: absolute;
    border-radius: 50%;
    background: linear-gradient(135deg, rgba(79, 172, 254, 0.3), rgba(67, 97, 238, 0.3));
    animation: float 20s infinite ease-in-out;

    &.circle-1 {
      width: 400px;
      height: 400px;
      top: -100px;
      left: -100px;
      animation-delay: 0s;
    }

    &.circle-2 {
      width: 300px;
      height: 300px;
      top: 50%;
      right: -100px;
      animation-delay: -5s;
      background: linear-gradient(135deg, rgba(254, 202, 87, 0.2), rgba(238, 90, 36, 0.2));
    }

    &.circle-3 {
      width: 200px;
      height: 200px;
      bottom: -50px;
      left: 30%;
      animation-delay: -10s;
      background: linear-gradient(135deg, rgba(16, 185, 129, 0.2), rgba(59, 130, 246, 0.2));
    }

    &.circle-4 {
      width: 150px;
      height: 150px;
      top: 20%;
      left: 20%;
      animation-delay: -15s;
      background: linear-gradient(135deg, rgba(139, 92, 246, 0.2), rgba(236, 72, 153, 0.2));
    }
  }
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  25% {
    transform: translate(50px, 50px) scale(1.1);
  }
  50% {
    transform: translate(0, 100px) scale(1);
  }
  75% {
    transform: translate(-50px, 50px) scale(0.9);
  }
}

.login-box {
  width: 420px;
  padding: 48px 40px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  box-shadow:
    0 25px 50px -12px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(255, 255, 255, 0.1);
  position: relative;
  z-index: 10;
}

.login-header {
  text-align: center;
  margin-bottom: 36px;

  .logo-icon {
    width: 80px;
    height: 80px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 16px;
    color: #fff;
    box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
  }

  h1 {
    margin: 0 0 8px 0;
    font-size: 28px;
    font-weight: 600;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  p {
    margin: 0;
    color: #6b7280;
    font-size: 14px;
  }
}

.login-form {
  :deep(.el-form-item) {
    margin-bottom: 20px;
  }

  :deep(.el-input__wrapper) {
    padding: 4px 16px;
    border-radius: 12px;
    box-shadow: 0 0 0 1px #e5e7eb;

    &:hover, &.is-focus {
      box-shadow: 0 0 0 2px #667eea;
    }
  }

  .captcha-code {
    width: 38%;
    height: 40px;
    background: linear-gradient(135deg, #f3f4f6, #e5e7eb);
    display: flex;
    align-items: center;
    justify-content: center;
    margin-left: 12px;
    border-radius: 10px;
    cursor: pointer;
    font-family: 'JetBrains Mono', 'Courier New', monospace;
    font-size: 20px;
    font-weight: bold;
    letter-spacing: 4px;
    color: #4b5563;
    transition: all 0.3s;

    &:hover {
      background: linear-gradient(135deg, #e5e7eb, #d1d5db);
      transform: scale(1.02);
    }
  }

  .form-options {
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .forgot-link {
    font-size: 13px;
  }

  .login-btn {
    width: 100%;
    height: 48px;
    border-radius: 12px;
    font-size: 16px;
    font-weight: 500;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
    box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
    transition: all 0.3s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
    }

    &:active {
      transform: translateY(0);
    }
  }
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  color: #6b7280;
  font-size: 14px;
}

.admin-quick-link {
  text-align: center;
  margin-top: 12px;

  :deep(.el-link) {
    color: #9ca3af;
    font-size: 13px;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    transition: color 0.3s;

    &:hover {
      color: #667eea;
    }
  }
}

.demo-accounts {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f3f4f6;

  :deep(.el-divider) {
    margin: 16px 0;

    .el-divider__text {
      background: #fff;
      color: #9ca3af;
      font-size: 12px;
      padding: 0 12px;
    }
  }

  .demo-info {
    display: flex;
    justify-content: center;
    gap: 24px;
  }

  .demo-item {
    display: flex;
    align-items: center;
    gap: 8px;

    .demo-value {
      font-family: 'JetBrains Mono', monospace;
      color: #4b5563;
      font-weight: 500;
    }
  }
}

.admin-entry {
  margin-top: 24px;

  :deep(.el-divider) {
    margin: 16px 0;

    .el-divider__text {
      background: #fff;
      color: #9ca3af;
      font-size: 12px;
      padding: 0 12px;
    }
  }

  .entry-buttons {
    display: flex;
    gap: 12px;
    justify-content: center;
    flex-wrap: wrap;
  }

  .entry-btn {
    height: 44px;
    border-radius: 10px;
    font-size: 14px;
    font-weight: 500;
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 0 24px;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-2px);
    }
  }

  .dashboard-btn {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
    box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
    color: #fff;

    &:hover {
      box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
    }
  }

  .admin-btn {
    background: #fff;
    border: 2px solid #e5e7eb;
    color: #4b5563;

    &:hover {
      border-color: #667eea;
      color: #667eea;
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
    }
  }
}
</style>
