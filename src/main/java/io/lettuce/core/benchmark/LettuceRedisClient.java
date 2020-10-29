package io.lettuce.core.benchmark;

import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.event.DefaultEventPublisherOptions;
import io.lettuce.core.metrics.CommandLatencyCollectorOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.Delay;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;

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


        DefaultClientResources resources =

                DefaultClientResources.builder().
                        commandLatencyPublisherOptions(DefaultEventPublisherOptions.disabled())
                        .commandLatencyCollectorOptions(CommandLatencyCollectorOptions.disabled())
                        .reconnectDelay(Delay.exponential())
                        .eventExecutorGroup(new DefaultEventExecutorGroup(64, new DefaultThreadFactory("my-compute-thread")))
                        .computationThreadPoolSize(64)
                        .ioThreadPoolSize(64)
                        .build();


        //redisIpPorts = "10.197.97.16:8001,10.197.97.17:8002,10.197.97.18:8001,10.197.97.16:8002,10.197.97.17:8001,10.197.97.18:8002";
        RedisClusterClient redisClient = RedisClusterClient.create(resources, "redis://" + redisIpPorts);


        ClusterTopologyRefreshOptions clusterTopologyRefreshOptions = ClusterTopologyRefreshOptions.builder()//
                .enablePeriodicRefresh(Duration.ofHours(10))// 定时关闭
                .enableAllAdaptiveRefreshTriggers()//
                .build();

        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder()//
                .topologyRefreshOptions(clusterTopologyRefreshOptions)//
                .publishOnScheduler(true)
                /**
                 * Use a dedicated {@link reactor.core.scheduler.Scheduler} to emit reactive data signals. Enabling this option can be
                 * useful for reactive sequences that require a significant amount of processing with a single/a few Redis connections.
                 * <p>
                 * A single Redis connection operates on a single thread. Operations that require a significant amount of processing can
                 * lead to a single-threaded-like behavior for all consumers of the Redis connection. When enabled, data signals will be
                 * emitted using a different thread served by {@link ClientResources#eventExecutorGroup()}. Defaults to {@code false}
                 * , see {@link #DEFAULT_PUBLISH_ON_SCHEDULER}.
                 *
                 * @param publishOnScheduler true/false
                 * @return {@code this}
                 * @since 5.2
                 * @see org.reactivestreams.Subscriber#onNext(Object)
                 * @see ClientResources#eventExecutorGroup()
                 */
                .build();

        redisClient.setOptions(clusterClientOptions);

        redisClient.connect().reactive().hget("map", "b").doOnNext(new Consumer() {
            @Override
            public void accept(Object o) {
                System.out.println(Thread.currentThread().getName());
                System.out.println(o);
            }
        }).map(new Function() {
            @Override
            public Object apply(Object o) {
                return o;
            }
        }).block();
        //redisClient.connect().sync();
        //redisClient.connect().reactive();
//        RedisAdvancedClusterCommands connection= redisClient.connect().sync();
//        while (true) {
//           connection.get("a"+new Random().nextInt());
//        }

    }
}
