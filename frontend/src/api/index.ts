import axios from 'axios'

/**
 * Axios HTTP 客户端实例。
 * <p>
 * 全局配置：
 * - baseURL：通过 VITE_API_BASE_URL 环境变量配置，开发环境默认走 Vite 代理
 * - withCredentials：携带 Session Cookie，支持跨域认证
 * - timeout：10 秒超时
 * <p>
 * 响应拦截器：不做全局 401 跳转（/api/users/me 对游客返回 401 属正常），
 * 受保护页面的跳转由路由守卫负责。
 */
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10000,
  withCredentials: true
})

api.interceptors.response.use(
  response => response,
  error => {
    // 不在此处做全局跳转：/api/users/me 等探测登录态的请求对游客返回 401 属正常情况。
    // 受保护页面的跳转由路由守卫 (router.beforeEach) 负责。
    return Promise.reject(error)
  }
)
