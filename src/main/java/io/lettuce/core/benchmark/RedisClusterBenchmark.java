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
package io.lettuce.core.benchmark;

import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.CRC16;
import org.apache.commons.io.FileUtils;
import sun.nio.ch.IOUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Mark Paluch
 */
public class RedisClusterBenchmark {
    static int KEY_SIZE = 500;
    static int THREAD_SIZE = 1;
    static int LOOP = 100;
    static int SLOT_SIZE = 64;
    static ExecutorService executorService;

    //java -cp lettuce-core-6.0.0.BUILD-SNAPSHOT.jar io.lettuce.core.benchmark.RedisClusterBenchmark
    public static void main(String[] args) throws InterruptedException, IOException {
        if (args.length > 0) {
            KEY_SIZE = Integer.valueOf(args[0]);
            THREAD_SIZE = Integer.valueOf(args[1]);
            LOOP = Integer.valueOf(args[2]);
            SLOT_SIZE = Integer.valueOf(args[3]);
        }
        executorService = Executors.newFixedThreadPool(THREAD_SIZE);
        // Syntax: redis://[password@]host[:port]
        String redisIpPorts="192.168.2.10:9000,192.168.2.14:9000,192.168.2.13:9000";
        //redisIpPorts="10.197.97.16:8001,10.197.97.17:8002,10.197.97.18:8001,10.197.97.16:8002,10.197.97.17:8001,10.197.97.18:8002";
        RedisClusterClient redisClient = RedisClusterClient.create("redis://"+redisIpPorts);
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

        List<String> hashTagKeys = new ArrayList<>();
        for (int i = 0; i < KEY_SIZE; i++) {
            String key = "mc:s-info-sku" + i + "" + new Random().nextInt(KEY_SIZE);
            int slot = CRC16.crc16(key.getBytes());
            int prefix = slot % SLOT_SIZE;
            keys[i] = "{" + prefix + "}" + key;
            hashTagKeys.add(keys[i]);
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

        StringBuilder benchmark = new StringBuilder();
        PartitionSlotDistribution slotDistribution = BenchmarkUtils.getPartitionSlotDistribution(redisClient.getPartitions(), hashTagKeys);
        for (String partition : slotDistribution.getPartitionSlotMap().keySet()) {
            List<Integer> slotList = slotDistribution.getPartitionSlotMap().get(partition);
            benchmark.append("partition-" + partition + ",slot-size-" + slotList.size() + "\n");
            for (Integer slot : slotList) {
                benchmark.append("slot-" + slot + ",key-size-" + slotDistribution.getPartitioned().get(slot).size() + "\n");
            }
        }
        TopPercentile tp2 = BenchmarkUtils.benchmark(redisClient, keys2, executorService, THREAD_SIZE, LOOP);
        TopPercentile tp1 = BenchmarkUtils.benchmark(redisClient, keys, executorService, THREAD_SIZE, LOOP);
        benchmark.append("hash-tag--" + tp1 + "\n");
        benchmark.append("non-hash-tag--" + tp2 + "\n");
        System.out.println(benchmark.toString());
        String fileName = String.format("./redis-cluster-benchmark-thread%s-loop%s-keysize%s-slotsize%s", THREAD_SIZE, LOOP, KEY_SIZE, SLOT_SIZE);
        FileUtils.write(new File(fileName), benchmark.toString(), Charset.defaultCharset());
        redisClient.shutdown();
        System.in.read();
    }

}

