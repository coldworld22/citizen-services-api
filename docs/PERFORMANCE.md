# Query performance

The API has three hot read paths. Each one has an index shaped for its exact `WHERE` and `ORDER BY`,
and each was measured with and without that index.

## How it was measured

[`scripts/query-plans/run.sh`](../scripts/query-plans/run.sh) builds the real schema
(`V1__initial_schema.sql`) in PostgreSQL 16, seeds it with [`seed.sql`](../scripts/query-plans/seed.sql)
and runs `EXPLAIN (ANALYZE, BUFFERS)` for each query with the index dropped and then recreated.

The data is roughly the size of a regional e-services portal: **50,000 citizens, 300,000 requests and
600,000 audit events**. Only 2% of requests are still waiting, like a real queue where most work is
finished.

The [Query plans](../.github/workflows/query-plans.yml) workflow re-runs this whenever a migration
or the benchmark changes. The figures below come from the run on 15 November 2026 on a GitHub-hosted
runner; absolute times vary by machine, but the shape of the plans doesn't.

## Results

| Query | Without index | With index | Pages read |
|---|---|---|---|
| Staff queue: newest `SUBMITTED` requests | 23.5 ms, parallel seq scan + sort | **0.11 ms**, index scan | 6,842 → 23 |
| Citizen: own requests, newest first | 21.7 ms, parallel seq scan + sort | **0.20 ms**, bitmap index scan | 6,842 → 12 |
| Admin: audit trail of one request | 36.0 ms, parallel seq scan + sort | **0.09 ms**, index scan | 10,348 → 5 |

Without the indexes, every page view scans the whole table. That's tens of milliseconds now, and it
grows linearly with the data. With them, the cost depends on the rows returned, not the table size.

## Why the indexes look the way they do

```sql
create index idx_service_requests_status  on service_requests (status, created_at desc);
create index idx_service_requests_citizen on service_requests (citizen_id, created_at desc);
create index idx_audit_events_entity      on audit_events (entity_id, occurred_at desc);
```

- **Filter column first, sort column second.** For the staff queue, PostgreSQL walks the index for
  `status = 'SUBMITTED'` already in `created_at desc` order and stops after 20 rows. No sort node,
  no scan of the other 98% of requests.
- **`desc` matches the API's ordering**, so pages are read in the order they're returned.
- **The citizen query uses a bitmap scan plus a tiny sort** because a citizen only has a handful of
  requests. The planner judged that cheaper than an ordered walk; both are sub-millisecond.
- **No index on `status` alone.** With five values it's too unselective to help any query on its own.
- **Paging is capped in the API** (100 requests, 200 audit events per page), so a client can't turn
  a cheap query into an expensive one.

## Reproduce locally

```bash
docker run -d --name bench -e POSTGRES_PASSWORD=bench -p 5432:5432 postgres:16-alpine
PGHOST=localhost PGUSER=postgres PGPASSWORD=bench PGDATABASE=postgres scripts/query-plans/run.sh
```
