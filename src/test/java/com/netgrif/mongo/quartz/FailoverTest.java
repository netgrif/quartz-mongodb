package com.netgrif.mongo.quartz;

import com.netgrif.quartz.mongodb.Constants;
import com.netgrif.quartz.mongodb.util.Keys;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.triggers.SimpleTriggerImpl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FailoverTest {

    private static final long quartzFinishWaittimeSecs = 2L;


    @BeforeEach
    public void setup() throws SchedulerException {
        MongoHelper.purgeCollections();
    }


    public void insertSimpleTrigger(Date fireTime, long repeatInterval, int repeatCount) {
        insertSimpleTrigger(fireTime, null, repeatInterval, repeatCount);
    }

    public void insertSimpleTriggerLong(Date fireTime, long finalFireTime, long repeatInterval, int repeatCount) {
        Map<String, Object> data = new HashMap<>();
        data.putAll(commonTriggerData());
        data.put("class", SimpleTriggerImpl.class.getName());
        data.put("startTime", fireTime);
        data.put("previousFireTime", fireTime);
        data.put("nextFireTime", null);
        data.put("finalFireTime", finalFireTime);
        data.put("repeatCount", repeatCount);
        data.put("repeatInterval", repeatInterval);

        MongoHelper.addTrigger(data);

    }

        public void insertSimpleTrigger(Date fireTime, Date finalFireTime, long repeatInterval, int repeatCount) {
        Map<String, Object> data = new HashMap<>();
        data.putAll(commonTriggerData());
        data.put("class", SimpleTriggerImpl.class.getName());
        data.put("startTime", fireTime);
        data.put("previousFireTime", fireTime);
        data.put("nextFireTime", null);
        data.put("finalFireTime", finalFireTime);
        data.put("repeatCount", repeatCount);
        data.put("repeatInterval", repeatInterval);

        MongoHelper.addTrigger(data);
    }

    public void insertOneshotTrigger(Date fireTime) {
        insertSimpleTrigger(fireTime, 0, 0);
    }

    public void insertTriggerLock(String instanceId) {
        Map<String, Object> lockData = new HashMap<>();
        lockData.put(Keys.LOCK_TYPE, Keys.LockType.t.name());
        lockData.put(Keys.KEY_GROUP, "g1");
        lockData.put(Keys.KEY_NAME, "t1");
        lockData.put(Constants.LOCK_INSTANCE_ID, instanceId);
        lockData.put(Constants.LOCK_TIME, new Date(1462820481910L));

        MongoHelper.addLock(lockData);
    }


    @Test
    public void shouldExecuteOneShotTriggerOnlyOnce() throws InterruptedException, SchedulerException {
        // Set up
        insertScheduler("single-node");
        insertJob("DeadJob1", false);
        insertOneshotTrigger(new Date(1462820481910L));
        insertTriggerLock("single-node");

        List<Scheduler> cluster = QuartzHelper.createCluster("single-node");

        TimeUnit.SECONDS.sleep(quartzFinishWaittimeSecs);

        assertEquals(0, MongoHelper.getCount("triggers"));
        assertEquals(0, MongoHelper.getCount("locks"));

        QuartzHelper.shutdown(cluster);
    }

    @Test
    public void shouldReexecuteOtherTriggerFromFailedExecution() throws Exception {
        insertScheduler("dead-node");
        insertJob("DeadJob2", false);
        insertTrigger();
        insertTriggerLock("dead-node");

        List<Scheduler> cluster = QuartzHelper.createCluster("single-node");
        DeadJob.job2RunSignaler.await(2, TimeUnit.SECONDS);

        assertEquals(0, DeadJob.job2RunSignaler.getCount());
        TimeUnit.SECONDS.sleep(quartzFinishWaittimeSecs);
        QuartzHelper.shutdown(cluster);
    }

    @Test
    public void shouldRecoverOwnOneShotTrigger() throws Exception {
        insertScheduler("single-node");
        insertJob("DeadJob3", true);
        insertOneshotTrigger(new Date(1462820481910L));
        insertTriggerLock("single-node");

        List<Scheduler> cluster = QuartzHelper.createCluster("single-node");
        DeadJob.job3RunSignaler.await(2, TimeUnit.SECONDS);

        assertEquals(0, DeadJob.job3RunSignaler.getCount());
        TimeUnit.SECONDS.sleep(quartzFinishWaittimeSecs);
        assertEquals(0, MongoHelper.getCount("triggers"));
        assertEquals(0, MongoHelper.getCount("jobs"));
        assertEquals(0, MongoHelper.getCount("locks"));
        QuartzHelper.shutdown(cluster);
    }

    @Test
    @Disabled("TODO")
    public void shouldRecoverOwnRepeatingTrigger() throws Exception {
        insertScheduler("single-node");
        insertJob("DeadJob4", true);
        insertTriggerLock("single-node");
        long repeatInterval = 1000L;
        int repeatCount = 2;
        Date fireTime = new Date();
        long finalFireTime = fireTime.getTime() + (repeatInterval * (repeatCount + 1));
        insertSimpleTriggerLong(fireTime, finalFireTime, repeatInterval, repeatCount);

        List<Scheduler> cluster = QuartzHelper.createCluster("single-node");
        DeadJob.job4RunSignaler.await(5, TimeUnit.SECONDS);

        assertEquals(0, DeadJob.job4RunSignaler.getCount());
        TimeUnit.SECONDS.sleep(quartzFinishWaittimeSecs);
        assertEquals(0, MongoHelper.getCount("triggers"));
        assertEquals(0, MongoHelper.getCount("jobs"));
        assertEquals(0, MongoHelper.getCount("locks"));
        QuartzHelper.shutdown(cluster);
    }



    private void insertScheduler(String id) {
        MongoHelper.addScheduler(new HashMap<>() {{
            put("instanceId", id);
            put("schedulerName", "test cluster");
            put("lastCheckinTime", 1462806352702L);
            put("checkinInterval", 7500L);
        }});
    }

    private void insertJob(String jobName, boolean recover) {
        MongoHelper.addJob(new HashMap<>() {{
            put("_id", new ObjectId("00000000ee78252adaba4534"));
            put("keyName", "job");
            put("keyGroup", "g1");
            put("jobDescription", null);
            put("jobClass", "com.netgrif.mongo.quartz.DeadJob$" + jobName);
            put("durability", false);
            put("requestsRecovery", recover);
        }});
    }

    private Map<String, Object> commonTriggerData() {
        return new HashMap<>() {{
            put("state", "waiting");
            put("calendarName", null);
            put("description", null);
            put("endTime", null);
            put("fireInstanceId", null);
            put("jobId", new ObjectId("00000000ee78252adaba4534"));
            put("keyName", "t1");
            put("keyGroup", "g1");
            put("misfireInstruction", 0);
            put("nextFireTime", null);
            put("priority", 5);
            put("timesTriggered", 1);
        }};
    }

    private void insertTrigger() {
        Map<String, Object> data = commonTriggerData();
        data.putAll(new HashMap<>() {{
            put("class", "org.quartz.impl.triggers.CalendarIntervalTriggerImpl");
            put("startTime", new Date(1462820481910L));
            put("previousFireTime", new Date(1462820481910L));
            put("nextFireTime", new Date(1462820483910L));
            put("finalFireTime", null);
            put("repeatIntervalUnit", "SECOND");
            put("repeatInterval", 2);
        }});
        MongoHelper.addTrigger(data);
    }

}
