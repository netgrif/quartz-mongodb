package com.netgrif.mongo.quartz;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.*;

public class MongoHelper {

    public static final String testDatabaseName = "quartz_mongodb_test";

    public static final int DEFAULT_MONGO_PORT = 27017;

    private static final MongoClient client = MongoClients.create(
            MongoClientSettings.builder()
                    .writeConcern(WriteConcern.JOURNALED)
                    .applyToClusterSettings(builder ->
                            builder.hosts(Collections.singletonList(new ServerAddress("localhost", DEFAULT_MONGO_PORT)))
                    )
                    .build());

    private static final MongoDatabase testDatabase = client.getDatabase(testDatabaseName);

    private static final Map<String, MongoCollection<Document>> collections = new HashMap<>();

    static {
        collections.put("calendars", testDatabase.getCollection("quartz_calendars"));
        collections.put("locks", testDatabase.getCollection("quartz_locks"));
        collections.put("jobs", testDatabase.getCollection("quartz_jobs"));
        collections.put("jobGroups", testDatabase.getCollection("quartz_paused_job_groups"));
        collections.put("schedulers", testDatabase.getCollection("quartz_schedulers"));
        collections.put("triggers", testDatabase.getCollection("quartz_triggers"));
        collections.put("triggerGroups", testDatabase.getCollection("quartz_paused_trigger_groups"));
    }

    public static void dropTestDB() {
        testDatabase.drop();
    }

    public static void clearColl(String colKey) {
        collections.get(colKey).deleteMany(new Document());
    }

    public static void purgeCollections() {
        clearColl("triggers");
        clearColl("jobs");
        clearColl("locks");
        clearColl("calendars");
        clearColl("schedulers");
        clearColl("triggerGroups");
        clearColl("jobGroups");
    }

    public static void addScheduler(Map<String, Object> dataMap) {
        collections.get("schedulers").insertOne(new Document(dataMap));
    }

    public static void addJob(Map<String, Object> dataMap) {
        collections.get("jobs").insertOne(new Document(dataMap));
    }

    public static void addLock(Map<String, Object> dataMap) {
        collections.get("locks").insertOne(new Document(dataMap));
    }

    public static void addTrigger(Map<String, Object> dataMap) {
        collections.get("triggers").insertOne(new Document(dataMap));
    }

    public static long getCount(String col) {
        return collections.get(col).countDocuments();
    }

    public static MongoCollection<Document> getCalendarsColl() {
        return collections.get("calendars");
    }

    public static MongoCollection<Document> getLocksColl() {
        return collections.get("locks");
    }

    public static MongoCollection<Document> getSchedulersColl() {
        return collections.get("schedulers");
    }

    public static MongoCollection<Document> getTriggersColl() {
        return collections.get("triggers");
    }

    public static Document getFirst(String col) {
        return getFirst(col, new HashMap<>());
    }

    public static Document getFirst(String col, Map<String, Object> amap) {
        return collections.get(col).find(new Document(amap)).first();
    }

    public static Collection<Document> findAll(String col) {
        return collections.get(col).find().into(new ArrayList<>());
    }
}
