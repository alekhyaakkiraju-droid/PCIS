package com.pcis.audit.infrastructure.persistence.repository;

import com.pcis.audit.infrastructure.persistence.entity.AuditIngestionIdempotencyEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditIngestionIdempotencyRepository
    extends JpaRepository<AuditIngestionIdempotencyEntity, UUID> {}
