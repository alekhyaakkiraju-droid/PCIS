package com.pcis.batch.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pcis.batch")
public class PcisBatchProperties {

  /** Maximum allowed skip count across all steps before exit code 1 is returned. */
  private int skipThreshold = 0;

  public int getSkipThreshold() {
    return skipThreshold;
  }

  public void setSkipThreshold(int skipThreshold) {
    this.skipThreshold = skipThreshold;
  }
}
