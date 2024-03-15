package it.qbsoftware.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import it.qbsoftware.core.util.MailboxInfo;
import java.util.ArrayList;
import java.util.Optional;
import org.bson.Document;
import org.bson.conversions.Bson;
import rs.ltt.jmap.gson.JmapAdapters;

public class MailboxInfoImp implements MailboxInfoDao {
    static final String COLLECTION = "MailboxInfo";
    static final Gson GSON;
    private MongoConnection db;

    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        JmapAdapters.register(gsonBuilder);
        GSON = gsonBuilder.create();
    }

    public MailboxInfoImp(MongoConnection db) {
        this.db = db;
    }

    @Override
    public ArrayList<MailboxInfo> readAll() {
        MongoCollection<Document> identityCollection = db.getDatabase().getCollection(COLLECTION);
        /*MongoCollection<Document> identityCollection =
        MongoConnectionSingleton.INSTANCE
                .getConnection()
                .mongoDatabase
                .getCollection(COLLECTION);*/
        FindIterable<Document> documentResults = identityCollection.find();

        ArrayList<MailboxInfo> mailboxsInfo = new ArrayList<MailboxInfo>();

        for (Document documentResult : documentResults) {
            mailboxsInfo.add(
                    GSON.fromJson(documentResult.toJson().replace("_id", "id"), MailboxInfo.class));
        }

        return mailboxsInfo;
    }

    @Override
    public Optional<MailboxInfo> read(String id) {
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
            return Optional.of(GSON.fromJson(doc.toJson().replace("_id", "id"), MailboxInfo.class));
        }
    }

    @Override
    public void write(MailboxInfo mailboxInfo) {
        Document doc =
                Document.parse(GSON.toJson(mailboxInfo, MailboxInfo.class).replace("id", "_id"));
        MongoCollection<Document> identityCollection = db.getDatabase().getCollection(COLLECTION);
        /*MongoCollection<Document> identityCollection =
        MongoConnectionSingleton.INSTANCE
                .getConnection()
                .mongoDatabase
                .getCollection(COLLECTION);*/

        identityCollection.insertOne(doc);
    }
}
