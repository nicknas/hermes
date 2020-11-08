package com.openbank.stepdefinitions.generic;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openbank.EndPoint;
import com.openbank.api.APIResource;
import com.openbank.stepdefinitions.MasterStepDefinitions;
import com.openbank.util.CreateHeaders;
import com.openbank.util.DataSource;
import com.openbank.util.aws_api.SecretsManagerAPI;

import org.apache.commons.text.StringSubstitutor;
import org.hamcrest.Matchers;

import com.openbank.util.VariablesExpander;
import com.openbank.util.aws_api.ParameterStoreAPI;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;

public class GenericStepDefinitions extends MasterStepDefinitions {

	@Steps
	APIResource apiResource;

	private void clientLogin(String user, String password, String country, String passKey, String type) {
		Serenity.getCurrentSession().remove("progressId");
		user = StringSubstitutor.replace(user, session().asMap());
		password = StringSubstitutor.replace(password, session().asMap());

		Headers headers = CreateHeaders.getHeaders();
		try {
			String passPositionsPath = "", accessTokenPath = "";

			if (country.equals("ES")){
				if (type.equalsIgnoreCase("end")) {
					passPositionsPath = DataSource.propertyReadSerenity("security.passPositions.endUser.ES");
					accessTokenPath = DataSource.propertyReadSerenity("security.login.endUser.ES");
				}
				if (type.equalsIgnoreCase("contact center")) {
					passPositionsPath = DataSource.propertyReadSerenity("security.passPositions.endUser.CC.ES");
					accessTokenPath = DataSource.propertyReadSerenity("security.login.endUser.CC.ES");
				}
			}
			if (country.equals("AR")){
				if (type.equalsIgnoreCase("end")) {
					passPositionsPath = DataSource.propertyReadSerenity("security.passPositions.endUser.AR");
					accessTokenPath = DataSource.propertyReadSerenity("security.login.endUser.AR");
				}
				if (type.equalsIgnoreCase("contact center")) {
					passPositionsPath = DataSource.propertyReadSerenity("security.passPositions.endUser.CC.AR");
					accessTokenPath = DataSource.propertyReadSerenity("security.login.endUser.CC.AR");
				}
			}
			Response passPosition = apiResource.given().headers(headers)
					.body("{\"documentType\": \"passport\", \"username\": \"" + user + "\"}").when()
					.post(passPositionsPath);

			passPosition.then().statusCode(200);
			List<String> positions = passPosition.jsonPath().getList("positions");
			if (positions.size() >= 1 && !positions.get(0).equals("*")) {
				String passwordModified = "";
				for (String position : positions) {
					passwordModified += password.charAt(Integer.parseInt(position) - 1);
				}
				password = passwordModified;
			}
			ObjectMapper objMapper = new ObjectMapper();

			String accessTokenBody = "{\"deviceInformation\": {\"deviceUUID\": \"13424f22-65ec-4dd5-9eb0-19a7af3ca72a\", \"webDeviceInfo\": {\"version\": \"Mozilla/5.0 (Macintosh; Intel Mac OS X 10.14; rv:65.0) Gecko/20100101 Firefox/65.0\"}}, \"documentType\": \"passport\", \"password\": "
					+ "\"" + password + "\"" + ", \"passwordPosition\": " + objMapper.writeValueAsString(positions)
					+ ", \"username\": " + "\"" + user + "\"";
			accessTokenBody += passKey.isEmpty() ? "}" : ", \"passKey\": \"" + passKey + "\"}";

			Response accessTokenResp = apiResource.given().headers(headers).body(accessTokenBody).post(accessTokenPath);
			accessTokenResp.then().statusCode(200);
			session().set("accessToken", accessTokenResp.jsonPath().getString("accessToken"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	@Given("I take the {string} JSON object {string} from the test collection database")
	public void iTakeTheJSONObjectFromTheTestCollectionDatabase(String collection, String name) {
		String object = getDocument(collection + "." + name).toJson();
		session().set("objectData", object);
	}

	@Given("I use the headers taken from the {string} JSON object {string}, that is in the test collection database")
	@SuppressWarnings("unchecked")
	public void iUseTheHeadersTakenFromTheJSONObjectThatIsInTheTestCollectionDatabase(String collection, String name) {
		String headersData = getDocument(collection + "." + name).toJson();
		ObjectMapper objMapper = new ObjectMapper();
		try {
			JsonNode data = objMapper.readTree(headersData);
			Map<String, String> headerMap = objMapper.convertValue(data, HashMap.class);
			session().set("headers", headerMap);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@Given("I log in as the service user {string} with password {string}")
	public void iLogInAsTheServiceUserWithPassword(String user, String password) {
		Headers headers = CreateHeaders.getHeaders();
		user = StringSubstitutor.replace(user, session().asMap());
		password = StringSubstitutor.replace(password, session().asMap());
		try {
			Response passPosition = apiResource.given().headers(headers)
					.body("{\"documentType\": \"service\", \"username\": \"" + user + "\"}").when()
					.post(DataSource.propertyReadSerenity("security.passPositions"));

			passPosition.then().statusCode(200);
			List<String> positions = passPosition.jsonPath().getList("positions");
			if (positions.size() >= 1 && !positions.get(0).equals("*")) {
				String passwordModified = "";
				for (String position : positions) {
					passwordModified += password.charAt(Integer.parseInt(position) - 1);
				}
				password = passwordModified;
			}
			ObjectMapper objMapper = new ObjectMapper();

			String accessTokenBody = "{\"deviceUUID\": \"string\", \"documentType\": \"service\", \"password\": " + "\""
					+ password + "\"" + ", \"passwordPosition\": " + objMapper.writeValueAsString(positions)
					+ ", \"userIP\": \"string\", \"username\": " + "\"" + user + "\"" + "}";
			String accessTokenPath = DataSource.propertyReadSerenity("security.login");
			Response accessTokenResp = apiResource.given().headers(headers).body(accessTokenBody).post(accessTokenPath);

			accessTokenResp.then().statusCode(200);
			session().set("accessToken", accessTokenResp.jsonPath().getString("accessToken"));
			session().set("progressId", "PATATASADA");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Given("I use the query parameters {string}")
	public void iUseTheQueryParameters(String queryParameters) {
		Map<String, String> queryMap = Arrays.stream(queryParameters.split("&")).filter(s -> !s.isEmpty())
				.map(s -> s.split("=")).collect(Collectors.toMap(s -> s[0], s -> s[1]));

		queryMap.forEach((queryName, queryValue) -> {
			queryValue = StringSubstitutor.replace(queryValue, session().asMap());
			queryMap.put(queryName, queryValue);
		});
		session().set("queryParameters", queryMap);
	}

	@Given("I use the headers {string}")
	public void iUseTheHeaders(String headers) {
		Map<String, String> headerMap = Arrays.stream(headers.split(",")).filter(s -> !s.isEmpty())
				.map(s -> s.split(":")).collect(Collectors.toMap(s -> s[0], s -> s[1]));
		
		headerMap.forEach((headerName, headerValue) -> {
			headerValue = StringSubstitutor.replace(headerValue, session().asMap());
			headerMap.put(headerName, headerValue);
		});
		session().set("headers", headerMap);
	}

	@Given("I log in as the end user {string} with password {string}")
	public void iLogInAsTheEndUserWithPassword(String user, String password) {
		Serenity.getCurrentSession().remove("progressId");
		String country = "";
		if (VariablesExpander.get().expand("${country}").equals("es")){
			country = "ES";
		}
		if (VariablesExpander.get().expand("${country}").equals("arg")){
			country = "AR";
		}
		clientLogin(user, password, country, "", "end");
	}

	@Given("I log in as the end user {string} with password {string} and pass key {string}")
	public void iLogInAsTheEndUserWithPasswordAndPassKey(String user, String password, String passKey) {
		Serenity.getCurrentSession().remove("progressId");
		String country = "";
		if (VariablesExpander.get().expand("${country}").equals("es")){
			country = "ES";
		}
		if (VariablesExpander.get().expand("${country}").equals("arg")){
			country = "AR";
		}
		clientLogin(user, password, country, passKey, "end");
	}

	@Given("I register the customer with id {string} in the security system")
	public void iRegisterTheCustomerWithIdInTheSecuritySystem(String id) {
		Headers headers = CreateHeaders.getHeaders();
		try {
			String t24CustomerId = StringSubstitutor.replace(id, session().asMap());
			String user = "", password = "", country = "";
			if (VariablesExpander.get().expand("${country}").equals("es")){
				user = "ES-" + t24CustomerId;
				password = "1234";
				country = "ES";
			}
			if (VariablesExpander.get().expand("${country}").equals("arg")){
				user = "AR-" + t24CustomerId;
				password = "123456";
				country = "AR";
			}
			String loginPath = DataSource.propertyReadSerenity("security.login");
			HashMap<String, String> obmCredentials = ParameterStoreAPI.getObmCredentials(country);
			String obmBody = "{\"documentType\": \"service\", \"password\": \"" + obmCredentials.get("password") + "\", \"passwordPosition\": [\"*\"], \"username\": \"" + obmCredentials.get("user") + "\"}";
			Response obm = apiResource.given().headers(headers)
					.body(obmBody).when()
					.post(loginPath);
			
			obm.then().statusCode(200);
			String obmAccessToken = obm.jsonPath().getString("accessToken");

			String createUserBody = "{\"documentType\": \"passport\", \"username\": \"" + user + "\", \"password\": \"" + password + "\", \"country\": \"" + country + "\", \"aaaUserID\": \"" + user + "\", \"t24CustomerId\": \"" + t24CustomerId + "\"}";
			String createUserPath = DataSource.propertyReadSerenity("security.create.endUser");
			List<Header> headerList = headers.asList();
			headerList = new ArrayList<>(headerList);
			headerList.add(new Header("accessToken", obmAccessToken));
			headers = new Headers(headerList);
			Response createUser = apiResource.given().headers(headers)
					.body(createUserBody).when()
					.post(createUserPath);

			createUser.then().statusCode(Matchers.allOf(Matchers.greaterThanOrEqualTo(200), Matchers.lessThan(300)));

			clientLogin(user, password, country, "", "end");

			String signatureKey = "12345678";
			String createSignatureKeyBody = "{\"signatureKey\": \"" + signatureKey + "\"}";
			String createSignatureKeyPath = DataSource.propertyReadSerenity("security.signature-key");

			headerList.remove(headerList.size()-1);
			headerList.add(new Header("accessToken", Serenity.getCurrentSession().get("accessToken").toString()));

			Response createSignatureKey = apiResource.given().headers(new Headers(headerList))
					.body(createSignatureKeyBody).when()
					.post(createSignatureKeyPath);

			createSignatureKey.then().statusCode(Matchers.allOf(Matchers.greaterThanOrEqualTo(200), Matchers.lessThan(300)));

			String passKey = "";
			if (country.equals("ES")) {
				passKey = "12345678";
			}
			if (country.equals("AR")) {
				passKey = "u" + t24CustomerId;
			}

			String createPassKeyBody = "{\"passKey\": \"" + passKey + "\"}";
			String createPassKeyPath = DataSource.propertyReadSerenity("security.pass-key");

			Response createPassKey = apiResource.given().headers(new Headers(headerList))
					.body(createPassKeyBody).when()
					.post(createPassKeyPath);

			createPassKey.then().statusCode(Matchers.allOf(Matchers.greaterThanOrEqualTo(200), Matchers.lessThan(300)));

			String activateClientBody = "{\"documentType\": \"passport\", \"username\": \"" + user + "\", \"role\": \"CLIENT\", \"t24UserId\": \"CUS" + t24CustomerId + "\"}";
			String activateClientPath = DataSource.propertyReadSerenity("security.activate.endUser");

			headerList.remove(headerList.size()-1);
			headerList.add(new Header("accessToken", obmAccessToken));
			Response activateClient = apiResource.given().headers(new Headers(headerList))
					.body(activateClientBody).when()
					.post(activateClientPath);
			
			activateClient.then().statusCode(200);

			clientLogin(user, password, country, passKey, "end");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Given("I log in as the contact center user {string} with password {string} and pass key {string}")
	public void iLogInAsTheContactCenterUserWithPasswordAndPassKey(String user, String password, String passKey) {
		Serenity.getCurrentSession().remove("progressId");
		String country = "";
		if (VariablesExpander.get().expand("${country}").equals("es")){
			country = "ES";
		}
		if (VariablesExpander.get().expand("${country}").equals("arg")){
			country = "AR";
		}
		clientLogin(user, password, country, passKey, "contact center");
	}

	@Given("I log in as the contact center user {string} with password {string}")
	public void iLogInAsTheContactCenterUserWithPassword(String user, String password) {
		Serenity.getCurrentSession().remove("progressId");
		String country = "";
		if (VariablesExpander.get().expand("${country}").equals("es")){	
			country = "ES";
		}
		if (VariablesExpander.get().expand("${country}").equals("arg")){
			country = "AR";
		}
		clientLogin(user, password, country, "", "contact center");
	}

	@When("I make the {string} request {string} using the {string} JSON object {string} from the test collection database")
	@SuppressWarnings("unchecked")
	public void iMakeTheRequestUsingTheJSONObjectFromTheTestCollectionDatabase(String verb, String url,
			String collection, String objectId) {

		String object = getDocument(collection + "." + objectId).toJson();
		object = expandBody(object);
		session().set("objectData", object);
		Headers headers = CreateHeaders.getHeaders();
		List<Header> headerList = new ArrayList<>(headers.asList());
		ObjectMapper objMapper = new ObjectMapper();
		if (!"GET".equalsIgnoreCase(verb)) {
			if (!Serenity.getCurrentSession().containsKey("accessToken")) {
				JsonNode credentials = SecretsManagerAPI.getSecret("DefaultServiceUser");
				iLogInAsTheServiceUserWithPassword(credentials.get("user").asText(),
						credentials.get("password").asText());
			}
			object = apiResource.asRequest(object);
			headerList.add(new Header("access_token", (String) Serenity.getCurrentSession().get("accessToken")));
			headerList.add(new Header("accessToken", (String) Serenity.getCurrentSession().get("accessToken")));
			if (Serenity.getCurrentSession().containsKey("progressId")) {
				headerList.add(new Header("progressId", (String) Serenity.getCurrentSession().get("progressId")));
			} else {
				try {
					URL urlProgressId = new URL(EndPoint.BASE.getUrl() + expandUrl(url));
					String urlProgressIdString = urlProgressId.getPath();
					if (urlProgressId.getQuery() != null) {
						urlProgressIdString += "?" + urlProgressId.getQuery();
					}
					String hostProgressId = DataSource.propertyReadSerenity("security.hostProgressId");
					String progressIdBody = "{\"operationCode\": \"NO_AUTH_REQUIRED\", \"url\": "
							+ objMapper.writeValueAsString(urlProgressIdString) + ", \"requestBody\": " + object + "}";
					Response progressIdResp = apiResource.given().headers(new Headers(headerList)).body(progressIdBody)
							.post(hostProgressId);
					progressIdResp.then().statusCode(201);
					headerList.add(new Header("progressId", progressIdResp.jsonPath().getString("progressId")));
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			if (Serenity.getCurrentSession().containsKey("headers")) {
				Map<String, String> headerMap = (Map<String, String>) Serenity.getCurrentSession().get("headers");
				for (Map.Entry<String, String> entry : headerMap.entrySet()) {
					headerList.add(new Header(entry.getKey(), entry.getValue()));
				}
			}
			setResponse(apiResource.given().headers(new Headers(headerList)).body(object).when()
					.request(verb.toUpperCase(), EndPoint.BASE.getUrl() + expandUrl(url)));
		}
	}

	@When("I send the {string} request {string} using the {string} JSON object {string} from the test collection database")
	@SuppressWarnings("unchecked")
	public void iSendTheRequestUsingTheJSONObjectFromTheTestCollectionDatabase(String verb, String url,
			String collection, String objectId) {

		String object = getDocument(collection + "." + objectId).toJson();
		object = expandBody(object);
		session().set("objectData", object);
		ObjectMapper objMapper = new ObjectMapper();
		Headers headers = CreateHeaders.getHeaders();
		List<Header> headerList = new ArrayList<>(headers.asList());
		if (!"GET".equalsIgnoreCase(verb)) {
			if (!Serenity.getCurrentSession().containsKey("accessToken")) {
				JsonNode credentials = SecretsManagerAPI.getSecret("DefaultServiceUser");
				iLogInAsTheServiceUserWithPassword(credentials.get("user").asText(),
						credentials.get("password").asText());
			}

			headerList.add(new Header("access_token", (String) Serenity.getCurrentSession().get("accessToken")));
			headerList.add(new Header("accessToken", (String) Serenity.getCurrentSession().get("accessToken")));
			if (Serenity.getCurrentSession().containsKey("progressId")) {
				headerList.add(new Header("progressId", (String) Serenity.getCurrentSession().get("progressId")));
			} else {
				try {
					URL urlProgressId = new URL(EndPoint.BASE.getUrl() + expandUrl(url));
					String urlProgressIdString = urlProgressId.getPath();
					if (urlProgressId.getQuery() != null) {
						urlProgressIdString += "?" + urlProgressId.getQuery();
					}
					String hostProgressId = DataSource.propertyReadSerenity("security.hostProgressId");
					String progressIdBody = "{\"operationCode\": \"NO_AUTH_REQUIRED\", \"url\": "
							+ objMapper.writeValueAsString(urlProgressIdString) + ", \"requestBody\": " + object + "}";
					Response progressIdResp = apiResource.given().headers(new Headers(headerList)).body(progressIdBody)
							.post(hostProgressId);
					progressIdResp.then().statusCode(201);
					headerList.add(new Header("progressId", progressIdResp.jsonPath().getString("progressId")));
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			if (Serenity.getCurrentSession().containsKey("headers")) {
				Map<String, String> headerMap = (Map<String, String>) Serenity.getCurrentSession().get("headers");
				for (Map.Entry<String, String> entry : headerMap.entrySet()) {
					headerList.add(new Header(entry.getKey(), entry.getValue()));
				}
			}
			setResponse(apiResource.given().headers(new Headers(headerList)).body(object).when()
			.request(verb.toUpperCase(), EndPoint.BASE.getUrl() + expandUrl(url)));
		}
	}

	@When("I make the {string} request {string}")
	@SuppressWarnings("unchecked")
	public void iMakeTheRequest(String verb, String url) {
		Headers headers = CreateHeaders.getHeaders();
		ObjectMapper objMapper = new ObjectMapper();
		List<Header> headerList = new ArrayList<>(headers.asList());
		if (!"GET".equalsIgnoreCase(verb)) {
			if (!Serenity.getCurrentSession().containsKey("accessToken")) {
				JsonNode credentials = SecretsManagerAPI.getSecret("DefaultServiceUser");
				iLogInAsTheServiceUserWithPassword(credentials.get("user").asText(),
						credentials.get("password").asText());
			}

			headerList.add(new Header("access_token", (String) Serenity.getCurrentSession().get("accessToken")));
			headerList.add(new Header("accessToken", (String) Serenity.getCurrentSession().get("accessToken")));
			if (Serenity.getCurrentSession().containsKey("progressId")) {
				headerList.add(new Header("progressId", (String) Serenity.getCurrentSession().get("progressId")));
			} else {
				try {
					URL urlProgressId = new URL(EndPoint.BASE.getUrl() + expandUrl(url));
					String urlProgressIdString = urlProgressId.getPath();
					if (urlProgressId.getQuery() != null) {
						urlProgressIdString += "?" + urlProgressId.getQuery();
					}
					String hostProgressId = DataSource.propertyReadSerenity("security.hostProgressId");
					String progressIdBody = "{\"operationCode\": \"NO_AUTH_REQUIRED\", \"url\": "
							+ objMapper.writeValueAsString(urlProgressIdString) + "}";
					Response progressIdResp = apiResource.given().headers(new Headers(headerList)).body(progressIdBody)
							.post(hostProgressId);
					progressIdResp.then().statusCode(201);
					headerList.add(new Header("progressId", progressIdResp.jsonPath().getString("progressId")));
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			if (Serenity.getCurrentSession().containsKey("headers")) {
				Map<String, String> headerMap = (Map<String, String>) Serenity.getCurrentSession().get("headers");
				for (Map.Entry<String, String> entry : headerMap.entrySet()) {
					headerList.add(new Header(entry.getKey(), entry.getValue()));
				}
			}
			setResponse(apiResource.given().headers(new Headers(headerList)).body("{}").when()
			.request(verb.toUpperCase(), EndPoint.BASE.getUrl() + expandUrl(url)));
		}

		else {
			if (!Serenity.getCurrentSession().containsKey("accessToken")) {
				JsonNode credentials = SecretsManagerAPI.getSecret("DefaultServiceUser");
				iLogInAsTheServiceUserWithPassword(credentials.get("user").asText(),
						credentials.get("password").asText());
			}

			headerList.add(new Header("access_token", (String) Serenity.getCurrentSession().get("accessToken")));
			headerList.add(new Header("accessToken", (String) Serenity.getCurrentSession().get("accessToken")));
			if (Serenity.getCurrentSession().containsKey("progressId")) {
				headerList.add(new Header("progressId", (String) Serenity.getCurrentSession().get("progressId")));
			} else {
				try {
					URL urlProgressId = new URL(EndPoint.BASE.getUrl() + expandUrl(url));
					String urlProgressIdString = urlProgressId.getPath();
					if (urlProgressId.getQuery() != null) {
						urlProgressIdString += "?" + urlProgressId.getQuery();
					}
					String hostProgressId = DataSource.propertyReadSerenity("security.hostProgressId");
					String progressIdBody = "{\"operationCode\": \"NO_AUTH_REQUIRED\", \"url\": "
							+ objMapper.writeValueAsString(urlProgressIdString) + "}";
					RequestSpecification progressIdRequest = apiResource.given().headers(new Headers(headerList));
					if (Serenity.getCurrentSession().containsKey("queryParameters")) {
						progressIdRequest = progressIdRequest
								.queryParams((Map<String, String>) Serenity.getCurrentSession().get("queryParameters"));
					}
					Response progressIdResp = progressIdRequest.body(progressIdBody).post(hostProgressId);
					progressIdResp.then().statusCode(201);
					headerList.add(new Header("progressId", progressIdResp.jsonPath().getString("progressId")));
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			if (Serenity.getCurrentSession().containsKey("headers")) {
				Map<String, String> headerMap = (Map<String, String>) Serenity.getCurrentSession().get("headers");
				for (Map.Entry<String, String> entry : headerMap.entrySet()) {
					headerList.add(new Header(entry.getKey(), entry.getValue()));
				}
			}
			if (Serenity.getCurrentSession().containsKey("queryParameters")) {
				Map<String, String> queryParameters = (Map<String, String>) Serenity.getCurrentSession()
						.get("queryParameters");
				setResponse(apiResource.given().queryParams(queryParameters).headers(new Headers(headerList)).when()
						.get(EndPoint.BASE.getUrl() + expandUrl(url)));
			} else {
				setResponse(apiResource.given().headers(new Headers(headerList)).when()
						.get(EndPoint.BASE.getUrl() + expandUrl(url)));
			}
		}
	}

	@When("I send the {string} request {string}")
	@SuppressWarnings("unchecked")
	public void iSendTheRequest(String verb, String url) {
		Headers headers = CreateHeaders.getHeaders();
		ObjectMapper objMapper = new ObjectMapper();
		List<Header> headerList = new ArrayList<>(headers.asList());
		if (!"GET".equalsIgnoreCase(verb)) {
			if (!Serenity.getCurrentSession().containsKey("accessToken")) {
				JsonNode credentials = SecretsManagerAPI.getSecret("DefaultServiceUser");
				iLogInAsTheServiceUserWithPassword(credentials.get("user").asText(),
						credentials.get("password").asText());
			}

			headerList.add(new Header("access_token", (String) Serenity.getCurrentSession().get("accessToken")));
			headerList.add(new Header("accessToken", (String) Serenity.getCurrentSession().get("accessToken")));
			if (Serenity.getCurrentSession().containsKey("progressId")) {
				headerList.add(new Header("progressId", (String) Serenity.getCurrentSession().get("progressId")));
			} else {
				try {
					URL urlProgressId = new URL(EndPoint.BASE.getUrl() + expandUrl(url));
					String urlProgressIdString = urlProgressId.getPath();
					if (urlProgressId.getQuery() != null) {
						urlProgressIdString += "?" + urlProgressId.getQuery();
					}
					String hostProgressId = DataSource.propertyReadSerenity("security.hostProgressId");
					String progressIdBody = "{\"operationCode\": \"NO_AUTH_REQUIRED\", \"url\": "
							+ objMapper.writeValueAsString(urlProgressIdString) + "}";
					Response progressIdResp = apiResource.given().headers(new Headers(headerList)).body(progressIdBody)
							.post(hostProgressId);
					progressIdResp.then().statusCode(201);
					headerList.add(new Header("progressId", progressIdResp.jsonPath().getString("progressId")));
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			if (Serenity.getCurrentSession().containsKey("headers")) {
				Map<String, String> headerMap = (Map<String, String>) Serenity.getCurrentSession().get("headers");
				for (Map.Entry<String, String> entry : headerMap.entrySet()) {
					headerList.add(new Header(entry.getKey(), entry.getValue()));
				}
			}
			setResponse(apiResource.given().headers(new Headers(headerList)).body("{}").when()
			.request(verb.toUpperCase(), EndPoint.BASE.getUrl() + expandUrl(url)));
		}

		else {
			if (!Serenity.getCurrentSession().containsKey("accessToken")) {
				JsonNode credentials = SecretsManagerAPI.getSecret("DefaultServiceUser");
				iLogInAsTheServiceUserWithPassword(credentials.get("user").asText(),
						credentials.get("password").asText());
			}

			headerList.add(new Header("access_token", (String) Serenity.getCurrentSession().get("accessToken")));
			headerList.add(new Header("accessToken", (String) Serenity.getCurrentSession().get("accessToken")));
			if (Serenity.getCurrentSession().containsKey("progressId")) {
				headerList.add(new Header("progressId", (String) Serenity.getCurrentSession().get("progressId")));
			} else {
				try {
					URL urlProgressId = new URL(EndPoint.BASE.getUrl() + expandUrl(url));
					String urlProgressIdString = urlProgressId.getPath();
					if (urlProgressId.getQuery() != null) {
						urlProgressIdString += "?" + urlProgressId.getQuery();
					}
					String hostProgressId = DataSource.propertyReadSerenity("security.hostProgressId");
					String progressIdBody = "{\"operationCode\": \"NO_AUTH_REQUIRED\", \"url\": "
							+ objMapper.writeValueAsString(urlProgressIdString) + "}";
					RequestSpecification progressIdRequest = apiResource.given().headers(new Headers(headerList));
					if (Serenity.getCurrentSession().containsKey("queryParameters")) {
						progressIdRequest = progressIdRequest
								.queryParams((Map<String, String>) Serenity.getCurrentSession().get("queryParameters"));
					}
					Response progressIdResp = progressIdRequest.body(progressIdBody).post(hostProgressId);
					progressIdResp.then().statusCode(201);
					headerList.add(new Header("progressId", progressIdResp.jsonPath().getString("progressId")));
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			if (Serenity.getCurrentSession().containsKey("headers")) {
				Map<String, String> headerMap = (Map<String, String>) Serenity.getCurrentSession().get("headers");
				for (Map.Entry<String, String> entry : headerMap.entrySet()) {
					headerList.add(new Header(entry.getKey(), entry.getValue()));
				}
			}
			if (Serenity.getCurrentSession().containsKey("queryParameters")) {
				Map<String, String> queryParameters = (Map<String, String>) Serenity.getCurrentSession()
						.get("queryParameters");
				setResponse(apiResource.given().queryParams(queryParameters).headers(new Headers(headerList)).when()
						.get(EndPoint.BASE.getUrl() + expandUrl(url)));
			} else {
				setResponse(apiResource.given().headers(new Headers(headerList)).when()
						.get(EndPoint.BASE.getUrl() + expandUrl(url)));
			}

		}

	}

	@When("I make the {string} request {string} using it")
	@SuppressWarnings("unchecked")
	public void iMakeTheRequestUsingIt(String verb, String url) {
		ObjectMapper objMapper = new ObjectMapper();
		Headers headers = CreateHeaders.getHeaders();
		List<Header> headerList = new ArrayList<>(headers.asList());
		if (!"GET".equalsIgnoreCase(verb)) {
			if (!Serenity.getCurrentSession().containsKey("accessToken")) {
				JsonNode credentials = SecretsManagerAPI.getSecret("DefaultServiceUser");
				iLogInAsTheServiceUserWithPassword(credentials.get("user").asText(),
						credentials.get("password").asText());
			}

			headerList.add(new Header("access_token", (String) Serenity.getCurrentSession().get("accessToken")));
			headerList.add(new Header("accessToken", (String) Serenity.getCurrentSession().get("accessToken")));
			if (Serenity.getCurrentSession().containsKey("progressId")) {
				headerList.add(new Header("progressId", (String) Serenity.getCurrentSession().get("progressId")));
			} else {
				try {
					URL urlProgressId = new URL(EndPoint.BASE.getUrl() + expandUrl(url));
					String urlProgressIdString = urlProgressId.getPath();
					if (urlProgressId.getQuery() != null) {
						urlProgressIdString += "?" + urlProgressId.getQuery();
					}
					String hostProgressId = DataSource.propertyReadSerenity("security.hostProgressId");
					String progressIdBody = "{\"operationCode\": \"NO_AUTH_REQUIRED\", \"url\": "
							+ objMapper.writeValueAsString(urlProgressIdString) + ", \"requestBody\": "
							+ apiResource.asRequest(
									Serenity.getCurrentSession().get("objectData").toString())
							+ "}";
					Response progressIdResp = apiResource.given().headers(new Headers(headerList)).body(progressIdBody)
							.post(hostProgressId);
					progressIdResp.then().statusCode(201);
					headerList.add(new Header("progressId", progressIdResp.jsonPath().getString("progressId")));
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			if (Serenity.getCurrentSession().containsKey("headers")) {
				Map<String, String> headerMap = (Map<String, String>) Serenity.getCurrentSession().get("headers");
				for (Map.Entry<String, String> entry : headerMap.entrySet()) {
					headerList.add(new Header(entry.getKey(), entry.getValue()));
				}
			}
			setResponse(apiResource.given().headers(new Headers(headerList))
					.body(apiResource
							.asRequest(StringSubstitutor.replace(Serenity.getCurrentSession().get("objectData").toString(), session().asMap())))
					.when().request(verb.toUpperCase(), EndPoint.BASE.getUrl() + expandUrl(url)));
		}
	}

	@When("I send the {string} request {string} using it")
	@SuppressWarnings("unchecked")
	public void iSendTheRequestUsingIt(String verb, String url) {
		Headers headers = CreateHeaders.getHeaders();
		ObjectMapper objMapper = new ObjectMapper();
		List<Header> headerList = new ArrayList<>(headers.asList());
		if (!"GET".equalsIgnoreCase(verb)) {
			if (!Serenity.getCurrentSession().containsKey("accessToken")) {
				JsonNode credentials = SecretsManagerAPI.getSecret("DefaultServiceUser");
				iLogInAsTheServiceUserWithPassword(credentials.get("user").asText(),
						credentials.get("password").asText());
			}

			headerList.add(new Header("access_token", (String) Serenity.getCurrentSession().get("accessToken")));
			headerList.add(new Header("accessToken", (String) Serenity.getCurrentSession().get("accessToken")));
			if (Serenity.getCurrentSession().containsKey("progressId")) {
				headerList.add(new Header("progressId", (String) Serenity.getCurrentSession().get("progressId")));
			} else {
				try {
					URL urlProgressId = new URL(EndPoint.BASE.getUrl() + expandUrl(url));
					String urlProgressIdString = urlProgressId.getPath();
					if (urlProgressId.getQuery() != null) {
						urlProgressIdString += "?" + urlProgressId.getQuery();
					}
					String hostProgressId = DataSource.propertyReadSerenity("security.hostProgressId");
					String progressIdBody = "{\"operationCode\": \"NO_AUTH_REQUIRED\", \"url\": "
							+ objMapper.writeValueAsString(urlProgressIdString) + ", \"requestBody\": "
							+ Serenity.getCurrentSession().get("objectData").toString() + "}";
					Response progressIdResp = apiResource.given().headers(new Headers(headerList)).body(progressIdBody)
							.post(hostProgressId);
					progressIdResp.then().statusCode(201);
					headerList.add(new Header("progressId", progressIdResp.jsonPath().getString("progressId")));
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			if (Serenity.getCurrentSession().containsKey("headers")) {
				Map<String, String> headerMap = (Map<String, String>) Serenity.getCurrentSession().get("headers");
				for (Map.Entry<String, String> entry : headerMap.entrySet()) {
					headerList.add(new Header(entry.getKey(), entry.getValue()));
				}
			}
			setResponse(apiResource.given().headers(new Headers(headerList))
					.body(StringSubstitutor.replace(Serenity.getCurrentSession().get("objectData").toString(), session().asMap())).when()
					.request(verb.toUpperCase(), EndPoint.BASE.getUrl() + expandUrl(url)));
		}
	}

	@When("I modify the field {string} with the value {string}")
	public void iModifyTheFieldWithTheValue(String fields, String value) {
		if (Serenity.getCurrentSession().containsKey("objectData")
				&& !fields.trim().isEmpty() 
				&& !value.trim().isEmpty()) {
			try {
				Object valueToModify = null;
				if (fields.contains("legalDocId") && value.equalsIgnoreCase("MDNIAR")) {
					valueToModify = generateDNI_AR("M");
				} else if (fields.contains("legalDocId") && value.equalsIgnoreCase("FDNIAR")) {
					valueToModify = generateDNI_AR("F");
				} else if (fields.contains("legalDocId") && value.equalsIgnoreCase("DNIAR")) {
					valueToModify = generateDNI_AR("");
				} else if (fields.contains("legalDocId") && value.equalsIgnoreCase("DNIES")) {
					valueToModify = getDNI();
				} else if (fields.contains("legalDocId") && (value.equalsIgnoreCase("MCUIT") || value.equalsIgnoreCase("MCUIL"))) {
					valueToModify = generatelegalId_AR("M");
				} else if (fields.contains("legalDocId") && (value.equalsIgnoreCase("FCUIL") || value.equalsIgnoreCase("FCUIT"))) {
					valueToModify = generatelegalId_AR("F");
				}
				else if (fields.contains("legalDocId") && (value.equalsIgnoreCase("IDPT"))){
					if (VariablesExpander.get().expand("${country}").equals("es")){
						valueToModify = generatePassportPortugal_ES();
					}
				}
				else if (fields.contains("legalDocId") && (value.equalsIgnoreCase("IDDE"))){
					if (VariablesExpander.get().expand("${country}").equals("es")){
						valueToModify = generatePassportDeutschland_ES();
					}
				}
				else if(fields.contains("legalDocId") && (value.equalsIgnoreCase("IDNL"))) {
					if (VariablesExpander.get().expand("${country}").equals("es")){
						valueToModify = generatePassportNetherlands_ES();
					}
				}else if (value.contains("TEL") || (fields.contains("legalDocId") && value.contains("PASSPORT"))) {
					if (value.contains("TEL"))
						valueToModify = generateRandomNumber(Integer.parseInt(value.replace("TEL", ""))).toString();
					else
						valueToModify = generateRandomNumber(Integer.parseInt(value.replace("PASSPORT", ""))).toString();
				} else if (value.equalsIgnoreCase("MAIL")) {
					valueToModify = generateRandomWord(7, "", false) + ((new Random().nextInt(2) == 0) ? "@gmail.com" : "@outlook.com");
				} else if (value.equalsIgnoreCase("null")) {
					valueToModify = null;
				} else if (value.equals("[]")) {
					valueToModify = new ObjectMapper().createArrayNode();
				} else if (value.equals("{}")) {
					valueToModify = new ObjectMapper().createObjectNode();
				} else {
					valueToModify = StringSubstitutor.replace(value, session().asMap());
				}
				ObjectMapper objMapper = new ObjectMapper();
				List<String> listFields = Arrays.stream(fields.split(";|\\s")).collect(Collectors.toList());
				JsonNode node = objMapper.readTree((String) Serenity.getCurrentSession().get("objectData"));

				for (String field : listFields) {
					JsonPointer pointerHijo = convertJsonPathToJsonPointer(field);
					JsonPointer pointerPadre = pointerHijo.head();
					JsonNode objectDataParent = node.at(pointerPadre);
					String fieldToModify = pointerHijo.last().toString().substring(1);
					if (valueToModify instanceof Number) {
						try {
							valueToModify = Float.parseFloat(valueToModify.toString());
							((ObjectNode) (objectDataParent)).put(fieldToModify, (float) valueToModify);
						} catch (NumberFormatException e) {
							try {
								valueToModify = Double.parseDouble(valueToModify.toString());
								((ObjectNode) (objectDataParent)).put(fieldToModify, (double) valueToModify);
							} catch (NumberFormatException e1) {
								try {
									valueToModify = Integer.parseInt(valueToModify.toString());
									((ObjectNode) (objectDataParent)).put(fieldToModify, (int) valueToModify);
								} catch (NumberFormatException e2) {
									try {
										valueToModify = Long.parseLong(valueToModify.toString());
										((ObjectNode) (objectDataParent)).put(fieldToModify, (long) valueToModify);
									} catch (NumberFormatException e3) {
										throw e3;
									}
								}
							}
						}

					} else {
						if (valueToModify == null) {
							((ObjectNode) (objectDataParent)).put(fieldToModify, "");
						} else {
							if (valueToModify instanceof ArrayNode)
								((ObjectNode) (objectDataParent)).putArray(fieldToModify);
							else if (valueToModify instanceof ObjectNode)
								((ObjectNode) (objectDataParent)).putObject(fieldToModify);
							else
								((ObjectNode) (objectDataParent)).put(fieldToModify, (String) valueToModify);
						}
					}

				}
				String objectData = node.toString();
				session().set("objectData", objectData);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@When("I modify the field {string} with the value {double}")
	public void iModifyTheFieldWithTheValue(String fields, double value) {
		if (Serenity.getCurrentSession().containsKey("objectData") && !fields.trim().isEmpty()) {
			try {
				ObjectMapper objMapper = new ObjectMapper();
				List<String> listFields = Arrays.stream(fields.split(";|\\s")).collect(Collectors.toList());
				JsonNode node = objMapper.readTree((String) Serenity.getCurrentSession().get("objectData"));

				for (String field : listFields) {
					JsonPointer pointerHijo = convertJsonPathToJsonPointer(field);
					JsonPointer pointerPadre = pointerHijo.head();
					JsonNode objectDataParent = node.at(pointerPadre);
					String fieldToModify = pointerHijo.last().toString().substring(1);
					((ObjectNode) (objectDataParent)).put(fieldToModify, value);
				}
				String objectData = node.toString();
				session().set("objectData", objectData);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@When("I modify the field {string} with the value {float}")
	public void iModifyTheFieldWithTheValue(String fields, float value) {
		if (Serenity.getCurrentSession().containsKey("objectData") && !fields.trim().isEmpty()) {
			try {
				ObjectMapper objMapper = new ObjectMapper();
				List<String> listFields = Arrays.stream(fields.split(";|\\s")).collect(Collectors.toList());
				JsonNode node = objMapper.readTree((String) Serenity.getCurrentSession().get("objectData"));

				for (String field : listFields) {
					JsonPointer pointerHijo = convertJsonPathToJsonPointer(field);
					JsonPointer pointerPadre = pointerHijo.head();
					JsonNode objectDataParent = node.at(pointerPadre);
					String fieldToModify = pointerHijo.last().toString().substring(1);
					((ObjectNode) (objectDataParent)).put(fieldToModify, value);
				}
				String objectData = node.toString();
				session().set("objectData", objectData);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@When("I modify the field {string} with the value {long}")
	public void iModifyTheFieldWithTheValue(String fields, long value) {
		if (Serenity.getCurrentSession().containsKey("objectData") && !fields.trim().isEmpty()) {
			try {
				ObjectMapper objMapper = new ObjectMapper();
				List<String> listFields = Arrays.stream(fields.split(";|\\s")).collect(Collectors.toList());
				JsonNode node = objMapper.readTree((String) Serenity.getCurrentSession().get("objectData"));

				for (String field : listFields) {
					JsonPointer pointerHijo = convertJsonPathToJsonPointer(field);
					JsonPointer pointerPadre = pointerHijo.head();
					JsonNode objectDataParent = node.at(pointerPadre);
					String fieldToModify = pointerHijo.last().toString().substring(1);
					((ObjectNode) (objectDataParent)).put(fieldToModify, value);
				}
				String objectData = node.toString();
				session().set("objectData", objectData);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@When("I modify the field {string} with a random value between {biginteger} and {biginteger}")
	public void iModifyTheFieldWithARandomValueBetweenAnd(String fields, BigInteger valueInitial, BigInteger valueEnd) {
		if (Serenity.getCurrentSession().containsKey("objectData") && !fields.trim().isEmpty()) {
			try {
				ObjectMapper objMapper = new ObjectMapper();
				List<String> listFields = Arrays.stream(fields.split(";|\\s")).collect(Collectors.toList());
				JsonNode node = objMapper.readTree((String) Serenity.getCurrentSession().get("objectData"));

				BigInteger bigInteger = valueEnd.subtract(valueInitial);
				Random randNum = new Random();
				int len = valueEnd.bitLength();
				BigInteger res = new BigInteger(len, randNum);
				if (res.compareTo(valueInitial) < 0)
				   res = res.add(valueInitial);
				if (res.compareTo(bigInteger) >= 0)
				   res = res.mod(bigInteger).add(valueInitial);

				for (String field : listFields) {
					JsonPointer pointerHijo = convertJsonPathToJsonPointer(field);
					JsonPointer pointerPadre = pointerHijo.head();
					JsonNode objectDataParent = node.at(pointerPadre);
					String fieldToModify = pointerHijo.last().toString().substring(1);
					((ObjectNode) (objectDataParent)).put(fieldToModify, res);
				}
				String objectData = node.toString();
				session().set("objectData", objectData);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@When("I modify the field {string} with a random value between {biginteger} and {biginteger} as a string")
	public void iModifyTheFieldWithARandomValueBetweenAndAsAString(String fields, BigInteger valueInitial, BigInteger valueEnd) {
		if (Serenity.getCurrentSession().containsKey("objectData") && !fields.trim().isEmpty()) {
			try {
				ObjectMapper objMapper = new ObjectMapper();
				List<String> listFields = Arrays.stream(fields.split(";|\\s")).collect(Collectors.toList());
				JsonNode node = objMapper.readTree((String) Serenity.getCurrentSession().get("objectData"));

				BigInteger bigInteger = valueEnd.subtract(valueInitial);
				Random randNum = new Random();
				int len = valueEnd.bitLength();
				BigInteger res = new BigInteger(len, randNum);
				if (res.compareTo(valueInitial) < 0)
				   res = res.add(valueInitial);
				if (res.compareTo(bigInteger) >= 0)
				   res = res.mod(bigInteger).add(valueInitial);

				for (String field : listFields) {
					JsonPointer pointerHijo = convertJsonPathToJsonPointer(field);
					JsonPointer pointerPadre = pointerHijo.head();
					JsonNode objectDataParent = node.at(pointerPadre);
					String fieldToModify = pointerHijo.last().toString().substring(1);
					((ObjectNode) (objectDataParent)).put(fieldToModify, res.toString());
				}
				String objectData = node.toString();
				session().set("objectData", objectData);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@When("I wait the request for {long} seconds")
	public void iWaitTheRequestForSeconds(long seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@When("I store the variable {string} with name {string} from the response")
	public void iStoreTheVariableWithNameFromTheResponse(String variable, String name) {
		assertExistsJsonNode(getResponse().jsonPath().get(variable));
		session().set(name, getResponse().jsonPath().get(variable));
	}

	@When("I store the variable {string} with name {string} from the database")
	public void iStoreTheVariableWithNameFromTheDatabase(String variable, String name) {
		assertExistsJsonNode(Serenity.getCurrentSession().get("objectData"));
		JsonPath objectDataJson = JsonPath.from((String) Serenity.getCurrentSession().get("objectData"));
		session().set(name, objectDataJson.get(variable));
	}

	@When("I store the actual date with name {string}")
	public void iStoreTheActualDateWithName(String name) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		session().set("dateFormat", "yyyy-MM-dd");
		session().set(name, formatter.format(Calendar.getInstance().getTime()));
	}

	@When("I store the actual date with name {string} and format {string}")
	public void iStoreTheActualDateWithNameAndFormat(String name, String format) {
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		session().set("dateFormat", format);
		session().set(name, formatter.format(Calendar.getInstance().getTime()));
	}

	@When("I {string} the value {string} to the variable {string}")
	public void iTheValueToTheVariable(String operation, String valueToOperate, String variableToOperate) {
		variableToOperate = variableToOperate.replace("$", "").replace("{", "").replace("}", "");
		String variable = Serenity.getCurrentSession().get(variableToOperate).toString();
		SimpleDateFormat formatter = new SimpleDateFormat(Serenity.getCurrentSession().get("dateFormat").toString());
		Calendar date = Calendar.getInstance();
		try {
			date.setTime(formatter.parse(variable));
			if (operation.equalsIgnoreCase("add")) {
				if (valueToOperate.contains("M")) {
					date.add(Calendar.MONTH, Integer.parseInt(valueToOperate.replace("M", "")));
				}
				else if (valueToOperate.contains("D")) {		
					date.add(Calendar.DAY_OF_MONTH, Integer.parseInt(valueToOperate.replace("D", "")));
				}
				else if (valueToOperate.contains("Y")) {
					date.add(Calendar.YEAR, Integer.parseInt(valueToOperate.replace("Y", "")));
				}
				
			}
			else if (operation.equalsIgnoreCase("subtract")) {
				if (valueToOperate.contains("M")) {
					date.add(Calendar.MONTH, -Integer.parseInt(valueToOperate.replace("M", "")));
				}
				else if (valueToOperate.contains("D")) {
					date.add(Calendar.DAY_OF_MONTH, -Integer.parseInt(valueToOperate.replace("D", "")));
				}
				else if (valueToOperate.contains("Y")) {
					date.add(Calendar.YEAR, -Integer.parseInt(valueToOperate.replace("Y", "")));
				}
			}
			session().set(variableToOperate, formatter.format(date.getTime()));
		} catch (ParseException e) {
			e.printStackTrace();
		}

	}

	@When("I {string} the value {long} to the variable {string}")
	public void iTheValueToTheVariable(String operation, long valueToOperate, String variableToOperate) {
		variableToOperate = variableToOperate.replace("$", "").replace("{", "").replace("}", "");
		String variable = Serenity.getCurrentSession().get(variableToOperate).toString();
		long result = Long.parseLong(variable);
		if (operation.equalsIgnoreCase("add")) {
			result += valueToOperate;
			
		}
		else if (operation.equalsIgnoreCase("subtract")) {
			result -= valueToOperate;
		}
		session().set(variableToOperate, result);
	}

	@When("I {string} the value {float} to the variable {string}")
	public void iTheValueToTheVariable(String operation, float valueToOperate, String variableToOperate) {
		variableToOperate = variableToOperate.replace("$", "").replace("{", "").replace("}", "");
		String variable = Serenity.getCurrentSession().get(variableToOperate).toString();
		float result = Float.parseFloat(variable);
		if (operation.equalsIgnoreCase("add")) {
			result += valueToOperate;
			
		}
		else if (operation.equalsIgnoreCase("subtract")) {
			result -= valueToOperate;
		}
		session().set(variableToOperate, result);
	}

	@When("I {string} the value {double} to the variable {string}")
	public void iTheValueToTheVariable(String operation, double valueToOperate, String variableToOperate) {
		variableToOperate = variableToOperate.replace("$", "").replace("{", "").replace("}", "");
		String variable = Serenity.getCurrentSession().get(variableToOperate).toString();
		double result = Double.parseDouble(variable);
		if (operation.equalsIgnoreCase("add")) {
			result += valueToOperate;
			
		}
		else if (operation.equalsIgnoreCase("subtract")) {
			result -= valueToOperate;
		}
		session().set(variableToOperate, result);
	}

	@When("I remove the field {string}")
	public void iRemoveTheField(String field) {
		ObjectMapper objMapper = new ObjectMapper();
		try {
			JsonNode nodeToRemove = objMapper.readTree(Serenity.getCurrentSession().get("objectData").toString());
			JsonPointer pointerToNode = convertJsonPathToJsonPointer(field);
			JsonPointer pointerToParent = pointerToNode.head();
			nodeToRemove = nodeToRemove.at(pointerToParent);
			((ObjectNode) (nodeToRemove)).remove(pointerToNode.last().toString().substring(1));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@When("I store a random word with length {int} and name {string}")
	public void iStoreARandomWordWithLengthAndName(int length, String name) {
		session().set(name, generateRandomWord(length, "", false));
	}

	@Then("Result should be {string}")
	public void resultShouldBe(String statusCode) {
		if (statusCode.equals("400")) {
			session().set("errorMessages", getResponse().body().asString());
		}
		Response resp = getResponse();
		resp.then().statusCode(Integer.parseInt(statusCode));
	}

	@Then("The response fields should be same of the request")
	public void theResponseFieldsShouldBeSameOfTheRequest() {
		if (getResponse().statusCode() == 200){
			if (Serenity.getCurrentSession().containsKey("customerData")) {
				String customerData = (String) Serenity.getCurrentSession().get("customerData");
				ObjectMapper objectMapper = new ObjectMapper();
				try {
					JsonNode jsonData = objectMapper.readTree(customerData);
					if (jsonData.has("body")) {
						assertAllTheJSONFields("", jsonData);
					}
					else {
						assertAllTheJSONFields("body", jsonData);
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
			else if (Serenity.getCurrentSession().containsKey("objectData")){
				String objectData = (String)Serenity.getCurrentSession().get("objectData");
				ObjectMapper objectMapper = new ObjectMapper();	
				try {
					JsonNode jsonData = objectMapper.readTree(objectData);
					if (jsonData.has("body")) {
						assertAllTheJSONFields("", jsonData);
					}
					else {
						assertAllTheJSONFields("body", jsonData);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}

	@Then("The number of elements returned is {string}") 
	public void theNumberOfElementsReturnedIs(String numberElements){
		if (getResponse().statusCode() == 200)
			assertJsonNode(getResponse(), "header.total_size", numberElements, true);
	}

	@Then("The number of elements returned is {long}") 
	public void theNumberOfElementsReturnedIs(long numberElements){
		if (getResponse().statusCode() == 200)
			assertJsonNode(getResponse(), "header.total_size", numberElements, true);
	}

	@Then("the result body should contain the field {string} with value {string}") 
	public void theResultBodyShouldContainTheFieldWithValue(String field, String value) {
		Object valueToAssert = StringSubstitutor.replace(value, session().asMap());
		if (value.length() > 0) {
			assertJsonNode(getResponse(), field, valueToAssert, true);
		}
		
	}

	@Then("the result body shouldn't contain the field {string} with value {string}")
	public void theResultBodyShouldntContainTheFieldWithValue(String field, String value) {
		Object valueToAssert = StringSubstitutor.replace(value, session().asMap());
		if (value.length() > 0) {
			assertJsonNode(getResponse(), field, valueToAssert, false);
		}
	}

	@Then("Any result body should contain the field {string} with value {string}")
	public void anyResultBodyShouldContainTheFieldWithValue(String field, String value) {
		Object valueToAssert = StringSubstitutor.replace(value, session().asMap());
		if (value.length() > 0) {
			assertListContains(getResponse(), field, valueToAssert);
		}
	}

	@Then("The {string} in field {string} is valid")
	public void theInFieldIsValid(String value, String field) {
		if (getResponse().statusCode() == 200) {
			if (value.equalsIgnoreCase("IBAN")) {
				JsonPath respJson = getResponse().jsonPath();
				String iban = respJson.getString(field);
				iban = iban.replace(" ", "").replace("[", "").replace("]", "");
				assertIBAN(iban);	
			}
		}
	}

	@Then("the result body should contain the field {string}")
	public void theResultBodyShouldContainTheField(String field) {
		assertExistsJsonNode(getResponse(), field, true);
	}

	@Then("the result body shouldn't contain the field {string}")
	public void theResultBodyShouldntContainTheField(String field) {
		assertExistsJsonNode(getResponse(), field, false);
	}

	@Then("the result body should contain the field {string} with format {string}")
	public void theResultBodyShouldContainTheFieldWithFormat(String field, String format) {
		assertFormatJsonNode(getResponse(), field, format, true);
	}
	
	@Then("the result body shouldn't contain the field {string} with format {string}")
	public void theResultBodyShouldntContainTheFieldWithFormat(String field, String format) {
		assertFormatJsonNode(getResponse(), field, format, false);
	}
	
}
