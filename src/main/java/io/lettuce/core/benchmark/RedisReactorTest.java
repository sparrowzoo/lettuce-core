package io.lettuce.core.benchmark;

import io.lettuce.core.KeyValue;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import reactor.core.publisher.Flux;

import java.util.Random;

public class RedisReactorTest {
    static int KEY_COUNT = 10;

    public static void main(String[] args) {
        String keys[] = new String[KEY_COUNT];


        String redisIpPorts = "192.168.2.10:9000,192.168.2.14:9000,192.168.2.13:9000";
        RedisClusterClient redisClient = RedisClusterClient.create("redis://" + redisIpPorts);
        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();
        RedisStringReactiveCommands<String, String> reactive = connection.reactive();
        for (int i = 0; i < KEY_COUNT; i++) {
            keys[i] = "mc:s-info-sku" + i + "" + new Random().nextInt(KEY_COUNT);
            connection.sync().set(keys[i], "test-mget");
        }

        Flux<KeyValue<String, String>> get = reactive.mget(keys);
        System.out.println("current thread" + Thread.currentThread().getName());
        get.collectList().subscribe(keyValues -> {
            System.out.println(keyValues);
            System.out.println("call back thread " + Thread.currentThread().getName());
        });
        System.out.println("return thread " + Thread.currentThread().getName());
        while (true) {

        }
    }
}
