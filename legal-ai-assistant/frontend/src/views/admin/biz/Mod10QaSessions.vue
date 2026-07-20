<template>
  <div class="qa-sessions-page">
    <div class="page-header">
      <div class="header-content"><h2 class="gradient-text">问答会话监控</h2><p>MOD-10 · 文件问答 / 消息统计 / 状态</p></div>
      <div class="header-actions"><el-button :icon="Refresh" @click="load">刷新</el-button></div>
    </div>
    <el-card class="glass table-card">
      <el-table :data="rows" v-loading="loading" stripe border row-key="id" @row-click="openDetail">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="session_uuid" label="UUID" width="160"><template #default="{row}"><span class="mono">{{row.session_uuid?.substring(0,12)}}</span></template></el-table-column>
        <el-table-column prop="title" label="标题" min-width="260" show-overflow-tooltip />
        <el-table-column prop="user_id" label="用户" width="100" />
        <el-table-column prop="kb_id" label="知识库ID" width="90" />
        <el-table-column label="消息数" width="80"><template #default="{row}"><el-tag size="small" type="primary" effect="plain">{{row.msg_count||0}}</el-tag></template></el-table-column>
        <el-table-column label="状态" width="90"><template #default="{row}"><el-tag :type="row.status===1?'success':'info'" size="small">{{row.status===1?'活跃':'结束'}}</el-tag></template></el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{row}">
            <el-button type="primary" link size="small" @click.stop="openDetail(row)">详情</el-button>
            <el-button type="danger" link size="small" @click.stop="confirmDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,sizes,prev,pager,next" :page-sizes="[10,20,50]" class="pager" />
    </el-card>

    <el-drawer v-model="drawerVisible" title="会话详情" size="600px" direction="rtl">
      <template v-if="currentSession">
        <div class="session-info">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="ID">{{currentSession.id}}</el-descriptions-item>
            <el-descriptions-item label="状态"><el-tag :type="currentSession.status===1?'success':'info'" size="small">{{currentSession.status===1?'活跃':'结束'}}</el-tag></el-descriptions-item>
            <el-descriptions-item label="用户ID">{{currentSession.user_id}}</el-descriptions-item>
            <el-descriptions-item label="知识库ID">{{currentSession.kb_id}}</el-descriptions-item>
            <el-descriptions-item label="知识库名称" :span="2">{{currentSession.kb_name || '-'}}</el-descriptions-item>
            <el-descriptions-item label="标题" :span="2">{{currentSession.title || '-'}}</el-descriptions-item>
            <el-descriptions-item label="会话UUID" :span="2"><span class="mono">{{currentSession.session_uuid}}</span></el-descriptions-item>
            <el-descriptions-item label="消息数">{{currentSession.msg_count || 0}}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{currentSession.created_at}}</el-descriptions-item>
            <el-descriptions-item label="更新时间" :span="2">{{currentSession.updated_at}}</el-descriptions-item>
          </el-descriptions>
          <div class="drawer-actions">
            <el-button type="primary" @click="exportSession(currentSession)">导出JSON</el-button>
            <el-button type="danger" @click="confirmDelete(currentSession)">删除会话</el-button>
          </div>
        </div>
        <el-divider content-position="left">消息历史</el-divider>
        <div class="message-list" v-loading="msgLoading">
          <div v-if="messages.length === 0 && !msgLoading" class="empty-messages">暂无消息记录</div>
          <div v-for="(pair, idx) in messages" :key="idx" class="qa-pair">
            <div class="question">
              <div class="msg-header"><el-icon><User /></el-icon> 用户 <span class="msg-time">{{pair.questionTime}}</span></div>
              <div class="msg-content">{{pair.question}}</div>
            </div>
            <div class="answer">
              <div class="msg-header"><el-icon><ChatDotRound /></el-icon> AI <span class="msg-time">{{pair.answerTime}}</span></div>
              <div class="msg-content">{{pair.answer}}</div>
            </div>
          </div>
        </div>
        <el-pagination v-model:current-page="msgPage" v-model:page-size="msgPageSize" :total="msgTotal" layout="total,sizes,prev,pager,next" :page-sizes="[10,20,50]" class="pager" small @current-change="loadMessages" @size-change="loadMessages" />
      </template>
    </el-drawer>
  </div>
</template>
<script setup>
import {ref,watch,onMounted} from 'vue'
import {Refresh, User, ChatDotRound} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import api from '../../../api'
const rows=ref([]);const total=ref(0);const loading=ref(false);const page=ref(1);const pageSize=ref(20)
const drawerVisible=ref(false);const currentSession=ref(null);const messages=ref([]);const msgLoading=ref(false)
const msgPage=ref(1);const msgPageSize=ref(50);const msgTotal=ref(0)
async function load(){loading.value=true;try{const res=await api.get('/admin/biz/mod10/qa-sessions',{params:{page:page.value,pageSize:pageSize.value}});rows.value=res?.list||[];total.value=res?.total||rows.value.length}catch(e){rows.value=[];total.value=0}finally{loading.value=false}}
async function openDetail(row){currentSession.value=row;drawerVisible.value=true;msgPage.value=1;await loadMessages()}
async function loadMessages(){if(!currentSession.value)return;msgLoading.value=true;try{const res=await api.mod10.sessionMessages(currentSession.value.id,{page:msgPage.value,pageSize:msgPageSize.value});messages.value=res?.list||[];msgTotal.value=res?.total||0}catch(e){messages.value=[];msgTotal.value=0}finally{msgLoading.value=false}}
async function confirmDelete(row){try{await ElMessageBox.confirm(`确定删除会话 "${row.title||row.id}" 吗？删除后将无法恢复。`,'删除确认',{confirmButtonText:'确定',cancelButtonText:'取消',type:'warning'});const res=await api.mod10.deleteSession(row.id);if(res?.ok){ElMessage.success('删除成功');if(currentSession.value?.id===row.id)drawerVisible.value=false;load()}else{ElMessage.error(res?.error||'删除失败')}}catch(e){if(e!=='cancel')ElMessage.error('删除失败')}}
async function exportSession(row){try{const res=await api.mod10.exportSession(row.id);const blob=new Blob([JSON.stringify(res,null,2)],{type:'application/json'});const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=`mod10-session-${row.id}.json`;a.click();URL.revokeObjectURL(url);ElMessage.success('导出成功')}catch(e){ElMessage.error('导出失败')}}
watch([page,pageSize],load)
onMounted(load)
</script>
<style lang="scss" scoped>
.qa-sessions-page{animation:adminFadeIn .4s ease;padding:0 4px}
.page-header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:16px}
.header-content h2{margin:0 0 6px;font-size:22px;font-weight:600}
.header-content p{margin:0;color:var(--color-text-muted);font-size:13px}
.header-actions{display:flex;gap:8px;align-items:center}
.mono{font-family:'Cascadia Code','Consolas',monospace;font-size:12px}
.pager{margin-top:14px;justify-content:flex-end;display:flex}
.table-card{cursor:pointer}
.session-info{margin-bottom:16px}
.drawer-actions{display:flex;gap:8px;margin-top:16px}
.message-list{max-height:60vh;overflow-y:auto;padding:0 4px}
.empty-messages{text-align:center;color:var(--color-text-muted);padding:40px 0}
.qa-pair{margin-bottom:20px;border-radius:8px;overflow:hidden;border:1px solid var(--el-border-color)}
.question{background:#f0f9eb;padding:12px}
.answer{background:#f5f7fa;padding:12px}
.msg-header{display:flex;align-items:center;gap:6px;font-size:13px;font-weight:600;color:var(--el-text-color-regular);margin-bottom:8px}
.msg-time{font-weight:400;color:var(--el-text-color-secondary);font-size:12px}
.msg-content{font-size:14px;line-height:1.6;white-space:pre-wrap;word-break:break-word}
</style>
