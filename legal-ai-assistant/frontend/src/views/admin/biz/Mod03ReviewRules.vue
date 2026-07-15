<template>
  <div class="review-rules-page">
    <div class="page-header">
      <div class="header-content"><h2>文书复核规则</h2><p>MOD-03 · 模板关联 / 运算符 / 阈值 / 触发动作</p></div>
      <div class="header-actions"><el-button :icon="Refresh" @click="load">刷新</el-button><el-button type="primary" @click="openCreate">新增规则</el-button></div>
    </div>
    <el-card>
      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="template_code" label="模板代码" width="150"><template #default="{row}"><el-tag size="small" type="primary">{{row.template_code}}</el-tag></template></el-table-column>
        <el-table-column prop="rule_type" label="规则类型" width="120"><template #default="{row}"><el-tag size="small">{{row.rule_type}}</el-tag></template></el-table-column>
        <el-table-column label="条件" width="190"><template #default="{row}"><span class="mono">{{row.rule_type}} {{operLabel(row.operator)}} {{row.threshold}}</span></template></el-table-column>
        <el-table-column prop="trigger_action" label="触发动作" width="110"><template #default="{row}"><el-tag size="small" type="warning">{{row.trigger_action}}</el-tag></template></el-table-column>
        <el-table-column label="状态" width="80"><template #default="{row}"><el-switch :model-value="row.status===1" @change="toggleRule(row)" size="small" /></template></el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{row}"><el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button><el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="showDialog" :title="form.id?'编辑规则':'新增规则'" width="540px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="模板代码" required><el-input v-model="form.template_code" :disabled="!!form.id" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="规则类型"><el-select v-model="form.rule_type" style="width:100%" filterable allow-create><el-option label="word_count" value="word_count"/><el-option label="clause_count" value="clause_count"/><el-option label="risk_score" value="risk_score"/><el-option label="field_required" value="field_required"/><el-option label="date_range" value="date_range"/></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="运算符"><el-select v-model="form.operator" style="width:100%"><el-option label="> 大于" value="gt"/><el-option label=">= 大于等于" value="gte"/><el-option label="< 小于" value="lt"/><el-option label="<= 小于等于" value="lte"/><el-option label="== 等于" value="eq"/></el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="阈值"><el-input-number v-model="form.threshold" :min="0" style="width:100%"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="触发动作"><el-select v-model="form.trigger_action" style="width:100%"><el-option label="block 阻止" value="block"/><el-option label="warn 警告" value="warn"/><el-option label="flag 标记" value="flag"/><el-option label="skip 跳过" value="skip"/></el-select></el-form-item></el-col>
        </el-row>
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
const form=reactive({id:null,template_code:'',rule_type:'word_count',operator:'gt',threshold:500,trigger_action:'warn',status:1})
function operLabel(o){return({gt:'>',gte:'>=',lt:'<',lte:'<=',eq:'=='}[o]||o)}
async function load(){loading.value=true;try{const res=await api.get('/admin/biz/mod03/review-rules');rows.value=res.data?.list||[]}catch(e){rows.value=[]}finally{loading.value=false}}
function openCreate(){Object.assign(form,{id:null,template_code:'',rule_type:'word_count',operator:'gt',threshold:500,trigger_action:'warn',status:1});showDialog.value=true}
function openEdit(row){Object.assign(form,{...row});showDialog.value=true}
async function handleSave(){if(!form.template_code||!form.rule_type){ElMessage.warning('模板代码和规则类型必填');return}const p={...form};delete p.id;try{let res;if(form.id){res=await api.post(`/admin/doc_review_rule/${form.id}/update`,p)}else{res=await api.post('/admin/doc_review_rule/create',p)}if(res.data?.ok){ElMessage.success('保存成功');showDialog.value=false;load()}else{ElMessage.error(res.data?.error||'保存失败')}}catch(e){ElMessage.error('保存失败')}}
async function handleDelete(row){try{await ElMessageBox.confirm(`删除规则：「${row.rule_name}」？`,'确认删除',{type:'warning'});await api.post(`/admin/doc_review_rule/${row.id}/delete`);ElMessage.success('已删除');load()}catch(e){if(e!=='cancel')ElMessage.error('删除失败')}}
async function toggleRule(row){try{await api.post(`/admin/doc_review_rule/${row.id}/toggle`,{status:row.status===1?0:1});ElMessage.success(row.status===1?'已停用':'已启用');load()}catch(e){ElMessage.error('切换失败')}}
onMounted(load)
</script>
<style lang="scss" scoped>
.review-rules-page{animation:fadeIn .4s ease;padding:0 4px}to{opacity:1;transform:translateY(0)}}.page-header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:16px}.header-content h2{margin:0 0 6px;font-size:22px;font-weight:600}.header-content p{margin:0;color: var(--color-text-muted);font-size:13px}.header-actions{display:flex;gap:8px;align-items:center}.mono{font-family:'Cascadia Code','Consolas',monospace;font-size:12px;color:#475569}
</style>