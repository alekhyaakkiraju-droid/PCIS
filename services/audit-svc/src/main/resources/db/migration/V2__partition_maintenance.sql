-- V2: partition maintenance for audit_log monthly range partitions

CREATE OR REPLACE FUNCTION maintain_audit_log_partitions(months_ahead INT DEFAULT 3)
RETURNS INT
LANGUAGE plpgsql
AS $$
DECLARE
    partition_start DATE;
    partition_end DATE;
    partition_name TEXT;
    created_count INT := 0;
    month_offset INT;
BEGIN
    IF months_ahead < 1 THEN
        RAISE EXCEPTION 'months_ahead must be at least 1';
    END IF;

    FOR month_offset IN 0..months_ahead LOOP
        partition_start := date_trunc('month', CURRENT_DATE + (month_offset || ' months')::INTERVAL)::DATE;
        partition_end := (partition_start + INTERVAL '1 month')::DATE;
        partition_name := format(
            'audit_log_y%sm%s',
            to_char(partition_start, 'YYYY'),
            to_char(partition_start, 'MM'));

        IF to_regclass(partition_name) IS NULL THEN
            EXECUTE format(
                'CREATE TABLE %I PARTITION OF audit_log FOR VALUES FROM (%L) TO (%L)',
                partition_name,
                partition_start,
                partition_end);
            EXECUTE format('GRANT INSERT, SELECT ON %I TO pcis_audit_app', partition_name);
            EXECUTE format('REVOKE UPDATE, DELETE ON %I FROM pcis_audit_app', partition_name);
            created_count := created_count + 1;
        END IF;
    END LOOP;

    RETURN created_count;
END;
$$;

COMMENT ON FUNCTION maintain_audit_log_partitions(INT) IS
    'Creates missing monthly audit_log partitions for the current month plus months_ahead.';

-- Ensure upcoming partitions exist at migration time.
SELECT maintain_audit_log_partitions(3);
