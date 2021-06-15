package com.atguigu.gmall.product.controller;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * @author atguigu-mqx
 */
public class CompletableFutureDemo {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        //  创建一个没有返回值！
        //        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.runAsync(() -> {
        //            System.out.println("come on !");
        //        });
        //
        //        CompletableFuture<Integer> integerCompletableFuture = CompletableFuture.supplyAsync(() -> {
        //            //  int i = 1/0;
        //            return 1024;
        //        }).thenApply(new Function<Integer, Integer>() {
        //            @Override
        //            public Integer apply(Integer integer) {
        //                System.out.println("thenApply======== integer:\t" + integer);
        //                //  返回数据
        //                return 1024*2;
        //            }
        //        }).whenComplete(new BiConsumer<Integer, Throwable>() {
        //            @Override
        //            public void accept(Integer integer, Throwable throwable) {
        //                System.out.println("whenCompleteAsync --- integer"+integer);
        //                System.out.println("whenCompleteAsync --- throwable"+throwable);
        //            }
        //        }).exceptionally(new Function<Throwable, Integer>() {
        //            @Override
        //            public Integer apply(Throwable throwable) {
        //                System.out.println("exceptionally --- throwable"+throwable);
        //                return 404;
        //            }
        //        });
        //
        //        System.out.println(integerCompletableFuture.get());
        //  七大核心参数：
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                2,
                5,
                3L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(3)
        );

        //  创建线程A
        CompletableFuture<String> completableFutureA = CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                return "hello";
            }
        },threadPoolExecutor);

        //  创建线程B 依赖线程A的返回结果！
        CompletableFuture<Void> completableFutureB = completableFutureA.thenAcceptAsync((s) -> {
            //  先睡一会：
            delaySec(3);
            //  输入数据：
            printCurrTime(s+"B线程");

        },threadPoolExecutor);
        //  创建线程C 依赖线程A的返回结果！
        CompletableFuture<Void> completableFutureC = completableFutureA.thenAcceptAsync((s) -> {
            //  先睡一会：
            delaySec(1);
            //  输入数据：
            printCurrTime(s+"C线程");
        },threadPoolExecutor);

    }

    //  打印方法
    private static void printCurrTime(String s) {
        System.out.println(s);
    }

    //  睡眠方法
    private static void delaySec(int i) {
        try {
            Thread.sleep(i*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
