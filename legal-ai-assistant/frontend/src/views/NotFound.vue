<template>
  <div class="not-found-container">
    <div class="not-found-content">
      <div class="error-code">404</div>
      <h1>页面未找到</h1>
      <p>抱歉，您访问的页面不存在或已被删除</p>
      <div class="actions">
        <el-button type="primary" @click="goHome">
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </el-button>
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回上一页
        </el-button>
      </div>
      <div class="suggestions">
        <h3>您可以：</h3>
        <ul>
          <li><router-link to="/">返回首页</router-link></li>
          <li><router-link to="/legal-search">使用AI搜法查找法律信息</router-link></li>
          <li><router-link to="/doc-qa">尝试AI文件问答</router-link></li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { HomeFilled, ArrowLeft } from '@element-plus/icons-vue'

const router = useRouter()

const goHome = () => {
  router.push('/')
}

const goBack = () => {
  router.back()
}
</script>

<style lang="scss" scoped>
.not-found-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.not-found-content {
  text-align: center;
  color: #fff;

  .error-code {
    font-size: 120px;
    font-weight: bold;
    opacity: 0.3;
    line-height: 1;
    margin-bottom: 16px;
  }

  h1 {
    font-size: 32px;
    margin: 0 0 16px 0;
  }

  p {
    font-size: 16px;
    opacity: 0.8;
    margin: 0 0 32px 0;
  }

  .actions {
    display: flex;
    gap: 16px;
    justify-content: center;
    margin-bottom: 48px;
  }

  .suggestions {
    background: rgba(255,255,255,0.1);
    border-radius: 16px;
    padding: 24px 48px;
    display: inline-block;
    text-align: left;

    h3 {
      margin: 0 0 16px 0;
      font-size: 16px;
    }

    ul {
      margin: 0;
      padding: 0;
      list-style: none;

      li {
        padding: 8px 0;
        a {
          color: #fff;
          text-decoration: none;
          &:hover {
            text-decoration: underline;
          }
        }
      }
    }
  }
}
</style>