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
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="管理员账号"
            size="large"
            :prefix-icon="User"
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
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="loading"
            @click="handleLogin"
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

        <div class="hint-box">
        <el-icon><InfoFilled /></el-icon>
        <span>默认账号: admin / admin</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, ArrowLeft, InfoFilled } from '@element-plus/icons-vue'
import api from '../api'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  console.log('handleLogin called, formRef:', formRef.value)
  if (!formRef.value) {
    console.log('formRef is null')
    return
  }

  try {
    console.log('Validating form...')
    await formRef.value.validate()
    console.log('Form validation passed')
  } catch (e) {
    console.log('Form validation failed:', e)
    return
  }

  console.log('Making API call to /auth/admin/login')
  loading.value = true
  try {
    const res = await api.post('/auth/admin/login', {
      username: form.username,
      password: form.password
    })
    console.log('Login success:', res)
    localStorage.setItem('admin_token', res.data.token)
    localStorage.setItem('admin_user', JSON.stringify(res.data.userInfo))
    ElMessage.success('登录成功')
    router.push('/admin')
  } catch (e) {
    console.log('Login failed:', e)
    console.log('Response data:', e?.response?.data)
    const msg = e?.response?.data?.error || e?.response?.data?.message || '登录失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
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
  margin-top: 24px;
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
</style>
