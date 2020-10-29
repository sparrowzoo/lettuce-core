package io.lettuce.core.protocol;

import io.netty.util.concurrent.DefaultThreadFactory;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ElasticThreadPoolProvider {
    private static class Inner {
        private static ElasticThreadPoolProvider elasticThreadPoolProvider = new ElasticThreadPoolProvider();
    }

    private Scheduler scheduler = null;
    private boolean useOtherThreadPool=false;

    public boolean isUseOtherThreadPool() {
        return useOtherThreadPool;
    }

    public void setUseOtherThreadPool(boolean useOtherThreadPool) {
        this.useOtherThreadPool = useOtherThreadPool;
    }

    public void resetThreadPool(ExecutorService executorService) {
        this.scheduler = Schedulers.fromExecutorService(executorService);
    }

    public static ElasticThreadPoolProvider getSchedulerProvider() {
        return Inner.elasticThreadPoolProvider;
    }

    public Scheduler getScheduler() {
        if (scheduler != null) {
            return scheduler;
        }
        synchronized (this) {
            if (scheduler != null) {
                return scheduler;
            }
            int coreThreads = Runtime.getRuntime().availableProcessors();
            ExecutorService executorService = new ThreadPoolExecutor(coreThreads, 1024, 1, TimeUnit.MINUTES,
                    new ArrayBlockingQueue<>(1024), new DefaultThreadFactory("lettuce-compute"), new ThreadPoolExecutor.CallerRunsPolicy());
            return Schedulers.fromExecutorService(executorService);
        }
    }
}
