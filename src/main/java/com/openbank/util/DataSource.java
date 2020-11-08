package com.openbank.util;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class DataSource {

	private static final MongoDatabase database;


	static {
    	String uri = expand("mongodb://${mongo.username}:${mongo.password}@${mongo.server}");
		MongoClient mongoClient = MongoClients.create(uri);
        database = mongoClient.getDatabase(expand("${mongo.db}"));
    }

    /**
     * wrapper function to get document with '<collection>.<name>' format
     *
     * @param dataSpec '<collection>.<name>'
     * @return document
     */
    public static Document getDocument(String dataSpec) {
        if (Objects.isNull(dataSpec) || dataSpec.isEmpty() || !dataSpec.contains(".")) {
            throw new IllegalArgumentException(String.format("'%s' is not a valid data specification! should follow format 'collection.name'", dataSpec));
        }
        String[] spec = dataSpec.split("\\.", -1);
        return getDocument(spec[0], spec[1]);
    }


	/**
     * find and get the document match the given id
     *
     * @param collection collection name
     * @param id         collection name
     * @return document
     */
    public static Document getDocument(String collection, String id) {
        assertNotNull(id);
        assertNotNull(collection);
        return (Document) Objects.requireNonNull(database.getCollection(collection).find(and(
				eq("name", id),
				eq("country", expand("${country}")),
				eq("environment", expand("${env}")))
		).first()).get("data");
    }


	public static String expand(String str) {
        return VariablesExpander.get().replace(str);
    }

    /**
     * assert collection id
     *
     * @param id collection id
     */
    private static void assertNotNull(String id) {
        if (Objects.isNull(id) || id.isEmpty()) {
            throw new IllegalArgumentException("Collection or id should not be null or empty");
        }
    }

    /**
     * session store instance
     *
     * @return session store instance
     */
    public static SessionStore sessionStore() {
        return SessionStore.get(database);
    }


    public static Map<String, String> randomVarsMap() {
        return new HashMap<String, String>() {
			private static final long serialVersionUID = 1L;

			{
                put("RINT1", String.valueOf(randInt(1)));
                put("RINT3", String.valueOf(randInt(3)));
                put("RINT5", String.valueOf(randInt(5)));

            }
        };
    }

    public static int randInt(int length) {
        Random rnd = new Random();
        switch (length) {
            case 1:
                return rnd.nextInt(9);
            case 3:
                return 100 + rnd.nextInt(999);
            case 5:
            default:
                return 10000 + rnd.nextInt(99999);
        }
    }


	public static String propertyReadSerenity(String key) throws IOException {
		Properties properties = new Properties();
			try (final InputStream stream = DataSource.class.getClassLoader().getResourceAsStream("serenity.properties")) {
				properties.load(stream);
			}
				return properties.getProperty(key);
   }


}
