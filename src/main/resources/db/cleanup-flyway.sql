-- Cleanup script to remove Flyway schema history
-- Run this manually using: psql -U postgres -d warehouse_db -f cleanup-flyway.sql

DROP TABLE IF EXISTS flyway_schema_history CASCADE;

-- Verify removal
SELECT tablename
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename LIKE 'flyway%';
