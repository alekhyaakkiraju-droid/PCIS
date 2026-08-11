-- Outbox relay metrics fixtures (WO-143).
-- Mix of published (PUBLISHED) and pending events at varied ages.

CREATE TABLE IF NOT EXISTS outbox_events (
    ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    AGGREGATE_TYPE VARCHAR(100) NOT NULL,
    AGGREGATE_ID VARCHAR(100) NOT NULL,
    EVENT_TYPE VARCHAR(100) NOT NULL,
    PAYLOAD JSONB NOT NULL,
    IDEMPOTENCY_KEY UUID NOT NULL,
    STATUS VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ATTEMPT_COUNT INTEGER NOT NULL DEFAULT 0,
    NEXT_ATTEMPT_AT TIMESTAMP,
    LAST_ERROR VARCHAR(500),
    CRT_USER VARCHAR(10),
    CRT_TIMESTAMP TIMESTAMP,
    UPD_USER VARCHAR(10),
    UPD_TIMESTAMP TIMESTAMP,
    CONSTRAINT uq_outbox_idempotency UNIQUE (IDEMPOTENCY_KEY)
);

CREATE INDEX IF NOT EXISTS idx_outbox_relay ON outbox_events (STATUS, NEXT_ATTEMPT_AT) WHERE STATUS = 'PENDING';

TRUNCATE outbox_events RESTART IDENTITY CASCADE;

-- Published events (should not affect pending metrics)
INSERT INTO outbox_events (
    AGGREGATE_TYPE, AGGREGATE_ID, EVENT_TYPE, PAYLOAD, IDEMPOTENCY_KEY,
    STATUS, CRT_TIMESTAMP
) VALUES
    ('Claim', 'CLM-001', 'ClaimCreated', '{"amount": 100}'::jsonb, '11111111-1111-1111-1111-111111111101', 'PUBLISHED', NOW() - INTERVAL '2 days'),
    ('Claim', 'CLM-002', 'ClaimUpdated', '{"amount": 200}'::jsonb, '11111111-1111-1111-1111-111111111102', 'PUBLISHED', NOW() - INTERVAL '1 day'),
    ('Policy', 'POL-001', 'PolicyIssued', '{}'::jsonb, '11111111-1111-1111-1111-111111111103', 'PUBLISHED', NOW() - INTERVAL '12 hours'),
    ('Policy', 'POL-002', 'PolicyRenewed', '{}'::jsonb, '11111111-1111-1111-1111-111111111104', 'PUBLISHED', NOW() - INTERVAL '6 hours'),
    ('Billing', 'BIL-001', 'InvoicePosted', '{}'::jsonb, '11111111-1111-1111-1111-111111111105', 'PUBLISHED', NOW() - INTERVAL '3 hours');

-- Pending events: 10s, 30s, and 60s ago (oldest = 60s drives lag metric)
INSERT INTO outbox_events (
    AGGREGATE_TYPE, AGGREGATE_ID, EVENT_TYPE, PAYLOAD, IDEMPOTENCY_KEY,
    STATUS, CRT_TIMESTAMP
) VALUES
    ('Claim', 'CLM-010', 'ClaimPaid', '{"amount": 50}'::jsonb, '22222222-2222-2222-2222-222222222201', 'PENDING', NOW() - INTERVAL '10 seconds'),
    ('Claim', 'CLM-011', 'ClaimClosed', '{}'::jsonb, '22222222-2222-2222-2222-222222222202', 'PENDING', NOW() - INTERVAL '30 seconds'),
    ('Billing', 'BIL-010', 'PaymentApplied', '{}'::jsonb, '22222222-2222-2222-2222-222222222203', 'PENDING', NOW() - INTERVAL '60 seconds');
