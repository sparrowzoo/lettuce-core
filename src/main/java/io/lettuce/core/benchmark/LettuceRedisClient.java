package io.lettuce.core.benchmark;

import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;

import java.sql.Connection;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * https://github.com/lettuce-io/lettuce-core/wiki/Connection-Pooling
 * 线程安全的
 */
public class LettuceRedisClient {
    public static void main(String[] args) {
        System.setProperty("io.netty.eventLoopThreads", 32 + "");

        //$ netstat -nat|grep :900|grep ESTABLISHED|wc -l
        //,192.168.2.14:9000,192.168.2.13:9000,192.168.2.10:9001,192.168.2.14:9001,192.168.2.13:9001
        String redisIpPorts = "192.168.2.10:9000";
        //redisIpPorts = "10.197.97.16:8001,10.197.97.17:8002,10.197.97.18:8001,10.197.97.16:8002,10.197.97.17:8001,10.197.97.18:8002";
        RedisClusterClient redisClient = RedisClusterClient.create("redis://" + redisIpPorts);
        ClusterTopologyRefreshOptions clusterTopologyRefreshOptions = ClusterTopologyRefreshOptions.builder()//
                .enablePeriodicRefresh(Duration.ofHours(10))// 定时关闭
                .enableAllAdaptiveRefreshTriggers()//
                .build();

        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder()//
                .topologyRefreshOptions(clusterTopologyRefreshOptions)//
                .build();
        redisClient.setOptions(clusterClientOptions);
        //redisClient.connect().sync();
        //redisClient.connect().reactive();
        RedisAdvancedClusterCommands connection= redisClient.connect().sync();
        while (true) {
           connection.get("a"+new Random().nextInt());
        }

    }
}
