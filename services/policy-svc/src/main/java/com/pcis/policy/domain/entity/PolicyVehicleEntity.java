package com.pcis.policy.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "policy_vehicle")
public class PolicyVehicleEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "vehicle_id", nullable = false)
  private Long vehicleId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pol_nbr", nullable = false)
  private PolicyEntity policy;

  @Column(name = "vin", length = 17)
  private String vin;

  @Column(name = "model_year")
  private Integer modelYear;

  @Column(name = "make", length = 30)
  private String make;

  @Column(name = "model", length = 30)
  private String model;

  public Long getVehicleId() { return vehicleId; }
  public PolicyEntity getPolicy() { return policy; }
  public void setPolicy(PolicyEntity policy) { this.policy = policy; }
  public String getVin() { return vin; }
  public void setVin(String vin) { this.vin = vin; }
  public Integer getModelYear() { return modelYear; }
  public void setModelYear(Integer modelYear) { this.modelYear = modelYear; }
  public String getMake() { return make; }
  public void setMake(String make) { this.make = make; }
  public String getModel() { return model; }
  public void setModel(String model) { this.model = model; }
}
