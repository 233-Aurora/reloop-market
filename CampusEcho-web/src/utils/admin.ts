import type { UserRole, UserStatus } from '../api/types'

export function roleLabel(role: UserRole): string {
  return role === 'ADMIN' ? '管理员' : '普通用户'
}

export function statusLabel(status: UserStatus): string {
  return status === 'ACTIVE' ? '正常' : '已停用'
}

export function canChangeUserStatus(
  actorId: number,
  target: { id: number; role: UserRole; status: UserStatus },
  next: UserStatus,
): string | null {
  if (actorId === target.id && next !== 'ACTIVE') {
    return '不能停用当前登录的管理员账户'
  }
  return null
}

export function canChangeUserRole(
  actorId: number,
  target: { id: number; role: UserRole; status: UserStatus },
  next: UserRole,
): string | null {
  if (actorId === target.id && next !== 'ADMIN') {
    return '不能降级当前登录的管理员账户'
  }
  return null
}
