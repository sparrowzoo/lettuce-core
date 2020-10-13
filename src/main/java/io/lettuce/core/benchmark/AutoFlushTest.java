package io.lettuce.core.benchmark;

import io.lettuce.core.KeyValue;
import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.models.partitions.Partitions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AutoFlushTest {
    public static void main(String[] args) {
        // Syntax: redis://[password@]host[:port]
        String redisIpPorts = "192.168.2.10:9000,192.168.2.14:9000,192.168.2.13:9000";
        //redisIpPorts = "10.197.97.16:8001,10.197.97.17:8002,10.197.97.18:8001,10.197.97.16:8002,10.197.97.17:8001,10.197.97.18:8002";
        RedisClusterClient redisClient = RedisClusterClient.create("redis://" + redisIpPorts);
        ClusterTopologyRefreshOptions clusterTopologyRefreshOptions = ClusterTopologyRefreshOptions.builder()//
                .enablePeriodicRefresh(10, TimeUnit.HOURS)//
                .enableAllAdaptiveRefreshTriggers()//
                .build();

        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder()//
                .topologyRefreshOptions(clusterTopologyRefreshOptions)//
                .build();
        redisClient.setOptions(clusterClientOptions);

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();
        String keys[] = new String[2];
        keys[0] = "aa";
        keys[1] = "bb";
//        List<KeyValue<String,String>> keyValues=   connection.sync().mget(keys);
//        connection.setAutoFlushCommands(false);

        RedisAdvancedClusterAsyncCommands<String, String> cmd = connection.async();
        List<RedisFuture<List<KeyValue<String, String>>>> executions = new ArrayList<>();
        RedisFuture<List<KeyValue<String, String>>> mget1 = cmd.mget(keys);
        RedisFuture<List<KeyValue<String, String>>> mget2 = cmd.mget(keys);
        executions.add(mget1);
        executions.add(mget2);
        cmd.flushCommands();
        boolean result = LettuceFutures.awaitAll(5, TimeUnit.SECONDS,
                executions.toArray(new RedisFuture[executions.size()]));

    }
}
