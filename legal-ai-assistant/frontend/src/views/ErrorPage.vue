<template>
  <div class="error-page">
    <div class="error-container">
      <div class="error-code">{{ code }}</div>
      <h1 class="error-title">{{ title }}</h1>
      <p class="error-description">{{ description }}</p>

      <div class="error-actions">
        <button @click="goBack" class="btn-back">
          <el-icon><ArrowLeft /></el-icon>
          返回上页
        </button>
        <button @click="goHome" class="btn-home">
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </button>
      </div>

      <div class="error-suggestions" v-if="suggestions.length">
        <h3>您可以尝试：</h3>
        <ul>
          <li v-for="suggestion in suggestions" :key="suggestion">
            {{ suggestion }}
          </li>
        </ul>
      </div>
    </div>

    <div class="error-decoration">
      <div class="decoration-circle"></div>
      <div class="decoration-circle"></div>
      <div class="decoration-circle"></div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { HomeFilled, ArrowLeft } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const code = computed(() => route.params.code || '404')

const title = computed(() => {
  const titles = {
    '400': '请求错误',
    '401': '未授权',
    '403': '禁止访问',
    '404': '页面未找到',
    '500': '服务器错误',
    '502': '网关错误',
    '503': '服务不可用'
  }
  return titles[code.value] || '出错了'
})

const description = computed(() => {
  const descriptions = {
    '400': '您发送的请求有误，请检查后重试',
    '401': '您没有权限访问该页面，请先登录',
    '403': '抱歉，您没有权限访问该页面',
    '404': '抱歉，您访问的页面不存在或已迁移',
    '500': '抱歉，服务器遇到了问题，请稍后重试',
    '502': '网关收到了无效响应，请稍后重试',
    '503': '服务暂时不可用，请稍后重试'
  }
  return descriptions[code.value] || '发生了未知错误'
})

const suggestions = computed(() => {
  if (code.value === '404') {
    return [
      '检查 URL 是否正确',
      '返回首页浏览其他内容',
      '如果问题持续存在，请联系管理员'
    ]
  }
  if (code.value === '500' || code.value === '502' || code.value === '503') {
    return [
      '刷新页面重试',
      '稍后再试',
      '如果问题持续存在，请联系管理员'
    ]
  }
  return []
})

const goBack = () => {
  router.back()
}

const goHome = () => {
  router.push('/')
}
</script>

<style lang="scss" scoped>
.error-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.error-container {
  text-align: center;
  z-index: 1;
  color: #fff;
}

.error-code {
  font-size: 120px;
  font-weight: 700;
  opacity: 0.2;
  line-height: 1;
  margin-bottom: -40px;
}

.error-title {
  font-size: 32px;
  font-weight: 600;
  margin-bottom: 16px;
}

.error-description {
  font-size: 16px;
  opacity: 0.9;
  margin-bottom: 40px;
}

.error-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-bottom: 40px;
}

.btn-back,
.btn-home {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 28px;
  font-size: 15px;
  font-weight: 500;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid rgba(255,255,255,0.3);
  background: rgba(255,255,255,0.1);
  color: #fff;

  &:hover {
    background: rgba(255,255,255,0.2);
    border-color: rgba(255,255,255,0.5);
  }
}

.error-suggestions {
  background: rgba(0,0,0,0.2);
  border-radius: 12px;
  padding: 24px 32px;
  text-align: left;

  h3 {
    font-size: 14px;
    margin-bottom: 12px;
    opacity: 0.9;
  }

  ul {
    margin: 0;
    padding-left: 20px;

    li {
      font-size: 13px;
      opacity: 0.8;
      margin-bottom: 8px;

      &:last-child {
        margin-bottom: 0;
      }
    }
  }
}

.error-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;

  .decoration-circle {
    position: absolute;
    border-radius: 50%;
    background: rgba(255,255,255,0.1);

    &:nth-child(1) {
      width: 300px;
      height: 300px;
      top: -100px;
      right: -100px;
    }

    &:nth-child(2) {
      width: 200px;
      height: 200px;
      bottom: -50px;
      left: -50px;
    }

    &:nth-child(3) {
      width: 150px;
      height: 150px;
      top: 50%;
      left: 20%;
    }
  }
}
</style>
