alter table items
    add column version bigint not null default 0;

alter table trade_requests
    add column version bigint not null default 0;
