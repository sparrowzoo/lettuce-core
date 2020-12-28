package io.lettuce.core.benchmark;

import com.google.common.util.concurrent.RateLimiter;
import io.lettuce.core.KeyValue;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.codec.StringCodec;
import io.netty.util.concurrent.EventExecutorGroup;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.lettuce.core.benchmark.Debugger.*;

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
        long time = 0;
        for (Integer key : samples.keySet()) {
            int currentCount = samples.get(key).get();
            count += currentCount;
            time += key * currentCount;
        }
        return (int) (time / count);
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

    private static Metric getTopPercentile(ConcurrentSkipListMap<Integer, AtomicInteger> samples) {
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
        Metric topPercentile = new Metric(tp99, tp95, tp999, avg, max);
        topPercentile.setAllCount(getCount(samples));
        return topPercentile;
    }

    private static void blocking(String[] keys, ConcurrentSkipListMap<Integer, AtomicInteger> tpMap, AtomicInteger sampleCount, CountDownLatch countDownLatch, StatefulRedisClusterConnection connection) {
        long t1 = System.currentTimeMillis();
        try {
            connection.sync().mget(keys);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            countDownLatch.countDown();
            sampleCount.incrementAndGet();
            Integer cost = (int) (System.currentTimeMillis() - t1);
            if (!tpMap.containsKey(cost)) {
                tpMap.put(cost, new AtomicInteger(0));
            }
            tpMap.get(cost).incrementAndGet();
        }
    }


    public static void benchmark(StringBuilder result, String[] keys, ExecutorService executorService, int threadSize, int loop, boolean reactive, RateLimiter rateLimiter, boolean publishOnScheduler, long sleep, boolean isShareConnection) throws InterruptedException {
        ConcurrentSkipListMap<Integer, AtomicInteger> tpMap = new ConcurrentSkipListMap<>();
        getDebugger().setPublishOnScheduler(publishOnScheduler);
        RedisClusterClient redisClusterClient = getDebugger().getClient(threadSize);
        AtomicInteger sampleCount = new AtomicInteger(0);
        CountDownLatch countDownLatch = new CountDownLatch(threadSize * loop);
        final StatefulRedisClusterConnection shareConnection = redisClusterClient.connect();
        String condition = "reactive=" + reactive + ",publishOnScheduler=" + publishOnScheduler + ",sleep=" + sleep + ",isShareConnection=" + isShareConnection;
        System.out.println("benckmark:" + condition);
        long t = System.currentTimeMillis();
        List<StatefulRedisClusterConnection> connections = new ArrayList<>();
        connections.add(shareConnection);
        for (int ti = 0; ti < threadSize; ti++) {
            final StatefulRedisClusterConnection localConnection;
            if (!isShareConnection) {
                localConnection = redisClusterClient.connect();
                connections.add(localConnection);
            } else {
                localConnection = shareConnection;
            }
            executorService.submit(() -> {
                for (int i = 0; i < loop; i++) {
                    if (reactive) {
                        reactive(keys, rateLimiter, tpMap, sampleCount, countDownLatch, localConnection, sleep);
                    } else {
                        blocking(keys, tpMap, sampleCount, countDownLatch, localConnection);
                    }
                }
            });
        }
        countDownLatch.await();
        System.out.println(countDownLatch.getCount() + "-" + sampleCount.get());
        Metric topPercentile = getTopPercentile(tpMap);
        topPercentile.setStartTime(t);
        topPercentile.setEndTime(System.currentTimeMillis());
        topPercentile.setSum((int) (System.currentTimeMillis() - t));

        //key-size="+keys.length+",threadSize="+threadSize+",loop="+loop+",
        topPercentile.setCondition(condition);
        for (StatefulRedisClusterConnection connection : connections) {
            connection.close();
        }
        closeClient(redisClusterClient);
        System.out.println(topPercentile);
        result.append(topPercentile + "\n");
    }

    public static void closeClient(RedisClusterClient redisClusterClient) {
        try {
            redisClusterClient.shutdown(0, 0, TimeUnit.SECONDS);
            redisClusterClient.getResources().shutdown(0, 0, TimeUnit.SECONDS);
            redisClusterClient.getResources().eventExecutorGroup().shutdownGracefully(0, 0, TimeUnit.SECONDS);
            Thread.sleep(30000);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void reactive(String[] keys, RateLimiter rateLimiter, ConcurrentSkipListMap<Integer, AtomicInteger> tpMap, AtomicInteger sampleCount, CountDownLatch countDownLatch, StatefulRedisClusterConnection connection, long sleep) {
        long t1 = System.currentTimeMillis();
        try {
//            if (rateLimiter == null) {
//                rateLimiter=RateLimiter.create(800);
//            }
//            rateLimiter.acquire();
            Flux<KeyValue<String, String>> flux = connection.reactive().mget(keys);
            flux.collectList().subscribe(stringStringKeyValue -> {
                if (sleep > 0L) {
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Integer cost = (int) (System.currentTimeMillis() - t1);
                if (!tpMap.containsKey(cost)) {
                    tpMap.put(cost, new AtomicInteger(0));
                }
                tpMap.get(cost).incrementAndGet();
                countDownLatch.countDown();
                sampleCount.incrementAndGet();
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    public static PartitionSlotDistribution getPartitionSlotDistribution(Partitions partitions, String[] keys) {
        Map<String, List<Integer>> partitionSlotMap = new HashMap<>();
        //map between slot-hash and an ordered list of keys.
        //每个slot的key 列表
        Map<Integer, List<String>> partitioned = SlotHash.partition(StringCodec.UTF8, Arrays.asList(keys));
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
