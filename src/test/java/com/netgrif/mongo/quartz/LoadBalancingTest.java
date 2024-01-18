package com.netgrif.mongo.quartz;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.*;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class LoadBalancingTest {

    private static List<String> counter = new CopyOnWriteArrayList<>();
    private List<Scheduler> cluster;

    @BeforeEach
    public void setup() throws SchedulerException {
        MongoHelper.purgeCollections();
        this.cluster =  QuartzHelper.createCluster("BaWiX", "Test");
    }


    @AfterEach
    public void cleanup() throws SchedulerException {
        QuartzHelper.shutdown(this.cluster);
    }

    @Test
    public void shouldExecuteTheJobOnlyOnce() throws InterruptedException, SchedulerException {
        counter.clear();
        JobDetail job = JobBuilder.newJob(SharedJob.class)
                .withIdentity("job1", "g1")
                .build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .startAt(new Date(System.currentTimeMillis() + 1000L))
                .withIdentity("t1", "g1")
                .build();

        this.cluster.get(0).scheduleJob(job, trigger);
        Thread.sleep(7000);

        assertEquals(1, counter.size());
    }

    public static class SharedJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            try {
                String id = context.getScheduler().getSchedulerInstanceId();
                System.out.println("Shared Job executed by: " + id);
                counter.add(id);
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new JobExecutionException(e);
            } catch (SchedulerException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
