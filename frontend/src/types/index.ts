/** 用户信息 */
export interface User {
  id: number
  username: string
  email: string
  nickname?: string
  avatarPath?: string
  bio?: string
  role: 'USER' | 'ADMIN' | 'MODERATOR'
  status?: 'ACTIVE' | 'INACTIVE' | 'BANNED'
  createdAt: string
}

/** 板块信息 */
export interface Board {
  id: number
  name: string
  slug: string
  description: string
  postCount?: number
  sortOrder?: number
  createdAt?: string
  icon?: string
}

/** 帖子（资源）信息 */
export interface Post {
  id: number
  title: string
  content: string
  boardId: number
  boardName: string
  boardSlug: string
  userId: number
  username: string
  nickname?: string
  avatarPath?: string
  viewCount: number
  likeCount: number
  isPinned: boolean
  isLocked: boolean
  createdAt: string
  lastRepliedAt?: string
  updatedAt: string
}

/**
 * 评论/回复信息。
 * parentId/rootId 用于楼中楼层级关系，null 表示顶层评论。
 */
export interface Reply {
  id: number
  content: string
  postId: number
  postTitle: string
  boardName: string
  boardSlug: string
  parentId?: number | null
  rootId?: number | null
  authorNickname: string
  authorUsername: string
  likeCount: number
  status: 'NORMAL' | 'DELETED'
  createdAt: string
}

/** 通知信息 */
export interface Notification {
  id: number
  type: 'REPLY' | 'LIKE'
  content: string
  targetUrl: string
  isRead: boolean
  createdAt: string
}
