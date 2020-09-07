package io.lettuce.core.cluster.api;

import io.lettuce.core.AbstractRedisAsyncCommands;
import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisFuture;

import java.util.List;

public interface PipelinedMget<K,V> {
    RedisFuture<List<KeyValue<K,V>>> mget(Iterable keys, AbstractRedisAsyncCommands commands);
}
