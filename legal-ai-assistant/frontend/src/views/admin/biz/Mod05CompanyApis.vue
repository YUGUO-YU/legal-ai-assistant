<template>
  <div class="company-apis-page">
    <div class="page-header">
      <div class="header-content"><h2>企业查询 API 配置</h2><p>MOD-05 · 供应商 / 额度 / 用量统计</p></div>
      <div class="header-actions"><el-button :icon="Refresh" @click="load">刷新</el-button><el-button type="primary" @click="openCreate">新增配置</el-button></div>
    </div>
    <el-card>
      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="api_name" label="API名称" min-width="160"><template #default="{row}"><el-tag size="small" type="primary">{{row.api_name}}</el-tag></template></el-table-column>
        <el-table-column prop="provider" label="供应商" width="110" />
        <el-table-column prop="endpoint" label="端点" min-width="200" show-overflow-tooltip />
        <el-table-column label="额度" width="150"><template #default="{row}"><el-progress :percentage="quotaPct(row)" :status="quotaPct(row)>80?'exception':''">{{row.used_count||0}} / {{row.monthly_quota||0}}</el-progress></template></el-table-column>
        <el-table-column label="状态" width="80"><template #default="{row}"><el-tag :type="row.status===1?'success':'info'" size="small">{{row.status===1?'启用':'停用'}}</el-tag></template></el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right"><template #default="{row}"><el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button><el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="showDialog" :title="form.id?'编辑配置':'新增配置'" width="560px">
      <el-form :model="form" :rules="rules" label-width="100px">
        <el-form-item label="API名称" required><el-input v-model="form.api_name" /></el-form-item>
        <el-form-item label="供应商"><el-select v-model="form.provider" style="width:100%" filterable allow-create><el-option v-for="p in providers" :key="p" :label="p" :value="p"/></el-select></el-form-item>
        <el-form-item label="端点URL"><el-input v-model="form.endpoint" /></el-form-item>
        <el-form-item label="API Key"><el-input v-model="form.api_key_enc" type="password" show-password placeholder="留空不修改" /></el-form-item>
        <el-form-item label="月度额度"><el-input-number v-model="form.monthly_quota" :min="0" :step="1000" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showDialog=false">取消</el-button><el-button type="primary" @click="handleSave">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import {ref,reactive,onMounted} from 'vue'
import {Refresh} from '@element-plus/icons-vue'
import {ElMessage,ElMessageBox} from 'element-plus'
import api from '../../../api'
const rows=ref([]);const loading=ref(false);const showDialog=ref(false)
const form=reactive({id:null,api_name:'',provider:'企查查',endpoint:'',api_key_enc:'',monthly_quota:10000,used_count:0,status:1})
const providers=['企查查','天眼查','启信宝','国家企业信用信息公示系统','爱企查']
const rules=reactive({
  api_name:[
    {required:true,message:'请输入API名称',trigger:'blur'},
    {max:100,message:'API名称不能超过100字符',trigger:'blur'}
  ],
  api_key_enc:[
    {required:true,message:'请输入API Key',trigger:'blur'}
  ]
})
function quotaPct(r){return r.monthly_quota?Math.round((r.used_count||0)/r.monthly_quota*100):0}
async function load(){loading.value=true;try{const res=await api.get('/admin/biz/mod05/company-apis');rows.value=res.data?.list||[]}catch(e){rows.value=[]}finally{loading.value=false}}
function openCreate(){Object.assign(form,{id:null,api_name:'',provider:'企查查',endpoint:'',api_key_enc:'',monthly_quota:10000,used_count:0,status:1});showDialog.value=true}
function openEdit(row){Object.assign(form,{...row});showDialog.value=true}
async function handleSave(){if(!form.api_name){ElMessage.warning('API名称必填');return}const p={...form};if(!p.api_key_enc)delete p.api_key_enc;delete p.id;delete p.used_count;try{const res=form.id?await api.post(`/admin/company_api_config/${form.id}/update`,p):await api.post('/admin/company_api_config/create',p);if(res.data?.ok){ElMessage.success('保存成功');showDialog.value=false;load()}else ElMessage.error(res.data?.error||'保存失败')}catch(e){ElMessage.error('保存失败')}}
async function handleDelete(row){try{await ElMessageBox.confirm(`删除「${row.api_name}」？`,'确认',{type:'warning'});await api.post(`/admin/company_api_config/${row.id}/delete`);ElMessage.success('已删除');load()}catch(e){if(e!=='cancel')ElMessage.error('删除失败')}}
onMounted(load)
</script>
<style lang="scss" scoped>
.company-apis-page{animation:adminFadeIn .4s ease;padding:0 4px}.page-header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:16px}.header-content h2{margin:0 0 6px;font-size:22px;font-weight:600}.header-content p{margin:0;color: var(--color-text-muted);font-size:13px}.header-actions{display:flex;gap:8px;align-items:center}
</style>