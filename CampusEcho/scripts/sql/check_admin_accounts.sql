select id, username, role, status, created_at
from users
where role = 'ADMIN' or username = 'admin'
order by id;
