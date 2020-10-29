package io.lettuce.core.benchmark.reactor;

import com.google.common.collect.Maps;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.benchmark.Debugger;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class HashCommandTest {
    public static void main(String[] args) {
        StatefulRedisClusterConnection connection = Debugger.getDebugger().getStageConnection();
        connection.sync().hset("map", "b", "b");
        connection.sync().hset("map", "c", "c");

        connection.reactive().hget("map","b").map(new Function<String, String>() {

            @Override
            public String apply(String s) {
                System.out.println(s);
                return s;
            }
        }).block();
        while (true) {
        }
    }
}
