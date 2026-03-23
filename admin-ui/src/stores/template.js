import { defineStore } from 'pinia'
import { listTemplates } from '../api/template'

export const useTemplateStore = defineStore('template', {
  state: () => ({
    templates: [],
    loading: false,
  }),
  actions: {
    async fetchTemplates() {
      this.loading = true
      try {
        this.templates = await listTemplates()
      } finally {
        this.loading = false
      }
    },
  },
})
