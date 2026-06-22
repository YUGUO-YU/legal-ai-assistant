<template>
  <div class="case-search-page">
    <div class="page-header">
      <div class="header-content"><h2>案例检索日志</h2><p>MOD-06 · 判例主数据 / 法院 / 案由 / 日期</p></div>
      <div class="header-actions"><el-button :icon="Refresh" @click="load">刷新</el-button></div>
    </div>
    <el-card class="filter-card">
      <el-form inline>
        <el-form-item label="案由"><el-input v-model="filter.cause" placeholder="案由关键词" clearable style="width:160px" @keyup.enter="load"/></el-form-item>
        <el-form-item><el-button type="primary" @click="load">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
    </el-card>
    <el-card>
      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="case_uuid" label="UUID" width="140"><template #default="{row}"><span class="mono">{{row.case_uuid?.substring(0,12)}}</span></template></el-table-column>
        <el-table-column prop="case_no" label="案号" width="170" show-overflow-tooltip />
        <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
        <el-table-column prop="case_type" label="类型" width="90"><template #default="{row}"><el-tag size="small">{{row.case_type}}</el-tag></template></el-table-column>
        <el-table-column prop="case_cause" label="案由" width="120" show-overflow-tooltip />
        <el-table-column prop="court" label="法院" min-width="160" show-overflow-tooltip />
        <el-table-column prop="judgment_date" label="裁判日期" width="110" />
        <el-table-column label="操作" width="100" fixed="right"><template #default="{row}"><el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,sizes,prev,pager,next" :page-sizes="[10,20,50]" class="pager" />
    </el-card>
    <el-drawer v-model="showDetail" title="判例详情" size="55%" direction="rtl">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="ID">{{detail.id}}</el-descriptions-item>
        <el-descriptions-item label="案号">{{detail.case_no}}</el-descriptions-item>
        <el-descriptions-item label="标题">{{detail.title}}</el-descriptions-item>
        <el-descriptions-item label="法院">{{detail.court}}</el-descriptions-item>
        <el-descriptions-item label="案由">{{detail.case_cause}}</el-descriptions-item>
        <el-descriptions-item label="裁判日期">{{detail.judgment_date}}</el-descriptions-item>
        <el-descriptions-item label="来源">{{detail.source_name}} <a v-if="detail.source_url" :href="detail.source_url" target="_blank">链接</a></el-descriptions-item>
      </el-descriptions>
      <h4 style="margin-top:16px">裁判摘要</h4>
      <pre class="content-preview">{{detail.summary||'-'}}</pre>
    </el-drawer>
  </div>
</template>
<script setup>
import {ref,reactive,watch,onMounted} from 'vue'
import {Refresh} from '@element-plus/icons-vue'
import api from '../../api'
const rows=ref([]);const total=ref(0);const loading=ref(false);const page=ref(1);const pageSize=ref(20)
const filter=reactive({cause:''});const showDetail=ref(false);const detail=ref(null)
async function load(){loading.value=true;try{const res=await api.get('/admin/biz/mod06/case-search-logs',{params:{page:page.value,pageSize:pageSize.value}});rows.value=res.data?.list||[];total.value=res.data?.total||rows.value.length}catch(e){rows.value=[];total.value=0}finally{loading.value=false}}
function openDetail(row){detail.value=row;showDetail.value=true}
function reset(){filter.cause='';load()}
watch([page,pageSize],load)
onMounted(load)
</script>
<style lang="scss" scoped>
.case-search-page{animation:fadeIn .4s ease;padding:0 4px}@keyframes fadeIn{from{opacity:0;transform:translateY(10px)}to{opacity:1;transform:translateY(0)}}.page-header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:16px}.header-content h2{margin:0 0 6px;font-size:22px;font-weight:600}.header-content p{margin:0;color:#64748b;font-size:13px}.filter-card{margin-bottom:16px}.mono{font-family:'Cascadia Code','Consolas',monospace;font-size:12px}.pager{margin-top:14px;justify-content:flex-end;display:flex}.content-preview{background:#f8fafc;padding:16px;border-radius:8px;white-space:pre-wrap;word-break:break-word;font-size:13px;line-height:1.6;border:1px solid #e2e8f0;max-height:40vh;overflow-y:auto}
</style>