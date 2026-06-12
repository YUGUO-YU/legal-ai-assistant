<template>
  <div class="page-card">
    <div class="page-header">
      <h2>个人设置</h2>
      <p>管理您的个人信息和偏好设置</p>
    </div>

    <el-row :gutter="24">
      <el-col :span="16">
        <el-tabs v-model="activeTab" class="settings-tabs">
          <el-tab-pane label="基本信息" name="profile">
            <el-form :model="profileForm" label-width="100px" :rules="profileRules" ref="profileFormRef">
              <el-form-item label="用户名">
                <el-input v-model="profileForm.username" disabled />
              </el-form-item>
              <el-form-item label="昵称" prop="nickname">
                <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="profileForm.email" placeholder="请输入邮箱">
                  <template #append>
                    <el-button @click="verifyEmail" :disabled="emailVerified">
                      {{ emailVerified ? '已验证' : '验证' }}
                    </el-button>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
              </el-form-item>
              <el-form-item label="个人简介">
                <el-input v-model="profileForm.bio" type="textarea" :rows="3" placeholder="介绍一下自己" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="saveProfile" :loading="saving">
                  保存修改
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="安全设置" name="security">
            <div class="security-section">
              <h4>修改密码</h4>
              <el-form :model="passwordForm" label-width="120px" class="password-form">
                <el-form-item label="当前密码">
                  <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
                </el-form-item>
                <el-form-item label="新密码">
                  <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
                </el-form-item>
                <el-form-item label="确认新密码">
                  <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="changePassword" :loading="changingPassword">
                    修改密码
                  </el-button>
                </el-form-item>
              </el-form>
            </div>

            <el-divider />

            <div class="security-section">
              <h4>两步验证</h4>
              <div class="setting-row">
                <div class="setting-info">
                  <span class="setting-title">手机验证码</span>
                  <span class="setting-desc">启用后，登录时需要输入手机验证码</span>
                </div>
                <el-switch v-model="twoFactorEnabled" />
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="偏好设置" name="preferences">
            <div class="preference-section">
              <h4>界面偏好</h4>
              <div class="setting-row">
                <div class="setting-info">
                  <span class="setting-title">深色模式</span>
                  <span class="setting-desc">开启后，界面将切换为深色主题</span>
                </div>
                <el-switch v-model="preferences.darkMode" @change="toggleDarkMode" />
              </div>

              <div class="setting-row">
                <div class="setting-info">
                  <span class="setting-title">简洁模式</span>
                  <span class="setting-desc">隐藏部分高级功能，简化界面</span>
                </div>
                <el-switch v-model="preferences.simpleMode" />
              </div>

              <div class="setting-row">
                <div class="setting-info">
                  <span class="setting-title">每页显示条数</span>
                  <span class="setting-desc">列表每页显示的默认条数</span>
                </div>
                <el-select v-model="preferences.pageSize" style="width: 120px">
                  <el-option :value="10" label="10条" />
                  <el-option :value="20" label="20条" />
                  <el-option :value="50" label="50条" />
                </el-select>
              </div>
            </div>

            <el-divider />

            <div class="preference-section">
              <h4>搜索偏好</h4>
              <div class="setting-row">
                <div class="setting-info">
                  <span class="setting-title">搜索历史</span>
                  <span class="setting-desc">自动保存搜索历史</span>
                </div>
                <el-switch v-model="preferences.saveSearchHistory" />
              </div>

              <div class="setting-row">
                <div class="setting-info">
                  <span class="setting-title">智能提示</span>
                  <span class="setting-desc">输入时显示搜索建议</span>
                </div>
                <el-switch v-model="preferences.smartSuggestions" />
              </div>
            </div>

            <el-divider />

            <div class="preference-section">
              <h4>通知设置</h4>
              <div class="setting-row">
                <div class="setting-info">
                  <span class="setting-title">邮件通知</span>
                  <span class="setting-desc">接收系统邮件通知</span>
                </div>
                <el-switch v-model="preferences.emailNotification" />
              </div>

              <div class="setting-row">
                <div class="setting-info">
                  <span class="setting-title">会话保存</span>
                  <span class="setting-desc">自动保存文档问答会话</span>
                </div>
                <el-switch v-model="preferences.saveSessions" />
              </div>
            </div>

            <el-form-item style="margin-top: 24px">
              <el-button type="primary" @click="savePreferences" :loading="savingPrefs">
                保存偏好设置
              </el-button>
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane label="数据管理" name="data">
            <div class="data-section">
              <h4>搜索历史</h4>
              <div class="data-item">
                <div class="data-info">
                  <span class="data-title">搜索历史记录</span>
                  <span class="data-desc">{{ searchHistoryCount }} 条记录</span>
                </div>
                <el-button @click="clearSearchHistory" :loading="clearing">
                  清除历史
                </el-button>
              </div>

              <div class="data-item">
                <div class="data-info">
                  <span class="data-title">会话记录</span>
                  <span class="data-desc">{{ sessionCount }} 条记录</span>
                </div>
                <el-button @click="clearSessions" :loading="clearingSessions">
                  清除会话
                </el-button>
              </div>
            </div>

            <el-divider />

            <div class="data-section">
              <h4>数据导出</h4>
              <div class="data-item">
                <div class="data-info">
                  <span class="data-title">导出个人数据</span>
                  <span class="data-desc">导出您的所有个人数据到文件</span>
                </div>
                <el-button type="primary" @click="exportData">
                  导出数据
                </el-button>
              </div>
            </div>

            <el-divider />

            <div class="data-section danger-zone">
              <h4>危险区域</h4>
              <div class="data-item">
                <div class="data-info">
                  <span class="data-title">注销账户</span>
                  <span class="data-desc">永久删除您的账户和所有数据，此操作不可恢复</span>
                </div>
                <el-button type="danger" @click="showDeleteConfirm">
                  注销账户
                </el-button>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-col>

      <el-col :span="8">
        <el-card class="user-card">
          <div class="user-header">
            <el-avatar :size="80" :icon="User" />
            <div class="user-info">
              <h3>{{ userInfo.nickname || userInfo.username }}</h3>
              <p>{{ userInfo.email }}</p>
              <el-tag size="small">{{ userInfo.role === 'lawyer' ? '律师' : '用户' }}</el-tag>
            </div>
          </div>
          <div class="user-stats">
            <div class="stat-item">
              <span class="stat-value">{{ stats.searchCount }}</span>
              <span class="stat-label">搜索次数</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ stats.docCount }}</span>
              <span class="stat-label">文档数</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ stats.sessionCount }}</span>
              <span class="stat-label">会话数</span>
            </div>
          </div>
        </el-card>

        <el-card class="info-card" style="margin-top: 16px">
          <template #header>
            <span>快捷操作</span>
          </template>
          <div class="quick-actions">
            <el-button size="small" @click="$router.push('/knowledge-base')">
              <el-icon><Folder /></el-icon> 管理知识库
            </el-button>
            <el-button size="small" @click="$router.push('/doc-qa')">
              <el-icon><ChatDotRound /></el-icon> 文档问答
            </el-button>
            <el-button size="small" @click="$router.push('/document')">
              <el-icon><Document /></el-icon> 文书起草
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="showDeleteDialog" title="注销账户确认" width="500px">
      <el-alert type="error" :closable="false" show-icon>
        <template #title>
          警告：此操作不可逆
        </template>
        <template #default>
          注销账户将永久删除您的所有数据，包括搜索历史、会话记录和个人设置。
          此操作无法撤销。
        </template>
      </el-alert>
      <div style="margin-top: 16px">
        <el-input v-model="deleteConfirmText" placeholder='请输入"注销"以确认' />
      </div>
      <template #footer>
        <el-button @click="showDeleteDialog = false">取消</el-button>
        <el-button type="danger" :disabled="deleteConfirmText !== '注销'" @click="deleteAccount">
          确认注销
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const activeTab = ref('profile')
const profileFormRef = ref(null)
const saving = ref(false)
const savingPrefs = ref(false)
const changingPassword = ref(false)
const clearing = ref(false)
const clearingSessions = ref(false)
const emailVerified = ref(false)
const showDeleteDialog = ref(false)
const deleteConfirmText = ref('')

const userInfo = reactive({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  bio: '',
  role: 'lawyer'
})

const profileForm = reactive({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  bio: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const preferences = reactive({
  darkMode: false,
  simpleMode: false,
  pageSize: 20,
  saveSearchHistory: true,
  smartSuggestions: true,
  emailNotification: true,
  saveSessions: true
})

const stats = reactive({
  searchCount: 156,
  docCount: 23,
  sessionCount: 8
})

const searchHistoryCount = ref(89)
const sessionCount = ref(12)

const profileRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度在2-20个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }
  ]
}

const loadUserInfo = () => {
  const saved = localStorage.getItem('userInfo')
  if (saved) {
    const info = JSON.parse(saved)
    Object.assign(userInfo, info)
    Object.assign(profileForm, {
      username: info.username || '',
      nickname: info.nickname || info.username || '',
      email: info.email || '',
      phone: info.phone || '',
      bio: info.bio || ''
    })
  }
}

const loadPreferences = () => {
  const saved = localStorage.getItem('preferences')
  if (saved) {
    Object.assign(preferences, JSON.parse(saved))
  }
}

const saveProfile = async () => {
  await profileFormRef.value?.validate(async (valid) => {
    if (!valid) return

    saving.value = true
    await new Promise(r => setTimeout(r, 1000))

    Object.assign(userInfo, {
      nickname: profileForm.nickname,
      email: profileForm.email,
      phone: profileForm.phone,
      bio: profileForm.bio
    })

    localStorage.setItem('userInfo', JSON.stringify(userInfo))
    ElMessage.success('个人信息已保存')
    saving.value = false
  })
}

const verifyEmail = () => {
  ElMessage.info('验证码已发送到邮箱，请查收')
}

const changePassword = async () => {
  if (!passwordForm.oldPassword) {
    ElMessage.warning('请输入当前密码')
    return
  }
  if (!passwordForm.newPassword || passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码长度不能少于6位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  changingPassword.value = true
  await new Promise(r => setTimeout(r, 1500))

  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  changingPassword.value = false
  ElMessage.success('密码修改成功')
}

const toggleDarkMode = () => {
  document.body.classList.toggle('dark-mode', preferences.darkMode)
}

const savePreferences = async () => {
  savingPrefs.value = true
  await new Promise(r => setTimeout(r, 500))

  localStorage.setItem('preferences', JSON.stringify(preferences))
  ElMessage.success('偏好设置已保存')
  savingPrefs.value = false
}

const clearSearchHistory = async () => {
  try {
    await ElMessageBox.confirm('确定要清除所有搜索历史吗？', '确认清除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    clearing.value = true
    await new Promise(r => setTimeout(r, 1000))

    searchHistoryCount.value = 0
    localStorage.removeItem('recentSearches')
    ElMessage.success('搜索历史已清除')
    clearing.value = false
  } catch (e) {
    // 用户取消
  }
}

const clearSessions = async () => {
  try {
    await ElMessageBox.confirm('确定要清除所有会话记录吗？', '确认清除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    clearingSessions.value = true
    await new Promise(r => setTimeout(r, 1000))

    sessionCount.value = 0
    ElMessage.success('会话记录已清除')
    clearingSessions.value = false
  } catch (e) {
    // 用户取消
  }
}

const exportData = () => {
  ElMessage.info('数据导出功能开发中...')
}

const showDeleteConfirm = () => {
  showDeleteDialog.value = true
  deleteConfirmText.value = ''
}

const deleteAccount = async () => {
  ElMessage.error('账户注销功能暂时关闭，请联系管理员')
  showDeleteDialog.value = false
}

onMounted(() => {
  loadUserInfo()
  loadPreferences()
})
</script>

<style lang="scss" scoped>
.settings-tabs {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
}

.security-section, .preference-section, .data-section {
  h4 {
    margin: 0 0 16px 0;
    font-size: 16px;
    color: #333;
  }
}

.setting-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;

  .setting-info {
    flex: 1;
    .setting-title {
      display: block;
      font-size: 14px;
      color: #333;
    }
    .setting-desc {
      display: block;
      font-size: 12px;
      color: #999;
      margin-top: 4px;
    }
  }
}

.data-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  margin-bottom: 12px;

  .data-info {
    .data-title {
      display: block;
      font-size: 14px;
      color: #333;
    }
    .data-desc {
      display: block;
      font-size: 12px;
      color: #999;
      margin-top: 4px;
    }
  }
}

.danger-zone {
  h4 { color: #ff4d4f; }
}

.user-card {
  .user-header {
    display: flex;
    gap: 16px;
    align-items: center;
    padding-bottom: 16px;
    border-bottom: 1px solid #f0f0f0;

    .user-info {
      h3 {
        margin: 0 0 4px 0;
        font-size: 18px;
      }
      p {
        margin: 0 0 8px 0;
        color: #666;
        font-size: 13px;
      }
    }
  }

  .user-stats {
    display: flex;
    justify-content: space-around;
    padding-top: 16px;

    .stat-item {
      text-align: center;
      .stat-value {
        display: block;
        font-size: 20px;
        font-weight: bold;
        color: #1890ff;
      }
      .stat-label {
        font-size: 12px;
        color: #999;
      }
    }
  }
}

.info-card {
  .quick-actions {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .el-button {
      justify-content: flex-start;
    }
  }
}

.password-form {
  max-width: 400px;
}
</style>