package com.pcis.authz.infrastructure.persistence.entity;

import com.pcis.authz.infrastructure.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "role_permission")
@IdClass(RolePermissionId.class)
public class RolePermissionEntity extends AuditableEntity {

  @Id
  @Column(name = "role_id")
  private Long roleId;

  @Id
  @Column(name = "permission_id")
  private Long permissionId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "role_id", insertable = false, updatable = false)
  private RoleEntity role;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "permission_id", insertable = false, updatable = false)
  private PermissionEntity permission;

  public Long getRoleId() {
    return roleId;
  }

  public void setRoleId(Long roleId) {
    this.roleId = roleId;
  }

  public Long getPermissionId() {
    return permissionId;
  }

  public void setPermissionId(Long permissionId) {
    this.permissionId = permissionId;
  }

  public RoleEntity getRole() {
    return role;
  }

  public PermissionEntity getPermission() {
    return permission;
  }
}
