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

import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.CRC16;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Mark Paluch
 */
public class RedisClusterReactorBenchmark {
    static int KEY_COUNT = 64;
    static int THREAD_SIZE = 2;
    static int SLOT_SIZE = 32;
    static int KEY_LENGTH = 64;
    static int LOOP = 1000;
    static ExecutorService executorService;

    //java -cp lettuce-core-6.0.0.BUILD-SNAPSHOT.jar io.lettuce.core.benchmark.RedisClusterBenchmark
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length > 0) {
            THREAD_SIZE = Integer.valueOf(args[0]);
            LOOP=Integer.valueOf(args[1]);
        }
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
        //System.setProperty("io.netty.eventLoopThreads", THREAD_SIZE + "");
        executorService = Executors.newFixedThreadPool(THREAD_SIZE);
        RedisClusterClient redisClient = Debugger.getDebugger().getClient(THREAD_SIZE);

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();
        String hashTagKeys[] = new String[KEY_COUNT];
        String withoutHashTagKeys[] = new String[KEY_COUNT];
        initBenchmarkData(connection, hashTagKeys, withoutHashTagKeys);
        StringBuilder benchmark = new StringBuilder("条件|开始时间 | 结束时间|tp999|tp99|tp95|max|sum|avg|allcount|QPS\n" +
                "---|---|---|---|---|---|---|---|---|---|---\n");
//        PartitionSlotDistribution slotDistribution = BenchmarkUtils.getPartitionSlotDistribution(redisClient.getPartitions(), hashTagKeys);
//        for (String partition : slotDistribution.getPartitionSlotMap().keySet()) {
//            List<Integer> slotList = slotDistribution.getPartitionSlotMap().get(partition);
//            benchmark.append("partition-" + partition + ",slot-size-" + slotList.size() + "\n");
//            for (Integer slot : slotList) {
//                benchmark.append("slot-" + slot + ",key-size-" + slotDistribution.getPartitioned().get(slot).size() + "\n");
//            }
//        }

        BenchmarkUtils.closeClient(redisClient);

        BenchmarkUtils.benchmark(benchmark, withoutHashTagKeys, executorService, THREAD_SIZE, LOOP, false, null, false, 0, true);
        BenchmarkUtils.benchmark(benchmark, withoutHashTagKeys, executorService, THREAD_SIZE, LOOP, false, null, false, 0, false);
        BenchmarkUtils.benchmark(benchmark, withoutHashTagKeys, executorService, THREAD_SIZE, LOOP, false, null, true, 0, true);
        BenchmarkUtils.benchmark(benchmark, withoutHashTagKeys, executorService, THREAD_SIZE, LOOP, false, null, true, 0, false);
        BenchmarkUtils.benchmark(benchmark, withoutHashTagKeys, executorService, THREAD_SIZE, LOOP, true, null, false, 0, true);
        BenchmarkUtils.benchmark(benchmark, withoutHashTagKeys, executorService, THREAD_SIZE, LOOP, true, null, false, 0, false);
        BenchmarkUtils.benchmark(benchmark, withoutHashTagKeys, executorService, THREAD_SIZE, LOOP, true, null, true, 0, true);
        BenchmarkUtils.benchmark(benchmark, withoutHashTagKeys, executorService, THREAD_SIZE, LOOP, true, null, true, 0, false);
        BenchmarkUtils.benchmark(benchmark, withoutHashTagKeys, executorService, THREAD_SIZE, LOOP, true, null, false, 1, true);
        BenchmarkUtils.benchmark(benchmark, withoutHashTagKeys, executorService, THREAD_SIZE, LOOP, true, null, false, 1, false);
        BenchmarkUtils.benchmark(benchmark, withoutHashTagKeys, executorService, THREAD_SIZE, LOOP, true, null, true, 1, true);
        BenchmarkUtils.benchmark(benchmark, withoutHashTagKeys, executorService, THREAD_SIZE, LOOP, true, null, true, 1, false);
        System.out.println(benchmark.toString());

        String fileName = "./redis-benchmark.md";
        FileUtils.write(new File(fileName), benchmark.toString(), Charset.defaultCharset());
        executorService.shutdownNow();
        System.out.println("end");
    }

    private static void initBenchmarkData(StatefulRedisClusterConnection<String, String> connection, String[] hashTagKeys, String[] withoutHashTagKeys) {
        String fixedLengthString = BenchmarkUtils.generateFixedLengthString(KEY_LENGTH);
        for (int i = 0; i < KEY_COUNT; i++) {
            String key = "mc:s-info-sku" + i + "" + new Random().nextInt(KEY_COUNT);
            int slot = CRC16.crc16(key.getBytes());
            int prefix = slot % SLOT_SIZE;
            hashTagKeys[i] = "{" + prefix + "}" + key;
            withoutHashTagKeys[i] = "mc:s-info-sku" + i + "" + new Random().nextInt(KEY_COUNT);
            connection.sync().set(hashTagKeys[i], fixedLengthString);
            connection.sync().set(withoutHashTagKeys[i], fixedLengthString);
        }
    }
}

