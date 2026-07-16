<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="header-content">
        <h2 class="gradient-text">分类管理</h2>
        <p>业务域 · 法规分类树形管理</p>
      </div>
    </div>
    <el-card class="glass table-card">
      <template #header>
        <div class="card-header">
          <span>分类管理</span>
          <el-button type="primary" :disabled="!selectedTypeId" @click="openDialog()">新增分类</el-button>
        </div>
      </template>
      <div class="content-row">
        <div class="left-panel">
          <el-form-item label="维度选择" style="margin-bottom: 12px;">
            <el-select v-model="selectedTypeId" placeholder="请选择维度" style="width: 200px;" @change="loadCategories">
              <el-option v-for="t in categoryTypes" :key="t.id" :label="t.typeName" :value="t.id" />
            </el-select>
          </el-form-item>
          <el-tree
            v-if="selectedTypeId"
            :data="treeData"
            :props="{ label: 'categoryName', children: 'children' }"
            node-key="id"
            default-expand-all
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <span>{{ data.categoryName }}</span>
                <span class="node-actions">
                  <el-button link type="primary" size="small" @click.stop="openDialog(data)">编辑</el-button>
                  <el-button link type="danger" size="small" @click.stop="handleDelete(data.id)">删除</el-button>
                </span>
              </span>
            </template>
          </el-tree>
          <el-empty v-else description="请先选择维度" />
        </div>
        <div class="right-panel">
          <el-card v-if="selectedCategory" shadow="never">
            <template #header>
              <span>分类详情</span>
            </template>
            <el-form :model="detailForm" label-width="100px">
              <el-form-item label="分类名称">
                <el-input v-model="detailForm.categoryName" />
              </el-form-item>
              <el-form-item label="上级分类">
                <el-input v-model="detailForm.parentName" disabled />
              </el-form-item>
              <el-form-item label="排序">
                <el-input-number v-model="detailForm.sortOrder" :min="0" />
              </el-form-item>
              <el-form-item label="描述">
                <el-input v-model="detailForm.description" type="textarea" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleUpdate">保存</el-button>
              </el-form-item>
            </el-form>
          </el-card>
          <el-empty v-else description="请在左侧选择分类" />
        </div>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="分类名称">
          <el-input v-model="form.categoryName" placeholder="如: 行政法规" />
        </el-form-item>
        <el-form-item label="上级分类">
          <el-tree-select
            v-model="form.parentId"
            :data="treeData"
            :props="{ label: 'categoryName', children: 'children', value: 'id' }"
            check-strictly
            clearable
            placeholder="请选择上级分类（可选）"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
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

const categoryTypes = ref([])
const selectedTypeId = ref(null)
const treeData = ref([])
const selectedCategory = ref(null)
const detailForm = ref({})
const dialogVisible = ref(false)
const dialogTitle = ref('新增分类')
const form = ref({ categoryName: '', parentId: null, sortOrder: 0, description: '', typeId: null })

const loadCategoryTypes = async () => {
  try {
    const res = await api.categoryTypes()
    categoryTypes.value = res || []
  } catch (e) {
    ElMessage.error('加载维度失败')
  }
}

const loadCategories = async () => {
  if (!selectedTypeId.value) return
  try {
    const res = await api.categories(selectedTypeId.value)
    treeData.value = res || []
    selectedCategory.value = null
  } catch (e) {
    ElMessage.error('加载分类失败')
  }
}

const handleNodeClick = (data) => {
  selectedCategory.value = data
  detailForm.value = { ...data, parentName: data.parentId ? getParentName(data.parentId) : '根分类' }
}

const getParentName = (parentId) => {
  const findNode = (nodes, id) => {
    for (const n of nodes) {
      if (n.id === id) return n.categoryName
      if (n.children) {
        const found = findNode(n.children, id)
        if (found) return found
      }
    }
    return null
  }
  return findNode(treeData.value, parentId) || ''
}

const openDialog = (row) => {
  if (row) {
    dialogTitle.value = '编辑分类'
    form.value = { ...row, typeId: selectedTypeId.value }
  } else {
    dialogTitle.value = '新增分类'
    form.value = { categoryName: '', parentId: null, sortOrder: 0, description: '', typeId: selectedTypeId.value }
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  try {
    if (form.value.id) {
      await api.updateCategory(form.value.id, form.value)
    } else {
      await api.createCategory(form.value)
    }
    dialogVisible.value = false
    loadCategories()
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const handleUpdate = async () => {
  try {
    await api.updateCategory(selectedCategory.value.id, detailForm.value)
    loadCategories()
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除?', '提示')
  try {
    await api.deleteCategory(id)
    loadCategories()
    ElMessage.success('删除成功')
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

onMounted(loadCategoryTypes)
</script>

<style scoped>
.page-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.content-row { display: flex; gap: 20px; }
.left-panel { width: 320px; flex-shrink: 0; }
.right-panel { flex: 1; }
.tree-node { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.node-actions { margin-left: auto; }
</style>
