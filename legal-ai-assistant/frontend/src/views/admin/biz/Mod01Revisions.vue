<template>
  <div class="revisions-page">
    <div class="page-header">
      <div class="header-content">
        <h2>法规修订追溯</h2>
        <p>MOD-01 · 修订类型 / 日期 / 来源URL</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>
    <el-card>
      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="law_id" label="法规ID" width="90" />
        <el-table-column prop="revision_no" label="修订编号" width="120">
          <template #default="{ row }"><el-tag size="small" type="primary">{{ row.revision_no }}</el-tag></template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="revTag(row.revision_type)">{{ revLabel(row.revision_type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="revision_date" label="修订日期" width="120" />
        <el-table-column prop="revision_note" label="修订说明" min-width="300" show-overflow-tooltip />
        <el-table-column prop="source_url" label="来源" min-width="200" show-overflow-tooltip />
        <el-table-column prop="created_at" label="入库时间" width="170" />
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,sizes,prev,pager,next" :page-sizes="[10,20,50]" class="pager" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import api from '../../../api'
const rows = ref([]); const total = ref(0); const loading = ref(false); const page = ref(1); const pageSize = ref(20)
function revLabel(t) { return ({1:'修订',2:'废止',3:'重新发布',4:'部分修正'}[t]||t) }
function revTag(t) { return ({1:'warning',2:'danger',3:'success',4:'info'}[t]||'') }
async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/law_revision/list',{params:{page:page.value,pageSize:pageSize.value}})
    rows.value = res.data?.list||[]; total.value = res.data?.total||rows.value.length
  } catch(e){rows.value=[];total.value=0} finally{loading.value=false}
}
watch([page,pageSize],load)
onMounted(load)
</script>
<style lang="scss" scoped>
.revisions-page{animation:fadeIn .4s ease;padding:0 4px}@keyframes fadeIn{from{opacity:0;transform:translateY(10px)}to{opacity:1;transform:translateY(0)}}.page-header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:16px}.header-content h2{margin:0 0 6px;font-size:22px;font-weight:600}.header-content p{margin:0;color:#64748b;font-size:13px}.pager{margin-top:14px;justify-content:flex-end;display:flex}
</style>