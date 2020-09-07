/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.examples;

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.CRC16;

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
        RedisStringReactiveCommands<String, String> reactive = connection.reactive();

        for (int i = 0; i < KEY_SIZE; i++) {
            String key = "mc:s-info-sku" + i + "" + new Random().nextInt(KEY_SIZE);
            int slot = CRC16.crc16(key.getBytes());
            int prefix = slot % SLOT_SIZE;
            keys[i] = "{" + prefix + "}" + key;
            keys2[i] = "mc:s-info-sku" + i + "" + new Random().nextInt(KEY_SIZE);
            connection.sync().set(keys[i], i + "");
            connection.sync().set(keys2[i], i + "");

            //Mono<String> set = reactive.set("key", "value");
            //Mono<String> get = reactive.get("key");
            //set.subscribe();
            //System.out.println(get.block());
        }
        //Mono<String> set = reactive.set("key", "value");
        //Flux<KeyValue<String, String>> get = reactive.mget(keys);
        //set.subscribe();
//        get.subscribe(keyValues -> {
//            System.out.println("end");
//            System.out.println(keyValues);
//        });
        ConcurrentSkipListMap tp1 = benchmark(connection, keys);
        ConcurrentSkipListMap tp2 = benchmark(connection, keys2);
        getTopPercentile(tp1);
        getTopPercentile(tp2);
        connection.close();
        redisClient.shutdown();
        System.in.read();
    }

    private static int getTopPercentile(Map<Integer, AtomicInteger> tp, int position) {
        int current = 0;
        int topPercentile = 0;
        for (Integer key : tp.keySet()) {
            current = current + tp.get(key).get();
            if (current > position) {
                topPercentile = key;
                break;
            }
        }
        return topPercentile;
    }


    public static int testTopPercentile() {
        ConcurrentSkipListMap<Integer, AtomicInteger> tp = new ConcurrentSkipListMap<>();
        for (int i = 0; i < 200; i++) {
            tp.put(i, new AtomicInteger(i));
        }
        return getTopPercentile(tp, 5);
    }

    private static TopPercentile getTopPercentile(ConcurrentSkipListMap<Integer, AtomicInteger> samples) {
        int count = 0;
        for (Integer key : samples.keySet()) {
            count += samples.get(key).get();
        }
        int tp99Position = new Double(Math.ceil(count * 0.99F)).intValue();
        int tp99 = getTopPercentile(samples, tp99Position);
        int tp95Position = new Double(Math.ceil(count * 0.95F)).intValue();
        int tp95 = getTopPercentile(samples, tp95Position);
        int max = samples.lastEntry().getKey();
        return new TopPercentile(tp99, tp95, max);
    }

    private static ConcurrentSkipListMap<Integer, AtomicInteger> benchmark(StatefulRedisClusterConnection<String, String> connection, String[] keys) throws InterruptedException {
        ConcurrentSkipListMap<Integer, AtomicInteger> tpMap = new ConcurrentSkipListMap<>();
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
        System.out.println("all time cost-" + (System.currentTimeMillis() - t));
        System.out.println(getTopPercentile(tpMap));
        return tpMap;
    }
}

class TopPercentile {
    private int tp99;
    private int tp95;
    private int max;

    public TopPercentile() {
    }

    public TopPercentile(int tp99, int tp95, int max) {
        this.tp99 = tp99;
        this.tp95 = tp95;
        this.max = max;
    }

    public int getTp99() {
        return tp99;
    }

    public void setTp99(int tp99) {
        this.tp99 = tp99;
    }

    public int getTp95() {
        return tp95;
    }

    public void setTp95(int tp95) {
        this.tp95 = tp95;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    @Override
    public String toString() {
        return "TopPercentile{" +
                "tp99=" + tp99 +
                ", tp95=" + tp95 +
                ", max=" + max +
                '}';
    }
}
