package io.lettuce.core.benchmark;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

public class LocalTest {
    public static void main(String[] args) throws IOException {
        System.setProperty("io.netty.eventLoopThreads", 32 + "");
        // Syntax: redis://[password@]host[:port]
        String redisIpPorts = "127.0.0.1:6379";

        RedisClient redisClient = RedisClient.create("redis://" + redisIpPorts);
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        String list[] = new String[2];
        list[0] = "25";
        list[1] = "21";
        long current = System.currentTimeMillis();
        connection.reactive().get("a").doOnSuccess(new Consumer<String>() {
            @Override
            public void accept(String s) {
                System.out.println(s);
                System.out.println("call back thread" + Thread.currentThread().getName());
                try {
                    Thread.sleep(Integer.MAX_VALUE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).subscribe();

        while (true) {
        }
    }
}
