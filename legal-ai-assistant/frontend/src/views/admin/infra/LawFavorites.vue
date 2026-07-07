<template>
  <div class="law-favorites-page">
    <div class="page-header">
      <div class="header-content">
        <h2>法规收藏管理</h2>
        <p>查看和管理所有用户的法规收藏记录</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="14" class="kpi-row">
      <el-col :xs="12" :sm="6">
        <el-card class="kpi-card" :body-style="{ padding: '14px' }">
          <div class="kpi-label">总收藏数</div>
          <div class="kpi-value">{{ stats.totalFavorites ?? '-' }}</div>
          <div class="kpi-foot">全部收藏记录</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="kpi-card today" :body-style="{ padding: '14px' }">
          <div class="kpi-label">今日新增</div>
          <div class="kpi-value">{{ stats.todayNew ?? '-' }}</div>
          <div class="kpi-foot">今日收藏</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="kpi-card article" :body-style="{ padding: '14px' }">
          <div class="kpi-label">收藏最多法规</div>
          <div class="kpi-value">{{ topArticleCount }}</div>
          <div class="kpi-foot">{{ topArticleTitle }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="kpi-card user" :body-style="{ padding: '14px' }">
          <div class="kpi-label">收藏最多用户</div>
          <div class="kpi-value">{{ topUserCount }}</div>
          <div class="kpi-foot">{{ topUserName }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>收藏列表</span>
          <div class="filter-area">
            <el-input v-model="filter.userId" placeholder="用户ID" size="small" style="width: 120px; margin-right: 8px;" clearable @keyup.enter="loadList" />
            <el-input v-model="filter.username" placeholder="用户名" size="small" style="width: 120px; margin-right: 8px;" clearable @keyup.enter="loadList" />
            <el-input v-model="filter.articleId" placeholder="法规ID" size="small" style="width: 120px; margin-right: 8px;" clearable @keyup.enter="loadList" />
            <el-button size="small" type="primary" @click="loadList">查询</el-button>
          </div>
        </div>
      </template>
      <el-table :data="list" stripe size="small" v-loading="loading" max-height="400">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="user_id" label="用户ID" width="100" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="article_id" label="法规ID" width="100" />
        <el-table-column prop="article_title" label="法规标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="created_at" label="收藏时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" text @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          :background="true"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../api'

const stats = ref({})
const list = ref([])
const loading = ref(false)
const filter = ref({
  userId: null,
  username: '',
  articleId: null
})
const pagination = ref({
  page: 1,
  pageSize: 20,
  total: 0
})

const topArticleCount = computed(() => {
  const top = stats.value.topArticles
  if (!top || top.length === 0) return 0
  return top[0].favorite_count || 0
})

const topArticleTitle = computed(() => {
  const top = stats.value.topArticles
  if (!top || top.length === 0) return '-'
  return (top[0].article_title || top[0].article_id) || '-'
})

const topUserCount = computed(() => {
  const top = stats.value.topUsers
  if (!top || top.length === 0) return 0
  return top[0].favorite_count || 0
})

const topUserName = computed(() => {
  const top = stats.value.topUsers
  if (!top || top.length === 0) return '-'
  return top[0].username || top[0].user_id || '-'
})

async function loadStats() {
  try {
    const res = await api.get('/admin/infra/law-favorites/stats')
    stats.value = res.data || {}
  } catch (e) {
    stats.value = {}
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await api.get('/admin/infra/law-favorites', {
      params: {
        userId: filter.value.userId || undefined,
        username: filter.value.username || undefined,
        articleId: filter.value.articleId || undefined,
        page: pagination.value.page,
        pageSize: pagination.value.pageSize
      }
    })
    list.value = res.data?.list || []
    pagination.value.total = res.data?.total || 0
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除这条收藏记录吗？用户「${row.username}」收藏的「${row.article_title || row.article_id}」将被移除。`,
      '删除确认',
      { type: 'warning' }
    )
    const res = await api.delete(`/admin/infra/law-favorites/${row.id}`)
    if (res.data?.ok) {
      ElMessage.success('删除成功')
      await loadAll()
    } else {
      ElMessage.error(res.data?.error || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

async function loadAll() {
  await Promise.all([loadStats(), loadList()])
}

onMounted(() => {
  loadAll()
})
</script>

<style lang="scss" scoped>
.law-favorites-page { animation: fadeIn 0.4s ease; }

@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;

  .header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
  .header-content p { margin: 0; color: var(--color-text-secondary); font-size: 13px; }
}

.kpi-row { margin-bottom: 14px; }

.kpi-card {
  border-left: 4px solid var(--color-border-dark);
  transition: transform 0.2s;

  &:hover { transform: translateY(-2px); }

  &.today { border-left-color: var(--color-success); }
  &.article { border-left-color: var(--color-info); }
  &.user { border-left-color: #8b5cf6; }

  .kpi-label { font-size: 12px; color: var(--color-text-secondary); }
  .kpi-value { font-size: 24px; font-weight: 700; color: var(--color-text-primary); margin: 4px 0; }
  .kpi-foot { font-size: 11px; color: var(--color-text-placeholder); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .filter-area {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
  }
}

.table-card {
  :deep(.el-card__header) {
    padding-bottom: 8px;
  }
}
</style>
