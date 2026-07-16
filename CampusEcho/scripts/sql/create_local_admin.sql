insert into users (
    username,
    email,
    password_hash,
    display_name,
    role,
    status
) values (
    'admin',
    'admin@campusecho.local',
    '$2y$12$E7DVndPNAHkd3EhV4LT2GudYLFVM5XNBlucxyIWCS2rEYmK3lvPOW',
    '系统管理员',
    'ADMIN',
    'ACTIVE'
)
on conflict (username) do update
set password_hash = excluded.password_hash,
    display_name = excluded.display_name,
    role = excluded.role,
    status = excluded.status,
    updated_at = current_timestamp;
