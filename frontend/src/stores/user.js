import { defineStore } from 'pinia'
import { authApi } from '@/api'
import { TOKEN_KEY, USER_KEY } from '@/api/request'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch (error) {
    return null
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readUser()
  }),

  getters: {
    isLoggedIn: (state) => Boolean(state.token && state.user),
    isAdmin: (state) => state.user?.role === 'ADMIN',
    isStudent: (state) => state.user?.role === 'STUDENT',
    displayName: (state) => state.user?.realName || state.user?.username || 'Guest',
    initials: (state) => {
      const name = state.user?.realName || state.user?.username || '?'
      return name
        .split(/\s+/)
        .map((part) => part[0])
        .slice(0, 2)
        .join('')
        .toUpperCase()
    }
  },

  actions: {
    persist(token, user) {
      this.token = token
      this.user = user
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    },

    async login(payload) {
      const data = await authApi.login(payload)
      this.persist(data.token, data.user)
      return data.user
    },

    async register(payload) {
      return authApi.register(payload)
    },

    /** Reloads the profile, also used to validate a stored token on boot. */
    async fetchProfile() {
      const user = await authApi.me()
      this.user = user
      localStorage.setItem(USER_KEY, JSON.stringify(user))
      return user
    },

    async updateProfile(payload) {
      const user = await authApi.updateProfile(payload)
      this.user = user
      localStorage.setItem(USER_KEY, JSON.stringify(user))
      return user
    },

    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
