package io.lettuce.core.benchmark;

import io.lettuce.core.RedisClient;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.event.DefaultEventPublisherOptions;
import io.lettuce.core.metrics.CommandLatencyCollectorOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.Delay;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.concurrent.RejectedExecutionHandlers;
import io.netty.util.internal.logging.InternalLogger;
import org.reactivestreams.Subscriber;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class Debugger {
    private boolean debug = false;
    private boolean publishOnScheduler=true;
    private String env = "dev";

    private Debugger() {
    }

    public void setPublishOnScheduler(boolean publishOnScheduler) {
        this.publishOnScheduler = publishOnScheduler;
    }

    public RedisClusterClient getClient(int threadSize){
        DefaultClientResources resources =
                DefaultClientResources.builder().
                        commandLatencyPublisherOptions(DefaultEventPublisherOptions.disabled())
                        .commandLatencyCollectorOptions(CommandLatencyCollectorOptions.disabled())
                        .reconnectDelay(Delay.exponential())
                        .eventExecutorGroup(new DefaultEventExecutorGroup(threadSize, new DefaultThreadFactory("my-compute-thread"),Integer.MAX_VALUE, RejectedExecutionHandlers.backoff(3,5,TimeUnit.MILLISECONDS)))
                        .computationThreadPoolSize(threadSize)
                        .ioThreadPoolSize(threadSize)
                        .build();


        //redisIpPorts = "10.197.97.16:8001,10.197.97.17:8002,10.197.97.18:8001,10.197.97.16:8002,10.197.97.17:8001,10.197.97.18:8002";
        RedisClusterClient redisClient = RedisClusterClient.create(resources, "redis://" + getIpPortPair());


        ClusterTopologyRefreshOptions clusterTopologyRefreshOptions = ClusterTopologyRefreshOptions.builder()//
                .enablePeriodicRefresh(Duration.ofHours(10))// 定时关闭
                .enableAllAdaptiveRefreshTriggers()//
                .build();

        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder()//
                .topologyRefreshOptions(clusterTopologyRefreshOptions)//
                .publishOnScheduler(publishOnScheduler)
                .socketOptions(SocketOptions.builder().keepAlive(true).connectTimeout(Duration.ofSeconds(600)).build())
                .timeoutOptions(TimeoutOptions.builder().fixedTimeout(Duration.ofSeconds(600)).build())
                .requestQueueSize(1000000)
                /**
                 * Use a dedicated {@link Scheduler} to emit reactive data signals. Enabling this option can be
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
                 * @see Subscriber#onNext(Object)
                 * @see ClientResources#eventExecutorGroup()
                 */
                .build();

        redisClient.setOptions(clusterClientOptions);
        return redisClient;
    }

    public StatefulRedisClusterConnection getConnection() {
        RedisClusterClient redisClient = RedisClusterClient.create("redis://" + getIpPortPair());
        return redisClient.connect();
    }

    public StatefulRedisConnection getLocalConnection() {
        RedisClient redisClient = RedisClient.create("redis://127.0.0.1:6379");
        return redisClient.connect();
    }

    public StatefulRedisClusterConnection getStageConnection() {
        RedisClusterClient redisClient = RedisClusterClient.create("redis://10.2.2.31:8030,10.2.1.61:8030,10.2.2.13:8030");
        return redisClient.connect();
    }

    public String getIpPortPair() {
        if (env.equalsIgnoreCase("press")) {
            return "10.197.97.16:8001,10.197.97.17:8002,10.197.97.18:8001,10.197.97.16:8002,10.197.97.17:8001,10.197.97.18:8002";
        }
        return "192.168.2.10:9000,192.168.2.14:9000,192.168.2.13:9000";
    }

    public void setEnv(String env) {
        this.env = env;
    }

    private static Debugger debugger = new Debugger();

    public static Debugger getDebugger() {
        return debugger;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void info(InternalLogger logger, String format, Object... args) {
        if (debug) {
            logger.info(format, args);
        }
    }

    public void info(InternalLogger logger, String info) {
        if (debug) {
            logger.info(info);
        }
    }
}
