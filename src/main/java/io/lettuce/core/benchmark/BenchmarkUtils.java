package io.lettuce.core.benchmark;

import com.google.common.util.concurrent.RateLimiter;
import io.lettuce.core.KeyValue;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.codec.StringCodec;
import reactor.core.publisher.Flux;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class BenchmarkUtils {

    private static int getCount(ConcurrentSkipListMap<Integer, AtomicInteger> samples) {
        //key-time
        //value-count
        int count = 0;
        for (Integer key : samples.keySet()) {
            int currentCount = samples.get(key).get();
            count += currentCount;
        }
        return count;
    }

    private static int getAvg(ConcurrentSkipListMap<Integer, AtomicInteger> samples) {
        //key-time
        //value-count
        int count = 0;
        int time = 0;
        for (Integer key : samples.keySet()) {
            int currentCount = samples.get(key).get();
            count += currentCount;
            time += key * currentCount;
        }
        return time / count;
    }

    private static int getTopPercentile(Map<Integer, AtomicInteger> tp, int position) {
        int current = 0;
        int topPercentile = 0;
        for (Integer key : tp.keySet()) {
            current = current + tp.get(key).get();
            topPercentile = key;
            if (current > position) {
                break;
            }
        }
        return topPercentile;
    }

    public static String generateFixedLengthString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append("a");
        }
        return sb.toString();
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
        int tp999Position = new Double(Math.ceil(count * 0.999F)).intValue();
        int tp999 = getTopPercentile(samples, tp999Position);


        int tp99Position = new Double(Math.ceil(count * 0.99F)).intValue();
        int tp99 = getTopPercentile(samples, tp99Position);

        int tp95Position = new Double(Math.ceil(count * 0.95F)).intValue();
        int tp95 = getTopPercentile(samples, tp95Position);
        int max = samples.lastEntry().getKey();
        int avg = getAvg(samples);
        TopPercentile topPercentile = new TopPercentile(tp99, tp95, tp999, avg, max);
        topPercentile.setAllCount(getCount(samples));
        return topPercentile;
    }

    public static TopPercentile benchmark(RedisClusterClient redisClusterClient, String[] keys, ExecutorService executorService, int threadSize, int loop) throws InterruptedException {
        ConcurrentSkipListMap<Integer, AtomicInteger> tpMap = new ConcurrentSkipListMap<>();
        long t = System.currentTimeMillis();
        AtomicInteger sampleCount = new AtomicInteger(0);
        CountDownLatch countDownLatch = new CountDownLatch(threadSize);
        for (int ti = 0; ti < threadSize; ti++) {
            StatefulRedisClusterConnection connection = redisClusterClient.connect();
            executorService.submit(() -> {
                for (int i = 0; i < loop; i++) {
                    long t1 = System.currentTimeMillis();
                    try {
                        connection.sync().mget(keys);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    } finally {
                        sampleCount.incrementAndGet();
                        Integer cost = (int) (System.currentTimeMillis() - t1);
                        if (!tpMap.containsKey(cost)) {
                            tpMap.put(cost, new AtomicInteger(0));
                        }
                        tpMap.get(cost).incrementAndGet();
                    }
                }
                if (connection != null) {
                    connection.close();
                }
                countDownLatch.countDown();
            });

        }
        countDownLatch.await();
        TopPercentile topPercentile = getTopPercentile(tpMap);
        topPercentile.setStartTime(t);
        topPercentile.setEndTime(System.currentTimeMillis());
        topPercentile.setSum((int) (System.currentTimeMillis() - t));
        return topPercentile;
    }

    public static TopPercentile benchmarkReactor(RedisClusterClient redisClusterClient, String[] keys, ExecutorService executorService, int threadSize, int loop) throws InterruptedException {
        return benchmarkReactor(redisClusterClient, keys, executorService, threadSize, loop, null);
    }


    public static TopPercentile benchmarkReactor(RedisClusterClient redisClusterClient, String[] keys, ExecutorService executorService, int threadSize, int loop, RateLimiter rateLimiter) throws InterruptedException {
        ConcurrentSkipListMap<Integer, AtomicInteger> tpMap = new ConcurrentSkipListMap<>();
        long t = System.currentTimeMillis();
        AtomicInteger sampleCount = new AtomicInteger(0);
        CountDownLatch countDownLatch = new CountDownLatch(threadSize * loop);
        List<StatefulRedisClusterConnection> connections = new ArrayList<>();
        for (int ti = 0; ti < threadSize; ti++) {
            StatefulRedisClusterConnection connection = redisClusterClient.connect();
            connections.add(connection);
            executorService.submit(() -> {
                for (int i = 0; i < loop; i++) {
                    long t1 = System.currentTimeMillis();
                    try {
                        if (rateLimiter != null) {
                            rateLimiter.acquire();
                        }
                        Flux<KeyValue<String, String>> flux = connection.reactive().mget(keys);
                        flux.collectList().subscribe(stringStringKeyValue -> {
                            Integer cost = (int) (System.currentTimeMillis() - t1);
                            if (!tpMap.containsKey(cost)) {
                                tpMap.put(cost, new AtomicInteger(0));
                            }
                            tpMap.get(cost).incrementAndGet();
                            countDownLatch.countDown();
                        });
                    } catch (Throwable e) {
                        e.printStackTrace();
                    } finally {
                        sampleCount.incrementAndGet();
                    }
                }
            });
        }
        countDownLatch.await();
        for (StatefulRedisClusterConnection connection : connections) {
            connection.close();
        }
        TopPercentile topPercentile = getTopPercentile(tpMap);
        topPercentile.setStartTime(t);
        topPercentile.setEndTime(System.currentTimeMillis());
        topPercentile.setSum((int) (System.currentTimeMillis() - t));
        return topPercentile;
    }


    public static PartitionSlotDistribution getPartitionSlotDistribution(Partitions partitions, List<String> keys) {
        Map<String, List<Integer>> partitionSlotMap = new HashMap<>();
        //map between slot-hash and an ordered list of keys.
        //每个slot的key 列表
        Map<Integer, List<String>> partitioned = SlotHash.partition(StringCodec.UTF8, keys);
        for (Integer slot : partitioned.keySet()) {
            String nodeId = partitions.getPartitionBySlot(slot).getNodeId();
            if (!partitionSlotMap.containsKey(nodeId)) {
                partitionSlotMap.put(nodeId, new ArrayList<>());
            }
            partitionSlotMap.get(nodeId).add(slot);
        }
        PartitionSlotDistribution partitionSlotDistribution = new PartitionSlotDistribution();
        partitionSlotDistribution.setPartitioned(partitioned);
        partitionSlotDistribution.setPartitionSlotMap(partitionSlotMap);
        return partitionSlotDistribution;
    }
}
