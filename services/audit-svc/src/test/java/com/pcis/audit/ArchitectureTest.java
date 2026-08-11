package com.pcis.audit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

  private static final JavaClasses CLASSES =
      new ClassFileImporter().importPackages("com.pcis.audit");

  @Test
  void applicationLayerMayDependOnContractAndInfrastructure() {
    assertThat(CLASSES).isNotEmpty();
  }

  @Test
  void domainLayerMustNotDependOnFrameworkTypes() {
    var rule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "jakarta.persistence..",
                "jakarta.servlet..",
                "org.hibernate..")
            .allowEmptyShould(true);

    rule.check(CLASSES);
  }
}
