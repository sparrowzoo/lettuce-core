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

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.lambdaworks.codec.CRC16;
import com.lambdaworks.redis.cluster.ClusterClientOptions;
import com.lambdaworks.redis.cluster.ClusterTopologyRefreshOptions;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;

/**
 * @author Mark Paluch
 */
public class ConnectToRedisClusterWithTopologyRefreshing {
    static int KEY_SIZE = 500;
    static int THREAD_SIZE = 100;
    static int LOOP = 100;
    static int SLOT_SIZE = 128;
    static ExecutorService executorService;

    public static void main(String[] args) throws InterruptedException, IOException {
        executorService = Executors.newFixedThreadPool(THREAD_SIZE);
        // Syntax: redis://[password@]host[:port]
        //192.168.2.10:9000,192.168.2.14:9000,192.168.2.13:9000
        RedisClusterClient redisClient = RedisClusterClient.create("redis://192.168.2.10:9000,192.168.2.14:9000,192.168.2.13:9000");

        ClusterTopologyRefreshOptions clusterTopologyRefreshOptions = ClusterTopologyRefreshOptions.builder()//
                .enablePeriodicRefresh(10, TimeUnit.HOURS)//
                .enableAllAdaptiveRefreshTriggers()//
                .build();

        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder()//
                .topologyRefreshOptions(clusterTopologyRefreshOptions)//
                .build();

        redisClient.setOptions(clusterClientOptions);

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();

        String keys[] = new String[KEY_SIZE];
        String keys2[] = new String[KEY_SIZE];
        for (int i = 0; i < KEY_SIZE; i++) {
            String key = "mc:s-info-sku" + i + "" + new Random().nextInt(KEY_SIZE);
            int slot = CRC16.crc16(key.getBytes());
            int prefix = slot % SLOT_SIZE;
            keys[i] = "{" + prefix + "}" + key;
            keys2[i] = "mc:s-info-sku" + i + "" + new Random().nextInt(KEY_SIZE);
            connection.reactive().get(keys[i]);
            connection.reactive().set(keys[i], i + "");
            connection.sync().set(keys2[i], i + "");
        }
        Map<Integer,AtomicInteger> tp1= benchmark(connection, keys);
        Map<Integer,AtomicInteger> tp2=benchmark(connection, keys2);
        connection.close();
        redisClient.shutdown();
        System.in.read();
    }

    private static Map<Integer, AtomicInteger> benchmark(StatefulRedisClusterConnection<String, String> connection, String[] keys) throws InterruptedException {
        Map<Integer, AtomicInteger> tpMap = new ConcurrentSkipListMap<>();
        long t = System.currentTimeMillis();
        CountDownLatch countDownLatch = new CountDownLatch(THREAD_SIZE);
        for (int ti = 0; ti < THREAD_SIZE; ti++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < LOOP; i++) {
                        long t1 = System.currentTimeMillis();
                        connection.sync().mget(keys);
                        Integer cost = (int) (System.currentTimeMillis() - t1);
                        if (!tpMap.containsKey(cost)) {
                            tpMap.put(cost, new AtomicInteger());
                        }
                        tpMap.get(cost).incrementAndGet();
                    }
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
        System.out.println("all time cost"+(System.currentTimeMillis() - t));
        return tpMap;
    }
}
