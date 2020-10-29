package io.lettuce.core.benchmark;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.netty.util.internal.logging.InternalLogger;

public class Debugger {
    private boolean debug = false;
    private String env = "dev";

    private Debugger() {
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
