package com.pcis.batch.common;

import org.springframework.batch.core.scope.context.ChunkContext;

@FunctionalInterface
public interface BatchRunLogCountersProvider {

  BatchRunLogCounters resolve(ChunkContext chunkContext);
}
