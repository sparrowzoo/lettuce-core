package io.lettuce.core.benchmark;

import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class ReactorGetHangTest {
    public static void main(String[] args) throws IOException {
        Debugger.getDebugger().setDebug(true);
        System.setProperty("io.netty.eventLoopThreads", 64 + "");
        // Syntax: redis://[password@]host[:port]
        String redisIpPorts = "192.168.2.10:9000,192.168.2.14:9000,192.168.2.13:9000";
        //redisIpPorts = "10.197.97.16:8001,10.197.97.17:8002,10.197.97.18:8001,10.197.97.16:8002,10.197.97.17:8001,10.197.97.18:8002";
        RedisClusterClient redisClient = Debugger.getDebugger().getClient(1);
        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();
        String list[] = new String[2];
        list[0] = "aa";
        list[1] = "bb";
        connection.sync().set("a", "aaaaaaaa");
//        connection.sync().mget(list);

        RedisStringReactiveCommands<String, String> reactive = connection.reactive();

        if (true) {
            long current = System.currentTimeMillis();
            System.out.println("current thread" + Thread.currentThread().getName());
            //System.out.println(connection.sync().get("a"));
            reactive.get("a").subscribeOn(Schedulers.elastic()).subscribe(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    System.out.println("aaaaa");
                }
            });
//            reactive.get("a")
//                    .doOnSuccess(new Consumer<String>() {
//                                     @Override
//                                     public void accept(String s) {
//                                         /**
//                                          * @see PooledClusterConnectionProvider
//                                          */
//                                         //connection.sync().get("a");//hang 住
//
//                                         //connection.sync().get("b");
//                                         System.out.println(s);
//                                         System.out.println("cost " + (System.currentTimeMillis() - current));
//                                         System.out.println("call back thread" + Thread.currentThread().getName());
//                                     }
//                                 }
//                    ).block();
        }
//        System.out.println("return thread " + Thread.currentThread().getId());
    }
}
