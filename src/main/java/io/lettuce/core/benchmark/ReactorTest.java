package io.lettuce.core.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class ReactorTest {
    public static void main(String[] args) {
        /**
         *  private static ForkJoinPool makeCommonPool() {
         *         int parallelism = -1;
         *         ForkJoinWorkerThreadFactory factory = null;
         *         UncaughtExceptionHandler handler = null;
         *         try {  // ignore exceptions in accessing/parsing properties
         *             String pp = System.getProperty
         *                 ("java.util.concurrent.ForkJoinPool.common.parallelism");
         *             String fp = System.getProperty
         *                 ("java.util.concurrent.ForkJoinPool.common.threadFactory");
         *             String hp = System.getProperty
         *                 ("java.util.concurrent.ForkJoinPool.common.exceptionHandler");
         *             if (pp != null)
         *                 parallelism = Integer.parseInt(pp);
         *             if (fp != null)
         *                 factory = ((ForkJoinWorkerThreadFactory)ClassLoader.
         *                            getSystemClassLoader().loadClass(fp).newInstance());
         *             if (hp != null)
         *                 handler = ((UncaughtExceptionHandler)ClassLoader.
         *                            getSystemClassLoader().loadClass(hp).newInstance());
         *         } catch (Exception ignore) {
         *         }
         *         if (factory == null) {
         *             if (System.getSecurityManager() == null)
         *                 factory = defaultForkJoinWorkerThreadFactory;
         *             else // use security-managed default
         *                 factory = new InnocuousForkJoinWorkerThreadFactory();
         *         }
         *         if (parallelism < 0 && // default 1 less than #cores
         *             (parallelism = Runtime.getRuntime().availableProcessors() - 1) <= 0)
         *             parallelism = 1;
         *         if (parallelism > MAX_CAP)
         *             parallelism = MAX_CAP;
         *         return new ForkJoinPool(parallelism, factory, handler, LIFO_QUEUE,
         *                                 "ForkJoinPool.commonPool-worker-");
         *     }
         */
        System.out.println(ForkJoinPool.commonPool());

        System.out.println(Thread.currentThread().getName());
        List<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < 10; i++) {
            list.add(i);
        }

        long st = System.currentTimeMillis();
        Long count = list.parallelStream().map(integer -> {
            System.out.println(Thread.currentThread().getName());
            return integer;
        }).count();
        long et = System.currentTimeMillis();
        System.out.println("用时：\t" + (et - st) + "\tms " + count);
    }
}
