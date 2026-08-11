package com.pcis.premium.contract;

import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves the canonical premium rating contract from the repository root. */
public final class PremiumRatingContractSupport {

  private PremiumRatingContractSupport() {}

  public static Path contractPath() {
    Path dir = Path.of("").toAbsolutePath();
    while (dir != null && !Files.isRegularFile(dir.resolve("contracts/premium-rating-v1.yaml"))) {
      dir = dir.getParent();
    }
    if (dir == null) {
      throw new IllegalStateException("contracts/premium-rating-v1.yaml not found from cwd");
    }
    return dir.resolve("contracts/premium-rating-v1.yaml");
  }

  public static String contractYaml() {
    try {
      return Files.readString(contractPath());
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to read premium rating contract", ex);
    }
  }
}
