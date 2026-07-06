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
        <span class="footer-divider">|</span>
        <el-link type="warning" @click="$router.push('/admin/login')">后台管理登录</el-link>
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
import { User, Lock, CircleCheck, Sunny, Odometer } from '@element-plus/icons-vue'
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
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.footer-divider {
  color: #d1d5db;
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
      font-family: 'JetBrains Mono', 'Courier New', monospace;
      color: #4b5563;
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
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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
      background: #fff;
      color: #9ca3af;
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
  border-bottom: 1px solid #f0f0f0;

  &:last-child { border-bottom: none; }

  &.priority-2 {
    background: #fff5f5;
    margin: 0 -12px;
    padding: 8px 12px;
    border-radius: 6px;
    border-bottom: none;
  }

  &.priority-1 {
    background: #fffbeb;
    margin: 0 -12px;
    padding: 8px 12px;
    border-radius: 6px;
    border-bottom: none;
  }
}

.announcement-header {
  display: flex;
  align-items: center;
  gap: 6px;

  .announcement-title {
    font-size: 13px;
    color: #374151;
    font-weight: 500;
  }
}

.announcement-content {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
  line-height: 1.5;
  padding-left: 2px;
}

.truncated {
  color: #9ca3af;
}

.announcement-actions {
  margin-top: 2px;
  text-align: right;
}

.announcement-time {
  font-size: 11px;
  color: #9ca3af;
  margin-left: auto;
}

.announcements-pagination {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}
</style>
