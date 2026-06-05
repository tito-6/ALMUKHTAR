CREATE MATERIALIZED VIEW IF NOT EXISTS branch_hourly_stats_view AS
SELECT
    b.id AS branch_id,
    DATE(t.created_at) AS stat_date,
    EXTRACT(HOUR FROM t.created_at)::int AS hour,
    COUNT(*) AS transaction_count,
    COALESCE(SUM(t.amount), 0) AS total_volume,
    0 AS avg_wait_minutes
FROM transactions t
JOIN users u ON t.sender_id = u.id
JOIN branches b ON u.branch_id = b.id
GROUP BY b.id, DATE(t.created_at), EXTRACT(HOUR FROM t.created_at);

CREATE UNIQUE INDEX IF NOT EXISTS idx_branch_hourly_stats_view
    ON branch_hourly_stats_view (branch_id, stat_date, hour);
