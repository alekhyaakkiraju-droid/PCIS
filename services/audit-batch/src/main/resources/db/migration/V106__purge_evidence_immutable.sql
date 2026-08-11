-- WO-171: enforce append-only purge_evidence at the database layer

CREATE OR REPLACE FUNCTION purge_evidence_immutable()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'purge_evidence records are immutable';
END;
$$;

CREATE TRIGGER trg_purge_evidence_immutable
    BEFORE UPDATE OR DELETE ON purge_evidence
    FOR EACH ROW EXECUTE FUNCTION purge_evidence_immutable();
