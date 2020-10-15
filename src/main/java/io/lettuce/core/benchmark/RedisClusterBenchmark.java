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

import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.CRC16;
import org.apache.commons.io.FileUtils;

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
public class RedisClusterBenchmark {
    static int KEY_COUNT = 500;
    static int THREAD_SIZE = 1;
    static int LOOP = 100;
    static int SLOT_SIZE = 64;
    static int KEY_LENGTH = 8;
    static ExecutorService executorService;

    //java -cp lettuce-core-6.0.0.BUILD-SNAPSHOT.jar io.lettuce.core.benchmark.RedisClusterBenchmark
    public static void main(String[] args) throws InterruptedException, IOException {
        if (args.length == 0) {
            System.out.println("key_count thread_size loop slot_size  key_length");
            System.exit(0);
        }
        //eventLoop 线程数默认是 Runtime.getRuntime().availableProcessors()
        System.setProperty("io.netty.eventLoopThreads", THREAD_SIZE + "");
        if (args.length > 0) {
            KEY_COUNT = Integer.valueOf(args[0]);
            THREAD_SIZE = Integer.valueOf(args[1]);
            LOOP = Integer.valueOf(args[2]);
            SLOT_SIZE = Integer.valueOf(args[3]);
            KEY_LENGTH = Integer.valueOf(args[4]);
        }
        //lettuce-nioEventLoop
        //lettuce-epollEventLoop
        // System.setProperty("io.netty.eventLoopThreads", THREAD_SIZE + "");

        /**
         * cat /proc/cpuinfo| grep "physical id"| sort| uniq| wc -l
         * 2
         * $ cat /proc/cpuinfo | grep "processor" | wc -l //Runtime.getRuntime().availableProcessors()
         * 40
         * $ cat /proc/cpuinfo | grep "cpu cores" | uniq
         * cpu cores : 10
         * 总核数 = 物理CPU个数 * 每颗物理CPU的核数；
         * 总逻辑CPU数 = 物理CPU个数 *每颗物理CPU的核数 * 超线程数。
         */

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
        String hashTag[] = new String[KEY_COUNT];
        String withoutHashTag[] = new String[KEY_COUNT];

        String fixedLengthString = BenchmarkUtils.generateFixedLengthString(KEY_LENGTH);


        List<String> hashTagKeys = new ArrayList<>();
        for (int i = 0; i < KEY_COUNT; i++) {
            String key = "mc:s-info-sku" + i + "" + new Random().nextInt(KEY_COUNT);
            int slot = CRC16.crc16(key.getBytes());
            int prefix = slot % SLOT_SIZE;
            hashTag[i] = "{" + prefix + "}" + key;
            hashTagKeys.add(hashTag[i]);
            withoutHashTag[i] = "mc:s-info-sku" + i + "" + new Random().nextInt(KEY_COUNT);
            connection.sync().set(hashTag[i], fixedLengthString);
            connection.sync().set(withoutHashTag[i], fixedLengthString);
        }


        StringBuilder benchmark = new StringBuilder();
        PartitionSlotDistribution slotDistribution = BenchmarkUtils.getPartitionSlotDistribution(redisClient.getPartitions(), hashTagKeys);
        for (String partition : slotDistribution.getPartitionSlotMap().keySet()) {
            List<Integer> slotList = slotDistribution.getPartitionSlotMap().get(partition);
            benchmark.append("partition-" + partition + ",slot-size-" + slotList.size() + "\n");
            for (Integer slot : slotList) {
                benchmark.append("slot-" + slot + ",key-size-" + slotDistribution.getPartitioned().get(slot).size() + "\n");
            }
        }
        TopPercentile withoutHashTagTp = BenchmarkUtils.benchmark(redisClient, withoutHashTag, executorService, THREAD_SIZE, LOOP);
        TopPercentile hashTagTp = BenchmarkUtils.benchmark(redisClient, hashTag, executorService, THREAD_SIZE, LOOP);
        benchmark.append("hash-tag--" + hashTagTp + "\n");
        benchmark.append("non-hash-tag--" + withoutHashTagTp + "\n");
        System.out.println(benchmark.toString());

        String fileName = String.format("./redis-benchmark-keycount%s-threadsize%s-loop%s-slotsize%s-keylength%s", KEY_COUNT, THREAD_SIZE, LOOP, SLOT_SIZE, KEY_LENGTH);
        FileUtils.write(new File(fileName), benchmark.toString(), Charset.defaultCharset());
        redisClient.shutdown();
        executorService.shutdownNow();
    }
}

