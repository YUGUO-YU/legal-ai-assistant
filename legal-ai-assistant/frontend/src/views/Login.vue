<template>
  <div class="login-page">
    <div class="bg-mesh"></div>
    <div class="bg-grid"></div>
    <div class="bg-glow bg-glow-1"></div>
    <div class="bg-glow bg-glow-2"></div>

    <div class="top-right-admin">
      <el-button text @click="$router.push('/admin/login')">
        <el-icon><Setting /></el-icon>
        后台管理
      </el-button>
    </div>

    <div class="login-container">
      <div class="login-left" :class="{ 'is-animated': mounted}">
        <div class="left-inner">
          <div class="brand-emblem">
            <svg viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M40 4L76 22V58L40 76L4 58V22L40 4Z" stroke="url(#goldGrad)" stroke-width="2" fill="none"/>
              <path d="M40 14L66 27V53L40 66L14 53V27L40 14Z" stroke="url(#goldGrad)" stroke-width="1.5" fill="rgba(201,168,76,0.06)"/>
              <path d="M28 40L36 48L52 32" stroke="#c9a84c" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
              <defs>
                <linearGradient id="goldGrad" x1="4" y1="4" x2="76" y2="76" gradientUnits="userSpaceOnUse">
                  <stop offset="0%" stop-color="#e8d5a3"/>
                  <stop offset="50%" stop-color="#c9a84c"/>
                  <stop offset="100%" stop-color="#a07830"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <h1 class="brand-title">法律AI助手</h1>
          <p class="brand-subtitle">智能法律服务解决方案</p>
          <div class="brand-divider"></div>
          <div class="feature-list">
            <div class="feature-item delay-1">
              <div class="feature-dot"></div>
              <span>智能法律文书生成</span>
            </div>
            <div class="feature-item delay-2">
              <div class="feature-dot"></div>
              <span>精准案例智能检索</span>
            </div>
            <div class="feature-item delay-3">
              <div class="feature-dot"></div>
              <span>合同风险智能审查</span>
            </div>
            <div class="feature-item delay-4">
              <div class="feature-dot"></div>
              <span>企业信息一站查询</span>
            </div>
          </div>
          <div class="left-footer">
            <span>© {{ new Date().getFullYear() }} 法律AI助手</span>
          </div>
        </div>
      </div>

      <div class="login-right" :class="{ 'is-animated': mounted}">
        <div class="login-card">
          <div class="card-header">
            <div class="card-title-row">
              <div class="card-icon">
                <el-icon :size="22"><UserFilled /></el-icon>
              </div>
              <div>
                <h2>用户登录</h2>
                <p>欢迎回来，请输入您的账号信息</p>
              </div>
            </div>
          </div>

          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="rules"
            class="login-form"
            @keyup.enter="handleLogin"
          >
            <div class="field-group" :class="{ 'is-animated': mounted}">
              <div class="field-item delay-1">
                <label class="field-label">用户名</label>
                <el-input
                  v-model="loginForm.username"
                  placeholder="请输入用户名"
                  size="large"
                  :prefix-icon="User"
                  class="field-input"
                />
              </div>

              <div class="field-item delay-2">
                <label class="field-label">密码</label>
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  placeholder="请输入密码"
                  size="large"
                  :prefix-icon="Lock"
                  show-password
                  class="field-input"
                />
              </div>

              <div class="field-item delay-3">
                <label class="field-label">验证码</label>
                <div class="captcha-row">
                  <el-input
                    v-model="loginForm.captcha"
                    placeholder="请输入验证码"
                    size="large"
                    :prefix-icon="CircleCheck"
                    class="field-input captcha-input"
                  />
                  <div class="captcha-code" @click="refreshCaptcha" title="点击刷新">
                    <span>{{ captchaText }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="form-options delay-4">
              <el-checkbox v-model="rememberMe">记住我</el-checkbox>
              <el-link type="primary" class="forgot-link" @click="$router.push('/forgot-password')">忘记密码？</el-link>
            </div>

            <div class="submit-area delay-5">
              <el-button
                type="primary"
                size="large"
                class="login-btn"
                :class="{ 'is-loading': loading }"
                :loading="loading"
                @click="handleLogin"
              >
                <span class="btn-text">登 录</span>
                <span class="btn-arrow">
                  <el-icon><ArrowRight /></el-icon>
                </span>
                <div class="btn-shimmer"></div>
              </el-button>
            </div>
          </el-form>

          <div class="login-footer delay-6">
            <span>还没有账号？</span>
            <el-link type="primary" @click="$router.push('/register')">立即注册</el-link>
          </div>

          <div class="demo-accounts delay-7">
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

          <div class="announcements-section delay-8" v-if="announcements.length > 0">
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
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, CircleCheck, UserFilled, Setting, Odometer, ArrowRight } from '@element-plus/icons-vue'
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
const mounted = ref(false)

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
  mounted.value = true
  refreshCaptcha()
  loadAnnouncements()
  if (localStorage.getItem('darkMode') === 'true') {
    document.documentElement.classList.add('dark')
  }
})
</script>

<style lang="scss" scoped>
/* ======================
   CSS Variables
   ====================== */
.login-page {
  --navy-900: #0a1628;
  --navy-800: #0f172a;
  --navy-700: #1a2744;
  --navy-600: #1e3a5f;
  --gold-500: #c9a84c;
  --gold-400: #d4b96a;
  --gold-300: #e8d5a3;
  --text-light: #e8e0d0;
  --text-muted: #8b9ab4;
  --glass-bg: rgba(15, 23, 42, 0.7);
  --glass-border: rgba(201, 168, 76, 0.15);
  --card-bg: rgba(15, 23, 42, 0.85);
}

/* ======================
   Background
   ====================== */
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--navy-900);
  position: relative;
  overflow: hidden;
}

.bg-mesh {
  position: fixed;
  inset: 0;
  background:
    radial-gradient(ellipse 80% 60% at 20% 10%, rgba(30, 58, 95, 0.6) 0%, transparent 60%),
    radial-gradient(ellipse 60% 80% at 80% 90%, rgba(26, 39, 68, 0.5) 0%, transparent 60%),
    radial-gradient(ellipse 50% 50% at 50% 50%, rgba(15, 23, 42, 0.8) 0%, transparent 70%);
  animation: meshDrift 20s ease-in-out infinite alternate;
  pointer-events: none;
}

@keyframes meshDrift {
  0% {
    background:
      radial-gradient(ellipse 80% 60% at 20% 10%, rgba(30, 58, 95, 0.6) 0%, transparent 60%),
      radial-gradient(ellipse 60% 80% at 80% 90%, rgba(26, 39, 68, 0.5) 0%, transparent 60%),
      radial-gradient(ellipse 50% 50% at 50% 50%, rgba(15, 23, 42, 0.8) 0%, transparent 70%);
  }
  100% {
    background:
      radial-gradient(ellipse 70% 70% at 25% 15%, rgba(30, 58, 95, 0.7) 0%, transparent 60%),
      radial-gradient(ellipse 70% 60% at 75% 85%, rgba(26, 39, 68, 0.6) 0%, transparent 60%),
      radial-gradient(ellipse 60% 40% at 50% 50%, rgba(15, 23, 42, 0.9) 0%, transparent 70%);
  }
}

.bg-grid {
  position: fixed;
  inset: 0;
  background-image:
    linear-gradient(rgba(201, 168, 76, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(201, 168, 76, 0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  animation: gridFade 8s ease-in-out infinite alternate;
  pointer-events: none;
}

@keyframes gridFade {
  0% { opacity: 0.4; }
  100% { opacity: 0.8; }
}

.bg-glow {
  position: fixed;
  border-radius: 50%;
  filter: blur(100px);
  pointer-events: none;
  animation: glowPulse 10s ease-in-out infinite alternate;
}

.bg-glow-1 {
  width: 600px;
  height: 600px;
  top: -200px;
  left: -100px;
  background: radial-gradient(circle, rgba(30, 58, 95, 0.4) 0%, transparent 70%);
  animation-delay: 0s;
}

.bg-glow-2 {
  width: 500px;
  height: 500px;
  bottom: -150px;
  right: -100px;
  background: radial-gradient(circle, rgba(26, 39, 68, 0.5) 0%, transparent 70%);
  animation-delay: -5s;
}

@keyframes glowPulse {
  0% { opacity: 0.5; transform: scale(1); }
  100% { opacity: 0.8; transform: scale(1.1); }
}

/* ======================
   Layout
   ====================== */
.login-container {
  display: flex;
  align-items: stretch;
  width: min(960px, 95vw);
  min-height: min(680px, 85vh);
  position: relative;
  z-index: 10;
  gap: 0;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.6), 0 0 0 1px rgba(201, 168, 76, 0.1);
}

/* ======================
   Left Panel
   ====================== */
.login-left {
  width: 38%;
  background: linear-gradient(160deg, var(--navy-800) 0%, var(--navy-700) 50%, var(--navy-600) 100%);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 36px;
  opacity: 0;
  transform: translateX(-40px);
  transition: opacity 0.8s cubic-bezier(0.16, 1, 0.3, 1), transform 0.8s cubic-bezier(0.16, 1, 0.3, 1);

  &.is-animated {
    opacity: 1;
    transform: translateX(0);
  }

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background:
      radial-gradient(ellipse at 30% 20%, rgba(201, 168, 76, 0.08) 0%, transparent 50%),
      radial-gradient(ellipse at 70% 80%, rgba(30, 58, 95, 0.3) 0%, transparent 50%);
  }

  &::after {
    content: '';
    position: absolute;
    right: 0;
    top: 0;
    bottom: 0;
    width: 1px;
    background: linear-gradient(to bottom, transparent, rgba(201, 168, 76, 0.2), transparent);
  }
}

.left-inner {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  height: 100%;
  justify-content: center;
}

.brand-emblem {
  width: 80px;
  height: 80px;
  margin-bottom: 24px;
  animation: emblemFloat 4s ease-in-out infinite;
  filter: drop-shadow(0 0 20px rgba(201, 168, 76, 0.3));

  svg {
    width: 100%;
    height: 100%;
  }
}

@keyframes emblemFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.brand-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--gold-300);
  margin: 0 0 8px 0;
  letter-spacing: 2px;
  text-shadow: 0 0 30px rgba(201, 168, 76, 0.3);
  animation: titleReveal 1s cubic-bezier(0.16, 1, 0.3, 1) 0.3s both;
}

@keyframes titleReveal {
  from { opacity: 0; letter-spacing: 8px; }
  to { opacity: 1; letter-spacing: 2px; }
}

.brand-subtitle {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0 0 32px 0;
  letter-spacing: 3px;
  text-transform: uppercase;
  animation: fadeIn 1s ease 0.5s both;
}

.brand-divider {
  width: 60px;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--gold-500), transparent);
  margin-bottom: 32px;
  animation: expandWidth 1s cubic-bezier(0.16, 1, 0.3, 1) 0.6s both;
}

@keyframes expandWidth {
  from { width: 0; }
  to { width: 60px; }
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  width: 100%;
  margin-bottom: auto;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border-radius: 10px;
  background: rgba(201, 168, 76, 0.05);
  border: 1px solid rgba(201, 168, 76, 0.1);
  font-size: 13px;
  color: var(--text-light);
  text-align: left;
  opacity: 0;
  transform: translateX(-20px);
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);

  &.delay-1 { animation: slideInLeft 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.8s both; }
  &.delay-2 { animation: slideInLeft 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.95s both; }
  &.delay-3 { animation: slideInLeft 0.6s cubic-bezier(0.16, 1, 0.3, 1) 1.1s both; }
  &.delay-4 { animation: slideInLeft 0.6s cubic-bezier(0.16, 1, 0.3, 1) 1.25s both; }

  &:hover {
    background: rgba(201, 168, 76, 0.1);
    border-color: rgba(201, 168, 76, 0.25);
    transform: translateX(4px) !important;
  }
}

@keyframes slideInLeft {
  from { opacity: 0; transform: translateX(-20px); }
  to { opacity: 1; transform: translateX(0); }
}

.feature-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--gold-500);
  box-shadow: 0 0 8px rgba(201, 168, 76, 0.5);
  flex-shrink: 0;
}

.left-footer {
  margin-top: 32px;
  font-size: 11px;
  color: rgba(139, 154, 180, 0.6);
  letter-spacing: 1px;
}

/* ======================
   Right Panel
   ====================== */
.login-right {
  flex: 1;
  background: var(--card-bg);
  backdrop-filter: blur(40px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 36px;
  opacity: 0;
  transform: translateX(40px);
  transition: opacity 0.8s cubic-bezier(0.16, 1, 0.3, 1) 0.15s, transform 0.8s cubic-bezier(0.16, 1, 0.3, 1) 0.15s;

  &.is-animated {
    opacity: 1;
    transform: translateX(0);
  }
}

.login-card {
  width: 100%;
  max-width: 380px;
}

.card-header {
  margin-bottom: 32px;
}

.card-title-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.card-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(201, 168, 76, 0.15), rgba(201, 168, 76, 0.05));
  border: 1px solid rgba(201, 168, 76, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--gold-500);
  flex-shrink: 0;
}

.card-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-light);
  margin: 0 0 4px 0;
}

.card-header p {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0;
}

/* ======================
   Form
   ====================== */
.field-group {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 20px;
}

.field-item {
  opacity: 0;
  transform: translateY(16px);

  &.delay-1 { animation: fieldReveal 0.5s cubic-bezier(0.16, 1, 0.3, 1) 0.4s both; }
  &.delay-2 { animation: fieldReveal 0.5s cubic-bezier(0.16, 1, 0.3, 1) 0.52s both; }
  &.delay-3 { animation: fieldReveal 0.5s cubic-bezier(0.16, 1, 0.3, 1) 0.64s both; }
}

@keyframes fieldReveal {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

.field-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-muted);
  margin-bottom: 8px;
  letter-spacing: 0.5px;
}

.field-input {
  :deep(.el-input__wrapper) {
    padding: 13px 16px;
    border-radius: 10px;
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid rgba(201, 168, 76, 0.12);
    box-shadow: none;
    transition: all 0.3s ease;

    &:hover {
      border-color: rgba(201, 168, 76, 0.3);
      background: rgba(255, 255, 255, 0.06);
    }

    &.is-focus {
      border-color: rgba(201, 168, 76, 0.5);
      background: rgba(255, 255, 255, 0.06);
      box-shadow: 0 0 0 3px rgba(201, 168, 76, 0.1), 0 0 20px rgba(201, 168, 76, 0.05);
    }
  }

  :deep(.el-input__inner) {
    color: var(--text-light);
    font-size: 14px;

    &::placeholder {
      color: rgba(139, 154, 180, 0.5);
    }
  }

  :deep(.el-input__prefix .el-icon) {
    color: var(--text-muted);
  }
}

.captcha-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.captcha-input {
  flex: 1;
}

.captcha-code {
  width: 110px;
  height: 44px;
  background: linear-gradient(135deg, rgba(201, 168, 76, 0.08), rgba(201, 168, 76, 0.03));
  border: 1px solid rgba(201, 168, 76, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  cursor: pointer;
  font-family: 'JetBrains Mono', 'Courier New', monospace;
  font-size: 22px;
  font-weight: bold;
  letter-spacing: 5px;
  color: var(--gold-400);
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
  flex-shrink: 0;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(90deg, transparent, rgba(255,255,255,0.05), transparent);
    transform: translateX(-100%);
    transition: transform 0.6s;
  }

  &:hover::before {
    transform: translateX(100%);
  }

  &:hover {
    border-color: rgba(201, 168, 76, 0.4);
    background: linear-gradient(135deg, rgba(201, 168, 76, 0.12), rgba(201, 168, 76, 0.05));
    transform: scale(1.02);
  }
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  opacity: 0;
  animation: fieldReveal 0.5s cubic-bezier(0.16, 1, 0.3, 1) 0.76s both;

  :deep(.el-checkbox__label) {
    color: var(--text-muted);
    font-size: 13px;
  }

  :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
    background: var(--gold-500);
    border-color: var(--gold-500);
  }

  .forgot-link {
    font-size: 13px;
    color: var(--gold-500);
    text-decoration: none;
    transition: color 0.2s;

    &:hover {
      color: var(--gold-400);
    }
  }
}

.submit-area {
  opacity: 0;
  animation: fieldReveal 0.5s cubic-bezier(0.16, 1, 0.3, 1) 0.88s both;
}

.login-btn {
  width: 100%;
  height: 50px;
  border-radius: 10px;
  background: linear-gradient(135deg, #c9a84c 0%, #a07830 100%);
  border: none;
  color: #0a1628;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 2px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s ease;
  box-shadow: 0 4px 16px rgba(201, 168, 76, 0.3);

  .btn-text {
    position: relative;
    z-index: 1;
    transition: transform 0.3s;
  }

  .btn-arrow {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    transition: transform 0.3s;
  }

  .btn-shimmer {
    position: absolute;
    inset: 0;
    background: linear-gradient(105deg, transparent 40%, rgba(255,255,255,0.3) 50%, transparent 60%);
    background-size: 200% 100%;
    animation: shimmerSweep 2.5s ease-in-out infinite;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(201, 168, 76, 0.4);
    background: linear-gradient(135deg, #d4b96a 0%, #b89040 100%);

    .btn-text { transform: translateX(-3px); }
    .btn-arrow { transform: translateX(3px); }
  }

  &:active {
    transform: translateY(0);
  }

  &.is-loading {
    cursor: not-allowed;
    opacity: 0.85;
  }
}

@keyframes shimmerSweep {
  0% { background-position: 200% 0; }
  60% { background-position: -50% 0; }
  100% { background-position: -50% 0; }
}

/* ======================
   Footer & Misc
   ====================== */
.login-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid rgba(201, 168, 76, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  opacity: 0;
  animation: fieldReveal 0.5s cubic-bezier(0.16, 1, 0.3, 1) 1s both;

  span {
    font-size: 13px;
    color: var(--text-muted);
  }

  :deep(.el-link) {
    font-size: 13px;
    font-weight: 500;
  }
}

.demo-accounts {
  margin-top: 20px;
  opacity: 0;
  animation: fieldReveal 0.5s cubic-bezier(0.16, 1, 0.3, 1) 1.12s both;

  :deep(.el-divider) {
    margin: 14px 0;

    .el-divider__text {
      background: var(--card-bg);
      color: var(--text-muted);
      font-size: 11px;
      padding: 0 12px;
      letter-spacing: 1px;
    }
  }

  .demo-info {
    display: flex;
    justify-content: center;
    gap: 28px;
  }

  .demo-item {
    display: flex;
    align-items: center;
    gap: 8px;

    .demo-value {
      font-family: 'JetBrains Mono', 'Courier New', monospace;
      color: var(--gold-400);
      font-weight: 500;
      font-size: 13px;
    }
  }
}

.announcements-section {
  margin-top: 20px;
  opacity: 0;
  animation: fieldReveal 0.5s cubic-bezier(0.16, 1, 0.3, 1) 1.24s both;

  :deep(.el-divider) {
    margin: 14px 0;

    .el-divider__text {
      background: var(--card-bg);
      color: var(--text-muted);
      font-size: 11px;
      padding: 0 12px;
      letter-spacing: 1px;
    }
  }
}

.announcements-list {
  max-height: 180px;
  overflow-y: auto;

  &::-webkit-scrollbar {
    width: 4px;
  }
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  &::-webkit-scrollbar-thumb {
    background: rgba(201, 168, 76, 0.2);
    border-radius: 2px;
  }
}

.announcement-item {
  padding: 8px 0;
  border-bottom: 1px solid rgba(201, 168, 76, 0.06);

  &:last-child { border-bottom: none; }

  &.priority-2 {
    background: rgba(239, 68, 68, 0.06);
    margin: 0 -8px;
    padding: 8px;
    border-radius: 8px;
    border-bottom: none;
  }

  &.priority-1 {
    background: rgba(245, 158, 11, 0.06);
    margin: 0 -8px;
    padding: 8px;
    border-radius: 8px;
    border-bottom: none;
  }
}

.announcement-header {
  display: flex;
  align-items: center;
  gap: 6px;

  .announcement-title {
    font-size: 12px;
    color: var(--text-light);
    font-weight: 500;
  }
}

.announcement-content {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 3px;
  line-height: 1.5;
  padding-left: 2px;
}

.truncated {
  color: rgba(139, 154, 180, 0.6);
}

.announcement-actions {
  margin-top: 2px;
  text-align: right;
}

.announcement-time {
  font-size: 10px;
  color: rgba(139, 154, 180, 0.5);
  margin-left: auto;
}

.announcements-pagination {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}

.dashboard-entry {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  opacity: 0;
  animation: fieldReveal 0.5s cubic-bezier(0.16, 1, 0.3, 1) 1.36s both;

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
    background: linear-gradient(135deg, #c9a84c 0%, #a07830 100%);
    border: none;
    box-shadow: 0 4px 15px rgba(201, 168, 76, 0.3);
    color: #0a1628;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(201, 168, 76, 0.4);
    }
  }
}

/* ======================
   Top Right Admin Button
   ====================== */
.top-right-admin {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 100;

  :deep(.el-button) {
    color: rgba(201, 168, 76, 0.8);
    font-size: 13px;
    display: flex;
    align-items: center;
    gap: 5px;
    padding: 7px 14px;
    border-radius: 8px;
    background: rgba(15, 23, 42, 0.6);
    border: 1px solid rgba(201, 168, 76, 0.15);
    backdrop-filter: blur(8px);
    transition: all 0.3s;

    &:hover {
      color: var(--gold-400);
      background: rgba(15, 23, 42, 0.8);
      border-color: rgba(201, 168, 76, 0.3);
    }

    .el-icon {
      font-size: 14px;
    }
  }
}

/* ======================
   Responsive
   ====================== */
@media (max-width: 768px) {
  .login-container {
    flex-direction: column;
    min-height: auto;
    width: 95vw;
  }

  .login-left {
    width: 100%;
    padding: 36px 24px;
    min-height: 200px;

    &::after {
      right: 0;
      left: 0;
      top: auto;
      bottom: 0;
      width: auto;
      height: 1px;
      background: linear-gradient(to right, transparent, rgba(201, 168, 76, 0.2), transparent);
    }
  }

  .feature-list {
    display: none;
  }

  .login-right {
    padding: 32px 24px;
  }

  .brand-emblem {
    width: 56px;
    height: 56px;
    margin-bottom: 16px;
  }

  .brand-title {
    font-size: 22px;
  }
}
</style>
