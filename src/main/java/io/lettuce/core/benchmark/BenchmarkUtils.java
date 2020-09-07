package io.lettuce.core.benchmark;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class BenchmarkUtils {

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
        int tp999Position = new Double(Math.ceil(count * 0.999F)).intValue();
        int tp999 = getTopPercentile(samples, tp999Position);


        int tp99Position = new Double(Math.ceil(count * 0.99F)).intValue();
        int tp99 = getTopPercentile(samples, tp99Position);

        int tp95Position = new Double(Math.ceil(count * 0.95F)).intValue();
        int tp95 = getTopPercentile(samples, tp95Position);
        int max = samples.lastEntry().getKey();
        return new TopPercentile(tp99, tp95, tp999, max);
    }

    public static TopPercentile benchmark(RedisClusterClient redisClusterClient, String[] keys, ExecutorService executorService, int threadSize, int loop) throws InterruptedException {
        ConcurrentSkipListMap<Integer, AtomicInteger> tpMap = new ConcurrentSkipListMap<>();
        long t = System.currentTimeMillis();
        AtomicInteger sampleCount=new AtomicInteger(0);
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
