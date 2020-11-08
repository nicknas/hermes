package com.openbank.util;

import com.google.gson.Gson;
import com.mongodb.client.MongoDatabase;
import net.serenitybdd.core.Serenity;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;


public class SessionStore {

    static SessionStore INSTANCE;
    private static MongoDatabase database;
    final Gson gson = new Gson();
    final String collection = "_output";

    private SessionStore(MongoDatabase database) {
        SessionStore.database = database;
        if (database.getCollection(collection) == null)
            database.createCollection(collection);
        if (database.getCollection(collection).find(session()).first() == null)
            database.getCollection(collection).insertOne(new Document("name", "session"));
    }

    public static SessionStore get(MongoDatabase database) {
        if (INSTANCE == null) {
            INSTANCE = new SessionStore(database);
        }
        return INSTANCE;
    }

    /**
     * helper method  for session filter
     *
     * @return session filter
     */
    private static Bson session() {
        return eq("name", "session");
    }

    /**
     * import stored serenity session from database
     */
    public void importSession() {
        Optional.ofNullable(database.getCollection(collection).find(session()).first())
                .ifPresent(Serenity.getCurrentSession()::putAll);

    }

    /**
     * set serenity session value
     *
     * @param key   session key
     * @param value session value
     */
    public void set(Object key, Object value) {
        Serenity.setSessionVariable(key).to(value);
    }

    /**
     * get serenity session value
     *
     * @param key session key
     * @return session value
     */
    public Object get(Object key) {
        return Serenity.getCurrentSession().get(key);
    }

    /**
     * write serenity session to database
     */
    public void writeSession() {
        Document session = new Document();
        Serenity.getCurrentSession().forEach((key, value) -> session.put(String.valueOf(key), value));
        database.getCollection(collection).findOneAndReplace(session(), session);
    }

    public Map<String,String> asMap() {
        Map<String,String> session = new HashMap<>();
        Serenity.getCurrentSession().forEach((key, value) -> session.put(String.valueOf(key), String.valueOf(value)));
        return session;
    }
}
