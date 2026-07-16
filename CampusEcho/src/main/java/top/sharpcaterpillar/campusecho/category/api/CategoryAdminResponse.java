package top.sharpcaterpillar.campusecho.category.api;

public record CategoryAdminResponse(Long id, String name, Long parentId, int sortOrder) {
}
