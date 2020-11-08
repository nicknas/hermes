package com.openbank.api;

import com.google.common.base.Joiner;
import com.openbank.util.DataSource;
import com.openbank.util.MergeFrom;
import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.core.Serenity;
import net.serenitybdd.rest.SerenityRest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class APIResource {

    private static RestAssuredConfig config = RestAssured.config()
            .encoderConfig(new EncoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false));

    protected RestAssuredConfig config() {
        return config;
    }

    /**
     * API specific request payload wrapper<br/>
     * Format:<br/>
     * {"header" : {}, "body" : $payload}
     *
     * @param body payload
     * @return wrapped payload
     */
    public String asRequest(String body) {
        return "{ \"header\": {},  \"body\": " + body + "}";
    }
    
    public String asRequestSSL(String body,String endUrl) throws MalformedURLException {
    	URL url = new URL (endUrl);
        return "{\r\n" + 
        "    \"operationCode\": \"NO_AUTH_REQUIRED\",\r\n" + 
        "    \"url\": \""+url.getPath()+"\",\r\n" + 
        "    \"requestBody\": {\r\n" + 
        "        \"header\": {},\r\n" + 
        "        \"body\": " + body +"" + 
        "        }\r\n" + 
        "    }\r\n";
    }
    
    public String asRequestSSLString(String body,String endUrl) throws MalformedURLException {
    	URL url = new URL (endUrl);
        return "{\r\n" + 
        "    \"operationCode\": \"NO_AUTH_REQUIRED\",\r\n" + 
        "    \"url\": \""+url.getPath()+"\",\r\n" + 
        "    \"requestBody\": \r\n" + 
        "     " + body +"" + 
        "    }\r\n";
    }
    
    public String asRequestSSLNoBody(String endUrl) throws MalformedURLException {
    	URL url = new URL (endUrl);
    	if(url.getQuery()==null) {
    		return "{\r\n" + "  \"operationCode\": \"NO_AUTH_REQUIRED\",\r\n"
    				+ "  \"url\": \""+url.getPath()+"\"\r\n" + "}";
    	}else {
    		return "{\r\n" + "  \"operationCode\": \"NO_AUTH_REQUIRED\",\r\n"
    				+ "  \"url\": \""+url.getPath()+"?"+url.getQuery()+"\"\r\n" + "}";
    	}
    }
    
    public String asRequestOverride(String body) {
        return "{ \"header\": {\"override_acceptId\": \"string\" },  \"body\": " + body + "}";
    }

    @Deprecated
    protected String asJsonString(Map<String, String> map) {
        List<String> entries = new ArrayList<>();
        map.forEach((key, value) -> entries.add(String.format("\"%s\":\"%s\"", key, value)));
        return "{ " + entries.stream().collect(Collectors.joining(",")) + "}";
    }

    protected String asQueryParam(Map<String, Object> queryParams) {
        return "?" + Joiner.on("&").withKeyValueSeparator("=").join(queryParams);
    }

    @Deprecated
    protected String asJsonString(String key, String value) {
        return String.format("{\"%s\":\"%s\"}", key, value);

    }

    @Deprecated
    protected String asJsonArrayString(String key, String value) {
        return String.format("{\"%s\": [%s]}", key, value);

    }

    protected String buildUrl(String parentUrl, Map<Object, Object> map) {
        for (Entry<Object, Object> entry : map.entrySet())
            parentUrl = parentUrl.replaceAll(toParamKey(Objects.toString(entry.getKey())),
                    Objects.toString(entry.getValue()));
        return parentUrl;
    }

    protected String toParamKey(String key) {
        return ":" + key;
    }

    protected Map<Object, Object> session() {
        return Serenity.getCurrentSession();
    }

    @Deprecated
    protected Map<String, String> applyParams(Map<String, String> data, Map<Object, Object> session) {
        Map<String, String> updated = new HashMap<>(data);
        data.forEach((key, value) -> {
            if (value.startsWith(":"))
                updated.put(key, Objects.toString(session.get(value.split(":")[1])));
        });
        return updated;
    }

    protected String withDynamicProps(String template){
       return MergeFrom.template(template)
               .withVarsFrom(DataSource.randomVarsMap())
               .withFieldsFrom(DataSource.sessionStore().asMap());
    }


    public RequestSpecification given() {
        return SerenityRest.given().config(config()).contentType(ContentType.JSON);
    }
    
    
}
