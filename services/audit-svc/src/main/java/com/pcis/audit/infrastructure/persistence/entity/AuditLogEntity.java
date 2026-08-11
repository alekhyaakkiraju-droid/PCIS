package com.pcis.audit.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@IdClass(AuditLogEntityId.class)
public class AuditLogEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "audit_log_id", nullable = false, updatable = false)
  private Long auditLogId;

  @Id
  @Column(name = "event_timestamp", nullable = false)
  private Instant eventTimestamp;

  @Column(name = "action_cd", nullable = false, length = 10)
  private String actionCd;

  @Column(name = "old_value", length = 100)
  private String oldValue;

  @Column(name = "new_value", length = 100)
  private String newValue;

  @Column(name = "key_value", length = 40)
  private String keyValue;

  @Column(name = "field_name", length = 30)
  private String fieldName;

  @Column(name = "correlation_id", nullable = false)
  private UUID correlationId;

  @Column(name = "service_name", nullable = false, length = 64)
  private String serviceName;

  @Column(name = "program_name", length = 10)
  private String programName;

  @Column(name = "actor", nullable = false, length = 10)
  private String actor;

  @Column(name = "resource_name", nullable = false, length = 50)
  private String resourceName;

  @Column(name = "operation", nullable = false, length = 30)
  private String operation;

  public Long getAuditLogId() {
    return auditLogId;
  }

  public void setAuditLogId(Long auditLogId) {
    this.auditLogId = auditLogId;
  }

  public String getActionCd() {
    return actionCd;
  }

  public void setActionCd(String actionCd) {
    this.actionCd = actionCd;
  }

  public String getOldValue() {
    return oldValue;
  }

  public void setOldValue(String oldValue) {
    this.oldValue = oldValue;
  }

  public String getNewValue() {
    return newValue;
  }

  public void setNewValue(String newValue) {
    this.newValue = newValue;
  }

  public String getKeyValue() {
    return keyValue;
  }

  public void setKeyValue(String keyValue) {
    this.keyValue = keyValue;
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public UUID getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(UUID correlationId) {
    this.correlationId = correlationId;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getProgramName() {
    return programName;
  }

  public void setProgramName(String programName) {
    this.programName = programName;
  }

  public String getActor() {
    return actor;
  }

  public void setActor(String actor) {
    this.actor = actor;
  }

  public String getResourceName() {
    return resourceName;
  }

  public void setResourceName(String resourceName) {
    this.resourceName = resourceName;
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public Instant getEventTimestamp() {
    return eventTimestamp;
  }

  public void setEventTimestamp(Instant eventTimestamp) {
    this.eventTimestamp = eventTimestamp;
  }
}
