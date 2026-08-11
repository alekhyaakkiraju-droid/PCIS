package com.pcis.audit.infrastructure.persistence.repository;

import com.pcis.audit.infrastructure.persistence.entity.AuditLogEntity;
import com.pcis.audit.infrastructure.persistence.entity.AuditLogEntityId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, AuditLogEntityId> {}
