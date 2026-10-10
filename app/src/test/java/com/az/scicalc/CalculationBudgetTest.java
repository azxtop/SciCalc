package com.az.scicalc;

import org.junit.Test;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class CalculationBudgetTest {
    @Test public void cancellationSurvivesAnEngineClearingInterruptedFlag() throws Exception {
        ThreadPoolExecutor worker=CalculationBudget.executor();CountDownLatch started=new CountDownLatch(1),cancelled=new CountDownLatch(1);
        try{
            Future<?> job=worker.submit(()->{started.countDown();try{Thread.sleep(10000);}catch(InterruptedException ignored){}
                try{CalculationBudget.remainingNanos();}catch(CancellationException expected){cancelled.countDown();}
            });
            assertTrue(started.await(2,TimeUnit.SECONDS));job.cancel(true);assertTrue(cancelled.await(2,TimeUnit.SECONDS));
            assertEquals("next",worker.submit(()->{CalculationBudget.remainingNanos();return "next";}).get(2,TimeUnit.SECONDS));
        }finally{worker.shutdownNow();}
    }
    @Test public void queuedCancellationDoesNotRunOrDelayNextJob() throws Exception {
        ThreadPoolExecutor worker=CalculationBudget.executor();CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1);
        try{
            Future<?> first=worker.submit(()->{started.countDown();release.await();return null;});assertTrue(started.await(2,TimeUnit.SECONDS));
            Future<?> queued=worker.submit(()->{fail("cancelled work ran");});queued.cancel(true);worker.remove((Runnable)queued);
            Future<String> next=worker.submit(()->"ok");release.countDown();first.get(2,TimeUnit.SECONDS);assertEquals("ok",next.get(2,TimeUnit.SECONDS));
        }finally{release.countDown();worker.shutdownNow();}
    }
    @Test public void deadlineIsSharedBySuccessiveCalls() throws Exception {
        CalculationBudget.begin(30,TimeUnit.MILLISECONDS);
        try{long first=CalculationBudget.remainingNanos();Thread.sleep(40);try{CalculationBudget.remainingNanos();fail("deadline reset");}catch(IllegalArgumentException expected){assertTrue(first>0);}}
        finally{CalculationBudget.end();}
        assertEquals(TimeUnit.SECONDS.toNanos(8),CalculationBudget.remainingNanos());
    }
    @Test public void cancelledSymbolicCallCannotPoisonNextEvaluation() throws Exception {
        ThreadPoolExecutor worker=CalculationBudget.executor();MathEngine engine=new MathEngine();CountDownLatch started=new CountDownLatch(1);
        try{
            worker.submit(engine::initialize).get(20,TimeUnit.SECONDS);
            Future<?> job=worker.submit(()->{started.countDown();return engine.evaluate("Pause(5);7/3",false);});assertTrue(started.await(2,TimeUnit.SECONDS));Thread.sleep(150);job.cancel(true);
            assertEquals("5/6",worker.submit(()->engine.evaluate("1/2+1/3",false)).get(5,TimeUnit.SECONDS));
        }finally{worker.shutdownNow();}
    }
}
