export type UserRole = 'USER' | 'ADMIN'

export type UserStatus = 'ACTIVE' | 'SUSPENDED'

export type ItemCondition = 'NEW' | 'LIKE_NEW' | 'GOOD' | 'FAIR'

export type ItemStatus = 'DRAFT' | 'ON_SALE' | 'RESERVED' | 'SOLD' | 'OFF_SHELF'

export type ItemModerationStatus = 'NORMAL' | 'BLOCKED'

export type TradeStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'COMPLETED'
  | 'DISPUTED'

export type DisputeReason =
  | 'NON_DELIVERY'
  | 'ITEM_NOT_AS_DESCRIBED'
  | 'PAYMENT_ISSUE'
  | 'NO_SHOW'
  | 'OTHER'

export type DisputeStatus = 'OPEN' | 'RESOLVED' | 'REJECTED'

export type DisputeResolution = 'COMPLETE_TRADE' | 'CANCEL_TRADE' | 'NONE'

export type ReportTargetType = 'ITEM' | 'USER'

export type ReportReason =
  | 'FRAUD'
  | 'PROHIBITED_ITEM'
  | 'MISLEADING_INFORMATION'
  | 'HARASSMENT'
  | 'IMPERSONATION'
  | 'OTHER'

export type ReportStatus = 'PENDING' | 'RESOLVED' | 'REJECTED'

export type ReportAction =
  | 'NONE'
  | 'BLOCK_ITEM'
  | 'SUSPEND_USER'
  | 'BLOCK_ITEM_AND_SUSPEND_USER'

export type MessageType =
  | 'TRADE_REQUEST_CREATED'
  | 'TRADE_REQUEST_ACCEPTED'
  | 'TRADE_REQUEST_REJECTED'
  | 'TRADE_REQUEST_CANCELLED'
  | 'TRADE_CONFIRMATION_RECEIVED'
  | 'TRADE_COMPLETED'
  | 'TRADE_CANCELLED_BY_MODERATION'
  | 'REPORT_RESOLVED'
  | 'REPORT_REJECTED'
  | 'ITEM_BLOCKED'
  | 'ITEM_RESTORED'
  | 'ACCOUNT_SUSPENDED'
  | 'DISPUTE_OPENED'
  | 'DISPUTE_RESOLVED'
  | 'DISPUTE_REJECTED'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ProblemDetail {
  type?: string
  title?: string
  status: number
  detail: string
  instance?: string
  code?: string
  violations?: Record<string, string>
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  displayName: string
  phone?: string | null
  campus?: string | null
}

export interface LoginRequest {
  login: string
  password: string
}

export interface CurrentUserResponse {
  id: number
  username: string
  email: string
  displayName: string
  phone: string | null
  campus: string | null
  avatarUrl: string | null
  role: UserRole
  reputationScore: number
  createdAt: string
}

export interface UserProfileResponse {
  id: number
  username: string
  displayName: string
  campus: string | null
  avatarUrl: string | null
  reputationScore: number
  createdAt: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresAt: string
  user: CurrentUserResponse
}

export interface UserProfileUpdateRequest {
  displayName: string
  phone?: string | null
  campus?: string | null
}

export interface CategoryTreeResponse {
  id: number
  name: string
  children: CategoryTreeResponse[]
}

export interface CategoryAdminResponse {
  id: number
  name: string
  parentId: number | null
  sortOrder: number
}

export interface CategoryWriteRequest {
  name: string
  parentId?: number | null
  sortOrder?: number
}

export interface ItemCreateRequest {
  categoryId: number
  title: string
  description: string
  price: number
  condition: ItemCondition
  locationText?: string | null
}

export type ItemUpdateRequest = ItemCreateRequest

export interface ItemImageResponse {
  id: number
  url: string
  sortOrder: number
  primary: boolean
  createdAt: string
}

export interface ItemResponse {
  id: number
  sellerId: number
  categoryId: number
  categoryName: string
  title: string
  description: string
  price: number
  condition: ItemCondition
  status: ItemStatus
  moderationStatus: ItemModerationStatus
  moderationReason: string | null
  locationText: string | null
  createdAt: string
  updatedAt: string
  images: ItemImageResponse[]
}

export interface ReportCreateRequest {
  targetType: ReportTargetType
  targetId: number
  reason: ReportReason
  description: string
}

export interface ReportResponse {
  id: number
  reporterId: number
  reporterUsername: string | null
  targetType: ReportTargetType
  targetId: number
  targetLabel: string | null
  targetOwnerId: number | null
  reason: ReportReason
  description: string
  status: ReportStatus
  action: ReportAction | null
  resolutionNote: string | null
  handledBy: number | null
  handlerUsername: string | null
  handledAt: string | null
  createdAt: string
  updatedAt: string
}

export interface ReportResolutionRequest {
  action: ReportAction
  resolutionNote: string
}

export interface ReportRejectionRequest {
  resolutionNote: string
}

export interface ItemRestoreRequest {
  reason: string
}

export interface ItemModerationResponse {
  itemId: number
  status: ItemStatus
  moderationStatus: ItemModerationStatus
  moderationReason: string | null
  moderatedBy: number | null
  moderatedAt: string | null
}

export interface MessageResponse {
  id: number
  messageType: MessageType
  content: string
  read: boolean
  senderId: number | null
  senderUsername: string | null
  tradeRequestId: number | null
  reportId: number | null
  disputeId: number | null
  itemId: number | null
  createdAt: string
}

export interface DisputeCreateRequest {
  tradeRequestId: number
  reason: DisputeReason
  description: string
}

export interface DisputeResponse {
  id: number
  tradeRequestId: number
  itemId: number
  itemTitle: string | null
  openerId: number
  openerUsername: string | null
  respondentId: number
  respondentUsername: string | null
  reason: DisputeReason
  description: string
  status: DisputeStatus
  resolution: DisputeResolution | null
  resolutionNote: string | null
  handledBy: number | null
  handlerUsername: string | null
  handledAt: string | null
  createdAt: string
  updatedAt: string
}

export interface DisputeResolutionRequest {
  resolution: DisputeResolution
  resolutionNote: string
}

export interface DisputeRejectionRequest {
  resolutionNote: string
}

export interface DisputeListQuery {
  status?: DisputeStatus
  reason?: DisputeReason
  page?: number
  size?: number
}

export interface UnreadCountResponse {
  unreadCount: number
}

export interface MessageListQuery {
  unreadOnly?: boolean
  messageType?: MessageType
  page?: number
  size?: number
}

export interface TradeRequestCreateRequest {
  itemId: number
  message?: string | null
}

export interface TradeRequestResponse {
  id: number
  itemId: number
  itemTitle: string | null
  requesterId: number
  sellerId: number
  message: string | null
  status: TradeStatus
  requesterConfirmed: boolean
  sellerConfirmed: boolean
  createdAt: string
  updatedAt: string
}

export interface ReviewCreateRequest {
  rating: number
  comment?: string | null
}

export interface ReviewResponse {
  id: number
  tradeRequestId: number
  reviewerId: number
  reviewerDisplayName: string | null
  reviewerAvatarUrl: string | null
  revieweeId: number
  rating: number
  comment: string | null
  createdAt: string
}

export interface HealthStatus {
  status: string
  database: string
}

export interface AdminDashboardResponse {
  totalUsers: number
  activeUsers: number
  suspendedUsers: number
  activeAdmins: number
  totalItems: number
  onSaleItems: number
  totalTradeRequests: number
  pendingTradeRequests: number
  completedTradeRequests: number
  totalReviews: number
  totalReports: number
  pendingReports: number
  totalDisputes: number
  openDisputes: number
}

export interface AdminUserResponse {
  id: number
  username: string
  email: string
  displayName: string
  phone: string | null
  campus: string | null
  avatarUrl: string | null
  role: UserRole
  status: UserStatus
  reputationScore: number
  createdAt: string
}

export interface AdminUserListQuery {
  keyword?: string
  role?: UserRole
  status?: UserStatus
  page?: number
  size?: number
}

export class ApiError extends Error {
  readonly status: number
  readonly code?: string
  readonly detail: string
  readonly violations?: Record<string, string>

  constructor(problem: ProblemDetail) {
    super(problem.detail || problem.title || '请求失败')
    this.name = 'ApiError'
    this.status = problem.status
    this.code = problem.code
    this.detail = problem.detail || problem.title || '请求失败'
    this.violations = problem.violations
  }
}
