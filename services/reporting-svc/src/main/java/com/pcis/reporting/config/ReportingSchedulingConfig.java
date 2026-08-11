package com.pcis.reporting.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "pcis.reporting.datasource", name = "url")
public class ReportingSchedulingConfig {}
