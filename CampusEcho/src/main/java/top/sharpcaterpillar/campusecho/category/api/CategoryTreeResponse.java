package top.sharpcaterpillar.campusecho.category.api;

import java.util.List;

public record CategoryTreeResponse(Long id, String name, List<CategoryTreeResponse> children) {
}
