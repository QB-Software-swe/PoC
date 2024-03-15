package it.qbsoftware.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import java.util.Optional;
import org.bson.Document;
import org.bson.conversions.Bson;
import rs.ltt.jmap.common.entity.Identity;
import rs.ltt.jmap.gson.JmapAdapters;

public class IdentityImp implements IdentityDao {
    static final String COLLECTION = "Identity";
    static final Gson GSON;
    private MongoConnection db;

    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        JmapAdapters.register(gsonBuilder);
        GSON = gsonBuilder.create();
    }

    public IdentityImp(MongoConnection db) {
        this.db = db;
    }

    @Override
    public Optional<Identity> read(String id) {
        Bson filterById = Filters.eq("_id", id);
        MongoCollection<Document> identityCollection = db.getDatabase().getCollection(COLLECTION);
        /*MongoCollection<Document> identityCollection =
        MongoConnectionSingleton.INSTANCE
                .getConnection()
                .mongoDatabase
                .getCollection(COLLECTION);*/
        FindIterable<Document> documentResults = identityCollection.find(filterById);

        Document doc = documentResults.first();

        if (doc == null) {
            return Optional.empty();
        } else {
            return Optional.of(GSON.fromJson(doc.toJson().replace("_id", "id"), Identity.class));
        }
    }

    @Override
    public void write(Identity identity) {
        Document doc =
                Document.parse(
                        GSON.toJson(identity, Identity.class).toString().replace("id", "_id"));

        MongoCollection<Document> identityCollection = db.getDatabase().getCollection(COLLECTION);
        /*MongoCollection<Document> identityCollection =
        MongoConnectionSingleton.INSTANCE
                .getConnection()
                .mongoDatabase
                .getCollection(COLLECTION);*/

        identityCollection.insertOne(doc);
    }
}
