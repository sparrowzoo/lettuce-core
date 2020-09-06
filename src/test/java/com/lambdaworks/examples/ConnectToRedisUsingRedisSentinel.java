/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.examples;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.StatefulRedisConnection;

import java.util.Random;

/**
 * @author Mark Paluch
 */
public class ConnectToRedisUsingRedisSentinel {

    public static void main(String[] args) {

        // Syntax: redis-sentinel://[password@]host[:port][,host2[:port2]][/databaseNumber]#sentinelMasterId
        //192.168.2.18:26880,192.168.2.17:26880,192.168.2.18:26880
        RedisClient redisClient = RedisClient.create("redis-sentinel://192.168.2.18:26880,192.168.2.17:26880,192.168.2.18:26880/0#gws");
        StatefulRedisConnection<String, String> connection = redisClient.connect();

        int KEY_SIZE = 100;
        int threadSize=128;
        int LOOP=1000;
        String keys[] = new String[KEY_SIZE];
        String keys2[] = new String[KEY_SIZE];
        for (int i = 0; i < KEY_SIZE; i++) {
            keys[i] = String.format("{mc%s}:s-info-sku",(new Random().nextInt(64))%threadSize) +i + "" + new Random().nextInt(KEY_SIZE);
            keys2[i] = "mc:s-info-sku" + i + "" + new Random().nextInt(KEY_SIZE);
            connection.sync().set(keys[i], i + "");
            connection.sync().set(keys2[i], i + "");
        }
        long t = System.currentTimeMillis();
        for (int i = 0; i < LOOP; i++) {
            connection.sync().mget(keys);
        }
        System.out.println(System.currentTimeMillis() - t);

        System.out.println("Connected to Redis using Redis Sentinel");

        connection.close();
        redisClient.shutdown();
    }
}
