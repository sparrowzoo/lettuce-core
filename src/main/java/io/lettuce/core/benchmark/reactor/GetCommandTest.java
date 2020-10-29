package io.lettuce.core.benchmark.reactor;

import io.lettuce.core.benchmark.Debugger;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;

import java.util.function.Consumer;
import java.util.function.Function;

public class GetCommandTest {
    public static void main(String[] args) {
        StatefulRedisClusterConnection connection= Debugger.getDebugger().getConnection();
        connection.sync().hset("map","b","b");
        connection.sync().hset("map","b","b");
        connection.reactive().hset("map","d","e").subscribe(new Consumer() {
            @Override
            public void accept(Object o) {
                System.out.println(o);
            }
        });

        connection.reactive().hget("map","b").doOnNext(new Consumer() {
            @Override
            public void accept(Object o) {
                System.out.println(o);
            }
        }).map(new Function() {
            @Override
            public Object apply(Object o) {
                return o;
            }
        }).block();
    }
}
