package com.openbank.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class DataSource {

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<Document> collection;
    private static File file = null;
    

    static {
    	String uri = expand("mongodb://${mongo.username}:${mongo.password}@${mongo.server}");
        mongoClient = MongoClients.create(uri);
        database = mongoClient.getDatabase(expand("${mongo.db}"));
//        mongoClient = MongoClients.create(expand("mongodb://${mongo.server}"));
//        database = mongoClient.getDatabase(expand("${mongo.db}"));
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
	public static void putDocument(String dataSpec) {
		if (Objects.isNull(dataSpec) || dataSpec.isEmpty()) {
			throw new IllegalArgumentException(String
					.format("'%s' is not a valid data specification! should follow format 'collection.name'", dataSpec));
		}
		String[] spec = dataSpec.split("\\.", -1);
		putDocument(spec[0], spec[1]);
	}
	
	
	/**
	 * This method is to update values in MongoDB document 
	 * 
	 * @param dataSpec
	 * @param key
	 * @param value
	 */
	public static void updateDocument(String dataSpec, String key, String value) {
		BasicDBObject newDocument = null;
		BasicDBObject searchQuery = null;
		String key1="data." + key;
		
		if (Objects.isNull(dataSpec) || dataSpec.isEmpty()) {
			throw new IllegalArgumentException(String
					.format("'%s' is not a valid data specification! should follow format 'collection.name'", dataSpec));
		}	
		
		String[] spec = dataSpec.split("\\.", -1);
		
		newDocument = new BasicDBObject();
		collection = DataSource.database.getCollection(spec[0]);
		newDocument.append("$set", new BasicDBObject().append(key1, value));
		
		searchQuery = new BasicDBObject().append("name", spec[1]);
		collection.updateOne(searchQuery, newDocument);	
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
        return (Document) database.getCollection(collection).find(and(
                eq("name", id),
                eq("country", expand("${country}")),
                eq("environment", expand("${env}")))
        ).first().get("data");
    }

    public static void putDocument(String collection,String custID)
	{
		assertNotNull(collection);		
		Document document = new Document();
		document.put("id", "customerID");
		document.put("country", "es");
		document.put("environment", "qa");
		Document data = new Document();
		
		data.put("customerID", custID);
		document.put("data", data);
		database.getCollection(collection).insertOne(document);
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
    
    public static String propertyRead(String key) throws IOException {
		Properties prob = new Properties();
		try (final InputStream stream = DataSource.class.getClassLoader()
				.getResourceAsStream("features/testData/testdata.properties")) {
			prob.load(stream);
		}

		return prob.getProperty(key);
	}

	public static void propertyWrite(String key, String value) throws IOException, URISyntaxException {
		OutputStream out = null;
		Properties prob = new Properties();
		try (final InputStream stream = DataSource.class.getClassLoader()
				.getResourceAsStream("features/testData/testdata.properties")) {
			prob.load(stream);
		}

		URL res = DataSource.class.getClassLoader().getResource("features/testData/testdata.properties");
		if (res.getProtocol().equals("jar")) {
			try {
				InputStream input = DataSource.class.getClassLoader()
						.getResourceAsStream("features/testData/testdata.properties");
				file = File.createTempFile("tempfile", ".tmp");
				out = new FileOutputStream(file);
				int read;
				byte[] bytes = new byte[1024];
				while ((read = input.read(bytes)) != -1) {
					out.write(bytes, 0, read);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}finally {
				if(out!=null) {
					out.close();	
				}
			}
		} else {
			// this will work While running from IDE, but not from a JAR
			URL url = DataSource.class.getClassLoader().getResource("features/testData/testdata.properties");
			File fileObject = new File(url.toURI());
			try (FileOutputStream filesave = new FileOutputStream(fileObject)) {
				prob.setProperty(key, value);
				prob.store(filesave, "Deposit Posting Restriction Details");
			}
		}
	}

	public static String propertyReadLegal(String key) throws IOException {
		Properties prob = new Properties();
		try (final InputStream stream = DataSource.class.getClassLoader()
				.getResourceAsStream("features/testData/legalDocument.properties")) {
			prob.load(stream);
		}
		return prob.getProperty(key);
	}

	public static void propertyWriteLegal(String key, String value) throws IOException, URISyntaxException {
		OutputStream out = null;
		Properties prob = new Properties();
		try (final InputStream stream = DataSource.class.getClassLoader()
				.getResourceAsStream("features/testData/legalDocument.properties")) {
			prob.load(stream);
		}
		URL res = DataSource.class.getClassLoader().getResource("features/testData/legalDocument.properties");
		if (res.getProtocol().equals("jar")) {
			try {
				InputStream input = DataSource.class.getClassLoader()
						.getResourceAsStream("features/testData/legalDocument.properties");
				file = File.createTempFile("tempfile", ".tmp");
				out = new FileOutputStream(file);
				int read;
				byte[] bytes = new byte[1024];
				while ((read = input.read(bytes)) != -1) {
					out.write(bytes, 0, read);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}finally{
				if(out!=null) {
					out.close();	
				}
			}
		} else {
			// this will work While running from IDE, but not from a JAR
			URL url = DataSource.class.getClassLoader().getResource("features/testData/legalDocument.properties");
			File fileObject = new File(url.toURI());
			try (FileOutputStream filesave = new FileOutputStream(fileObject)) {
				prob.setProperty(key, value);
				prob.store(filesave, "LegalDocument Values");
			}
		}
	}

	public static String propertyReadData(String key) throws IOException {
		Properties prob = new Properties();
		try (final InputStream stream = DataSource.class.getClassLoader()
				.getResourceAsStream("features/testData/testdata.properties")) {
			prob.load(stream);
		}
		return prob.getProperty(key);
	}

	public static String propertyReadDeposit(String key) throws IOException {
		Properties prob = new Properties();
		try (final InputStream stream = DataSource.class.getClassLoader()
				.getResourceAsStream("features/testData/deposit.properties")) {
			prob.load(stream);
		}
		return prob.getProperty(key);
	}

	/**
	 * This method is used to read the property values related to Lending Module 
	 * 
	 * @param key
	 * @return
	 * @throws IOException
	 */
	public static String propertyReadLending(String key) throws IOException {
		Properties prob = new Properties();
		try (final InputStream stream = DataSource.class.getClassLoader().getResourceAsStream("features/testData/lending.properties")) {
			prob.load(stream);
		}
		return prob.getProperty(key);
	}

	public static void propertyWriteLending(String key, String value) throws IOException, URISyntaxException {
		OutputStream out = null;
		Properties prob = new Properties();
		try (final InputStream stream = DataSource.class.getClassLoader()
				.getResourceAsStream("features/testData/lending.properties")) {
			System.out.println(">>>>>>>>> "+ stream);
			prob.load(stream);
		}

		URL res = DataSource.class.getClassLoader().getResource("features/testData/lending.properties");
		if (res.getProtocol().equals("jar")) {
			try {
				InputStream input = DataSource.class.getClassLoader()
						.getResourceAsStream("features/testData/lending.properties");
				file = File.createTempFile("tempfile", ".tmp");
				out = new FileOutputStream(file);
				int read;
				byte[] bytes = new byte[1024];
				while ((read = input.read(bytes)) != -1) {
					out.write(bytes, 0, read);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}finally {
				if(out!=null) {
					out.close();	
				}
			}
		} else {
			// this will work While running from IDE, but not from a JAR
//			URL url = DataSource.class.getClassLoader().getResource("features/testData/lending.properties");
//			File fileObject = new File(url.toURI());
//			System.out.println(">>>>>>>> :- "+ fileObject);
			try (FileOutputStream filesave = new FileOutputStream("./src/main/resources/features/testData/lending.properties")) {
				prob.setProperty(key, value);
				prob.store(filesave, null);
			}
		}
	}
	
	
	public static void propertyWriteDeposit(String key, String value)
			throws IOException, InterruptedException, URISyntaxException {
		OutputStream out = null;
		Properties prob = new Properties();
		try (final InputStream stream = DataSource.class.getClassLoader()
				.getResourceAsStream("features/testData/deposit.properties")) {
			prob.load(stream);
		}
		URL res = DataSource.class.getClassLoader().getResource("features/testData/deposit.properties");
		if (res.getProtocol().equals("jar")) {
			try {
				InputStream input = DataSource.class.getClassLoader()
						.getResourceAsStream("features/testData/deposit.properties");
				file = File.createTempFile("tempfile", ".tmp");
				out = new FileOutputStream(file);
				int read;
				byte[] bytes = new byte[1024];
				while ((read = input.read(bytes)) != -1) {
					out.write(bytes, 0, read);
				}
				out.close();
				file.deleteOnExit();
			} catch (Exception e) {
				e.printStackTrace();
			}finally{
				if(out!=null) {
					out.close();	
				}
			}
		} else {
			// this will work While running from IDE, but not from a JAR
//			URL url = DataSource.class.getClassLoader().getResource("features/testData/legalDocument.properties");
//			File fileObject = new File(url.toURI());
			try (FileOutputStream filesave = new FileOutputStream("./src/main/resources/features/testData/deposit.properties")) {
				prob.setProperty(key, value);
				prob.store(filesave, "Deposit Posting Restriction Details");
			}

		}
	}

	public static String propertyReadSerenity(String key) throws IOException {
		Properties properties = new Properties();
			try (final InputStream stream = DataSource.class.getClassLoader().getResourceAsStream("serenity.properties")) {
				properties.load(stream);
			}
				return properties.getProperty(key);
   }
	
	public static String propertyReadBeneficiary(String key) throws IOException {
		Properties prob = new Properties();
		try (final InputStream stream = DataSource.class.getClassLoader()
				.getResourceAsStream("features/testData/testdata.properties")) {
			prob.load(stream);
		}
		return prob.getProperty(key);
	}

	public static void propertyWriteBeneficiary(String key, String value) throws IOException, URISyntaxException {
		OutputStream out = null;
		Properties prob = new Properties();
		try (final InputStream stream = DataSource.class.getClassLoader()
				.getResourceAsStream("features/testData/testdata.properties")) {
			prob.load(stream);
		}
		URL res = DataSource.class.getClassLoader().getResource("features/testData/testdata.properties");
		if (res.getProtocol().equals("jar")) {
			try {
				InputStream input = DataSource.class.getClassLoader()
						.getResourceAsStream("features/testData/testdata.properties");
				file = File.createTempFile("tempfile", ".tmp");
				out = new FileOutputStream(file);
				int read;
				byte[] bytes = new byte[1024];
				while ((read = input.read(bytes)) != -1) {
					out.write(bytes, 0, read);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}finally{
				if(out!=null) {
					out.close();	
				}
			}
		} else {
			// this will work While running from IDE, but not from a JAR
			URL url = DataSource.class.getClassLoader().getResource("features/testData/testdata.properties");
			File fileObject = new File(url.toURI());
			try (FileOutputStream filesave = new FileOutputStream(fileObject)) {
				prob.setProperty(key, value);
				prob.store(filesave, "Beneficiary Values");
			}
		}
	}
	
	/**
	 * This Method is to read property data from MongoDB.
	 * 
	 * @param fileName
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static TreeMap<String, String> readMongoProperties(String fileName) throws JsonParseException, JsonMappingException, IOException{
		 TreeMap<String, String> tmap = new TreeMap<String, String>();
		 ObjectMapper mapper = new ObjectMapper();
		 Document doc = DataSource.getDocument(fileName);
		 tmap = mapper.readValue(doc.toJson(), TreeMap.class);
		 
		 return tmap;	 
	}
	
	
	
}
