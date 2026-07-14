<template>
  <div class="login-page">
    <ParticleBackground 
      :particle-count="60"
      particle-color="#ffffff"
      :interactive="true"
    />

    <div class="top-right-admin">
      <el-button text @click="$router.push('/admin/login')">
        <el-icon><Setting /></el-icon>
        后台管理
      </el-button>
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

      <div class="announcements-section" v-if="announcements.length > 0">
        <el-divider>
          <span class="demo-divider-text">系统公告</span>
        </el-divider>
        <div class="announcements-list" v-loading="announcementsLoading">
          <div v-for="a in announcements" :key="a.id" class="announcement-item" :class="'priority-' + a.priority">
            <div class="announcement-header">
              <el-tag v-if="a.priority === 2" type="danger" size="small" effect="dark">紧急</el-tag>
              <el-tag v-else-if="a.priority === 1" type="warning" size="small">重要</el-tag>
              <el-tag v-else type="info" size="small">{{ typeLabel(a.type) }}</el-tag>
              <span class="announcement-title">{{ a.title }}</span>
              <span class="announcement-time">{{ formatTime(a.created_at) }}</span>
            </div>
            <div class="announcement-content" v-if="expandedAnnouncements.includes(a.id)">
              {{ a.content }}
            </div>
            <div class="announcement-content truncated" v-else-if="a.content && a.content.length > 100">
              {{ a.content.substring(0, 100) }}...
            </div>
            <div class="announcement-actions">
              <el-button link size="small" @click="toggleExpand(a.id)">
                {{ expandedAnnouncements.includes(a.id) ? '收起' : '查看全部' }}
              </el-button>
            </div>
          </div>
        </div>
        <div class="announcements-pagination" v-if="total > pageSize">
          <el-pagination
            layout="prev, pager, next"
            :total="total"
            :page-size="pageSize"
            :current-page="currentPage"
            @current-change="loadAnnouncements"
          />
        </div>
      </div>

      <div class="dashboard-entry" v-if="isLoggedIn">
        <el-button
          type="primary"
          size="large"
          class="entry-btn dashboard-btn"
          @click="goDashboard"
        >
          <el-icon><Odometer /></el-icon>
          <span>进入工作台</span>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, CircleCheck, Sunny, Odometer, Setting } from '@element-plus/icons-vue'
import ParticleBackground from '@/components/common/ParticleBackground.vue'
import api from '../api'

const router = useRouter()
const loginFormRef = ref(null)
const loading = ref(false)
const rememberMe = ref(false)
const captchaText = ref('')
const announcements = ref([])
const expandedAnnouncements = ref([])
const announcementsLoading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(5)

const isLoggedIn = computed(() => !!localStorage.getItem('token'))

function typeLabel(t) { return ['', '系统公告', '功能更新', '维护通知', '安全警告'][t] || '公告' }

function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return minutes + '分钟前'
  if (hours < 24) return hours + '小时前'
  if (days < 7) return days + '天前'
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

function toggleExpand(id) {
  const i = expandedAnnouncements.value.indexOf(id)
  if (i >= 0) expandedAnnouncements.value.splice(i, 1)
  else expandedAnnouncements.value.push(id)
}

async function loadAnnouncements(page = 1) {
  announcementsLoading.value = true
  try {
    currentPage.value = page
    const res = await api.get('/api/v1/announcements', { params: { page, pageSize: pageSize.value } })
    announcements.value = res.data?.list || []
    total.value = res.data?.total || 0
    expandedAnnouncements.value = announcements.value.filter(a => a.priority === 2).map(a => a.id)
  } catch (e) {
    announcements.value = []
  } finally {
    announcementsLoading.value = false
  }
}

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
    window.dispatchEvent(new Event('login-state-change'))

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

const goDashboard = () => {
  router.push('/dashboard')
}

onMounted(() => {
  refreshCaptcha()
  loadAnnouncements()
  if (localStorage.getItem('darkMode') === 'true') {
    document.documentElement.classList.add('dark')
  }
})
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  position: relative;
  overflow: hidden;
}

.top-right-admin {
  position: absolute;
  top: 24px;
  right: 24px;
  z-index: 100;

  :deep(.el-button) {
    color: rgba(255, 255, 255, 0.85);
    font-size: 14px;
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 16px;
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.2);
    transition: all 0.3s;

    &:hover {
      color: #fff;
      background: rgba(255, 255, 255, 0.2);
      border-color: rgba(255, 255, 255, 0.3);
    }

    .el-icon {
      font-size: 16px;
    }
  }
}

.login-box {
  width: 420px;
  padding: 48px 40px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  box-shadow:
    0 25px 50px -12px rgba(0, 0, 0, 0.25),
    0 0 0 1px rgba(255, 255, 255, 0.1) inset;
  animation: slideUp 0.6s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
  z-index: 10;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(40px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.login-header {
  text-align: center;
  margin-bottom: 40px;

  .logo-icon {
    width: 72px;
    height: 72px;
    margin: 0 auto 20px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);

    svg {
      width: 40px;
      height: 40px;
      fill: #fff;
    }
  }

  h1 {
    font-size: 26px;
    font-weight: 700;
    color: #1a1a2e;
    margin-bottom: 8px;
  }

  p {
    font-size: 14px;
    color: #6b7280;
  }
}

.login-form {
  .form-item {
    margin-bottom: 24px;
  }

  :deep(.el-form-item) {
    margin-bottom: 24px;
  }

  :deep(.el-input__wrapper) {
    padding: 14px 16px;
    border-radius: 12px;
    box-shadow: 0 0 0 1px #e5e7eb;
    transition: all 0.3s ease;

    &:hover, &:focus {
      box-shadow: 0 0 0 2px #667eea;
    }

    &.is-focus {
      box-shadow: 0 0 0 2px #667eea;
    }
  }

  .input-icon {
    font-size: 18px;
    color: #9ca3af;
  }

  .captcha-code {
    width: 38%;
    height: 40px;
    background: linear-gradient(135deg, var(--color-border-light), var(--color-border));
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
    color: var(--color-text-secondary);
    transition: all 0.3s;

    &:hover {
      background: linear-gradient(135deg, var(--color-border), var(--color-border-dark));
      transform: scale(1.02);
    }
  }

  .form-options {
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .remember-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 28px;

    :deep(.el-checkbox__label) {
      color: #6b7280;
      font-size: 13px;
    }

    .forgot-link {
      font-size: 13px;
      color: #667eea;
      text-decoration: none;
      transition: color 0.2s;

      &:hover {
        color: #764ba2;
      }
    }
  }

  .login-btn {
    width: 100%;
    height: 52px;
    font-size: 16px;
    font-weight: 600;
    border-radius: 12px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
    color: #fff;
    cursor: pointer;
    transition: all 0.3s ease;
    box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(102, 126, 234, 0.5);
    }

    &:active {
      transform: translateY(0);
    }

    &.is-loading {
      opacity: 0.8;
      cursor: not-allowed;
    }
  }
}

.login-footer {
  text-align: center;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;

  span {
    font-size: 14px;
    color: #6b7280;
  }

  :deep(.el-link) {
    font-size: 14px;
    font-weight: 500;
  }
}

.login-divider {
  display: flex;
  align-items: center;
  margin: 28px 0;

  span {
    flex: 1;
    height: 1px;
    background: #e5e7eb;
  }

  em {
    padding: 0 16px;
    font-style: normal;
    font-size: 12px;
    color: #9ca3af;
  }
}

.social-login {
  display: flex;
  gap: 12px;

  .social-btn {
    flex: 1;
    height: 48px;
    border-radius: 12px;
    border: 1px solid #e5e7eb;
    background: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    cursor: pointer;
    transition: all 0.2s ease;
    font-size: 14px;
    color: #4b5563;

    &:hover {
      border-color: #667eea;
      color: #667eea;
      background: rgba(102, 126, 234, 0.05);
    }

    svg {
      width: 20px;
      height: 20px;
    }
  }
}

.login-decoration {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;

  .decoration {
    position: absolute;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.1);

    &.decoration-1 {
      width: 300px;
      height: 300px;
      top: -100px;
      right: -50px;
      animation: float 8s ease-in-out infinite;
    }

    &.decoration-2 {
      width: 200px;
      height: 200px;
      bottom: -50px;
      left: -50px;
      animation: float 6s ease-in-out 1s infinite;
    }

    &.decoration-3 {
      width: 150px;
      height: 150px;
      top: 50%;
      left: 20%;
      animation: float 7s ease-in-out 2s infinite;
    }
  }
}

.admin-quick-link {
  text-align: center;
  margin-top: 12px;

  :deep(.el-link) {
    color: var(--color-text-placeholder);
    font-size: 13px;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    transition: color 0.3s;

    &:hover {
      color: var(--color-primary);
    }
  }
}

.demo-accounts {
  margin-top: 24px;

  :deep(.el-divider) {
    margin: 16px 0;

    .el-divider__text {
      background: var(--color-bg);
      color: var(--color-text-placeholder);
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
      font-family: 'JetBrains Mono', 'Courier New', monospace;
      color: var(--color-text-secondary);
      font-weight: 500;
    }
  }
}

.dashboard-entry {
  margin-top: 24px;
  display: flex;
  justify-content: center;

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
    background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
    border: none;
    box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
    color: #fff;

    &:hover {
      box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
    }
  }
}

.announcements-section {
  margin-top: 20px;

  :deep(.el-divider) {
    margin: 12px 0;

    .el-divider__text {
      background: var(--color-bg);
      color: var(--color-text-placeholder);
      font-size: 12px;
      padding: 0 12px;
    }
  }
}

.announcements-list {
  max-height: 200px;
  overflow-y: auto;
}

.announcement-item {
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border-light);

  &:last-child { border-bottom: none; }

  &.priority-2 {
    background: rgba(245, 108, 108, 0.08);
    margin: 0 -12px;
    padding: 8px 12px;
    border-radius: var(--radius-sm);
    border-bottom: none;
  }

  &.priority-1 {
    background: rgba(230, 162, 60, 0.08);
    margin: 0 -12px;
    padding: 8px 12px;
    border-radius: var(--radius-sm);
    border-bottom: none;
  }
}

.announcement-header {
  display: flex;
  align-items: center;
  gap: 6px;

  .announcement-title {
    font-size: 13px;
    color: var(--color-text-primary);
    font-weight: 500;
  }
}

.announcement-content {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 4px;
  line-height: 1.5;
  padding-left: 2px;
}

.truncated {
  color: var(--color-text-placeholder);
}

.announcement-actions {
  margin-top: 2px;
  text-align: right;
}

.announcement-time {
  font-size: 11px;
  color: var(--color-text-placeholder);
  margin-left: auto;
}

.announcements-pagination {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}
</style>
