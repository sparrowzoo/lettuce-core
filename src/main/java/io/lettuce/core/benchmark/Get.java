package io.lettuce.core.benchmark;

import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public class Get {
    public static void main(String[] args) {
        Debugger.getDebugger().setDebug(false);

        // Syntax: redis://[password@]host[:port]
        String redisIpPorts = "192.168.2.10:9000,192.168.2.14:9000,192.168.2.13:9000";
        //redisIpPorts = "10.197.97.16:8001,10.197.97.17:8002,10.197.97.18:8001,10.197.97.16:8002,10.197.97.17:8001,10.197.97.18:8002";
        RedisClusterClient redisClient = RedisClusterClient.create("redis://" + redisIpPorts);
        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();
        String list[] = new String[2];
        list[0] = "aa";
        list[1] = "bb";
        String a=connection.sync().get(list[0]);
        System.out.println(a);
    }
}
