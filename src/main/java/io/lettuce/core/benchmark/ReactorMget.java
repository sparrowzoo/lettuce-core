package io.lettuce.core.benchmark;

import io.lettuce.core.KeyValue;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import reactor.core.publisher.Flux;

import java.io.IOException;

public class ReactorMget {
    public static void main(String[] args) throws IOException {
        System.setProperty("io.netty.eventLoopThreads", 32 + "");

        // Syntax: redis://[password@]host[:port]
        String redisIpPorts = "192.168.2.10:9000,192.168.2.14:9000,192.168.2.13:9000";
        //redisIpPorts = "10.197.97.16:8001,10.197.97.17:8002,10.197.97.18:8001,10.197.97.16:8002,10.197.97.17:8001,10.197.97.18:8002";
        RedisClusterClient redisClient = RedisClusterClient.create("redis://" + redisIpPorts);
        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();

        RedisStringReactiveCommands<String, String> reactive = connection.reactive();
        int SIZE = 100;
        String[] keys = new String[SIZE];
        for (int i = 0; i < SIZE; i++) {
            keys[i] = "key" + i;
            connection.sync().set(keys[i],"a");
        }
        Flux<KeyValue<String, String>> get = reactive.mget(keys);
        System.out.println("current thread" + Thread.currentThread().getId());
        get.collectList().subscribe(keyValues -> {
            System.out.println(keyValues);
            System.out.println("call back thread" + Thread.currentThread().getId());
        });
        System.out.println("return thread " + Thread.currentThread().getId());
        System.in.read();
    }
}
