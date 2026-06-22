<template>
  <div class="mod07-laws-page">
    <div class="page-header">
      <div class="header-content"><h2>法规查询管理</h2><p>MOD-07 · 法规主数据 / 分类 / 效力状态</p></div>
      <div class="header-actions"><el-button :icon="Refresh" @click="load">刷新</el-button></div>
    </div>
    <el-card class="filter-card">
      <el-form inline :model="filter">
        <el-form-item label="分类"><el-select v-model="filter.cat" clearable placeholder="全部" style="width:140px"><el-option v-for="c in categories" :key="c" :label="c" :value="c"/></el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="filter.status" clearable placeholder="全部" style="width:110px"><el-option label="现行" :value="1"/><el-option label="废止" :value="2"/><el-option label="修订中" :value="3"/></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="load">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
    </el-card>
    <el-card>
      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="280" show-overflow-tooltip />
        <el-table-column prop="category_l1" label="一级分类" width="110"><template #default="{row}"><el-tag size="small" type="primary">{{row.category_l1}}</el-tag></template></el-table-column>
        <el-table-column prop="category_l2" label="二级分类" width="110" show-overflow-tooltip />
        <el-table-column prop="issuing_authority" label="制定机关" min-width="160" show-overflow-tooltip />
        <el-table-column prop="effective_date" label="生效日期" width="110" />
        <el-table-column label="状态" width="90"><template #default="{row}"><el-tag :type="lawStatusTag(row.status)" size="small">{{lawStatusLabel(row.status)}}</el-tag></template></el-table-column>
        <el-table-column label="浏览" width="70"><template #default="{row}">{{row.view_count||0}}</template></el-table-column>
        <el-table-column prop="created_at" label="入库时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right"><template #default="{row}"><el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,sizes,prev,pager,next" :page-sizes="[10,20,50]" class="pager" />
    </el-card>
    <el-drawer v-model="showDetail" title="法规详情" size="55%" direction="rtl">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="ID">{{detail.id}}</el-descriptions-item>
        <el-descriptions-item label="标题">{{detail.title}}</el-descriptions-item>
        <el-descriptions-item label="简称">{{detail.short_title||'-'}}</el-descriptions-item>
        <el-descriptions-item label="分类">{{detail.category_l1}} / {{detail.category_l2||'-'}}</el-descriptions-item>
        <el-descriptions-item label="制定机关">{{detail.issuing_authority||'-'}}</el-descriptions-item>
        <el-descriptions-item label="发布日期">{{detail.issue_date||'-'}}</el-descriptions-item>
        <el-descriptions-item label="生效日期">{{detail.effective_date||'-'}}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="lawStatusTag(detail.status)">{{lawStatusLabel(detail.status)}}</el-tag></el-descriptions-item>
        <el-descriptions-item label="来源">{{detail.source_name||'-'}} <a v-if="detail.source_url" :href="detail.source_url" target="_blank">链接</a></el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>
<script setup>
import {ref,reactive,computed,watch,onMounted} from 'vue'
import {Refresh} from '@element-plus/icons-vue'
import api from '../../api'
const rows=ref([]);const total=ref(0);const loading=ref(false);const page=ref(1);const pageSize=ref(20)
const filter=reactive({cat:'',status:''});const showDetail=ref(false);const detail=ref(null)
const categories=computed(()=>[...new Set(rows.value.map(r=>r.category_l1))].sort())
function lawStatusLabel(s){return({1:'现行',2:'废止',3:'修订中',4:'未生效',5:'部分失效'}[s]||s)}
function lawStatusTag(s){return({1:'success',2:'info',3:'warning',4:'info',5:'warning'}[s]||'')}
async function load(){loading.value=true;try{const res=await api.get('/admin/biz/mod07/laws',{params:{page:page.value,pageSize:pageSize.value}});rows.value=res.data?.list||[];total.value=res.data?.total||rows.value.length}catch(e){rows.value=[];total.value=0}finally{loading.value=false}}
function openDetail(row){detail.value=row;showDetail.value=true}
function reset(){filter.cat='';filter.status='';load()}
watch([page,pageSize],load)
onMounted(load)
</script>
<style lang="scss" scoped>
.mod07-laws-page{animation:fadeIn .4s ease;padding:0 4px}@keyframes fadeIn{from{opacity:0;transform:translateY(10px)}to{opacity:1;transform:translateY(0)}}.page-header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:16px}.header-content h2{margin:0 0 6px;font-size:22px;font-weight:600}.header-content p{margin:0;color:#64748b;font-size:13px}.header-actions{display:flex;gap:8px;align-items:center}.filter-card{margin-bottom:16px}.pager{margin-top:14px;justify-content:flex-end;display:flex}
</style>