import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

/**
 * Vue Router 路由配置。
 * <p>
 * 路由 meta 字段：
 * - requiresAuth: 需要登录，未登录跳转到登录页
 * - requiresAdmin: 需要 ADMIN 角色
 * - requiresStaff: 需要 ADMIN 或 MODERATOR 角色
 * <p>
 * 兜底路由（/:pathMatch(.*)*）将未匹配的路径重定向到首页。
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/Home.vue')
    },
    {
      path: '/boards',
      name: 'boards',
      component: () => import('../views/Boards.vue')
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/Login.vue')
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/Register.vue')
    },
    {
      path: '/user/:username',
      name: 'user-profile',
      component: () => import('../views/UserProfile.vue')
    },
    {
      path: '/settings/profile',
      name: 'profile-settings',
      component: () => import('../views/ProfileSettings.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/notifications',
      name: 'notifications',
      component: () => import('../views/Notifications.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/posts/new',
      name: 'new-post',
      component: () => import('../views/NewPost.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/posts/:id',
      name: 'post-detail',
      component: () => import('../views/PostDetail.vue')
    },
    {
      path: '/search',
      name: 'search',
      component: () => import('../views/Search.vue')
    },
    {
      path: '/admin/boards',
      name: 'admin-boards',
      component: () => import('../views/AdminBoards.vue'),
      meta: { requiresAuth: true, requiresStaff: true }
    },
    {
      path: '/admin/users',
      name: 'admin-users',
      component: () => import('../views/AdminUsers.vue'),
      meta: { requiresAuth: true, requiresStaff: true }
    },
    // 兜底：未迁移的页面暂时重定向到首页，避免路由匹配警告
    {
      path: '/:pathMatch(.*)*',
      redirect: '/'
    }
    // 其他路由待后续模块迁移时添加
  ]
})

/**
 * 全局路由守卫。
 * <p>
 * 按优先级依次检查：requiresAuth → requiresAdmin → requiresStaff。
 * 权限不足时跳转到首页，未登录时跳转到登录页（携带 redirect 参数）。
 */
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'login', query: { redirect: to.fullPath } })
  } else if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next({ name: 'home' })
  } else if (to.meta.requiresStaff && !userStore.isStaff) {
    next({ name: 'home' })
  } else {
    next()
  }
})

export default router
