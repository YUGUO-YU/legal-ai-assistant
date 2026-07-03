<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <span>AI 法规导入</span>
      </template>
      <div class="import-layout">
        <div class="left-upload">
          <el-upload
            ref="uploadRef"
            drag
            :auto-upload="false"
            :limit="1"
            accept=".doc,.docx"
            :on-change="handleFileChange"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽 Word 文件到此处，或 <em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 .doc, .docx 格式</div>
            </template>
          </el-upload>
          <el-button type="primary" :disabled="!uploadFile" style="margin-top: 16px; width: 100%;" @click="handlePreview">
            上传并预览
          </el-button>
        </div>
        <div class="right-preview">
          <el-card v-if="previewData" shadow="never">
            <template #header>
              <div class="preview-header">
                <span>预览信息</span>
                <el-button v-if="previewData" type="success" @click="handleConfirm">确认导入</el-button>
              </div>
            </template>
            <el-form :model="previewForm" label-width="110px">
              <el-form-item label="法规标题">
                <el-input v-model="previewForm.title" />
              </el-form-item>
              <el-form-item label="文号">
                <el-input v-model="previewForm.documentNo" />
              </el-form-item>
              <el-form-item label="发布机关">
                <el-input v-model="previewForm.issuingAuthority" />
              </el-form-item>
              <el-form-item label="发布日期">
                <el-date-picker v-model="previewForm.issueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%;" />
              </el-form-item>
              <el-form-item label="分类">
                <el-select v-model="selectedCategoryIds" multiple placeholder="请选择分类" style="width: 100%;">
                  <el-option v-for="c in categoryList" :key="c.id" :label="c.categoryName" :value="c.id" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="previewData?.chapterTree?.length" label="章节结构">
                <el-tree :data="chapterTree" :props="{ label: 'title' }" default-expand-all style="max-height: 300px; overflow-y: auto;" />
              </el-form-item>
            </el-form>
          </el-card>
          <el-empty v-else description="上传文件后可预览" />
        </div>
      </div>
    </el-card>

    <el-card style="margin-top: 16px;">
      <template #header>
        <span>导入历史</span>
      </template>
      <el-table :data="historyData" stripe>
        <el-table-column prop="title" label="法规标题" />
        <el-table-column prop="documentNo" label="文号" />
        <el-table-column prop="issuingAuthority" label="发布机关" />
        <el-table-column prop="issueDate" label="发布日期" width="120" />
        <el-table-column prop="importedAt" label="导入时间" width="180" />
        <el-table-column prop="status" label="状态" width="80" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import api from '@/api'

const uploadRef = ref(null)
const uploadFile = ref(null)
const previewData = ref(null)
const previewForm = ref({})
const selectedCategoryIds = ref([])
const categoryList = ref([])
const historyData = ref([])

const chapterTree = computed(() => {
  if (!previewData.value?.chapterTree) return []
  return previewData.value.chapterTree.map(ch => ({ title: ch.title, children: ch.children || [] }))
})

const handleFileChange = (file) => {
  uploadFile.value = file.raw
}

const loadCategories = async () => {
  try {
    const typesRes = await api.categoryTypes()
    const types = typesRes.data || []
    const allCats = []
    for (const t of types) {
      const catsRes = await api.categories(t.id)
      allCats.push(...(catsRes.data || []))
    }
    categoryList.value = allCats
  } catch (e) {
    ElMessage.error('加载分类失败')
  }
}

const handlePreview = async () => {
  if (!uploadFile.value) return
  const formData = new FormData()
  formData.append('file', uploadFile.value)
  try {
    const res = await api.importPreview(formData)
    previewData.value = res.data || res
    previewForm.value = {
      lawTitle: res.data?.lawTitle || '',
      shortTitle: res.data?.shortTitle || '',
      documentNo: res.data?.documentNo || '',
      issuingAuthority: res.data?.issuingAuthority || '',
      issueDate: res.data?.issueDate || '',
      effectiveDate: res.data?.effectiveDate || ''
    }
    ElMessage.success('预览生成成功')
  } catch (e) {
    ElMessage.error('预览失败')
  }
}

const handleConfirm = async () => {
  try {
    await api.importConfirm({
      ...previewForm.value,
      categoryIds: selectedCategoryIds.value,
      chapters: previewData.value?.chapters || []
    })
    ElMessage.success('导入成功')
    previewData.value = null
    uploadFile.value = null
    uploadRef.value?.clearFiles()
    loadHistory()
  } catch (e) {
    ElMessage.error('导入失败')
  }
}

const loadHistory = async () => {
  try {
    const res = await api.lawImportHistory()
    historyData.value = res.data || []
  } catch (e) {
    ElMessage.error('加载历史失败')
  }
}

onMounted(() => {
  loadCategories()
  loadHistory()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.import-layout { display: flex; gap: 20px; }
.left-upload { width: 300px; flex-shrink: 0; }
.right-preview { flex: 1; }
.preview-header { display: flex; justify-content: space-between; align-items: center; }
</style>
