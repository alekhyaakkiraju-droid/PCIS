package com.pcis.authz.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DomainLayerTest {

  @Test
  void domainLayerMarkerIsNotInstantiable() throws Exception {
    var constructor = DomainLayer.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    assertThat(constructor.newInstance()).isNotNull();
  }
}
