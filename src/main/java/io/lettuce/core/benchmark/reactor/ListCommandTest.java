package io.lettuce.core.benchmark.reactor;

import io.lettuce.core.benchmark.Debugger;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.protocol.ElasticThreadPoolProvider;

import java.util.function.Consumer;
import java.util.function.Function;

public class ListCommandTest {
    public static void main(String[] args) {
        ElasticThreadPoolProvider.getSchedulerProvider().setUseOtherThreadPool(false);
        StatefulRedisClusterConnection connection= Debugger.getDebugger().getConnection();
        connection.reactive().lpush("list","d","e").subscribe(new Consumer() {
            @Override
            public void accept(Object o) {
                System.out.println(o);
            }
        });

        connection.reactive().lrange("list",-1,-1).doOnNext(new Consumer() {
            @Override
            public void accept(Object o) {
                System.out.println(o);
            }
        }).map(new Function() {
            @Override
            public Object apply(Object o) {
                return o;
            }
        }).collectList().block();
    }
}
