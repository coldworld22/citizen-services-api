-- Synthetic data at roughly the scale of a regional e-services portal:
-- 50,000 citizens, 300,000 requests, 600,000 audit events.
insert into users (id, national_id, full_name, role)
select gen_random_uuid(), (1000000000 + g)::text, 'Citizen ' || g, 'CITIZEN'
from generate_series(1, 50000) g;

insert into users (id, national_id, full_name, role) values
    ('00000000-0000-0000-0000-0000000000e1', '1999999991', 'Employee', 'EMPLOYEE');

with citizens as (select id, row_number() over () as n from users where role = 'CITIZEN')
insert into service_requests (id, reference_no, type, title, description, status, citizen_id,
                              assignee_id, created_at, updated_at)
select gen_random_uuid(),
       'REQ-2026-' || lpad(g::text, 6, '0'),
       (array['BUILDING_PERMIT','BUSINESS_LICENSE','RESIDENCY_CERTIFICATE','COMPLAINT'])[1 + g % 4],
       'Request ' || g,
       'Generated request number ' || g,
       -- Most requests are finished; only a few percent are waiting in the queue.
       case when g % 50 = 0 then 'SUBMITTED' when g % 50 = 1 then 'IN_REVIEW'
            when g % 7 = 0 then 'REJECTED' else 'APPROVED' end,
       c.id,
       null,
       timestamp '2024-01-01' + (g * interval '3 minutes'),
       timestamp '2024-01-01' + (g * interval '3 minutes')
from generate_series(1, 300000) g
join citizens c on c.n = 1 + (g % 50000);

insert into audit_events (id, occurred_at, actor_id, actor_role, action, entity_type, entity_id)
select gen_random_uuid(), r.created_at + (k * interval '1 hour'), r.citizen_id, 'CITIZEN',
       case k when 0 then 'REQUEST_SUBMITTED' else 'REQUEST_ASSIGNED' end, 'SERVICE_REQUEST', r.id
from service_requests r cross join generate_series(0, 1) k;

analyze;
