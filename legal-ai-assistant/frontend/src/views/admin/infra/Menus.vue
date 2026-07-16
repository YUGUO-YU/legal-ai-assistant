<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="header-content"><h2 class="gradient-text">菜单权限管理</h2><p>基础设施域 · 树形菜单 / 目录-菜单-按钮 / 权限标识</p></div>
      <div class="header-actions"><el-button :icon="Refresh" @click="load">刷新</el-button><el-button type="primary" @click="openCreate">新增菜单</el-button></div>
    </div>
    <el-card class="glass table-card">
      <el-table :data="treeRows" v-loading="loading" stripe border row-key="id" default-expand-all>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="menu_name" label="名称" min-width="200"><template #default="{row}"><span :style="{paddingLeft:(row._depth*20)+'px'}">{{row.menu_name}}</span></template></el-table-column>
        <el-table-column label="类型" width="90"><template #default="{row}"><el-tag size="small" :type="menuTypeTag(row.menu_type)">{{menuTypeLabel(row.menu_type)}}</el-tag></template></el-table-column>
        <el-table-column prop="path" label="路径" min-width="180" show-overflow-tooltip />
        <el-table-column prop="permission" label="权限标识" min-width="160" show-overflow-tooltip><template #default="{row}"><span v-if="row.permission" class="mono">{{row.permission}}</span><span v-else class="text-muted">-</span></template></el-table-column>
        <el-table-column prop="icon" label="图标" width="80" />
        <el-table-column prop="sort_order" label="排序" width="70" />
        <el-table-column prop="biz_module" label="模块" width="100"><template #default="{row}"><el-tag v-if="row.biz_module" size="small" type="success">{{row.biz_module}}</el-tag></template></el-table-column>
        <el-table-column label="状态" width="80"><template #default="{row}"><el-tag :type="row.status===1?'success':'info'" size="small">{{row.status===1?'启用':'停用'}}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="160" fixed="right"><template #default="{row}"><el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button><el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="showDialog" :title="form.id?'编辑菜单':'新增菜单'" width="600px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="上级菜单"><el-input-number v-model="form.parent_id" :min="0" style="width:100%"/></el-form-item>
        <el-form-item label="名称" required><el-input v-model="form.menu_name"/></el-form-item>
        <el-form-item label="类型"><el-select v-model="form.menu_type" style="width:100%"><el-option label="目录" :value="1"/><el-option label="菜单" :value="2"/><el-option label="按钮" :value="3"/></el-select></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="路径"><el-input v-model="form.path"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="组件"><el-input v-model="form.component"/></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="权限标识"><el-input v-model="form.permission"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="图标"><el-input v-model="form.icon"/></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="模块"><el-input v-model="form.biz_module" placeholder="MOD-01"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="排序"><el-input-number v-model="form.sort_order" :min="0" style="width:100%"/></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer><el-button @click="showDialog=false">取消</el-button><el-button type="primary" @click="handleSave">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import {ref,reactive,computed,onMounted} from 'vue'
import {Refresh} from '@element-plus/icons-vue'
import {ElMessage,ElMessageBox} from 'element-plus'
import api from '../../../api'
const rows=ref([]);const loading=ref(false);const showDialog=ref(false)
const form=reactive({id:null,parent_id:0,menu_name:'',menu_type:2,path:'',component:'',permission:'',icon:'',sort_order:0,biz_module:'',status:1})
function menuTypeLabel(t){return({1:'目录',2:'菜单',3:'按钮'}[t]||t)}
function menuTypeTag(t){return({1:'warning',2:'primary',3:'info'}[t]||'')}
const treeRows=computed(()=>{
  const map={};const roots=[]
  rows.value.forEach(r=>{map[r.id]=r;r.children=[]})
  rows.value.forEach(r=>{if(r.parent_id&&map[r.parent_id])map[r.parent_id].children.push(r);else roots.push(r)})
  function flatten(list,depth=0){let out=[];list.forEach(r=>{r._depth=depth;out.push(r);if(r.children.length)out=out.concat(flatten(r.children,depth+1))});return out}
  return flatten(roots)
})
async function load(){loading.value=true;try{const res=await api.get('/admin/infra/menus');rows.value=res.data?.list||[]}catch(e){rows.value=[]}finally{loading.value=false}}
function openCreate(){Object.assign(form,{id:null,parent_id:0,menu_name:'',menu_type:2,path:'',component:'',permission:'',icon:'',sort_order:0,biz_module:'',status:1});showDialog.value=true}
function openEdit(row){Object.assign(form,{...row});showDialog.value=true}
async function handleSave(){if(!form.menu_name){ElMessage.warning('菜单名称必填');return}const p={...form};delete p.id;try{const res=form.id?await api.post(`/admin/admin_menu/${form.id}/update`,p):await api.post('/admin/admin_menu/create',p);if(res.data?.ok){ElMessage.success('保存成功');showDialog.value=false;load()}else ElMessage.error(res.data?.error||'保存失败')}catch(e){ElMessage.error('保存失败')}}
async function handleDelete(row){try{await ElMessageBox.confirm(`删除菜单「${row.menu_name}」？`,'确认',{type:'warning'});await api.post(`/admin/admin_menu/${row.id}/delete`);ElMessage.success('已删除');load()}catch(e){if(e!=='cancel')ElMessage.error('删除失败')}}
onMounted(load)
</script>
<style lang="scss" scoped>
.menus-page{animation:adminFadeIn .4s ease;padding:0 4px}.page-header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:16px}.header-content h2{margin:0 0 6px;font-size:22px;font-weight:600}.header-content p{margin:0;color:var(--color-text-secondary);font-size:13px}.header-actions{display:flex;gap:8px;align-items:center}.mono{font-family:'Cascadia Code','Consolas',monospace;font-size:12px}.text-muted{color:var(--color-text-placeholder);font-size:12px}
</style>