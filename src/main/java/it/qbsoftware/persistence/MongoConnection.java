package it.qbsoftware.persistence;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoConnection {
    MongoClient mongoClient;
    MongoDatabase mongoDatabase;

    public MongoConnection() {
        try {
            mongoClient = MongoClients.create("mongodb://rootuser:rootpass@dbhost:27017/");
            mongoDatabase = mongoClient.getDatabase("jmap");
        } catch (Exception exception) {
            System.exit(1);
        }
    }

    public MongoDatabase getDatabase() {
        return mongoDatabase;
    }
}
