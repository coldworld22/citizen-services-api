create table users (
    id          uuid primary key,
    national_id varchar(10)  not null unique,
    full_name   varchar(120) not null,
    phone       varchar(20),
    role        varchar(20)  not null check (role in ('CITIZEN', 'EMPLOYEE', 'SUPERVISOR', 'ADMIN')),
    active      boolean      not null default true,
    created_at  timestamptz  not null default now()
);

create table otp_challenges (
    id          uuid primary key,
    national_id varchar(10)  not null,
    code_hash   varchar(100) not null,
    attempts    integer      not null default 0,
    expires_at  timestamptz  not null,
    consumed_at timestamptz,
    created_at  timestamptz  not null
);

create index idx_otp_challenges_lookup on otp_challenges (national_id, created_at desc);

create sequence service_request_ref_seq start with 1;

create table service_requests (
    id            uuid primary key,
    reference_no  varchar(20)   not null unique,
    type          varchar(30)   not null
        check (type in ('BUILDING_PERMIT', 'BUSINESS_LICENSE', 'RESIDENCY_CERTIFICATE', 'COMPLAINT')),
    title         varchar(150)  not null,
    description   varchar(2000) not null,
    status        varchar(20)   not null
        check (status in ('SUBMITTED', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED')),
    citizen_id    uuid          not null references users (id),
    assignee_id   uuid          references users (id),
    decision_note varchar(1000),
    created_at    timestamptz   not null,
    updated_at    timestamptz   not null,
    version       bigint        not null default 0
);

create index idx_service_requests_citizen on service_requests (citizen_id, created_at desc);
create index idx_service_requests_status on service_requests (status, created_at desc);

create table audit_events (
    id          uuid primary key,
    occurred_at timestamptz   not null,
    actor_id    uuid,
    actor_role  varchar(20),
    action      varchar(40)   not null,
    entity_type varchar(40)   not null,
    entity_id   uuid,
    details     varchar(1000),
    ip_address  varchar(45)
);

create index idx_audit_events_entity on audit_events (entity_id, occurred_at desc);
create index idx_audit_events_actor on audit_events (actor_id, occurred_at desc);
