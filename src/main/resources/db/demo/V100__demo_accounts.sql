-- Demo accounts with made-up IDs. Not loaded when FLYWAY_LOCATIONS excludes db/demo.
insert into users (id, national_id, full_name, phone, role) values
    ('00000000-0000-0000-0000-000000000001', '1000000001', 'Demo Citizen',    '0500000001', 'CITIZEN'),
    ('00000000-0000-0000-0000-000000000002', '1000000002', 'Demo Employee',   '0500000002', 'EMPLOYEE'),
    ('00000000-0000-0000-0000-000000000003', '1000000003', 'Demo Supervisor', '0500000003', 'SUPERVISOR'),
    ('00000000-0000-0000-0000-000000000004', '1000000004', 'Demo Admin',      '0500000004', 'ADMIN'),
    ('00000000-0000-0000-0000-000000000005', '2000000005', 'Demo Resident',   '0500000005', 'CITIZEN');
