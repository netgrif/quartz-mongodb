package com.netgrif.mongo.quartz;

import org.joda.time.DateTime;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class QuartzHelper {

    /**
     * Creates standard properties with MongoDBJobStore.
     */
    public static Properties createProps() {
        Properties props = new Properties();
        props.setProperty("org.quartz.jobStore.class", "com.netgrif.quartz.mongodb.DynamicMongoDBJobStore");
        props.setProperty("org.quartz.jobStore.mongoUri", "mongodb://localhost:" + MongoHelper.DEFAULT_MONGO_PORT);
        props.setProperty("org.quartz.scheduler.idleWaitTime", "1000");
        props.setProperty("org.quartz.jobStore.dbName", "quartz_mongodb_test");
        props.setProperty("org.quartz.threadPool.threadCount", "1");
        props.setProperty("org.quartz.scheduler.skipUpdateCheck", "true");
        props.setProperty("org.quartz.plugin.triggHistory.class", "org.quartz.plugins.history.LoggingTriggerHistoryPlugin");
        props.setProperty("org.quartz.plugin.jobHistory.class", "org.quartz.plugins.history.LoggingJobHistoryPlugin");
        return props;
    }

    /**
     * Creates properties for clustered scheduler.
     */
    public static Properties createClusteredProps(String instanceName) {
        Properties props = createProps();
        props.setProperty("org.quartz.jobStore.isClustered", "true");
        props.setProperty("org.quartz.scheduler.instanceId", instanceName);
        props.setProperty("org.quartz.scheduler.instanceName", "test cluster");
        return props;
    }

    /**
     * Create a new Scheduler with the following properties.
     */
    public static Scheduler createScheduler(Properties properties) throws SchedulerException {
        StdSchedulerFactory factory = new StdSchedulerFactory();
        factory.initialize(properties);
        Scheduler scheduler = factory.getScheduler();
        scheduler.start();
        return scheduler;
    }

    /**
     * Create and start the default scheduler.
     */
    public static Scheduler startDefaultScheduler() throws SchedulerException {
        return createScheduler(createProps());
    }

    public static Scheduler createClusteredScheduler(String instanceName) throws SchedulerException {
        return createScheduler(createClusteredProps(instanceName));
    }

    /**
     * Creates a cluster of schedulers with given names.
     */
    public static List<Scheduler> createCluster(String... names) throws SchedulerException {
        List<Scheduler> schedulers = new ArrayList<>();
        for (String name : names) {
            schedulers.add(createClusteredScheduler(name));
        }
        return schedulers;
    }

    /**
     * Shutdown all schedulers in given cluster.
     */
    public static void shutdown(List<Scheduler> cluster) {
        for (Scheduler scheduler : cluster) {
            try {
                scheduler.shutdown();
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        }
    }

    public static Date inSeconds(int n) {
        return DateTime.now().plusSeconds(n).toDate();
    }

    public static Date in2Months() {
        return DateTime.now().plusMonths(2).toDate();
    }
}
