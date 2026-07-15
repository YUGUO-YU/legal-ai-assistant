<template>
  <div class="gray-page">
    <div class="page-header">
      <div class="header-content">
        <h2>Prompt 灰度发布记录</h2>
        <p>AI 域 · 版本对比 · 灰度日志 · A/B 实验回滚</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="prompt_code" label="代码" width="140" />
        <el-table-column prop="version" label="版本" width="90" />
        <el-table-column label="比例" width="100">
          <template #default="{ row }">
            <el-progress :percentage="Number(row.gray_ratio)" :status="row.status === 'active' ? 'success' : 'exception'" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : row.status === 'rollback' ? 'warning' : 'info'" size="small">{{ labelStatus(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="target_teams" label="白名单" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.target_teams">{{ row.target_teams }}</span>
            <span v-else class="text-muted">全量 {{ row.gray_ratio }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="started_at" label="开始时间" width="170" />
        <el-table-column prop="ended_at" label="结束/回滚时间" width="170">
          <template #default="{ row }">
            <span v-if="row.ended_at">{{ row.ended_at }}</span>
            <el-tag v-else size="small" type="success">运行中</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rollback_reason" label="回滚原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        class="pager"
      />
    </el-card>

    <el-drawer v-model="showDetail" title="灰度发布详情" size="50%" direction="rtl">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="代码">{{ detail.prompt_code }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ detail.version }}</el-descriptions-item>
        <el-descriptions-item label="比例">{{ detail.gray_ratio }}%</el-descriptions-item>
        <el-descriptions-item label="目标团队">{{ detail.target_teams || '全量' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 'active' ? 'success' : 'warning'" size="small">{{ labelStatus(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ detail.started_at }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ detail.ended_at || '-' }}</el-descriptions-item>
        <el-descriptions-item label="回滚原因">{{ detail.rollback_reason || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import api from '../../../api'

const rows = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const showDetail = ref(false)
const detail = ref(null)

function labelStatus(s) {
  return { active: '灰度中', rollback: '已回滚', ended: '已结束', cancelled: '已取消' }[s] || s || '-'
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/ai/gray-releases', { params: { page: page.value, pageSize: pageSize.value } })
    rows.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    rows.value = []; total.value = 0
  } finally {
    loading.value = false
  }
}

function openDetail(row) {
  detail.value = row
  showDetail.value = true
}

watch([page, pageSize], load)
onMounted(load)
</script>

<style lang="scss" scoped>
.gray-page { animation: adminFadeIn 0.4s ease; padding: 0 4px; }
 to { opacity: 1; transform: translateY(0); } }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: var(--color-text-muted); font-size: 13px; }
.text-muted { color: var(--color-text-muted); font-size: 12px; }
.pager { margin-top: 14px; justify-content: flex-end; display: flex; }
</style>