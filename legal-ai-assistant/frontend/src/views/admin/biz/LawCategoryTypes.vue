<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="header-content">
        <h2 class="gradient-text">分类维度管理</h2>
        <p>业务域 · 法规分类维度定义</p>
      </div>
    </div>
    <el-card class="glass table-card">
      <template #header>
        <div class="card-header">
          <span>分类维度管理</span>
          <el-button type="primary" @click="openDialog()">新增维度</el-button>
        </div>
      </template>
      <el-table :data="tableData" stripe>
        <el-table-column prop="typeCode" label="维度代码" />
        <el-table-column prop="typeName" label="维度名称" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="维度代码">
          <el-input v-model="form.typeCode" placeholder="如: level" />
        </el-form-item>
        <el-form-item label="维度名称">
          <el-input v-model="form.typeName" placeholder="如: 效力层级" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const tableData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增维度')
const form = ref({ typeCode: '', typeName: '', description: '', sortOrder: 0 })

const loadData = async () => {
  try {
    const res = await api.categoryTypes()
    tableData.value = res.data || []
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

const openDialog = (row) => {
  if (row) {
    dialogTitle.value = '编辑维度'
    form.value = { ...row }
  } else {
    dialogTitle.value = '新增维度'
    form.value = { typeCode: '', typeName: '', description: '', sortOrder: 0 }
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  try {
    if (form.value.id) {
      await api.updateCategoryType(form.value.id, form.value)
    } else {
      await api.createCategoryType(form.value)
    }
    dialogVisible.value = false
    loadData()
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除?', '提示')
  try {
    await api.deleteCategoryType(id)
    loadData()
    ElMessage.success('删除成功')
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
