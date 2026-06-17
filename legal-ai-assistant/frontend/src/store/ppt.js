import { defineStore } from 'pinia'
import api from '@/api'

export const usePptStore = defineStore('ppt', {
  state: () => ({
    currentDocument: null,
    documents: [],
    templates: [],
    loading: false,
    generating: false,
    error: null
  }),

  getters: {
    slides: (state) => state.currentDocument?.slides || [],
    currentTemplate: (state) => state.templates.find(t => t.id === state.currentDocument?.templateId) || state.templates[0]
  },

  actions: {
    async generatePpt(title, searchResults, templateId = 'legal-blue') {
      this.generating = true
      this.error = null
      try {
        const response = await api.ppt.generate({
          title,
          searchResults,
          templateId,
          userId: localStorage.getItem('userId') || 'default'
        })
        this.currentDocument = response
        return response
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.generating = false
      }
    },

    async loadDocument(id) {
      this.loading = true
      this.error = null
      try {
        const response = await api.ppt.getById(id)
        this.currentDocument = response
        return response
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    },

    async loadDocumentByUuid(uuid) {
      this.loading = true
      this.error = null
      try {
        const response = await api.ppt.getByUuid(uuid)
        this.currentDocument = response
        return response
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    },

    async updateDocument(id, data) {
      this.loading = true
      this.error = null
      try {
        const response = await api.ppt.update(id, data)
        this.currentDocument = response
        return response
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    },

    async deleteDocument(id) {
      this.loading = true
      this.error = null
      try {
        await api.ppt.delete(id)
        this.documents = this.documents.filter(d => d.id !== id)
        if (this.currentDocument?.id === id) {
          this.currentDocument = null
        }
        return true
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    },

    async listDocuments() {
      this.loading = true
      this.error = null
      try {
        const response = await api.ppt.list()
        this.documents = response
        return response
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    },

    async loadTemplates() {
      try {
        const response = await api.ppt.getTemplates()
        this.templates = response
        return response
      } catch (error) {
        this.error = error.message
        throw error
      }
    },

    async recommendTemplates(scenario) {
      try {
        const response = await api.ppt.recommendTemplates(scenario)
        return response
      } catch (error) {
        this.error = error.message
        throw error
      }
    },

    async downloadPpt(id) {
      try {
        const response = await api.get(`/ppt/${id}/download`, {
          responseType: 'blob'
        })
        return response
      } catch (error) {
        this.error = error.message
        throw error
      }
    },

    addSlide(afterIndex = -1) {
      if (!this.currentDocument) return
      const newSlide = {
        id: `slide-${Date.now()}`,
        layout: 'title_content',
        title: '新幻灯片',
        bulletPoints: ['请输入内容'],
        notes: ''
      }
      if (afterIndex >= 0 && afterIndex < this.currentDocument.slides.length) {
        this.currentDocument.slides.splice(afterIndex + 1, 0, newSlide)
      } else {
        this.currentDocument.slides.push(newSlide)
      }
    },

    deleteSlide(index) {
      if (!this.currentDocument || this.currentDocument.slides.length <= 1) return
      this.currentDocument.slides.splice(index, 1)
    },

    updateSlide(index, data) {
      if (!this.currentDocument || !this.currentDocument.slides[index]) return
      this.currentDocument.slides[index] = {
        ...this.currentDocument.slides[index],
        ...data
      }
    },

    setCurrentDocument(doc) {
      this.currentDocument = doc
    },

    clearError() {
      this.error = null
    }
  }
})
