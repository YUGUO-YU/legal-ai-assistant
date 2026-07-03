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
          <el-icon :size="48"><Key /></el-icon>
        </div>
        <h1>忘记密码</h1>
        <p>输入用户名获取验证码</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
        @keyup.enter="handleGetCode"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
            :disabled="codeSent"
          />
        </el-form-item>

        <template v-if="codeSent">
          <el-alert
            :title="'验证码: ' + displayCode"
            type="success"
            :closable="false"
            show-icon
            class="code-alert"
          />
          <div class="code-hint">
            <el-icon><InfoFilled /></el-icon>
            <span>验证码同时输出在后台日志中</span>
          </div>

          <el-form-item prop="code">
            <el-input
              v-model="form.code"
              placeholder="请输入6位验证码"
              size="large"
              :prefix-icon="CircleCheck"
              maxlength="6"
            />
          </el-form-item>

          <el-form-item prop="newPassword">
            <el-input
              v-model="form.newPassword"
              type="password"
              placeholder="新密码（6-32字符）"
              size="large"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="确认新密码"
              size="large"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>
        </template>

        <el-form-item>
          <el-button
            v-if="!codeSent"
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleGetCode"
          >
            获取验证码
          </el-button>
          <el-button
            v-else
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleReset"
          >
            重置密码
          </el-button>
        </el-form-item>

        <el-form-item v-if="codeSent">
          <el-button
            text
            size="small"
            class="back-btn"
            @click="handleBack"
          >
            返回上一步
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <span>想起密码了？</span>
        <el-link type="primary" @click="$router.push('/')">立即登录</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key, CircleCheck, InfoFilled } from '@element-plus/icons-vue'
import api from '../api'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const codeSent = ref(false)
const serverCode = ref('')

onMounted(() => {
  if (localStorage.getItem('darkMode') === 'true') {
    document.documentElement.classList.add('dark')
  }
})

const form = reactive({
  username: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const displayCode = computed(() => serverCode.value || '------')

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码需在6-32个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleGetCode = async () => {
  if (!formRef.value) return
  await formRef.value.validateField('username', async (errorMsg) => {
    if (errorMsg) return
    loading.value = true
    try {
      const res = await api.auth.sendVerifyCode(form.username)
      serverCode.value = res.data.code
      codeSent.value = true
      ElMessage.success('验证码已生成，请查收')
    } catch (e) {
      console.error('获取验证码失败:', e)
      ElMessage.error(e?.response?.data?.message || e?.message || '获取验证码失败')
    } finally {
      loading.value = false
    }
  })
}

const handleReset = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await api.auth.resetPassword({
        username: form.username,
        code: form.code,
        newPassword: form.newPassword
      })
      ElMessage.success('密码重置成功，请使用新密码登录')
      router.push('/')
    } catch (e) {
      console.error('重置密码失败:', e)
      ElMessage.error(e?.response?.data?.message || e?.message || '重置失败')
    } finally {
      loading.value = false
    }
  })
}

const handleBack = () => {
  codeSent.value = false
  serverCode.value = ''
  form.code = ''
  form.newPassword = ''
  form.confirmPassword = ''
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

    &.circle-1 { width: 400px; height: 400px; top: -100px; left: -100px; animation-delay: 0s; }
    &.circle-2 { width: 300px; height: 300px; top: 50%; right: -100px; animation-delay: -5s; background: linear-gradient(135deg, rgba(254, 202, 87, 0.2), rgba(238, 90, 36, 0.2)); }
    &.circle-3 { width: 200px; height: 200px; bottom: -50px; left: 30%; animation-delay: -10s; background: linear-gradient(135deg, rgba(16, 185, 129, 0.2), rgba(59, 130, 246, 0.2)); }
    &.circle-4 { width: 150px; height: 150px; top: 20%; left: 20%; animation-delay: -15s; background: linear-gradient(135deg, rgba(139, 92, 246, 0.2), rgba(236, 72, 153, 0.2)); }
  }
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(50px, 50px) scale(1.1); }
  50% { transform: translate(0, 100px) scale(1); }
  75% { transform: translate(-50px, 50px) scale(0.9); }
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
    background: linear-gradient(135deg, #f59e0b, #d97706);
    border-radius: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 16px;
    color: #fff;
    box-shadow: 0 10px 30px rgba(245, 158, 11, 0.4);
  }

  h1 {
    margin: 0 0 8px 0;
    font-size: 26px;
    font-weight: 600;
    background: linear-gradient(135deg, #f59e0b, #d97706);
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
      box-shadow: 0 0 0 2px #f59e0b;
    }
  }

  .login-btn {
    width: 100%;
    height: 48px;
    border-radius: 12px;
    font-size: 16px;
    font-weight: 500;
    background: linear-gradient(135deg, #f59e0b, #d97706);
    border: none;
    box-shadow: 0 4px 15px rgba(245, 158, 11, 0.4);
    transition: all 0.3s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(245, 158, 11, 0.5);
    }
  }

  .back-btn {
    width: 100%;
    color: #9ca3af;
  }

  .code-alert {
    margin-bottom: 16px;
    border-radius: 10px;

    :deep(.el-alert__title) {
      font-size: 20px;
      font-family: 'JetBrains Mono', monospace;
      letter-spacing: 4px;
    }
  }

  .code-hint {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #9ca3af;
    font-size: 12px;
    margin-bottom: 20px;
    margin-top: -12px;
  }
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  color: #6b7280;
  font-size: 14px;
}
</style>
