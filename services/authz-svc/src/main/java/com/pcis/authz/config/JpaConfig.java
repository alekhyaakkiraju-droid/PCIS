package com.pcis.authz.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {"com.pcis.authz.infrastructure.persistence.entity", "com.pcis.outbox"})
@EnableJpaRepositories(
    basePackages = {"com.pcis.authz.infrastructure.persistence.repository", "com.pcis.outbox"})
public class JpaConfig {}
