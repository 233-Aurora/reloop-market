package top.sharpcaterpillar.campusecho.health;

import top.sharpcaterpillar.campusecho.common.api.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ApiResponse<HealthStatus> health() {
        String database = jdbcTemplate.queryForObject("select current_database()", String.class);
        return ApiResponse.success(new HealthStatus("UP", database));
    }
}
