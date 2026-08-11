package com.pcis.schema.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SequenceGenerator;

/**
 * Canonical pattern for PCIS entities whose primary key is a business document key
 * generated from a named PostgreSQL SEQUENCE (e.g. {@code SEQ_CUSTOMER_ID}).
 *
 * <p>This class is NOT intended for direct inheritance because each business-key entity uses a
 * different PostgreSQL sequence. Instead, copy the field declaration below into each concrete
 * entity and supply the correct {@code sequenceName} and {@code @Column} values:
 *
 * <pre>{@code
 * @Entity
 * @Table(name = "CUSTOMER_T")
 * public class CustomerEntity {
 *
 *     @Id
 *     @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_customer_id")
 *     @SequenceGenerator(
 *         name           = "seq_customer_id",
 *         sequenceName   = "SEQ_CUSTOMER_ID",
 *         allocationSize = 1          // MUST be 1 — matches PostgreSQL INCREMENT BY 1
 *     )
 *     @Column(name = "CUST_ID")
 *     private Long custId;
 * }
 * }</pre>
 *
 * <h2>allocationSize = 1 is mandatory</h2>
 * Hibernate's default {@code allocationSize = 50} would advance the PostgreSQL sequence
 * by 50 per allocated block. During the parallel-run period this would exhaust the
 * 9,000,000-value gap between PostgreSQL ({@code START WITH 10,000,000}) and legacy Db2
 * values ({@code < 1,000,000}) approximately 50× faster than expected.
 *
 * @see IdentityKeyEntity for surrogate-key tables
 * @see <a href="docs/key-generation-strategy.md">Key Generation Strategy</a>
 */
@MappedSuperclass
public abstract class SequenceKeyEntity {

    /**
     * Subclasses must declare their own {@code @Id} field with the entity-specific
     * {@code @SequenceGenerator} and {@code @Column(name = "...")} annotations.
     * {@code allocationSize} must always be {@code 1}.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_placeholder")
    @SequenceGenerator(
            name           = "seq_placeholder",
            sequenceName   = "SEQ_PLACEHOLDER",
            allocationSize = 1
    )
    private Long id;

    public Long getId() {
        return id;
    }
}
