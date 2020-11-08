package com.openbank.util;

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
    final String collection = "_output";

    private SessionStore(MongoDatabase database) {
        SessionStore.database = database;
        database.getCollection(collection);
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

    public Map<String,String> asMap() {
        Map<String,String> session = new HashMap<>();
        Serenity.getCurrentSession().forEach((key, value) -> session.put(String.valueOf(key), String.valueOf(value)));
        return session;
    }
}
