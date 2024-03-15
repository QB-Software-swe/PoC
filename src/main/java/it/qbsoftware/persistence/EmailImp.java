package it.qbsoftware.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.bson.Document;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.gson.JmapAdapters;

public class EmailImp implements EmailDao {
    static final String COLLECTION = "Email";
    static final Gson GSON;
    private MongoConnection db;

    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        JmapAdapters.register(gsonBuilder);
        GSON = gsonBuilder.create();
    }

    public EmailImp(MongoConnection db) {
        this.db = db;
    }

    @Override
    public ArrayList<Email> readFromMailbox(String mailboxsId) {
        MongoCollection<Document> identityCollection = db.getDatabase().getCollection(COLLECTION);
        /*MongoCollection<Document> identityCollection =
        MongoConnectionSingleton.INSTANCE
                .getConnection()
                .mongoDatabase
                .getCollection(COLLECTION);*/
        FindIterable<Document> documentResults = identityCollection.find();

        ArrayList<Email> emails = new ArrayList<Email>();

        for (Document document : documentResults) {
            emails.add(GSON.fromJson(document.toJson(), Email.class));
        }

        return emails.stream()
                .filter(e -> e.getMailboxIds().containsKey(mailboxsId))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public void write(Email email) {
        Document doc = Document.parse(GSON.toJson(email, Email.class));
        doc.put("_id", email.getId());
        db.getDatabase()
                .getCollection(COLLECTION)
                .replaceOne(
                        Filters.eq("_id", email.getId()), doc, new ReplaceOptions().upsert(true));
        /*MongoConnectionSingleton.INSTANCE
        .getConnection()
        .mongoDatabase
        .getCollection(COLLECTION)
        .replaceOne(
                Filters.eq("_id", email.getId()), doc, new ReplaceOptions().upsert(true));*/
    }

    @Override
    public ArrayList<Email> getAll() {
        MongoCollection<Document> identityCollection = db.getDatabase().getCollection(COLLECTION);
        /*MongoCollection<Document> identityCollection =
        MongoConnectionSingleton.INSTANCE
                .getConnection()
                .mongoDatabase
                .getCollection(COLLECTION);*/
        FindIterable<Document> documentResults = identityCollection.find();

        ArrayList<Email> emails = new ArrayList<Email>();

        for (Document document : documentResults) {
            emails.add(GSON.fromJson(document.toJson(), Email.class));
        }

        return emails;
    }
}
