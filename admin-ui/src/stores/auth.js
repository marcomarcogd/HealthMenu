import { defineStore } from 'pinia'
import { fetchCurrentUser, login, logout } from '../api/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    currentUser: null,
    initialized: false,
    loadingCurrentUser: false,
  }),
  getters: {
    isLoggedIn: (state) => !!state.currentUser,
  },
  actions: {
    async ensureLoaded() {
      if (this.initialized) {
        return this.currentUser
      }
      return this.loadCurrentUser()
    },
    async loadCurrentUser(options = {}) {
      if (this.loadingCurrentUser) {
        return this.currentUser
      }
      this.loadingCurrentUser = true
      try {
        const data = await fetchCurrentUser({
          silentErrorMessage: options.silentErrorMessage ?? true,
          skipAuthRedirect: options.skipAuthRedirect ?? true,
        })
        this.currentUser = data
        return data
      } catch {
        this.currentUser = null
        return null
      } finally {
        this.initialized = true
        this.loadingCurrentUser = false
      }
    },
    async signIn(payload) {
      const data = await login(payload, {
        silentErrorMessage: true,
        skipAuthRedirect: true,
      })
      this.currentUser = data
      this.initialized = true
      return data
    },
    async signOut() {
      try {
        await logout({
          silentErrorMessage: true,
          skipAuthRedirect: true,
        })
      } finally {
        this.currentUser = null
        this.initialized = true
      }
    },
  },
})
