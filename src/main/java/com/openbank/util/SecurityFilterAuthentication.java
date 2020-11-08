package com.openbank.util;

import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.net.URL;

import com.google.gson.Gson;
import com.openbank.EndPoint;
import com.openbank.api.APIResource;
import com.openbank.stepdefinitions.MasterStepDefinitions;

import org.bson.Document;
import org.json.simple.parser.ParseException;
import org.junit.Assert;

import io.restassured.response.Response;

public class SecurityFilterAuthentication extends MasterStepDefinitions {
	
	Response resp = null;
	Response respInitiate = null;
	Document payloadSSL = null;

	APIResource apiResource = new APIResource();

	public Response createAccessToke(Document payload) {
		System.out.println("Body is ::" +withDynamicProps(payload.toJson()));
		return given().headers("Content-Type", "application/json").body(withDynamicProps(payload.toJson())).when()
				.post(EndPoint.LOGINACCESSTOKEN.getUrl());
	}

	public Response validateAccessToke(Document payload) {
//		System.out.println("Body is ::" +withDynamicProps(payload.toJson()));
		return given().headers("Content-Type", "application/json").headers("accessToken", "EUeptFnkZ3gcBUdpcHjAk53JKoHY0tNoJcZgTin6gd0=").body(withDynamicProps(payload.toJson())).when()
				.post(EndPoint.LOGINVALIDATETOKEN.getUrl());
	}

	public Response createPasswordPosition(Document payload, String accessToken) {
		System.out.println("Body is ::" + withDynamicProps(payload.toJson()));
		return given().headers("Content-Type", "application/json").headers("accessToken", accessToken)
				.body(withDynamicProps(payload.toJson())).when().post(EndPoint.LOGINPASSWORDPOSITION.getUrl());
	}

	public Response createAcessTokenForLogin(String payload, String accessToken) {
		System.out.println("Body is ::" +payload);
		System.out.println(EndPoint.LOGINACCESSTOKEN.getUrl());
		return given().headers("Content-Type", "application/json").headers("accessToken", accessToken).body(payload)
				.when().post(EndPoint.LOGINACCESSTOKEN.getUrl());
	}

	public Response initiateChallenge(Object payload, String accessToken, String urlInitiate) throws ParseException, IOException {
		
		try {
			Gson gson=new Gson();
			String payloadJson = null;
			if(!(payload.toString().contains("body"))) {
				payloadJson = gson.toJson(payload);
			}else {
				payloadJson=(String) payload;
			}
			System.out.println("URL Initiate is  ::" +urlInitiate);
			System.out.println("Body is ::" +apiResource.asRequestSSLNoBody(urlInitiate));
			System.out.println("Body---->" +withDynamicProps(payloadJson));
			System.out.println("payload" +withDynamicProps(payload.toString()).contains("requestBody"));
			System.out.println("Post Body When Payload is Document ::" +apiResource.asRequestSSL(withDynamicProps(payloadJson), urlInitiate));
			if(payload.toString().contains("operationCode")) {
				return given().headers("Content-Type", "application/json").headers("accessToken", accessToken).body(apiResource.asRequestSSLNoBody(urlInitiate)).when().post(EndPoint.INITIATECHALLANGE.getUrl());
			}else if(payload.toString().contains("body")) {
				System.out.println("Post Body When Payload  is String ::" +apiResource.asRequestSSLString(withDynamicProps(payloadJson), urlInitiate));
				return given().headers("Content-Type", "application/json").headers("accessToken", accessToken).body(apiResource.asRequestSSLString(withDynamicProps(payloadJson),urlInitiate)).when().post(EndPoint.INITIATECHALLANGE.getUrl());	
			}else {
				return given().headers("Content-Type", "application/json").headers("accessToken", accessToken).body(apiResource.asRequestSSL(withDynamicProps(payloadJson),urlInitiate)).when().post(EndPoint.INITIATECHALLANGE.getUrl());
			}
		} catch (Exception e) {
			Assert.fail("Initiate Challange Failed...");
		}
		return resp;
	    
	}

	public void secFilter(String url, Object payload) {
		
		accessTokenCreation(resp, payloadSSL);
		accessTokenValidation(resp, payloadSSL);
		createPasswordPosition(resp, payloadSSL);
		accesTokenEndPoint(resp);
		initiateChallengeEndpoint(resp, payloadSSL, url, payload);
	}

	public void accessTokenCreation(Response resp, Document payloadCreateAccessToken) {

		try {
			payloadCreateAccessToken = getDocument("SSL.loginServiceAccessToken");
			resp = createAccessToke(payloadCreateAccessToken);
			System.out.println("Reponse is ::" + resp.prettyPrint());
			assertStatus200(resp);
			System.out.println("Token is ::" + resp.jsonPath().getString("accessToken"));
			session().set("AccessToken", resp.jsonPath().getString("accessToken"));
		} catch (Exception e) {
			Assert.fail("Access Token Creation Failed...");
		}
	}

	public void accessTokenValidation(Response resp, Document payloadValidateAccessToken) {

		try {
			payloadValidateAccessToken = getDocument("SSL.validateAccessToken");
			payloadValidateAccessToken.replace("accessToken", session().get("AccessToken"));
			resp = validateAccessToke(payloadValidateAccessToken);
			System.out.println("Reponse is ::" + resp.prettyPrint());
			assertStatus200(resp);
		} catch (Exception e) {
			Assert.fail("Access Token Validation Failed...");
		}
	}

	public void createPasswordPosition(Response resp, Document payload) {

		try {
			payload = getDocument("SSL.passwordPosition");
			resp = createPasswordPosition(payload, (String) session().get("AccessToken"));
			System.out.println("Reponse is ::" + resp.prettyPrint());
			assertStatus200(resp);
			String position = resp.jsonPath().getString("positions").toString();
			String[] str1 = position.substring(1, position.length() - 1).split(", ");
			String positions = "[\"" + str1[0] + "\",\"" + str1[1] + "\",\"" + str1[2] + "\",\"" + str1[3] + "\"]";
			session().set("Position", positions);
			System.out.println(session().get("Position"));
			String password = "" + str1[0] + "" + str1[1] + "" + str1[2] + "" + str1[3] + "";
			session().set("Password", password);
		} catch (Exception e) {
			Assert.fail("Password Position Creation Failed...");
		}

	}

	public void accesTokenEndPoint(Response resp) {

		try {
			String payload = "{\r\n" + "    \"deviceUUID\": \"string\",\r\n" + "    \"documentType\": \"passport\",\r\n"
					+ "    \"password\": \"" + session().get("Password") + "\",\r\n" + "    \"passwordPosition\": "
					+ session().get("Position") + ",\r\n" + "    \"userIP\": \"string\",\r\n"
					+ "    \"username\": \"openbankapi\",\r\n" + "    \"geolocation\": \"ES\"\r\n"
					+ "}";

			System.out.println(payload);
			resp = createAcessTokenForLogin(payload, (String) session().get("AccessToken"));
			System.out.println("Reponse is ::" + resp.prettyPrint());
			assertStatus200(resp);
			session().set("FinalAccessToken", resp.jsonPath().getString("accessToken"));
			System.out.println(session().get("FinalAccessToken"));
		} catch (Exception e) {
			Assert.fail("Final Aceess Token Generation Failed...");
		}
	}

	public void initiateChallengeEndpoint(Response resp, Document payloadSSL, String urlInitiate, Object payload) {

		try {
			URL urlPath = new URL (urlInitiate);
			if(payload==null) {
				payloadSSL = getDocument("SSL.initiateChallenge");
				payloadSSL.remove("requestBody");
				payloadSSL.replace("url", urlPath.getPath());
				resp = initiateChallenge(payloadSSL, (String) session().get("FinalAccessToken"), urlInitiate);
			}else {
				resp = initiateChallenge(payload, (String) session().get("FinalAccessToken"), urlInitiate);
			}
			assertStatus201(resp);
            System.out.println("Progress Is response is ::" +resp.prettyPrint());
			session().set("progressId", resp.jsonPath().getString("progressId"));
			System.out.println(session().get("progressId"));
		} catch (Exception e) {
			Assert.fail("Initiate CHallenge Failed...");
		}
	}
}
