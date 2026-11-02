import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '../types'
import { api } from '../api'

/**
 * 用户状态管理（Pinia Store）。
 * <p>
 * 管理当前登录用户的全局状态，提供登录态探测、角色判断和用户操作。
 * - {@link isLoggedIn} - 是否已登录
 * - {@link isAdmin} - 是否管理员（仅 ADMIN）
 * - {@link isStaff} - 是否管理员或版主（ADMIN 或 MODERATOR）
 */
export const useUserStore = defineStore('user', () => {
  const currentUser = ref<User | null>(null)
  const isLoggedIn = computed(() => currentUser.value !== null)
  const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')
  const isStaff = computed(() => currentUser.value?.role === 'ADMIN' || currentUser.value?.role === 'MODERATOR')

  async function fetchCurrentUser() {
    try {
      const response = await api.get('/api/users/me')
      currentUser.value = response.data.data
    } catch (error) {
      currentUser.value = null
    }
  }

  async function login(username: string, password: string, remember: boolean = false) {
    const response = await api.post('/api/login', { username, password, remember })
    await fetchCurrentUser()
    return response.data
  }

  async function logout() {
    await api.get('/api/logout')
    currentUser.value = null
  }

  async function updateProfile(data: { email?: string; nickname?: string; bio?: string }) {
    const response = await api.put('/api/users/me', data)
    currentUser.value = response.data.data
    return response.data.data
  }

  async function uploadAvatar(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    const response = await api.post('/api/users/me/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    currentUser.value = response.data.data
    return response.data.data
  }

  return {
    currentUser,
    isLoggedIn,
    isAdmin,
    isStaff,
    fetchCurrentUser,
    login,
    logout,
    updateProfile,
    uploadAvatar
  }
})
