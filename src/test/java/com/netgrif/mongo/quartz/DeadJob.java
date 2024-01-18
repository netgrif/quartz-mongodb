package com.netgrif.mongo.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.concurrent.CountDownLatch;


public class DeadJob {

    public static final CountDownLatch job2RunSignaler = new CountDownLatch(1);
    public static final CountDownLatch job3RunSignaler = new CountDownLatch(1);
    public static final CountDownLatch job4RunSignaler = new CountDownLatch(3);


    public static class DeadJob1 implements Job {

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            System.out.println("Executing DeadJob1");
            throw new IllegalStateException("Should not be executed!");
        }
    }


    public static class DeadJob2 implements Job {
        @Override
        public void execute(JobExecutionContext ctx) throws JobExecutionException {
            System.out.println("Executing DeadJob2");
            if (ctx.isRecovering()) {
                throw new IllegalStateException("Should not be in recovering state!");
            }
            job2RunSignaler.countDown();
        }
    }


    public static class DeadJob3 implements Job {
        @Override
        public void execute(JobExecutionContext ctx) throws JobExecutionException {
            System.out.println("Executing DeadJob3");
            if (!ctx.isRecovering()) {
                throw new IllegalStateException("Should not be in recovering state!");
            }
            job3RunSignaler.countDown();
        }
    }


    public static class DeadJob4 implements Job {
        @Override
        public void execute(JobExecutionContext ctx) throws JobExecutionException {
            System.out.println("Executing DeadJob4");
            if (!ctx.isRecovering()) {
                throw new IllegalStateException("Should not be in recovering state!");
            }
            job4RunSignaler.countDown();
        }
    }
}