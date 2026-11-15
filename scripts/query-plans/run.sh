#!/usr/bin/env bash
# Measures the three hot queries with and without their indexes and prints the plans.
# Usage: PGHOST=... PGUSER=... PGPASSWORD=... PGDATABASE=... scripts/query-plans/run.sh
set -euo pipefail
cd "$(dirname "$0")"

psql -v ON_ERROR_STOP=1 -q -f ../../src/main/resources/db/migration/V1__initial_schema.sql
psql -v ON_ERROR_STOP=1 -q -f seed.sql

CITIZEN=$(psql -tAc "select citizen_id from service_requests limit 1")
REQUEST=$(psql -tAc "select id from service_requests order by created_at desc limit 1")

measure() {  # name, index to drop, recreate statement, query
  local name="$1" index="$2" create="$3" query="$4"
  echo "=================== $name ==================="
  psql -q -c "drop index $index"
  echo "--- without index"
  psql -tA -c "explain (analyze, buffers, costs off) $query"
  psql -q -c "$create" -c "analyze"
  echo "--- with index"
  psql -tA -c "explain (analyze, buffers, costs off) $query"
}

measure "Staff queue: newest submitted requests" \
  idx_service_requests_status \
  "create index idx_service_requests_status on service_requests (status, created_at desc)" \
  "select * from service_requests where status = 'SUBMITTED' order by created_at desc limit 20"

measure "Citizen: my requests" \
  idx_service_requests_citizen \
  "create index idx_service_requests_citizen on service_requests (citizen_id, created_at desc)" \
  "select * from service_requests where citizen_id = '$CITIZEN' order by created_at desc limit 20"

measure "Admin: audit trail for one request" \
  idx_audit_events_entity \
  "create index idx_audit_events_entity on audit_events (entity_id, occurred_at desc)" \
  "select * from audit_events where entity_id = '$REQUEST' order by occurred_at desc limit 50"
