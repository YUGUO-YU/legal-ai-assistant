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
          <el-icon :size="48"><Avatar /></el-icon>
        </div>
        <h1>用户注册</h1>
        <p>创建您的法律AI助手账号</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
        @keyup.enter="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名（3-32字符）"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码（6-32字符）"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <div class="password-strength" v-if="form.password">
          <div class="strength-bar">
            <div :style="{ width: (passwordStrength.level / 4 * 100) + '%', background: passwordStrength.color }"></div>
          </div>
          <span :style="{ color: passwordStrength.color }">{{ passwordStrength.label }}</span>
        </div>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="确认密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="realName">
          <el-input
            v-model="form.realName"
            placeholder="真实姓名（选填）"
            size="large"
            :prefix-icon="Postcard"
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            placeholder="邮箱（选填）"
            size="large"
            :prefix-icon="Message"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleRegister"
          >
            注 册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <span>已有账号？</span>
        <el-link type="primary" @click="$router.push('/')">立即登录</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Lock, Postcard, Message, Avatar } from '@element-plus/icons-vue'
import api from '../api'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const passwordStrength = computed(() => {
  const p = form.password
  if (!p) return { level: 0, label: '', color: '' }
  let score = 0
  if (p.length >= 8) score++
  if (/[a-z]/.test(p) && /[A-Z]/.test(p)) score++
  if (/\d/.test(p)) score++
  if (/[^a-zA-Z0-9]/.test(p)) score++
  const levels = [
    { level: 0, label: '', color: '' },
    { level: 1, label: '弱', color: '#F56C6C' },
    { level: 2, label: '中', color: '#E6A23C' },
    { level: 3, label: '强', color: '#67C23A' },
    { level: 4, label: '非常强', color: '#409EFF' },
  ]
  return levels[score]
})

onMounted(() => {
  if (localStorage.getItem('darkMode') === 'true') {
    document.documentElement.classList.add('dark')
  }
})

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  email: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名需在3-32个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码需在6-32个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    if (!form.realName || !form.email) {
      try {
        await ElMessageBox.confirm(
          '您还未填写真实姓名和邮箱，补充这些信息可以提升账号安全性。是否现在补充？',
          '提示',
          {
            confirmButtonText: '现在补充',
            cancelButtonText: '稍后补充',
            type: 'info'
          }
        )
        return
      } catch {
      }
    }

    loading.value = true
    try {
      await api.auth.register({
        username: form.username,
        password: form.password,
        realName: form.realName,
        email: form.email || null
      })
      ElMessage.success('注册成功！您的账号正在等待管理员审核，审核通过后可登录')
      router.push('/')
    } catch (e) {
      console.error('注册失败:', e)
      const errMsg = e?.response?.data?.message || e?.message || '注册失败'
      ElMessage.error(errMsg)
    } finally {
      loading.value = false
    }
  })
}
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
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  z-index: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  .circle {
    position: absolute;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(1px);
    will-change: transform;
    animation: float 20s infinite ease-in-out;

    &.circle-1 { width: 400px; height: 400px; top: -100px; left: -100px; animation-delay: 0s; }
    &.circle-2 { width: 300px; height: 300px; top: 50%; right: -50px; animation-delay: -5s; background: rgba(255, 255, 255, 0.08); }
    &.circle-3 { width: 250px; height: 250px; bottom: -50px; left: 30%; animation-delay: -10s; background: rgba(255, 255, 255, 0.08); }
    &.circle-4 { width: 350px; height: 350px; bottom: 20%; right: 20%; animation-delay: -15s; background: rgba(255, 255, 255, 0.08); }
  }

  &::before {
    content: '';
    position: absolute;
    top: 20%;
    left: 20%;
    width: 500px;
    height: 500px;
    background: radial-gradient(circle, rgba(255,255,255,0.15) 0%, transparent 70%);
    border-radius: 50%;
    animation: pulse 8s infinite ease-in-out;
  }
}

@keyframes float {
  0%, 100% { transform: translate3d(0, 0, 0) rotate(0deg); }
  25% { transform: translate3d(30px, -30px, 0) rotate(5deg); }
  50% { transform: translate3d(-20px, 20px, 0) rotate(-5deg); }
  75% { transform: translate3d(20px, 30px, 0) rotate(3deg); }
}

@keyframes pulse {
  0%, 100% { opacity: 0.5; transform: scale(1); }
  50% { opacity: 0.8; transform: scale(1.1); }
}

@media (max-width: 768px) {
  .bg-animation {
    .circle {
      width: 200px !important;
      height: 200px !important;
    }
    &::before {
      width: 250px !important;
      height: 250px !important;
    }
  }
}

.login-box {
  width: 420px;
  padding: 48px 40px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.4), 0 0 0 1px rgba(255, 255, 255, 0.1);
  position: relative;
  z-index: 10;
}

.login-header {
  text-align: center;
  margin-bottom: 36px;

  .logo-icon {
    width: 80px;
    height: 80px;
    background: linear-gradient(135deg, var(--color-success), #059669);
    border-radius: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 16px;
    color: #fff;
    box-shadow: 0 10px 30px rgba(16, 185, 129, 0.4);
  }

  h1 {
    margin: 0 0 8px 0;
    font-size: 26px;
    font-weight: 600;
    background: linear-gradient(135deg, var(--color-success), #059669);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  p {
    margin: 0;
    color: var(--color-text-secondary);
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
    box-shadow: 0 0 0 1px var(--color-border);

    &:hover, &.is-focus {
      box-shadow: 0 0 0 2px var(--color-success);
    }
  }

  .login-btn {
    width: 100%;
    height: 48px;
    border-radius: 12px;
    font-size: 16px;
    font-weight: 500;
    background: linear-gradient(135deg, var(--color-success), #059669);
    border: none;
    box-shadow: 0 4px 15px rgba(16, 185, 129, 0.4);
    transition: all 0.3s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(16, 185, 129, 0.5);
    }
  }
}

.password-strength {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: -12px;
  margin-bottom: 16px;
  padding: 0 2px;

  .strength-bar {
    flex: 1;
    height: 4px;
    background: var(--color-border);
    border-radius: 2px;
    overflow: hidden;

    div {
      height: 100%;
      transition: all 0.3s;
      border-radius: 2px;
    }
  }

  span {
    font-size: 12px;
    font-weight: 500;
    min-width: 40px;
  }
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  color: var(--color-text-secondary);
  font-size: 14px;
}
</style>
