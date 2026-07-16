<template>
  <div class="admin-login-container">
    <div class="bg-particles">
      <div class="particle p1"></div>
      <div class="particle p2"></div>
      <div class="particle p3"></div>
      <div class="particle p4"></div>
      <div class="particle p5"></div>
    </div>

    <div class="login-card">
      <div class="card-header">
        <div class="shield-icon">
          <el-icon :size="36"><Lock /></el-icon>
        </div>
        <h1>后台管理系统</h1>
        <p>Legal AI Assistant Admin</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
        @submit.prevent
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="管理员账号"
            size="large"
            :prefix-icon="User"
            maxlength="32"
            show-word-limit
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="管理员密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            maxlength="32"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <div v-if="lockoutRemaining > 0" class="lockout-hint">
          <el-icon><Clock /></el-icon>
          账号已锁定，请在 {{ lockoutMinutes }} 分钟后重试
        </div>

        <el-form-item>
          <div class="form-options">
            <el-checkbox v-model="rememberMe" label="记住账号" size="small" />
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="loading"
            :disabled="lockoutRemaining > 0"
            @click.prevent="handleLogin"
          >
            登 录 后 台
          </el-button>
        </el-form-item>
      </el-form>

      <div class="card-footer">
        <el-link type="info" underline="never" @click="$router.push('/')">
          <el-icon><ArrowLeft /></el-icon>
          返回首页
        </el-link>
      </div>

        <div v-if="lastLoginAt" class="last-login-hint">
        <el-icon><Clock /></el-icon>
        <span>上次登录: {{ lastLoginAt }}</span>
      </div>

      <div class="hint-box">
        <el-icon><InfoFilled /></el-icon>
        <span>默认账号: admin / admin123</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, ArrowLeft, InfoFilled, Clock } from '@element-plus/icons-vue'
import api from '../api'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const lockoutRemaining = ref(0)
const lastLoginAt = ref('')
const rememberMe = ref(false)
let lockoutTimer = null

const form = reactive({
  username: '',
  password: ''
})

const lockoutMinutes = computed(() => Math.ceil(lockoutRemaining.value / 60000))

function sanitize(str) {
  if (!str) return ''
  return String(str).replace(/[<>'"&]/g, '')
}

const rules = {
  username: [
    { required: true, message: '请输入管理员账号', trigger: 'blur' },
    { min: 2, max: 32, message: '账号长度为2-32位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为6-32位', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  if (lockoutRemaining.value > 0) {
    ElMessage.warning('账号已锁定，请稍后再试')
    return
  }

  const safeUsername = sanitize(form.username.trim())
  const safePassword = form.password

  loading.value = true
  try {
    const res = await api.auth.adminLogin({
      username: safeUsername,
      password: safePassword
    })

    localStorage.setItem('admin_token', res.token)
    localStorage.setItem('admin_user', JSON.stringify(res.userInfo))

    if (rememberMe.value) {
      localStorage.setItem('admin_username', safeUsername)
    } else {
      localStorage.removeItem('admin_username')
    }

    if (res.lastLoginAt) {
      localStorage.setItem('admin_last_login', res.lastLoginAt)
    }

    ElMessage.success('登录成功')
    router.push('/admin')
  } catch (e) {
    const msg = e?.response?.data?.error || e?.response?.data?.message || '登录失败'
    ElMessage.error(msg)

    if (msg.includes('锁定')) {
      lockoutRemaining.value = 5 * 60 * 1000
      startLockoutTimer()
    }
  } finally {
    loading.value = false
  }
}

function startLockoutTimer() {
  if (lockoutTimer) clearInterval(lockoutTimer)
  lockoutTimer = setInterval(() => {
    lockoutRemaining.value = Math.max(0, lockoutRemaining.value - 1000)
    if (lockoutRemaining.value <= 0) {
      clearInterval(lockoutTimer)
    }
  }, 1000)
}

onMounted(() => {
  const savedUsername = localStorage.getItem('admin_username')
  if (savedUsername) {
    form.username = savedUsername
    rememberMe.value = true
  }
  const savedLastLogin = localStorage.getItem('admin_last_login')
  if (savedLastLogin) {
    lastLoginAt.value = savedLastLogin
  }
})
</script>

<style lang="scss" scoped>
.admin-login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  position: relative;
  overflow: hidden;
}

.bg-particles {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  pointer-events: none;

  .particle {
    position: absolute;
    border-radius: 2px;
    background: rgba(99, 102, 241, 0.15);
    animation: drift linear infinite;

    &.p1 { width: 4px; height: 4px; top: 20%; left: 15%; animation-duration: 18s; animation-delay: 0s; }
    &.p2 { width: 2px; height: 2px; top: 60%; left: 80%; animation-duration: 22s; animation-delay: -5s; }
    &.p3 { width: 6px; height: 6px; top: 30%; left: 65%; animation-duration: 25s; animation-delay: -10s; }
    &.p4 { width: 3px; height: 3px; top: 75%; left: 25%; animation-duration: 20s; animation-delay: -15s; }
    &.p5 { width: 5px; height: 5px; top: 10%; left: 45%; animation-duration: 15s; animation-delay: -8s; }
  }
}

@keyframes drift {
  0% { transform: translate(0, 0); opacity: 0; }
  10% { opacity: 1; }
  90% { opacity: 1; }
  100% { transform: translate(100px, -80px); opacity: 0; }
}

.login-card {
  width: 420px;
  padding: 48px 40px 36px;
  background: rgba(30, 41, 59, 0.85);
  backdrop-filter: blur(24px);
  border-radius: 20px;
  border: 1px solid rgba(99, 102, 241, 0.15);
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.5), 0 0 40px rgba(99, 102, 241, 0.08);
  position: relative;
  z-index: 10;
}

.card-header {
  text-align: center;
  margin-bottom: 36px;

  .shield-icon {
    width: 72px;
    height: 72px;
    background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
    border-radius: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 20px;
    color: #fff;
    box-shadow: 0 12px 30px rgba(99, 102, 241, 0.35);
  }

  h1 {
    margin: 0 0 8px 0;
    font-size: 24px;
    font-weight: 600;
    color: #e2e8f0;
  }

  p {
    margin: 0;
    color: #64748b;
    font-size: 13px;
    letter-spacing: 1px;
  }
}

.login-form {
  :deep(.el-form-item) {
    margin-bottom: 22px;
  }

  :deep(.el-input__wrapper) {
    background: rgba(15, 23, 42, 0.6);
    border-radius: 10px;
    box-shadow: 0 0 0 1px rgba(99, 102, 241, 0.2);
    padding: 4px 16px;

    &:hover, &.is-focus {
      box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.5);
    }
  }

  :deep(.el-input__inner) {
    color: #e2e8f0;

    &::placeholder {
      color: #475569;
    }
  }

  :deep(.el-input__prefix) {
    color: #6366f1;
  }

  .submit-btn {
    width: 100%;
    height: 48px;
    border-radius: 10px;
    font-size: 15px;
    font-weight: 600;
    letter-spacing: 2px;
    background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
    border: none;
    box-shadow: 0 4px 20px rgba(99, 102, 241, 0.4);
    transition: all 0.3s;
    margin-top: 8px;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 30px rgba(99, 102, 241, 0.5);
    }
  }
}

.card-footer {
  text-align: center;
  margin-top: 20px;

  :deep(.el-link) {
    color: #64748b;
    font-size: 13px;
    display: inline-flex;
    align-items: center;
    gap: 4px;

    &:hover {
      color: #818cf8;
    }
  }
}

.hint-box {
  margin-top: 16px;
  padding: 12px 16px;
  background: rgba(99, 102, 241, 0.08);
  border: 1px solid rgba(99, 102, 241, 0.12);
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #818cf8;
  font-size: 12px;
  justify-content: center;
}

.lockout-hint {
  margin-bottom: 16px;
  padding: 10px 16px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fca5a5;
  font-size: 13px;
  justify-content: center;
  animation: shake 0.4s ease-in-out;
}

.last-login-hint {
  margin-top: 12px;
  padding: 8px 16px;
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.12);
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6ee7b7;
  font-size: 12px;
  justify-content: center;
}

.form-options {
  width: 100%;
  display: flex;
  justify-content: flex-end;

  :deep(.el-checkbox__label) {
    color: #64748b;
    font-size: 13px;
  }
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-5px); }
  75% { transform: translateX(5px); }
}
</style>
