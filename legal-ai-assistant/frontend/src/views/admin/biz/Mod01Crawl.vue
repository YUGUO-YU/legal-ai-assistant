<template>
  <div class="crawl-page">
    <div class="page-header">
      <div class="header-content">
        <h2>爬虫任务管理</h2>
        <p>MOD-01 · Cron 调度 / 源配置 / 爬取统计</p>
      </div>
      <div class="header-actions">
        <el-tag :type="filter.status === 'running' ? 'warning':'info'" size="small" @click="filter.status=filter.status==='running'?'':'running';load()" style="cursor:pointer">运行中 {{runningCount}}</el-tag>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新建任务</el-button>
      </div>
    </div>
    <el-card>
      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="task_name" label="任务名称" min-width="180" />
        <el-table-column prop="source" label="来源" width="90"><template #default="{row}"><el-tag size="small">{{row.source}}</el-tag></template></el-table-column>
        <el-table-column prop="crawl_type" label="类型" width="90" />
        <el-table-column prop="cron_expression" label="Cron" width="140"><template #default="{row}"><span class="mono">{{row.cron_expression}}</span></template></el-table-column>
        <el-table-column label="进度" width="180">
          <template #default="{row}">
            <div><span class="text-success">{{row.success_count||0}}</span> / <span class="text-danger">{{row.fail_count||0}}</span> / <span>{{row.total_crawled||0}} 总</span></div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90"><template #default="{row}"><el-tag :type="crawlTag(row.status)" size="small">{{crawlLabel(row.status)}}</el-tag></template></el-table-column>
        <el-table-column prop="last_run_at" label="上次执行" width="170" />
        <el-table-column prop="next_run_at" label="下次执行" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{row}">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="openLogs(row)">查看日志</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,sizes,prev,pager,next" :page-sizes="[10,20,50]" class="pager" />
    </el-card>

    <el-dialog v-model="showDialog" :title="form.id?'编辑任务':'新建任务'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="任务名称" required><el-input v-model="form.task_name" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="来源"><el-select v-model="form.source" style="width:100%" filterable allow-create><el-option v-for="s in sources" :key="s" :label="s" :value="s"/></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="类型"><el-select v-model="form.crawl_type" style="width:100%"><el-option label="全量" value="full"/><el-option label="增量" value="incremental"/><el-option label="监控" value="monitor"/></el-select></el-form-item></el-col>
        </el-row>
        <el-form-item label="目标URL"><el-input v-model="form.target_url" placeholder="https://..." /></el-form-item>
        <el-form-item label="Cron"><el-input v-model="form.cron_expression" placeholder="0 2 * * *" /></el-form-item>
        <el-form-item label="JSON配置"><el-input v-model="form.config" type="textarea" :rows="3" placeholder='{"headers":{},"timeout":30}' /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showDialog=false">取消</el-button><el-button type="primary" @click="handleSave">保存</el-button></template>
    </el-dialog>

    <el-drawer v-model="showLogDrawer" :title="'爬虫日志 - ' + (logTask.task_name||'')" size="700px">
      <el-table :data="logRows" v-loading="logLoading" stripe border max-height="60vh">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="状态" width="90">
          <template #default="{row}">
            <el-tag :type="logTag(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="抓取结果" width="140">
          <template #default="{row}">
            <span class="text-success">{{ row.success_count||0 }}</span> /
            <span class="text-danger">{{ row.fail_count||0 }}</span> /
            <span>{{ row.total_fetched||0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="duration_seconds" label="耗时(秒)" width="90" />
        <el-table-column prop="started_at" label="开始时间" width="160" />
        <el-table-column prop="finished_at" label="结束时间" width="160" />
        <el-table-column label="错误信息" min-width="120">
          <template #default="{row}">
            <span v-if="row.error_message" class="text-danger">{{ row.error_message }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="logPage" v-model:page-size="logPageSize" :total="logTotal" layout="total,sizes,prev,pager,next" :page-sizes="[10,20,50]" class="pager" @change="loadLogs" />
    </el-drawer>
  </div>
</template>

<script setup>
import {ref,reactive,computed,watch,onMounted} from 'vue'
import {Refresh} from '@element-plus/icons-vue'
import {ElMessage,ElMessageBox} from 'element-plus'
import api from '../../../api'
const API_CREATE='/admin/crawl_task/create'
const API_UPDATE=(id)=>`/admin/crawl_task/${id}/update`
const API_DELETE=(id)=>`/admin/crawl_task/${id}/delete`
const rows=ref([]);const total=ref(0);const loading=ref(false);const page=ref(1);const pageSize=ref(20)
const filter=reactive({status:''});const showDialog=ref(false)
const form=reactive({id:null,task_name:'',source:'官方网站',crawl_type:'full',target_url:'',cron_expression:'0 2 * *',config:'{}',status:0})
const sources=['官方网站','裁判文书网','政府公告','法律法规库','学术期刊']
const runningCount=computed(()=>rows.value.filter(r=>r.status===1).length)
function crawlLabel(s){return({0:'待启动',1:'运行中',2:'已完成',3:'失败',4:'暂停'}[s]||s)}
function crawlTag(s){return({0:'info',1:'warning',2:'success',3:'danger',4:'info'}[s]||'')}

const showLogDrawer=ref(false)
const logTask=ref({})
const logRows=ref([]);const logTotal=ref(0);const logLoading=ref(false)
const logPage=ref(1);const logPageSize=ref(20)
function logTag(s){return({RUNNING:'warning',SUCCESS:'success',FAILED:'danger',UNKNOWN:'info'}[s]||'')}
async function loadLogs(){
  logLoading.value=true
  try{
    const res=await api.get('/admin/biz/mod01/crawl-logs',{params:{taskId:logTask.value.id,page:logPage.value,pageSize:logPageSize.value}})
    logRows.value=res.data?.list||[];logTotal.value=res.data?.total||0
  }catch(e){logRows.value=[];logTotal.value=0}
  finally{logLoading.value=false}
}
function openLogs(row){
  logTask.value=row;logPage.value=1;showLogDrawer.value=true;loadLogs()
}
async function load(){
  loading.value=true
  try{const res=await api.get('/admin/biz/mod01/crawl-tasks',{params:{page:page.value,pageSize:pageSize.value}});rows.value=res.data?.list||[];total.value=res.data?.total||rows.value.length}catch(e){rows.value=[];total.value=0}finally{loading.value=false}
}
function openCreate(){Object.assign(form,{id:null,task_name:'',source:'官方网站',crawl_type:'full',target_url:'',cron_expression:'0 2 * * *',config:'{}',status:0});showDialog.value=true}
function openEdit(row){Object.assign(form,{...row});showDialog.value=true}
async function handleSave(){
  if(!form.task_name){ElMessage.warning('任务名称必填');return}
  const p={...form};delete p.id
  try{const res=form.id?await api.post(API_UPDATE(form.id),p):await api.post(API_CREATE,p);if(res.data?.ok){ElMessage.success('保存成功');showDialog.value=false;load()}else ElMessage.error(res.data?.error||'保存失败')}catch(e){ElMessage.error('保存失败')}
}
async function handleDelete(row){try{await ElMessageBox.confirm(`删除任务「${row.task_name}」？`,'确认',{type:'warning'});await api.post(API_DELETE(row.id));ElMessage.success('已删除');load()}catch(e){if(e!=='cancel')ElMessage.error('删除失败')}}
watch([page,pageSize],load)
onMounted(load)
</script>
<style lang="scss" scoped>
.crawl-page{animation:fadeIn .4s ease;padding:0 4px}to{opacity:1;transform:translateY(0)}}.page-header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:16px;flex-wrap:wrap;gap:10px}.header-content h2{margin:0 0 6px;font-size:22px;font-weight:600}.header-content p{margin:0;color: var(--color-text-muted);font-size:13px}.header-actions{display:flex;gap:8px;align-items:center}.mono{font-family:'Cascadia Code','Consolas',monospace;font-size:12px}.text-success{color: var(--color-success);font-weight:600}.text-danger{color: var(--color-danger);font-weight:600}.pager{margin-top:14px;justify-content:flex-end;display:flex}
</style>