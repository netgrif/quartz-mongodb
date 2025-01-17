package com.netgrif.quartz.mongodb.db;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation of {@link MongoConnector} that doesn't own the lifecycle of {@link MongoClient}.
 */
public class ExternalMongoConnector implements MongoConnector {


    private static final Logger log = LoggerFactory.getLogger(ExternalMongoConnector.class);

    private final WriteConcern writeConcern;
    private final MongoDatabase database;

    /**
     * Constructs an instance of {@link ExternalMongoConnector}.
     *
     * @param writeConcern instance of {@link WriteConcern}. Each {@link MongoCollection} produced by
     *                     {@link #getCollection(String)} will be configured with this write concern.
     * @param database     {@link MongoDatabase} instance. Will be used to produce collections.
     */
    public ExternalMongoConnector(final WriteConcern writeConcern, final MongoDatabase database) {
        this.database = database;
        this.writeConcern = writeConcern;
    }

    public ExternalMongoConnector(MongoClient mongoClient, String dbName) {
        this(WriteConcern.W1, mongoClient.getDatabase(dbName));
    }

    /**
     * Constructs an instance of {@link ExternalMongoConnector}.
     *
     * @param writeConcern instance of {@link WriteConcern}. Each {@link MongoCollection} produced by
     *                     {@link #getCollection(String)} will be configured with this write concern.
     * @param mongoClient  instance of {@link MongoClient}.
     * @param dbName       name of the database that will be used to produce collections.
     */
    public ExternalMongoConnector(final WriteConcern writeConcern, final MongoClient mongoClient, final String dbName) {
        this(writeConcern, mongoClient.getDatabase(dbName));
    }

    @Override
    public MongoCollection<Document> getCollection(String collectionName) {
        return database.getCollection(collectionName).withWriteConcern(writeConcern);
    }

    @Override
    public void close() {
        // we don't own the lifecycle of MongoClient, ignore.
    }

}
