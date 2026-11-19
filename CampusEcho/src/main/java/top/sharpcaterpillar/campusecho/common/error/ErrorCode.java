package top.sharpcaterpillar.campusecho.common.error;

public enum ErrorCode {
    VALIDATION_FAILED(400, "请求参数不合法"),
    UNAUTHORIZED(401, "认证失败"),
    FORBIDDEN(403, "没有权限执行此操作"),
    DUPLICATE_RESOURCE(409, "资源已存在"),
    RESOURCE_NOT_FOUND(404, "资源不存在"),
    BUSINESS_RULE_VIOLATION(409, "业务规则不允许此操作"),
    ITEM_UNAVAILABLE(409, "物品当前不可交易"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的文件类型"),
    FILE_TOO_LARGE(413, "文件过大"),
    FILE_STORAGE_ERROR(500, "文件存储失败");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int status() {
        return status;
    }

    public String message() {
        return message;
    }
}
