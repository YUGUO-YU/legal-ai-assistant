<template>
  <div class="help-center">
    <div class="page-header">
      <h2>使用帮助</h2>
      <p class="subtitle">了解如何更好地使用法律AI助手</p>
    </div>

    <el-row :gutter="24">
      <el-col :span="16">
        <el-card v-for="section in helpSections" :key="section.title" class="help-section" shadow="hover">
          <template #header>
            <div class="section-header">
              <el-icon><component :is="section.icon" /></el-icon>
              <span>{{ section.title }}</span>
            </div>
          </template>
          <div v-for="item in section.items" :key="item.q" class="faq-item">
            <div class="faq-q" @click="item.open = !item.open">
              <span>{{ item.q }}</span>
              <el-icon class="faq-arrow" :class="{ open: item.open }"><ArrowRight /></el-icon>
            </div>
            <div v-if="item.open" class="faq-a">{{ item.a }}</div>
          </div>
        </el-card>

        <el-card class="help-section" shadow="hover">
          <template #header>
            <div class="section-header">
              <el-icon><Clock /></el-icon>
              <span>快捷键</span>
            </div>
          </template>
          <el-table :data="shortcuts" border size="small">
            <el-table-column prop="key" label="按键" width="120" />
            <el-table-column prop="desc" label="功能" />
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="help-section" shadow="hover">
          <template #header>
            <div class="section-header">
              <el-icon><Lightning /></el-icon>
              <span>功能快速导航</span>
            </div>
          </template>
          <div v-for="feature in features" :key="feature.name" class="feature-item" @click="$router.push(feature.path)">
            <div class="feature-icon" :style="{ background: feature.gradient }">
              <el-icon><component :is="feature.icon" /></el-icon>
            </div>
            <div class="feature-text">
              <span class="feature-name">{{ feature.name }}</span>
              <span class="feature-desc">{{ feature.desc }}</span>
            </div>
            <el-icon class="feature-arrow"><Right /></el-icon>
          </div>
        </el-card>

        <el-card class="help-section contact-card" shadow="hover">
          <template #header>
            <div class="section-header">
              <el-icon><ChatDotRound /></el-icon>
              <span>联系支持</span>
            </div>
          </template>
          <p>如果遇到问题或建议，请通过以下方式联系我们：</p>
          <div class="contact-item">
            <el-icon><Message /></el-icon>
            <span>support@legalaibot.com</span>
          </div>
          <div class="contact-item">
            <el-icon><Phone /></el-icon>
            <span>400-888-8888</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  Search,
  DocumentCopy,
  Connection,
  Stamp,
  ChatDotRound,
  Lightning,
  Clock,
  Message,
  Phone,
  Right,
  OfficeBuilding,
  Collection,
  Box
} from '@element-plus/icons-vue'

const router = useRouter()

const helpSections = ref([
  {
    title: 'AI搜索与检索',
    icon: Search,
    items: [
      { q: 'AI搜法和普通搜索有什么区别？', a: 'AI搜法结合了向量检索和关键词检索，可以理解自然语言查询，自动提取检索条件，同时支持法规的溯源和效力状态查询。', open: false },
      { q: '如何查看法规的历史版本？', a: '在法规详情页面，点击"版本历史"标签页，可以看到该法规的所有修订版本、修正记录以及历次修改的内容对比。', open: false },
      { q: '检索结果可以导出吗？', a: '支持将检索结果导出为Word或PDF格式，方便离线阅读和引用。点击结果右上角的导出按钮即可。', open: false }
    ]
  },
  {
    title: 'AI类案分析',
    icon: Connection,
    items: [
      { q: '类案分析需要输入哪些信息？', a: '输入案件的基本事实描述越详细，类案匹配越精准。系统会自动提取案件要素（当事人、案由、争议焦点等），您也可以手动补充关键信息。', open: false },
      { q: '类案结果支持哪些维度的分析？', a: '支持判决结果分布、裁判要点提取、相似度评分、争议焦点归纳等维度，帮助您全面了解类案裁判趋势。', open: false }
    ]
  },
  {
    title: 'AI合同审查',
    icon: Stamp,
    items: [
      { q: '合同审查支持哪些类型的合同？', a: '目前支持：采购合同、服务合同、劳动合同、租赁合同、保密协议、竞业禁止协议等常见商业合同类型。', open: false },
      { q: '合同审查的风险等级是如何划分的？', a: '系统从8个维度评估合同风险：高危（红色）、中危（橙色）、低危（黄色）、提示（灰色）。每个风险点都标注了法律依据和改进建议。', open: false }
    ]
  },
  {
    title: 'AI文书起草',
    icon: DocumentCopy,
    items: [
      { q: '支持起草哪些类型的法律文书？', a: '支持20种常用法律文书，包括：民事起诉状、答辩状、上诉状、申请书、合同文本、律师函等。选择模板后填写关键信息即可生成。', open: false },
      { q: '起草的文书可以直接使用吗？', a: 'AI生成的文书仅作为起草参考，建议在使用前由专业律师审核确认。系统会标注文书中的不确定内容，提醒您重点关注。', open: false }
    ]
  }
])

const shortcuts = ref([
  { key: '/', desc: '聚焦搜索框（全局）' },
  { key: 'Esc', desc: '关闭弹窗' },
  { key: 'Enter', desc: '确认/提交' },
  { key: 'Tab', desc: '切换焦点' }
])

const features = ref([
  { name: 'AI搜法', desc: '法规智能检索', path: '/legal-search', icon: Search, gradient: 'linear-gradient(135deg, #667eea, #764ba2)' },
  { name: 'AI类案', desc: '相似案例匹配', path: '/case-similar', icon: Connection, gradient: 'linear-gradient(135deg, #f093fb, #f5576c)' },
  { name: 'AI文书', desc: '法律文书起草', path: '/document', icon: DocumentCopy, gradient: 'linear-gradient(135deg, #4facfe, #00f2fe)' },
  { name: '合同审查', desc: '风险条款分析', path: '/contract-review', icon: Stamp, gradient: 'linear-gradient(135deg, #a18cd1, #fbc2eb)' },
  { name: '企业查询', desc: '工商信息检索', path: '/company', icon: OfficeBuilding, gradient: 'linear-gradient(135deg, #fa709a, #fee140)' },
  { name: '知识库', desc: '案例法规库', path: '/knowledge-base', icon: Box, gradient: 'linear-gradient(135deg, #11998e, #38ef7d)' }
])
</script>

<style scoped>
.help-center {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 600;
}

.subtitle {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.help-section {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
}

.faq-item {
  padding: 10px 0;
  border-bottom: 1px solid var(--el-fill-color-light);
}

.faq-item:last-child {
  border-bottom: none;
}

.faq-q {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.faq-q:hover {
  color: var(--el-color-primary);
}

.faq-arrow {
  transition: transform 0.2s;
  flex-shrink: 0;
}

.faq-arrow.open {
  transform: rotate(90deg);
}

.faq-a {
  margin-top: 10px;
  padding: 10px 12px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

.feature-item:hover {
  background: var(--el-fill-color-light);
}

.feature-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.feature-text {
  flex: 1;
  min-width: 0;
}

.feature-name {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.feature-desc {
  display: block;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.feature-arrow {
  color: var(--el-text-color-placeholder);
  flex-shrink: 0;
}

.contact-card p {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--el-text-color-primary);
  margin-bottom: 8px;
}

.contact-item:last-child {
  margin-bottom: 0;
}
</style>
