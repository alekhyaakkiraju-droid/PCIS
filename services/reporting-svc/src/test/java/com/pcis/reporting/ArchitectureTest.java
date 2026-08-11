package com.pcis.reporting;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
  @Test
  void domainLayerMustNotDependOnFrameworkTypes() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("org.springframework..", "jakarta.persistence..")
        .allowEmptyShould(true)
        .check(new ClassFileImporter().importPackages("com.pcis.reporting"));
  }
}
