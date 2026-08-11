-- V2: AUDIT_LOG_T monthly range partition maintenance (WO-156)
-- Pre-creates partitions so retention and audit writes never fail for a missing future partition.

CREATE OR REPLACE FUNCTION maintain_audit_log_t_partitions(months_ahead INT DEFAULT 3)
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
            'audit_log_t_y%sm%s',
            to_char(partition_start, 'YYYY'),
            to_char(partition_start, 'MM'));

        IF to_regclass(partition_name) IS NULL THEN
            EXECUTE format(
                'CREATE TABLE %I PARTITION OF AUDIT_LOG_T FOR VALUES FROM (%L) TO (%L)',
                partition_name,
                partition_start,
                partition_end);
            created_count := created_count + 1;
        END IF;
    END LOOP;

    RETURN created_count;
END;
$$;

COMMENT ON FUNCTION maintain_audit_log_t_partitions(INT) IS
    'Creates missing monthly AUDIT_LOG_T partitions for the current month plus months_ahead.';

-- Detach expired partitions without row-level DELETE (metadata-only retention hook).
CREATE OR REPLACE FUNCTION detach_audit_log_t_partition(partition_table REGCLASS)
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    IF partition_table IS NULL THEN
        RAISE EXCEPTION 'partition_table must not be null';
    END IF;
    EXECUTE format('ALTER TABLE AUDIT_LOG_T DETACH PARTITION %s', partition_table);
END;
$$;

COMMENT ON FUNCTION detach_audit_log_t_partition(REGCLASS) IS
    'Detaches an expired AUDIT_LOG_T monthly partition as a metadata operation (no DELETE FROM AUDIT_LOG_T).';

-- Ensure upcoming partitions exist at migration time.
SELECT maintain_audit_log_t_partitions(3);
