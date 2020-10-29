package io.lettuce.core.benchmark;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OriginReactorTest {
    private static Logger log = LoggerFactory.getLogger(ReactorTest.class);

    public static void main(String[] args) throws InterruptedException {
        Publisher<Integer> publisher = new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                        for (int i = 0; i < n; i++) {
                            subscriber.onNext(i);
                        }
                    }

                    @Override
                    public void cancel() {
                    }
                });
            }
        };
        publisher.subscribe(new Subscriber<Integer>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                System.out.println("订阅成功。。" + Thread.currentThread().getName());
                subscription.request(10);
            }


            @Override
            public void onNext(Integer item) {
                log.info("【onNext 接受到数据 item : {}】 thread-name " + Thread.currentThread().getName(), item);
            }

            @Override
            public void onError(Throwable throwable) {
                log.info("【onError 出现异常】");
                subscription.cancel();
            }

            @Override
            public void onComplete() {
                log.info("【onComplete 所有数据接收完成】");
            }
        });
        System.out.println("main-thread" + Thread.currentThread().getName());
        //主线程睡一会
        Thread.currentThread().join(100000);
    }
}
