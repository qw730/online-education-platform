import axios from 'axios'
import { ElMessage } from 'element-plus'

export const TOKEN_KEY = 'oep_token'
export const USER_KEY = 'oep_user'

const request = axios.create({
  baseURL: '/api',
  timeout: 20000
})

/* ------------------------------------------------------------------ request */
request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/* ----------------------------------------------------------------- response */
request.interceptors.response.use(
  (response) => {
    const body = response.data
    // The backend always answers with { code, message, data }.
    if (body && typeof body.code === 'number' && body.code !== 200) {
      ElMessage.error(body.message || 'Request failed')
      return Promise.reject(new Error(body.message || 'Request failed'))
    }
    return body ? body.data : null
  },
  async (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || 'Network error'

    if (status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      if (!window.location.pathname.startsWith('/login')) {
        ElMessage.warning('Your session has expired, please sign in again')
        const { default: router } = await import('@/router')
        router.push({ path: '/login', query: { redirect: window.location.pathname } })
      }
      return Promise.reject(error)
    }

    if (status === 403) {
      ElMessage.error(message || 'You do not have permission to do that')
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default request
