package top.sharpcaterpillar.campusecho.admin.api;

public record AdminDashboardResponse(
        long totalUsers,
        long activeUsers,
        long suspendedUsers,
        long activeAdmins,
        long totalItems,
        long onSaleItems,
        long totalTradeRequests,
        long pendingTradeRequests,
        long completedTradeRequests,
        long totalReviews,
        long totalReports,
        long pendingReports,
        long totalDisputes,
        long openDisputes) {
}
