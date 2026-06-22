<template>
  <div class="strategy-page">
    <div class="page-header">
      <div class="header-content"><h2>知识库分块策略</h2><p>MOD-09 · 分块大小 / 重叠 / 分割器</p></div>
      <div class="header-actions"><el-button :icon="Refresh" @click="load">刷新</el-button><el-button type="primary" @click="openCreate">新增策略</el-button></div>
    </div>
    <el-card>
      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="kb_id" label="知识库ID" width="100" />
        <el-table-column label="分块大小" width="140"><template #default="{row}"><el-tag size="small" type="primary">{{row.chunk_size}} tokens</el-tag></template></el-table-column>
        <el-table-column label="重叠" width="100"><template #default="{row}"><el-tag size="small" type="warning">{{row.chunk_overlap}} tokens</el-tag></template></el-table-column>
        <el-table-column prop="splitter" label="分割器" width="130"><template #default="{row}"><el-tag size="small">{{row.splitter}}</el-tag></template></el-table-column>
        <el-table-column label="状态" width="80"><template #default="{row}"><el-tag :type="row.status===1?'success':'info'" size="small">{{row.status===1?'启用':'停用'}}</el-tag></template></el-table-column>
        <el-table-column prop="updated_at" label="更新时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right"><template #default="{row}"><el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button><el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="showDialog" :title="form.id?'编辑策略':'新增策略'" width="500px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="知识库ID"><el-input-number v-model="form.kb_id" :min="1" style="width:100%"/></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="分块大小"><el-input-number v-model="form.chunk_size" :min="64" :max="4096" :step="64" style="width:100%"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="重叠大小"><el-input-number v-model="form.chunk_overlap" :min="0" :max="512" :step="8" style="width:100%"/></el-form-item></el-col>
        </el-row>
        <el-form-item label="分割器"><el-select v-model="form.splitter" style="width:100%"><el-option label="Recursive" value="recursive"/><el-option label="Character" value="character"/><el-option label="Sentence" value="sentence"/><el-option label="Markdown" value="markdown"/></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="showDialog=false">取消</el-button><el-button type="primary" @click="handleSave">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import {ref,reactive,onMounted} from 'vue'
import {Refresh} from '@element-plus/icons-vue'
import {ElMessage,ElMessageBox} from 'element-plus'
import api from '../../api'
const rows=ref([]);const loading=ref(false);const showDialog=ref(false)
const form=reactive({id:null,kb_id:1,chunk_size:512,chunk_overlap:64,splitter:'recursive',status:1})
async function load(){loading.value=true;try{const res=await api.get('/admin/biz/mod09/kb-strategies');rows.value=res.data?.list||[]}catch(e){rows.value=[]}finally{loading.value=false}}
function openCreate(){Object.assign(form,{id:null,kb_id:1,chunk_size:512,chunk_overlap:64,splitter:'recursive',status:1});showDialog.value=true}
function openEdit(row){Object.assign(form,{...row});showDialog.value=true}
async function handleSave(){const p={...form};delete p.id;try{const res=form.id?await api.post(`/admin/{table}/${form.id}/update`.replace('{table}','kb_chunk_strategy'),p):await api.post('/admin/{table}/create'.replace('{table}','kb_chunk_strategy'),p);if(res.data?.ok){ElMessage.success('保存成功');showDialog.value=false;load()}else ElMessage.error(res.data?.error||'保存失败')}catch(e){ElMessage.error('保存失败')}}
async function handleDelete(row){try{await ElMessageBox.confirm(`删除策略？`,'确认',{type:'warning'});await api.post(`/admin/{table}/${row.id}/delete`.replace('{table}','kb_chunk_strategy'));ElMessage.success('已删除');load()}catch(e){if(e!=='cancel')ElMessage.error('删除失败')}}
onMounted(load)
</script>
<style lang="scss" scoped>
.strategy-page{animation:fadeIn .4s ease;padding:0 4px}@keyframes fadeIn{from{opacity:0;transform:translateY(10px)}to{opacity:1;transform:translateY(0)}}.page-header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:16px}.header-content h2{margin:0 0 6px;font-size:22px;font-weight:600}.header-content p{margin:0;color:#64748b;font-size:13px}.header-actions{display:flex;gap:8px;align-items:center}
</style>