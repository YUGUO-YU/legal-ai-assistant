<template>
  <div class="page-card">
    <div class="page-header">
      <h2>AI文书起草</h2>
      <p>选择文书模板，输入案件信息，智能生成法律文书</p>
    </div>

    <el-row :gutter="24">
      <el-col :span="8">
        <div class="template-list">
          <div class="search-box">
            <el-input v-model="templateSearch" placeholder="搜索模板" clearable prefix-icon="Search" />
          </div>

          <el-tabs v-model="activeCategory" class="category-tabs">
            <el-tab-pane label="民事诉讼" name="民事诉讼">
              <div class="template-grid">
                <div
                  v-for="tpl in filteredTemplatesByCategory('民事诉讼')"
                  :key="tpl.templateCode"
                  :class="['template-card', { active: selectedTemplate === tpl.templateCode }]"
                  @click="selectTemplate(tpl)"
                >
                  <div class="template-icon">
                    <el-icon><Document /></el-icon>
                  </div>
                  <div class="template-info">
                    <span class="template-name">{{ tpl.templateName }}</span>
                    <el-tag v-if="tpl.popular" type="warning" size="small">常用</el-tag>
                  </div>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="劳动人事" name="劳动人事">
              <div class="template-grid">
                <div
                  v-for="tpl in filteredTemplatesByCategory('劳动人事')"
                  :key="tpl.templateCode"
                  :class="['template-card', { active: selectedTemplate === tpl.templateCode }]"
                  @click="selectTemplate(tpl)"
                >
                  <div class="template-icon">
                    <el-icon><User /></el-icon>
                  </div>
                  <div class="template-info">
                    <span class="template-name">{{ tpl.templateName }}</span>
                    <el-tag v-if="tpl.popular" type="warning" size="small">常用</el-tag>
                  </div>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="商业函件" name="商业函件">
              <div class="template-grid">
                <div
                  v-for="tpl in filteredTemplatesByCategory('商业函件')"
                  :key="tpl.templateCode"
                  :class="['template-card', { active: selectedTemplate === tpl.templateCode }]"
                  @click="selectTemplate(tpl)"
                >
                  <div class="template-icon">
                    <el-icon><Message /></el-icon>
                  </div>
                  <div class="template-info">
                    <span class="template-name">{{ tpl.templateName }}</span>
                    <el-tag v-if="tpl.popular" type="warning" size="small">常用</el-tag>
                  </div>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="知识产权" name="知识产权">
              <div class="template-grid">
                <div
                  v-for="tpl in filteredTemplatesByCategory('知识产权')"
                  :key="tpl.templateCode"
                  :class="['template-card', { active: selectedTemplate === tpl.templateCode }]"
                  @click="selectTemplate(tpl)"
                >
                  <div class="template-icon">
                    <el-icon><Connection /></el-icon>
                  </div>
                  <div class="template-info">
                    <span class="template-name">{{ tpl.templateName }}</span>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-col>
      <el-col :span="16">
        <div class="form-area">
          <div class="selected-template-info" v-if="selectedTemplate">
            <el-alert
              :title="'已选择：' + getTemplateName(selectedTemplate)"
              type="info"
              :closable="false"
              show-icon
            />
          </div>

          <el-form :model="formData" label-width="120px" :rules="formRules" ref="formRef">
            <el-form-item label="案件类型" prop="caseType">
              <el-select v-model="formData.caseType" placeholder="请选择案件类型">
                <el-option label="合同纠纷" value="contract" />
                <el-option label="劳动争议" value="labor" />
                <el-option label="侵权纠纷" value="tort" />
                <el-option label="婚姻家庭" value="family" />
                <el-option label="借款纠纷" value="loan" />
                <el-option label="建设工程" value="construction" />
              </el-select>
            </el-form-item>

            <el-form-item label="原告/申请人" prop="plaintiffName">
              <el-input v-model="formData.plaintiffName" placeholder="请输入原告或申请人姓名">
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="被告/被申请人" prop="defendantName">
              <el-input v-model="formData.defendantName" placeholder="请输入被告或被申请人姓名">
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="诉讼请求" prop="claimAmount">
              <el-input-number v-model="formData.claimAmount" :min="0" :precision="2" placeholder="请输入金额" style="width: 100%">
                <template #append>元</template>
              </el-input-number>
            </el-form-item>

            <el-form-item label="请求描述" prop="claimDescription">
              <el-input v-model="formData.claimDescription" type="textarea" :rows="3" placeholder="请简要描述诉讼请求" />
            </el-form-item>

            <el-form-item label="事实与理由" prop="facts">
              <el-input v-model="formData.facts" type="textarea" :rows="5" placeholder="请详细描述案件事实与理由" />
            </el-form-item>

            <el-form-item label="管辖法院">
              <el-input v-model="formData.courtName" placeholder="请输入管辖法院名称">
                <template #prefix><el-icon><OfficeBuilding /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleDraft" :loading="loading" :disabled="!selectedTemplate">
                <el-icon v-if="!loading"><Document /></el-icon>
                生成文书
              </el-button>
              <el-button @click="resetForm">
                <el-icon><Refresh /></el-icon>
                重置
              </el-button>
            </el-form-item>
          </el-form>

          <loading v-if="loading" text="正在生成法律文书..." />
        </div>
      </el-col>
    </el-row>

    <el-drawer v-model="showResult" title="生成结果" size="60%" direction="rtl">
      <div v-if="draftResult" class="result-content">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="文书正文" name="content">
            <pre class="document-content">{{ draftResult.documentContent }}</pre>
          </el-tab-pane>
          <el-tab-pane label="风险提示" name="risk">
            <div class="risk-content" v-html="draftResult.riskPrompt"></div>
          </el-tab-pane>
          <el-tab-pane label="免责声明" name="disclaimer">
            <el-alert type="warning" :closable="false" show-icon>
              <template #title>{{ draftResult.disclaimer }}</template>
            </el-alert>
          </el-tab-pane>
          <el-tab-pane label="法律依据" name="laws">
            <div class="laws-list">
              <div v-for="law in draftResult.referencedLaws" :key="law" class="law-item">
                <el-icon><Document /></el-icon>
                {{ law }}
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>

        <div class="actions">
          <el-button type="primary" @click="copyDocument">
            <el-icon><CopyDocument /></el-icon>
            复制文书
          </el-button>
          <el-button @click="downloadDocument">
            <el-icon><Download /></el-icon>
            下载文档
          </el-button>
          <el-button @click="printDocument">
            <el-icon><Printer /></el-icon>
            打印
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import Loading from '../components/Loading.vue'

const formRef = ref(null)
const loading = ref(false)
const draftResult = ref(null)
const activeTab = ref('content')
const selectedTemplate = ref('')
const activeCategory = ref('民事诉讼')
const templateSearch = ref('')
const showResult = ref(false)

const formData = reactive({
  plaintiffName: '',
  defendantName: '',
  caseType: 'contract',
  claimAmount: 0,
  claimDescription: '',
  facts: '',
  courtName: ''
})

const formRules = {
  plaintiffName: [
    { required: true, message: '请输入原告/申请人姓名', trigger: 'blur' }
  ],
  defendantName: [
    { required: true, message: '请输入被告/被申请人姓名', trigger: 'blur' }
  ],
  caseType: [
    { required: true, message: '请选择案件类型', trigger: 'change' }
  ]
}

const templates = ref([])

const filteredTemplatesByCategory = (category) => {
  return templates.value.filter(t =>
    t.category === category &&
    (!templateSearch.value || t.templateName.includes(templateSearch.value))
  )
}

const selectTemplate = (tpl) => {
  selectedTemplate.value = tpl.templateCode
  ElMessage.success(`已选择模板：${tpl.templateName}`)
}

const getTemplateName = (code) => {
  const tpl = templates.value.find(t => t.templateCode === code)
  return tpl ? tpl.templateName : code
}

const handleDraft = async () => {
  if (!selectedTemplate.value) {
    ElMessage.warning('请先选择文书模板')
    return
  }

  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const res = await api.document.draft({
        templateCode: selectedTemplate.value,
        caseType: formData.caseType,
        caseData: {
          plaintiffName: formData.plaintiffName,
          defendantName: formData.defendantName,
          claimAmount: formData.claimAmount,
          claimDescription: formData.claimDescription,
          facts: formData.facts,
          courtName: formData.courtName
        },
        includeRiskPrompt: true
      })

      draftResult.value = res.data
      showResult.value = true
      ElMessage.success('文书生成成功')
    } catch (e) {
      console.error(e)
      ElMessage.error('生成失败，请稍后重试')
    } finally {
      loading.value = false
    }
  })
}

const resetForm = () => {
  formRef.value?.resetFields()
  draftResult.value = null
}

const copyDocument = () => {
  if (draftResult.value?.documentContent) {
    navigator.clipboard.writeText(draftResult.value.documentContent)
    ElMessage.success('已复制到剪贴板')
  }
}

const downloadDocument = () => {
  ElMessage.info('下载功能开发中...')
}

const printDocument = () => {
  window.print()
}

const loadTemplates = async () => {
  try {
    const res = await api.document.getTemplates()
    templates.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  loadTemplates()
})
</script>

<style lang="scss" scoped>
.template-list {
  background: #fafafa;
  padding: 16px;
  border-radius: 8px;
  height: fit-content;

  .search-box {
    margin-bottom: 16px;
  }
}

.category-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 12px;
  }
}

.template-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.template-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    border-color: #1890ff;
    background: #e6f7ff;
  }

  &.active {
    border-color: #1890ff;
    background: #e6f7ff;
  }

  .template-icon {
    width: 40px;
    height: 40px;
    background: #fff;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #1890ff;
  }

  .template-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 4px;

    .template-name {
      font-size: 14px;
      color: #333;
    }
  }
}

.form-area {
  background: #fff;
  padding: 24px;
  border-radius: 8px;

  .selected-template-info {
    margin-bottom: 20px;
  }
}

.result-content {
  padding: 20px;

  .document-content {
    background: #fafafa;
    padding: 20px;
    border-radius: 8px;
    white-space: pre-wrap;
    font-family: 'Songti SC', 'SimSun', serif;
    line-height: 1.8;
    font-size: 14px;
    max-height: 500px;
    overflow-y: auto;
  }

  .risk-content {
    padding: 16px;
    background: #fffbe6;
    border-radius: 8px;
    line-height: 1.8;
  }

  .laws-list {
    .law-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px;
      border-bottom: 1px solid #f0f0f0;
      color: #333;
    }
  }

  .actions {
    margin-top: 24px;
    display: flex;
    gap: 12px;
    padding-top: 20px;
    border-top: 1px solid #f0f0f0;
  }
}
</style>