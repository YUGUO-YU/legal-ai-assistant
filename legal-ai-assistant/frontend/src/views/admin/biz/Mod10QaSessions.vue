<template>
  <div class="qa-sessions-page">
    <div class="page-header">
      <div class="header-content"><h2>问答会话监控</h2><p>MOD-10 · 文件问答 / 消息统计 / 状态</p></div>
      <div class="header-actions"><el-button :icon="Refresh" @click="load">刷新</el-button></div>
    </div>
    <el-card>
      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="session_uuid" label="UUID" width="160"><template #default="{row}"><span class="mono">{{row.session_uuid?.substring(0,12)}}</span></template></el-table-column>
        <el-table-column prop="title" label="标题" min-width="260" show-overflow-tooltip />
        <el-table-column prop="user_id" label="用户" width="100" />
        <el-table-column prop="kb_id" label="知识库ID" width="90" />
        <el-table-column label="消息数" width="80"><template #default="{row}"><el-tag size="small" type="primary" effect="plain">{{row.msg_count||0}}</el-tag></template></el-table-column>
        <el-table-column label="状态" width="90"><template #default="{row}"><el-tag :type="row.status===1?'success':'info'" size="small">{{row.status===1?'活跃':'结束'}}</el-tag></template></el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="170" />
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,sizes,prev,pager,next" :page-sizes="[10,20,50]" class="pager" />
    </el-card>
  </div>
</template>
<script setup>
import {ref,watch,onMounted} from 'vue'
import {Refresh} from '@element-plus/icons-vue'
import api from '../../../api'
const rows=ref([]);const total=ref(0);const loading=ref(false);const page=ref(1);const pageSize=ref(20)
async function load(){loading.value=true;try{const res=await api.get('/admin/biz/mod10/qa-sessions',{params:{page:page.value,pageSize:pageSize.value}});rows.value=res.data?.list||[];total.value=res.data?.total||rows.value.length}catch(e){rows.value=[];total.value=0}finally{loading.value=false}}
watch([page,pageSize],load)
onMounted(load)
</script>
<style lang="scss" scoped>
.qa-sessions-page{animation:fadeIn .4s ease;padding:0 4px}to{opacity:1;transform:translateY(0)}}.page-header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:16px}.header-content h2{margin:0 0 6px;font-size:22px;font-weight:600}.header-content p{margin:0;color:#64748b;font-size:13px}.header-actions{display:flex;gap:8px;align-items:center}.mono{font-family:'Cascadia Code','Consolas',monospace;font-size:12px}.pager{margin-top:14px;justify-content:flex-end;display:flex}
</style>