package com.az.scicalc;

import java.util.concurrent.*;

/** One deadline covers every engine invocation in a submitted operation. */
final class CalculationBudget {
    private static final class Budget {volatile boolean cancelled;long deadline;}
    private static final ThreadLocal<Budget> deadline=new ThreadLocal<>();
    static void begin(long duration,TimeUnit unit){Budget budget=new Budget();budget.deadline=System.nanoTime()+unit.toNanos(duration);deadline.set(budget);}
    static void end(){deadline.remove();}
    static long remainingNanos(){
        Budget budget=deadline.get();
        if(Thread.currentThread().isInterrupted()||budget!=null&&budget.cancelled)throw new CancellationException("已取消计算");
        long remaining=budget==null?TimeUnit.SECONDS.toNanos(8):budget.deadline-System.nanoTime();
        if(remaining<=0)throw new IllegalArgumentException("本次计算超过 20 秒，请减少数据或简化表达式");
        return Math.min(TimeUnit.SECONDS.toNanos(8),remaining);
    }
    static ThreadPoolExecutor executor(){return new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new LinkedBlockingQueue<>()){
        @Override protected <T> RunnableFuture<T> newTaskFor(Callable<T> task){return new Job<>(task);}
        @Override protected <T> RunnableFuture<T> newTaskFor(Runnable task,T value){return new Job<>(Executors.callable(task,value));}
        @Override protected void beforeExecute(Thread thread,Runnable work){super.beforeExecute(thread,work);if(work instanceof Job<?>){Budget budget=((Job<?>)work).budget;budget.deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);deadline.set(budget);}else begin(20,TimeUnit.SECONDS);}
        @Override protected void afterExecute(Runnable work,Throwable error){end();super.afterExecute(work,error);}
    };}
    private static final class Job<T> extends FutureTask<T>{
        final Budget budget=new Budget();
        Job(Callable<T> task){super(task);}
        @Override public boolean cancel(boolean interrupt){budget.cancelled=true;return super.cancel(interrupt);}
    }
}
