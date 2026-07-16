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
);
