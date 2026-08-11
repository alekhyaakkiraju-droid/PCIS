package com.pcis.classification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pcis.classification")
public class DataClassificationProperties {

  /** Spring resource location for pcis-data-classification.yaml. */
  private String registryLocation = "file:config/pcis-data-classification.yaml";

  /** When true, startup fails if registry entities/columns drift from information_schema. */
  private boolean driftDetectionEnabled = true;

  /** When true, upserts registry rows into data_classification at startup. */
  private boolean loaderEnabled = true;

  public String getRegistryLocation() {
    return registryLocation;
  }

  public void setRegistryLocation(String registryLocation) {
    this.registryLocation = registryLocation;
  }

  public boolean isDriftDetectionEnabled() {
    return driftDetectionEnabled;
  }

  public void setDriftDetectionEnabled(boolean driftDetectionEnabled) {
    this.driftDetectionEnabled = driftDetectionEnabled;
  }

  public boolean isLoaderEnabled() {
    return loaderEnabled;
  }

  public void setLoaderEnabled(boolean loaderEnabled) {
    this.loaderEnabled = loaderEnabled;
  }
}
