select version || ':' || success
from flyway_schema_history
order by installed_rank;
