alter table messages
    add column report_id bigint references reports (id) on delete set null,
    add column item_id bigint references items (id) on delete set null;

alter table messages
    add constraint ck_messages_message_type check (
        message_type in (
            'TRADE_REQUEST_CREATED',
            'TRADE_REQUEST_ACCEPTED',
            'TRADE_REQUEST_REJECTED',
            'TRADE_REQUEST_CANCELLED',
            'TRADE_CONFIRMATION_RECEIVED',
            'TRADE_COMPLETED',
            'TRADE_CANCELLED_BY_MODERATION',
            'REPORT_RESOLVED',
            'REPORT_REJECTED',
            'ITEM_BLOCKED',
            'ITEM_RESTORED',
            'ACCOUNT_SUSPENDED'
        )
    );

create index idx_messages_report_id on messages (report_id);
create index idx_messages_item_id on messages (item_id);
create index idx_messages_trade_request_id on messages (trade_request_id);
