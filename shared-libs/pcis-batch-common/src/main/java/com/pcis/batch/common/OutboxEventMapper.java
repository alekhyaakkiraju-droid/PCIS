package com.pcis.batch.common;

@FunctionalInterface
public interface OutboxEventMapper<T> {

  OutboxEventSpec map(T item);
}
