package io.lettuce;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

public class ReactorTest {
    private static Logger log = LoggerFactory.getLogger(ReactorTest.class);

    public static void main(String[] args) throws InterruptedException {


        Subscriber<Integer> subscriber = new Subscriber<Integer>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                System.out.println("订阅成功。。");
                subscription.request(1);
                System.out.println("订阅方法里请求一个数据");
            }


            @Override
            public void onNext(Integer item) {
                try {
                    Thread.sleep(500000000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.info("【onNext 接受到数据 item : {}】 thread-id" + Thread.currentThread().getId(), item);
                subscription.request(item);
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
        };


        Integer[] a = new Integer[]{1, 3, 4};
        Flux publisher = Flux.fromArray(a);
        //3。发布者和订阅者 建立订阅关系 就是回调订阅者的onSubscribe方法传入订阅合同
        publisher.subscribe(integer -> {
            System.out.println("thread-id" + Thread.currentThread().getId());
            System.out.println(integer);
        });
        System.out.println("main-tread" + Thread.currentThread().getId());

        //5.发布者 数据都已发布完成后，关闭发送，此时会回调订阅者的onComplete方法
        //主线程睡一会
        Thread.currentThread().join(100000);
    }
}
