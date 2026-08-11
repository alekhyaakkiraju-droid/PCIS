# Key Generation Strategy (WO-155 / Edge Case I-06)

## Rule

| Use | When |
|-----|------|
| **SEQUENCE** | Business document keys that appear on printed documents, external communications, or are exchanged with external systems. These keys must survive parallel runs without collision with legacy Db2 values. |
| **IDENTITY** (`GENERATED ALWAYS AS IDENTITY`) | Internal surrogate keys — table-private, never surfaced to end users or external systems. Values are immaterial outside the database. |

## Rationale

During the Strangler Fig parallel-run period, both the legacy Db2 for i system and the new PostgreSQL 17 instance generate keys simultaneously. Any business key visible to external parties (printed on a policy, shared with a third-party system) **must not collide**. PostgreSQL SEQUENCE objects start at `10,000,000`, which is conservatively above the maximum legacy Db2 sequence value (assumed below `1,000,000` — validated in Phase 0 against production).

Internal surrogate keys (IDENTITY) carry no parallel-run collision risk because they are never shared across systems.

## Range Separation

| System | Range |
|--------|-------|
| Legacy Db2 for i | `1` – `999,999` (estimated; validate before Phase 1 go-live) |
| PostgreSQL (new) | `10,000,000` – `9,999,999,999` |

**Action required before Phase 1**: confirm the highest current Db2 sequence value across all business key sequences and verify it remains below `9,000,000` (with 1M headroom buffer).

## Business Key Tables (SEQUENCE)

These tables hold keys printed on documents or exchanged with external parties. Their primary key column is a formatted VARCHAR populated by application code from the corresponding PostgreSQL SEQUENCE.

| Table | PK Column | PostgreSQL SEQUENCE | JPA Strategy |
|-------|-----------|---------------------|--------------|
| `CUSTOMER_T` | `CUST_ID VARCHAR(10)` | `SEQ_CUSTOMER_ID` | `GenerationType.SEQUENCE` + `@SequenceGenerator(allocationSize=1)` |
| `AGENT_T` | `AGT_ID VARCHAR(8)` | `SEQ_AGENT_ID` | `GenerationType.SEQUENCE` + `@SequenceGenerator(allocationSize=1)` |
| `QUOTE_T` | `QUOTE_ID VARCHAR(12)` | `SEQ_QUOTE_ID` | `GenerationType.SEQUENCE` + `@SequenceGenerator(allocationSize=1)` |
| `POLICY_T` | `POL_NBR VARCHAR(12)` | `SEQ_POLICY_NBR` | `GenerationType.SEQUENCE` + `@SequenceGenerator(allocationSize=1)` |
| `COVERAGE_T` | `COVERAGE_ID VARCHAR(12)` | `SEQ_COVERAGE_ID` | `GenerationType.SEQUENCE` + `@SequenceGenerator(allocationSize=1)` |
| `CLAIM_T` | `CLAIM_ID VARCHAR(12)` | `SEQ_CLAIM_ID` | `GenerationType.SEQUENCE` + `@SequenceGenerator(allocationSize=1)` |
| `CLAIM_ADJUSTER_T` | `ADJUSTER_ID VARCHAR(10)` | `SEQ_ADJUSTER_ID` | `GenerationType.SEQUENCE` + `@SequenceGenerator(allocationSize=1)` |
| `REINSURANCE_TREATY_T` | `TREATY_ID VARCHAR(12)` | `SEQ_TREATY_ID` | `GenerationType.SEQUENCE` + `@SequenceGenerator(allocationSize=1)` |
| `COMMISSION_T` | `COMMISSION_ID` | `SEQ_COMMISSION_ID` | `GenerationType.SEQUENCE` + `@SequenceGenerator(allocationSize=1)` |
| `INVOICE_T` | `INVOICE_ID` | `SEQ_INVOICE_ID` | `GenerationType.SEQUENCE` + `@SequenceGenerator(allocationSize=1)` |
| `PAYMENT_T` | `PAYMENT_ID` | `SEQ_PAYMENT_ID` | `GenerationType.SEQUENCE` + `@SequenceGenerator(allocationSize=1)` |
| `REFUND_T` | `REFUND_ID` | `SEQ_REFUND_ID` | `GenerationType.SEQUENCE` + `@SequenceGenerator(allocationSize=1)` |
| `DOCUMENT_T` | `DOCUMENT_ID` | `SEQ_DOCUMENT_ID` | `GenerationType.SEQUENCE` + `@SequenceGenerator(allocationSize=1)` |
| `COMMISSION_LEDGER_T` | `LEDGER_ID` | `SEQ_LEDGER_ID` | `GenerationType.SEQUENCE` + `@SequenceGenerator(allocationSize=1)` |

### JPA Mapping Pattern (SEQUENCE)

```java
@Entity
@Table(name = "CUSTOMER_T")
public class CustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_customer_id")
    @SequenceGenerator(
        name           = "seq_customer_id",
        sequenceName   = "SEQ_CUSTOMER_ID",
        allocationSize = 1           // MUST be 1 — matches PostgreSQL INCREMENT BY 1
    )
    @Column(name = "CUST_ID")
    private Long custId;
}
```

> **Critical**: `allocationSize = 1` is mandatory. Hibernate's default of 50 would advance the PostgreSQL sequence by 50 for each allocated block, creating large gaps and — worse — allowing the sequence to advance at 50× the normal rate, potentially overlapping the legacy range far sooner than expected.

## Surrogate Key Tables (IDENTITY)

Internal database-only keys. No parallel-run collision risk. Use `GENERATED ALWAYS AS IDENTITY` in DDL and `GenerationType.IDENTITY` in JPA.

| Table | PK Column | JPA Strategy |
|-------|-----------|--------------|
| `APPROVAL_T` | `APPROVAL_ID BIGINT` | `GenerationType.IDENTITY` |
| `AUDIT_LOG_T` | `LOG_ID BIGINT` | `GenerationType.IDENTITY` |
| `AUDIT_LOG_ARCHIVE_T` | `LOG_ID BIGINT` | `GenerationType.IDENTITY` |
| `RPT_RUN_LOG_T` | `RUN_ID BIGINT` | `GenerationType.IDENTITY` |
| `outbox_events` | `ID BIGINT` | `GenerationType.IDENTITY` |
| `AGENT_COMMISSION_T` | `AGT_COMM_ID BIGINT` | `GenerationType.IDENTITY` |
| `AGENT_LICENSE_T` | `LICENSE_ID BIGINT` | `GenerationType.IDENTITY` |
| `BILLING_NOTICE_T` | `NOTICE_ID BIGINT` | `GenerationType.IDENTITY` |
| `BILLING_PLAN_T` | `BILL_PLAN_ID BIGINT` | `GenerationType.IDENTITY` |
| `BILLING_SCHEDULE_T` | `BILL_SCHED_ID BIGINT` | `GenerationType.IDENTITY` |
| `CLAIM_PAYMENT_T` | `PAYMENT_ID BIGINT` | `GenerationType.IDENTITY` |
| `CLAIM_RESERVE_HISTORY_T` | `RES_HIST_ID BIGINT` | `GenerationType.IDENTITY` |
| `CLAIM_RESERVE_T` | `RESERVE_HIST_ID BIGINT` | `GenerationType.IDENTITY` |

### JPA Mapping Pattern (IDENTITY)

```java
@Entity
@Table(name = "APPROVAL_T")
public class ApprovalEntity extends IdentityKeyEntity {

    @AttributeOverride(name = "id", column = @Column(name = "APPROVAL_ID"))
    // id field and @GeneratedValue(IDENTITY) inherited from IdentityKeyEntity
}
```

Or equivalently (if not extending the base class):

```java
@Entity
@Table(name = "APPROVAL_T")
public class ApprovalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "APPROVAL_ID")
    private Long approvalId;
}
```

## Base Entity Classes

See `com.pcis.schema.entity.IdentityKeyEntity` for the reusable `@MappedSuperclass` base for IDENTITY tables.

For SEQUENCE-backed tables, there is no shared base class because each entity uses a different PostgreSQL sequence. Follow the JPA mapping pattern above for each entity.

## CI Gate

`KeyGenerationStrategyTest` (in `shared-libs/pcis-schema`) uses reflection to verify:
1. All classes annotated `@IdentityBacked` use `GenerationType.IDENTITY` on their `@Id` field.
2. All classes annotated `@SequenceBacked` use `GenerationType.SEQUENCE` with `allocationSize = 1` on their `@Id` field.

## Composite Business Keys (Not Sequence-Generated)

Some tables use composite natural keys (no sequence involved):

| Table | Key Columns | Notes |
|-------|-------------|-------|
| `CODE_TABLE_T` | `(CODE_TYPE, CODE_VALUE)` | Static reference data |
| `ROLE_MENU_T` | Composite | Role-to-menu permission mapping |
| `CANCELLATION_REASON_T` | `CANCEL_REASON CHAR(4)` | Short code, admin-managed |

These are mapped with `@EmbeddedId` or `@IdClass` and are not affected by this strategy.
