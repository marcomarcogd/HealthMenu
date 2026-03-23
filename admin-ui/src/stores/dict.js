import { defineStore } from 'pinia'
import { listDictItems, listDictTypes } from '../api/dict'

export const useDictStore = defineStore('dict', {
  state: () => ({
    types: [],
    items: [],
    activeTypeId: null,
    loading: false,
  }),
  actions: {
    async fetchTypes() {
      this.loading = true
      try {
        this.types = await listDictTypes()
        if (!this.activeTypeId && this.types.length) {
          this.activeTypeId = this.types[0].id
        }
      } finally {
        this.loading = false
      }
    },
    async fetchItems(dictTypeId) {
      if (!dictTypeId) {
        this.items = []
        return
      }
      this.activeTypeId = dictTypeId
      this.items = await listDictItems(dictTypeId)
    },
  },
})
