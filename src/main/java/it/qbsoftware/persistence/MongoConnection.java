package it.qbsoftware.persistence;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

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

    public String getState(){
        return mongoDatabase.getCollection("Account").find(Filters.eq("_id","0")).first().getString("state");
    } 

    public void increaseState() {
        mongoDatabase
                .getCollection("Account")
                .updateOne(
                        Filters.eq("_id", "0"),
                        Updates.set("state", String.valueOf(Integer.valueOf(getState()) + 1)));
    }
}
