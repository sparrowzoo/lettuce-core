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

import com.google.common.util.concurrent.RateLimiter;
import io.lettuce.core.KeyValue;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.CRC16;
import org.apache.commons.io.FileUtils;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Mark Paluch
 */
public class RedisClusterReactorWithLimitBenchmark {
    static int KEY_COUNT = 500;
    static int THREAD_SIZE = 1;
    static int LOOP = 100;
    static int SLOT_SIZE = 64;
    static int KEY_LENGTH = 8;
    static double QPS;
    static ExecutorService executorService;

    //java -cp lettuce-core-6.0.0.BUILD-SNAPSHOT.jar io.lettuce.core.benchmark.RedisClusterBenchmark
    public static void main(String[] args) throws InterruptedException, IOException {
//        if (args.length == 0) {
//            System.out.println("key_count thread_size loop slot_size  key_length");
//            System.exit(0);
//        }

        KEY_COUNT = Integer.valueOf(args[0]);
        THREAD_SIZE = Integer.valueOf(args[1]);
        LOOP = Integer.valueOf(args[2]);
        SLOT_SIZE = Integer.valueOf(args[3]);
        KEY_LENGTH = Integer.valueOf(args[4]);
        if (args.length >= 6) {
            QPS = Double.valueOf(args[5]);
        }

        executorService = Executors.newFixedThreadPool(THREAD_SIZE);
        // Syntax: redis://[password@]host[:port]
        String redisIpPorts = Debugger.getDebugger().getIpPortPair();
        RedisClusterClient redisClient = RedisClusterClient.create("redis://" + redisIpPorts);
        ClusterTopologyRefreshOptions clusterTopologyRefreshOptions = ClusterTopologyRefreshOptions.builder()//
                .enablePeriodicRefresh(10, TimeUnit.HOURS)//
                .enableAllAdaptiveRefreshTriggers()//
                .build();

        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder()//
                .topologyRefreshOptions(clusterTopologyRefreshOptions)//
                .build();
        redisClient.setOptions(clusterClientOptions);

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();
        String keys[] = new String[KEY_COUNT];
        String keys2[] = new String[KEY_COUNT];

        String fixedLengthString = BenchmarkUtils.generateFixedLengthString(KEY_LENGTH);

        RedisStringReactiveCommands<String, String> reactive = connection.reactive();

        List<String> hashTagKeys = new ArrayList<>();
        for (int i = 0; i < KEY_COUNT; i++) {
            String key = "mc:s-info-sku" + i + "" + new Random().nextInt(KEY_COUNT);
            int slot = CRC16.crc16(key.getBytes());
            int prefix = slot % SLOT_SIZE;
            keys[i] = "{" + prefix + "}" + key;
            hashTagKeys.add(keys[i]);
            keys2[i] = "mc:s-info-sku" + i + "" + new Random().nextInt(KEY_COUNT);
            connection.sync().set(keys[i], fixedLengthString);
            connection.sync().set(keys2[i], fixedLengthString);
        }


        Flux<KeyValue<String, String>> get = reactive.mget(keys);
        System.out.println("current thread" + Thread.currentThread().getName());
        get.collectList().subscribe(keyValues -> {
            System.out.println(keyValues);
            System.out.println("call back thread" + Thread.currentThread().getName());
        });
        System.out.println("return thread " + Thread.currentThread().getId());

        StringBuilder benchmark = new StringBuilder();
        PartitionSlotDistribution slotDistribution = BenchmarkUtils.getPartitionSlotDistribution(redisClient.getPartitions(), hashTagKeys);
        for (String partition : slotDistribution.getPartitionSlotMap().keySet()) {
            List<Integer> slotList = slotDistribution.getPartitionSlotMap().get(partition);
            benchmark.append("partition-" + partition + ",slot-size-" + slotList.size() + "\n");
            for (Integer slot : slotList) {
                benchmark.append("slot-" + slot + ",key-size-" + slotDistribution.getPartitioned().get(slot).size() + "\n");
            }
        }
        RateLimiter rateLimiter = RateLimiter.create(QPS);
        TopPercentile noHashTagTp = BenchmarkUtils.benchmarkReactor(redisClient, keys2, executorService, THREAD_SIZE, LOOP, rateLimiter);
        TopPercentile hashTagTp = BenchmarkUtils.benchmarkReactor(redisClient, keys, executorService, THREAD_SIZE, LOOP, rateLimiter);
        benchmark.append("hash-tag--" + hashTagTp + "\n");
        benchmark.append("non-hash-tag--" + noHashTagTp + "\n");
        System.out.println(benchmark.toString());

        String fileName = String.format("./redis-reactor-benchmark-with-limit-keycount%s-threadsize%s-loop%s-slotsize%s-keylength%s-rate %s", KEY_COUNT, THREAD_SIZE, LOOP, SLOT_SIZE, KEY_LENGTH, QPS);
        FileUtils.write(new File(fileName), benchmark.toString(), Charset.defaultCharset());
        redisClient.shutdown();
        executorService.shutdownNow();
    }
}

