package com.openbank.stepdefinitions;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.openbank.api.APIResource;
import com.openbank.util.DataSource;
import com.openbank.util.MergeFrom;
import com.openbank.util.RandomNumberGenerator;
import com.openbank.util.SessionStore;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import net.serenitybdd.core.Serenity;
import org.apache.commons.text.StringSubstitutor;
import org.bson.Document;
import org.hamcrest.Matchers;
import org.hamcrest.core.AnyOf;
import org.hamcrest.core.Is;
import org.hamcrest.text.MatchesPattern;
import org.json.simple.parser.ParseException;
import org.junit.Assert;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

public class MasterStepDefinitions {

	private class FormatWord {
		private String word;
		private int initWord, endWord;
		public FormatWord(String word, int initWord, int endWord){
			this.word = word;
			this.initWord = initWord;
			this.endWord = endWord;
		}
		public String getWord(){
			return this.word;
		}
		public int getInitWord() {
			return this.initWord;
		}
		public int getEndWord() {
			return this.endWord;
		}
	}

	protected final static Logger log = Logger.getLogger(MasterStepDefinitions.class.getName());
	public static String URLPagination = null;
	APIResource apiResourse = new APIResource();


	static {
		DataSource.sessionStore().importSession();
	}

	public Response getResponse() {
		return response;
	}

	public void setResponse(Response response) {
		if (response != null && response.statusCode() < 400) {
			try {
				JsonPath responseJson = response.jsonPath();
				Object accountId = responseJson.get("body.properties.balance.body.accountId");
				Object accountId2 = null;
				if (responseJson.get("body") instanceof List) {
					accountId2 = responseJson.get("body[0].accountId");
				}
				Object paymentId = responseJson.get("body.paymentSystemReference");
				Object customerId = responseJson.get("customerID");
				Object country = responseJson.get("country");
				if (accountId != null && !(accountId instanceof List)) {
					session().set("recordid", accountId);
				} 
				else if (accountId2 != null) {
					session().set("recordid", accountId2);
				}
				else if (response.jsonPath().get("header.id") != null){
					session().set("recordid", response.jsonPath().get("header.id"));
				}
				if (paymentId != null) {
					session().set("paymentReferenceId", paymentId);
				}
				if (customerId != null) {
					session().set("customerID", customerId);
				}
				if (country != null) {
					session().set("country", country);
				}

			} catch (io.restassured.path.json.exception.JsonPathException exception) {
			}
		}
		this.response = response;
	}

	private Response response;

	public void assertStatus200(Response resp) {
		resp.then().statusCode(200);
	}

	public void assertStatus201(Response resp) {
		resp.then().statusCode(201);
	}

	protected void assertStatus404(Response resp) {
		resp.then().statusCode(404);
	}

	protected void assertStatus405(Response resp) {
		resp.then().statusCode(405);
	}

	protected boolean assertMessage(String response, String message) {
		return response.contains(message);
	}

	public void assertHeaderStatusSuccess(Response resp) {
		resp.then().body("header.status", Matchers.equalTo("success"));
	}

	protected void assertId(Response resp, String expected) {
		resp.then().body("header.id", Matchers.equalTo(expected));
	}

	protected void assertIdFromList(Response resp, String customerId) {
		resp.then().body("body.customerId", Matchers.hasItem(customerId));
	}

	public void assertListContains(Response resp, String jsonPath, Object expected) {
		resp.then().body(jsonPath, Matchers.hasItem(expected));
	}

	protected void assertKeyContains(Response resp, String jsonPath, String expected) {
		String[] param = expected.split(",");
		for (String p : param)
			resp.then().body(jsonPath + p, Matchers.notNullValue());
	}

	public void assertEquals(Response resp, String jsonPath, Object expected) {
		resp.then().body(jsonPath, Matchers.equalTo(expected));
	}

	public void assertEqual(Response resp, String jsonPath, String expected) {
		resp.then().body(jsonPath, Matchers.equalTo(expected));
	}

	public void assertEqualsJson(Response actualResult, String respjsonPath, String exptdRequest, String reqJsonPath)
			throws ParseException {
		String strAttributeValue = "";
		try {
			strAttributeValue = JsonPath.given(exptdRequest).getString(reqJsonPath);
		} catch (Exception e) {
		}
		if (strAttributeValue != "") {
			String body = actualResult.jsonPath().getString(respjsonPath);
			Assert.assertEquals(strAttributeValue, body);
		}
	}

	public void assertEqualsJson(Response actualResult, String respjsonPath, Document exptdRequest, String reqJsonPath)
			throws ParseException {
		String strAttributeValue = "";
		try {
			String request = apiResourse.asRequest(withDynamicProps(exptdRequest.toJson()));
			System.out.println("request is  ::" + request);
			System.out.println("req " + JsonPath.given(request).getString(reqJsonPath));
			strAttributeValue = JsonPath.given(request).getString(reqJsonPath);
			log.info(strAttributeValue);
		} catch (Exception e) {
		}
		if (strAttributeValue != "") {
			String body = actualResult.jsonPath().getString(respjsonPath);
			Assert.assertEquals(strAttributeValue, body);
		}
	}

	protected void assertExternalId(Response actualResult, String jsonPath, String exptdRequest, String customerID) {
		try {
			customerID = "CUS" + customerID;
			String strAttributeValue = "";
			try {
				strAttributeValue = actualResult.path("body.customerTypeTCode").toString();
				log.info(strAttributeValue);
			} catch (Exception e) {
			}
			if (strAttributeValue.equals("ACTIVE")) {
				actualResult.then().body(jsonPath, Matchers.equalTo(customerID));
			}
		} catch (Exception e) {
			Assert.fail("External Id assertion Failed...");
		}

	}

	protected void validateCNAECNOCode(Response actualResult, String exptdRequest, String reqJsonPath) {
		try {
			String strActivityCode = JsonPath.given(exptdRequest).getString(reqJsonPath);
			if (strActivityCode.equals("01")) {
				assertEqualsJson(actualResult, "body.cnaeTCode", exptdRequest, "cnaeTCode");
			} else if (strActivityCode.equals("02")) {
				assertEqualsJson(actualResult, "body.cnoTCode", exptdRequest, "cnoTCode");
			} else if (strActivityCode.equals("03")) {
				assertEqualsJson(actualResult, "body.cnaeTCode", exptdRequest, "cnaeTCode");
				assertEqualsJson(actualResult, "body.cnoTCode", exptdRequest, "cnoTCode");
			}
		} catch (ParseException e) {
			Assert.fail("Validate CNAECNO Code failed...");
		}
	}

	protected void validateCustomerInformation(Response actualResult, String exptdRequest, String customerId) {
		try {
			assertEqualsJson(actualResult, "body.countryOfBirthTCode", exptdRequest, "countryOfBirthTCode");
			assertEqualsJson(actualResult, "body.provinceOfBirthTCode", exptdRequest, "provinceOfBirthTCode");
			assertEqualsJson(actualResult, "body.relationDetails[0].relationTCode", exptdRequest,
					"relationDetails[0].relationTCode");
			assertEqualsJson(actualResult, "body.email", exptdRequest, "email");
			assertEqualsJson(actualResult, "body.activityTypeTCode", exptdRequest, "activityTypeTCode");
			assertEqualsJson(actualResult, "body.addressDetails[0].streetLine1", exptdRequest,
					"addressDetails[0].streetLine1");
			assertEqualsJson(actualResult, "body.addressDetails[0].streetLine2", exptdRequest,
					"addressDetails[0].streetLine2");
			assertEqualsJson(actualResult, "body.addressDetails[0].provinceTCode", exptdRequest,
					"addressDetails[0].provinceTCode");
			assertEqualsJson(actualResult, "body.addressDetails[0].countryTCode", exptdRequest,
					"addressDetails[0].countryTCode");
			assertEqualsJson(actualResult, "body.addressDetails[0].city", exptdRequest, "addressDetails[0].city");
			assertEqualsJson(actualResult, "body.addressDetails[0].postCode", exptdRequest,
					"addressDetails[0].postCode");
			assertEqualsJson(actualResult, "body.addressDetails[0].addressTypeTCode", exptdRequest,
					"addressDetails[0].addressTypeTCode");
			assertEqualsJson(actualResult, "body.telephones[0].countryCodeTCode", exptdRequest,
					"telephones[0].countryCodeTCode");
			assertEqualsJson(actualResult, "body.telephones[0].telephoneNumber", exptdRequest,
					"telephones[0].telephoneNumber");
			assertEqualsJson(actualResult, "body.telephones[0].typeOfLineTCode", exptdRequest,
					"telephones[0].typeOfLineTCode");
			assertEqualsJson(actualResult, "body.telephones[0].startTime", exptdRequest, "telephones[0].startTime");
			assertEqualsJson(actualResult, "body.telephones[0].endTime", exptdRequest, "telephones[0].endTime");
			validateCNAECNOCode(actualResult, exptdRequest, "activityTypeTCode");
			assertExternalId(actualResult, "body.externalUserId", exptdRequest, customerId);
		} catch (ParseException e) {
			Assert.fail("Validate Customer Information Failed...");
		}
		// try {
		// assertEqualsJson(actualResult, "body.countryOfBirthTCode",
		// exptdRequest, "countryOfBirthTCode");
		// assertEqualsJson(actualResult, "body.provinceOfBirthTCode",
		// exptdRequest, "provinceOfBirthTCode");
		// JSONObject jsonReq = new JSONObject(exptdRequest);
		// JSONArray jArray;
		//
		// if (!jsonReq.isNull("relationDetails")) {
		// jArray = jsonReq.getJSONArray("relationDetails");
		// for (int i = 0; i < jArray.length(); i++) {
		// assertEqualsJson(actualResult, "body.relationDetails[" + i +
		// "].relationTCode", exptdRequest,
		// "relationDetails[" + i + "].relationTCode");
		// // assertEqualsJson(actualResult,
		// "body.relationDetails["+i+"].customerId",
		// // exptdRequest,
		// // "relationDetails["+i+"].customerId");
		// }
		// }
		//
		// assertEqualsJson(actualResult, "body.email", exptdRequest, "email");
		// assertEqualsJson(actualResult, "body.activityTypeTCode",
		// exptdRequest, "activityTypeTCode");
		//
		// if (!jsonReq.isNull("addressDetails")) {
		// jArray = jsonReq.getJSONArray("addressDetails");
		// for (int i = 0; i < jArray.length(); i++) {
		// assertEqualsJson(actualResult, "body.addressDetails[" + i +
		// "].streetLine1", exptdRequest,
		// "addressDetails[" + i + "].streetLine1");
		// assertEqualsJson(actualResult, "body.addressDetails[" + i +
		// "].streetLine2", exptdRequest,
		// "addressDetails[" + i + "].streetLine2");
		// assertEqualsJson(actualResult, "body.addressDetails[" + i +
		// "].provinceTCode", exptdRequest,
		// "addressDetails[" + i + "].provinceTCode");
		// assertEqualsJson(actualResult, "body.addressDetails[" + i +
		// "].countryTCode", exptdRequest,
		// "addressDetails[" + i + "].countryTCode");
		// assertEqualsJson(actualResult, "body.addressDetails[" + i + "].city",
		// exptdRequest,
		// "addressDetails[" + i + "].city");
		// assertEqualsJson(actualResult, "body.addressDetails[" + i +
		// "].postCode", exptdRequest,
		// "addressDetails[" + i + "].postCode");
		// assertEqualsJson(actualResult, "body.addressDetails[" + i +
		// "].addressTypeTCode", exptdRequest,
		// "addressDetails[" + i + "].addressTypeTCode");
		// }
		// }
		//
		// if (!jsonReq.isNull("telephones")) {
		// jArray = jsonReq.getJSONArray("telephones");
		// for (int i = 0; i < jArray.length(); i++) {
		// assertEqualsJson(actualResult, "body.telephones[" + i +
		// "].countryCodeTCode", exptdRequest,
		// "telephones[" + i + "].countryCodeTCode");
		// assertEqualsJson(actualResult, "body.telephones[" + i +
		// "].telephoneNumber", exptdRequest,
		// "telephones[" + i + "].telephoneNumber");
		// assertEqualsJson(actualResult, "body.telephones[" + i +
		// "].typeOfLineTCode", exptdRequest,
		// "telephones[" + i + "].typeOfLineTCode");
		// assertEqualsJson(actualResult, "body.telephones[" + i +
		// "].startTime", exptdRequest,
		// "telephones[" + i + "].startTime");
		// assertEqualsJson(actualResult, "body.telephones[" + i + "].endTime",
		// exptdRequest,
		// "telephones[" + i + "].endTime");
		// }
		// }
		//
		// validateCNAECNOCode(actualResult, exptdRequest, "activityTypeTCode");
		// assertExternalId(actualResult, "body.externalUserId", exptdRequest,
		// customerId);
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

	}

	/**
	 * This method is use to generate PASSPORT/REDCARD document number based on the
	 * logic provided by functional team
	 **/
	protected String getLegalDocID(String legalDocument) {
		return RandomNumberGenerator.random7DigitGenerator();
	}

	/**
	 * This method is use to generate NIE document number based on the logic
	 * provided by functional team
	 * 
	 * @throws IOException
	 **/
	protected String getNIE() throws IOException {
		return generateNIE();
	}

	/**
	 * This method is use to generate DNI document number based on the logic
	 * provided by functional team
	 **/
	protected String getDNI() throws IOException {
		return generateDNI();
	}

	/**
	 * This method is use to generate NIF document number based on the logic
	 * provided by functional team
	 **/
	protected String getKNIF() throws IOException {
		return generateKNIF();
	}

	protected void assertJsonNode(Response resp, String node, Object value, boolean isValue) {
		if (isValue) {
			resp.then().body(node, Matchers.hasToString(value.toString()));
		}
		else {
			resp.then().body(node, Matchers.not(Matchers.hasToString(value.toString())));
		}
	}

	protected void assertExistsJsonNode(Response resp, String node, boolean shouldExist) {
		if (shouldExist) {
			resp.then().body(node, Matchers.notNullValue());
		}
		else {
			resp.then().body(node, Matchers.nullValue());
		}
	}

	protected void assertExistsJsonNode(Object node) {
		Assert.assertThat(node, Matchers.notNullValue());
	}

	protected void assertFormatJsonNode(Response resp, String node, String format, boolean shouldExist){
		int initWord = -1, totalSize = format.length();
		String formatWord = "";
		List<FormatWord> formatWords = new ArrayList<>();
		for (int i = 0; i < totalSize; i++){
			if (initWord == -1 && format.charAt(i) != '?'){
				initWord = i;
				formatWord += format.charAt(i);
			}
			else if (initWord > -1 && format.charAt(i) != '?'){
				formatWord += format.charAt(i);
			}
			else if (format.charAt(i) == '?' && !formatWord.isEmpty()){
				formatWords.add(new FormatWord(formatWord, initWord, initWord + formatWord.length() - 1));
				formatWord = "";
				initWord = -1;
			}
		}
		String pattern = "";
		FormatWord lastFormatWord = null;
		for (FormatWord word : formatWords) {
			if (pattern.isEmpty()) {
				pattern += "^.{" + Integer.toString(word.getInitWord()) + "}(" + word.getWord() + "){1}";
			}
			else {
				pattern += ".{" 
				+ Integer.toString(word.getInitWord() - lastFormatWord.getEndWord() - 1)
				+ "}("
				+ word.getWord()
				+ "){1}";
			}
			lastFormatWord = word;
		}

		pattern += ".{"  
		+ Integer.toString((format.length() - 1) - lastFormatWord.getEndWord())
		+ "}$";

		if (shouldExist) {
			resp.then().body(node, MatchesPattern.matchesPattern(pattern));
		}
		else {
			resp.then().body(node, Matchers.not(MatchesPattern.matchesPattern(pattern)));
		}
	}

	protected void assertGeneric(Response resp) {
		assertStatus200(resp);
		assertHeaderStatusSuccess(resp);
	}

	public Document getDocument(String spec) {
		return DataSource.getDocument(spec);
	}

	protected String withDynamicProps(String template) {
		return MergeFrom.template(template).withVarsFrom(DataSource.randomVarsMap())
				.withFieldsFrom(DataSource.sessionStore().asMap());
	}

	public SessionStore session() {
		return DataSource.sessionStore();
	}

	public String findLegalDocument(String legalDocument) throws IOException {
		switch (legalDocument) {
		case "PASSPORT":
			case "REDCARD":
				legalDocument = getLegalDocID(legalDocument);
			break;
			case "DNI":
			legalDocument = getDNI();
			break;
		case "NIE":
			legalDocument = getNIE();
			break;
		case "NIF":
			legalDocument = getKNIF();
			break;
		}
		return legalDocument;

	}

	/**
	 * This method is use to get the data id of mongo colections to get the
	 * appropriate data
	 **/
	public String findInputAccountJsonRequest(String accType, String ibanType, String holder) throws IOException {
		// String dataSpec = datasource.propertyRead(activityType);
		TreeMap<String, String> mappers = new TreeMap<String, String>();
		mappers = DataSource.readMongoProperties("properties.testData_Properties");
		accType = mappers.get(accType);
		holder = mappers.get(holder);
		ibanType = mappers.get(ibanType);
		String JSON = "account." + "AC_holder" + holder + "_" + ibanType;
		return JSON;
	}

	private String generateNIE() throws IOException {

		String NIENumber = RandomNumberGenerator.random7DigitGenerator();
		// input from sheet */ String checkDigit = "Z";
		String i = "0";
		/* Generate CheckDigit in Loop for Random */

		int NIEUpdatedNo = (Integer.parseInt(NIENumber));
		String NIEextn = i + NIEUpdatedNo;

		int Rem = Integer.parseInt(NIEextn) % 23;
		String expectedvalue = null;

		/*---------------------------------------------------------------*/
		switch (Rem) {
		case 0:
			expectedvalue = "T";
			break;
		case 1:
			expectedvalue = "R";
			break;
		case 2:
			expectedvalue = "W";
			break;
		case 3:
			expectedvalue = "A";
			break;
		case 4:
			expectedvalue = "G";
			break;
		case 5:
			expectedvalue = "M";
			break;
		case 6:
			expectedvalue = "Y";
			break;
		case 7:
			expectedvalue = "F";
			break;
		case 8:
			expectedvalue = "P";
			break;
		case 9:
			expectedvalue = "D";
			break;
		case 10:
			expectedvalue = "X";
			break;
		case 11:
			expectedvalue = "B";
			break;
		case 12:
			expectedvalue = "N";
			break;
		case 13:
			expectedvalue = "J";
			break;
		case 14:
			expectedvalue = "Z";
			break;
		case 15:
			expectedvalue = "S";
			break;
		case 16:
			expectedvalue = "Q";
			break;
		case 17:
			expectedvalue = "V";
			break;
		case 18:
			expectedvalue = "H";
			break;
		case 19:
			expectedvalue = "L";
			break;
		case 20:
			expectedvalue = "C";
			break;
		case 21:
			expectedvalue = "K";
			break;
		case 22:
			expectedvalue = "E";
			break;
		}
		String updatedNIE = "X" + NIEUpdatedNo + expectedvalue;
		return updatedNIE;
	}

	private String generateDNI() throws IOException {
		int IntDNI = RandomNumberGenerator.random8DigitGenerator();
		int Rem = IntDNI % 23;
		/* This is to write the updated 7 digit number to the data sheet */

		String expectedvalue = null;
		switch (Rem) {
		case 0:
			expectedvalue = "T";
			break;
		case 1:
			expectedvalue = "R";
			break;
		case 2:
			expectedvalue = "W";
			break;
		case 3:
			expectedvalue = "A";
			break;
		case 4:
			expectedvalue = "G";
			break;
		case 5:
			expectedvalue = "M";
			break;
		case 6:
			expectedvalue = "Y";
			break;
		case 7:
			expectedvalue = "F";
			break;
		case 8:
			expectedvalue = "P";
			break;
		case 9:
			expectedvalue = "D";
			break;
		case 10:
			expectedvalue = "X";
			break;
		case 11:
			expectedvalue = "B";
			break;
		case 12:
			expectedvalue = "N";
			break;
		case 13:
			expectedvalue = "J";
			break;
		case 14:
			expectedvalue = "Z";
			break;
		case 15:
			expectedvalue = "S";
			break;
		case 16:
			expectedvalue = "Q";
			break;
		case 17:
			expectedvalue = "V";
			break;
		case 18:
			expectedvalue = "H";
			break;
		case 19:
			expectedvalue = "L";
			break;
		case 20:
			expectedvalue = "C";
			break;
		case 21:
			expectedvalue = "K";
			break;
		case 22:
			expectedvalue = "E";
			break;
		}
		String strUpdatedDNI = IntDNI + expectedvalue;
		return strUpdatedDNI;
	}

	public String amendLegalDocumentValue(String request, String legalDocument) throws Exception {
		String newrequest = request.replace("legalDocIDchange", legalDocument);
		return newrequest;
	}

	protected String getDate(int days) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
		LocalDate date = LocalDate.now().minusDays(days);
		return (date.format(formatter).toString());
	}

	private String generateKNIF() throws IOException {
		String updatedKNIF = RandomNumberGenerator.random7DigitGenerator();

		String[] Numbers = updatedKNIF.split("(?!^)");
		int addEven = 0, addOdd = 0, remaining = 0;
		for (int i = 1; i < Numbers.length; i++) {
			Integer evenNumber = Integer.valueOf(Numbers[i]);
			addEven = addEven + evenNumber;
			i = i + 1;
		}
		int[] n = new int[4];
		for (int i = 0; i < Numbers.length; i++) {
			Integer oddNumber = Integer.valueOf(Numbers[i]) * 2;
			n[addOdd] = Integer.valueOf(oddNumber);
			i = i + 1;
			addOdd = addOdd + 1;
		}
		String[] oddNumber1 = String.valueOf(n[0]).split("(?!^)");
		int oddNumberfirst = oddNumber1.length;
		String[] oddNumber2 = String.valueOf(n[1]).split("(?!^)");
		int oddNumbersecond = oddNumber2.length;
		String[] oddNumber3 = String.valueOf(n[2]).split("(?!^)");
		int oddNumberthird = oddNumber3.length;
		String[] oddNumber4 = String.valueOf(n[3]).split("(?!^)");
		int oddNumberfourth = oddNumber4.length;
		int firstvalue = 0, secondvalue = 0, thirdvalue = 0, fourthvalue = 0;
		for (int i = 0; i < oddNumberfirst; i++) {
			firstvalue = firstvalue + Integer.valueOf(oddNumber1[i]);
		}
		for (int i = 0; i < oddNumbersecond; i++) {
			secondvalue = secondvalue + Integer.valueOf(oddNumber2[i]);
		}
		for (int i = 0; i < oddNumberthird; i++) {
			thirdvalue = thirdvalue + Integer.valueOf(oddNumber3[i]);
		}
		for (int i = 0; i < oddNumberfourth; i++) {
			fourthvalue = fourthvalue + Integer.valueOf(oddNumber4[i]);
		}
		int totalodd = firstvalue + secondvalue + thirdvalue + fourthvalue;
		int totalvalue = addEven + totalodd;
		String[] splittotal = String.valueOf(totalvalue).split("(?!^)");
		int size = splittotal.length;
		if (splittotal[size - 1].equals("0")) {
			remaining = 0;
		} else {
			remaining = 10 - Integer.valueOf(splittotal[size - 1]);
		}
		System.out.println(remaining);
		String expectedvalue = null;
		switch (remaining) {
		case 0:
			expectedvalue = "J";
			break;
		case 1:
			expectedvalue = "A";
			break;
		case 2:
			expectedvalue = "B";
			break;
		case 3:
			expectedvalue = "C";
			break;
		case 4:
			expectedvalue = "D";
			break;
		case 5:
			expectedvalue = "E";
			break;
		case 6:
			expectedvalue = "F";
			break;
		case 7:
			expectedvalue = "G";
			break;
		case 8:
			expectedvalue = "H";
			break;
		case 9:
			expectedvalue = "I";
			break;
		}
		updatedKNIF = "K" + updatedKNIF + expectedvalue;
		return updatedKNIF;
	}

	protected List<Document> getLegalDocument() throws IOException {
		String dataSpecName = DataSource.propertyReadData("legalDocumentsPayload");
		Document payloadLegalDocuments = getDocument(dataSpecName);
		String documentID = "PP" + new Random().nextInt(25000);
		session().set("documentID", documentID);
		session().set("documentName", "PASSPORT");
		payloadLegalDocuments.append("legalDocId", documentID).append("legalDocTypeTCode", "PASSPORT")
				.append("issueDate", "201407" + new Random().nextInt(31));
		List<Document> owners_Documents = new ArrayList<>();
		owners_Documents.add(payloadLegalDocuments);
		return owners_Documents;
	}

	protected boolean assertIdContains(String resp, String customerId) {
		return resp.contains(customerId);
	}

	// Creates Deposit for Given depositType and participantType
	protected String findDepositPayload(String depositType, String participantType) {
		String depositPayload = null;
		if ((depositType.equals("Open Bank Deposit") || depositType.equals("Traditional_Term_deposit"))
				&& (participantType.equals("single")))
			depositPayload = "OpenBankDepositSingle";
		else if ((depositType.equals("New Money Deposit") || depositType.equals("Early_redemption"))
				&& (participantType.equals("single")))
			depositPayload = "NewMoneyDepositSingle";
		else if ((depositType.equals("Open Bank Deposit") || depositType.equals("Traditional_Term_deposit"))
				&& participantType.equals("NA"))
			depositPayload = "NewMoneyDepositNA";
		else if ((depositType.equals("New Money Deposit") || depositType.equals("Early_redemption"))
				&& participantType.equals("NA"))
			depositPayload = "OpenBankDepositNA";
		else if ((depositType.equals("Open Bank Deposit") || depositType.equals("Traditional_Term_deposit"))
				&& participantType.equals("multiple"))
			depositPayload = "OpenBankDepositMultiple";
		else if ((depositType.equals("New Money Deposit") || depositType.equals("Early_redemption"))
				&& participantType.equals("multiple"))
			depositPayload = "NewMoneyDepositMultiple";
		System.out.println("depositPayload >>> " + depositPayload);
		return depositPayload;
	}

	// This method is use for the get the product name
	protected String getProductName(String newDepositType) {
		String productName = null;
		if (newDepositType.equals("Open Bank Deposit")) {
			productName = "ES.OB.13M.DEPOSIT";
		} else if (newDepositType.equals("New Money Deposit")) {
			productName = "ES.OB.DEPOSIT.NM.13M";
		} else if (newDepositType.equals("Early_redemption")) {
			productName = "AR.OB.EARLY.REDEM";
		} else if (newDepositType.equals("Traditional_Term_deposit")) {
			productName = "AR.OB.TRADITIONAL.FD";
		}
		return productName;

	}

	public void setSignotoriesCustId(String customerId) {
		session().set("signatory_custId", customerId);
	}

	protected String amendCustomerIdandAcct(String request, String customerId, String acctORiBAN) {
		String newrequest = null;

		if (acctORiBAN != null)
			newrequest = request.replace("customerIdChange", customerId).replace("acctAndiBANChange", acctORiBAN);
		else
			newrequest = request.replace("customerIdChange", customerId);
		return newrequest;
	}

	protected String amendCustomerId(String request, String customerId) {
		String newrequest = null;
		newrequest = request.replace("customerIdChange", customerId);
		return newrequest;
	}

	public String createJsonfeild(String fieldName, String value) {
		fieldName = "\"" + fieldName + "\"";
		value = "\"" + value + "\"";
		String fieldvalue = fieldName + ":" + value;
		String payload = "\"cardDetails\":[{" + fieldvalue + "}]";
		return "{" + payload + "}";
	}

	public String createJsonfeild(String fieldName1, String value1, String fieldName2, String value2) {
		fieldName1 = "\"" + fieldName1 + "\"";
		String fieldvalue1 = fieldName1 + ":" + value1;
		fieldName2 = "\"" + fieldName2 + "\"";
		String fieldvalue2 = fieldName2 + ":" + value2;
		String payload = "\"cardDetails\":[{" + fieldvalue1 + "," + fieldvalue2 + "}]";
		return "{" + payload + "}";
	}

	protected String amendCardIdandcmsAccountID(String request) {
		String amemdedRequest = null;
		String cardID = getcardID();
		String cmsAccountId = getcmsAccountID();
		System.out.println("cardID >> " + cardID);
		System.out.println("cmsAccountId >> " + cmsAccountId);
		amemdedRequest = request.replace("CardIdChange", cardID);
		amemdedRequest = amemdedRequest.replace("cmsAccountIdChange", cmsAccountId);
		System.out.println("Updated newrequest >> " + amemdedRequest);
		return amemdedRequest;
	}

	/**
	 * This method is use to generate PASSPORT/REDCARD document number based on the
	 * logic provided by functional team
	 **/
	protected String getcardID() {
		return RandomNumberGenerator.random19DigitGenerator();
	}

	/**
	 * This method is use to generate PASSPORT/REDCARD document number based on the
	 * logic provided by functional team
	 **/
	protected String getcmsAccountID() {
		return RandomNumberGenerator.random9DigitGenerator();
	}

	protected String amendPermissionSecrestriction(String request, String permission, String SECRestriction) {
		String amemdedRequest = null;
		String secStatusTCodeChange = null;
		String atmCashAllowedTBooleanChange = null;
		String inPersonPurchaseAllowedTBooleanChange = null;
		String onlinePurchaseAllowedTBooleanChange = null;

		if (SECRestriction.equalsIgnoreCase("WithSEC"))
			secStatusTCodeChange = "1";
		else if (SECRestriction.equalsIgnoreCase("WithoutSEC"))
			secStatusTCodeChange = "2";
		else if (SECRestriction.equalsIgnoreCase("WithOrWithoutSEC"))
			secStatusTCodeChange = "3";
		else if (SECRestriction.equalsIgnoreCase("Both"))
			secStatusTCodeChange = "4";

		String[] Permission = permission.split("_");

		for (int i = 0; i < Permission.length; i++) {
			switch (Permission[i]) {
			case "WithATM":
				atmCashAllowedTBooleanChange = "Y";
				break;
			case "WithPersonPurchase":
				inPersonPurchaseAllowedTBooleanChange = "Y";
				break;
			case "WithOnlinePurchase":
				onlinePurchaseAllowedTBooleanChange = "Y";
				break;
			case "WithoutATM":
				atmCashAllowedTBooleanChange = "N";
				break;
			case "WithoutPersonPurchase":
				inPersonPurchaseAllowedTBooleanChange = "N";
				break;
			case "WithoutOnlinePurchase":
				onlinePurchaseAllowedTBooleanChange = "N";
				break;

			}
		}

		amemdedRequest = request.replace("atmCashAllowedTBooleanChange", atmCashAllowedTBooleanChange);
		amemdedRequest = amemdedRequest.replace("inPersonPurchaseAllowedTBooleanChange",
				inPersonPurchaseAllowedTBooleanChange);
		amemdedRequest = amemdedRequest.replace("onlinePurchaseAllowedTBooleanChange",
				onlinePurchaseAllowedTBooleanChange);
		amemdedRequest = amemdedRequest.replace("secStatusTCodeChange", secStatusTCodeChange);

		System.out.println("Updated newrequest >> " + amemdedRequest);
		return amemdedRequest;
	}

	protected String amendCustomerIdandAccountId(String request, String customerId, String accountId) {
		String amemdedRequest = null;
		// System.out.println(" customerId >> " +customerId + "accountId >> "
		// +accountId);
		amemdedRequest = request.replace("CustomerIdChange", customerId);
		// System.out.println("newrequest >> " +amemdedRequest);
		amemdedRequest = amemdedRequest.replace("AccountIdChange", accountId);
		System.out.println("Updated newrequest >> " + amemdedRequest);
		return amemdedRequest;
	}

	protected String amendPaymentMethod(String request, String paymentMethod) {
		String amemdedRequest = null;
		String paymentMethodChange = null;

		if (paymentMethod.equalsIgnoreCase("FullBalance"))
			paymentMethodChange = "1";
		else if (paymentMethod.equalsIgnoreCase("Fixed"))
			paymentMethodChange = "2";
		else if (paymentMethod.equalsIgnoreCase("Percentage"))
			paymentMethodChange = "3";

		amemdedRequest = request.replace("PaymentMethodChange", paymentMethodChange);

		System.out.println("Updated newrequest >> " + amemdedRequest);
		return amemdedRequest;
	}

	/**
	 * This Method is to validate Pagination functionality.
	 */
	public void validatePagination() {
		int totalSize = 0;
		String urlPagination1 = null;
		try {

			log.info(">>>>>  Validate Pagination Started <<<<<");
			log.info(">>>> URL:- " + URLPagination);

			urlPagination1 = removePageStartAndSizeFromURL(URLPagination);
			log.info(">>>>URL to check Pagination :- " + urlPagination1);

			Response resp = hitURL(urlPagination1);
			assertStatus200(resp);

			// Validate page_start and page_size
			assertEquals(resp, "header.page_start", 1);
			log.info("Page start value is :- " + resp.jsonPath().getString("header.page_start"));
			assertEquals(resp, "header.page_size", 99);
			log.info("Page size value is :- " + resp.jsonPath().getString("header.page_size"));

			totalSize = Integer.parseInt(resp.jsonPath().getString("header.total_size").toString());
			log.info("Total size is :- " + totalSize);

			// Validate lastPage and no. of element on last page
			validateLastPagePagination(urlPagination1, totalSize);

			log.info(">>>>> Validated Pagination passed <<<<<");

		} catch (JsonSyntaxException jse) {
			jse.printStackTrace();
			Assert.fail("Testcase failed in Validating Pagination functionality...");
		} catch (JsonParseException jpe) {
			jpe.printStackTrace();
			Assert.fail("Testcase failed in Validating Pagination functionality...");
		} catch (ArithmeticException ae) {
			ae.printStackTrace();
			Assert.fail("Testcase failed in Validating Pagination functionality...");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Testcase failed in Validating Pagination functionality...");
		}

	}

	/**
	 * This method is used to hit the URL.
	 * 
	 * @param url
	 * @return
	 */
	private Response hitURL(String url) {
		return apiResourse.given().when().get(url);
	}

	/**
	 * This Method is used to remove parameters (PageStart and PageSize) from URL
	 * 
	 * @param URL
	 * @return
	 * @throws Exception
	 */
	private String removePageStartAndSizeFromURL(String URL) throws Exception {
		String newURL = null, base = null;
		String[] param = null;
		int count = 0;

		if (URL.contains("?")) {
			String[] urlArr = URL.split("\\?");
			base = urlArr[0];

			if (urlArr[1].contains("&")) {
				param = urlArr[1].split("&");
				String base1 = base.concat("?");
				for (int i = 0; i < param.length; i++) {
					if (!param[i].contains("page_size=") && !param[i].contains("page_start=")) {
						newURL = base1.concat(param[i]).concat("&");
					} else
						count++;
				}

				if (param.length == count)
					newURL = base1;

				if (newURL.charAt(newURL.length() - 1) == '&' || newURL.charAt(newURL.length() - 1) == '?')
					newURL = newURL.substring(0, newURL.length() - 1);
			} else {
				if (urlArr[1].contains("page_size="))
					newURL = base;
				else
					newURL = base.concat("?").concat(urlArr[1]);
			}
		} else
			newURL = URL;

		return newURL;
	}

	/**
	 * This Method is used to validate lastPage, no. of Elements on last Page and
	 * validate error on Max+1 page.
	 * 
	 * @param urlPagination1
	 * @param totalSize
	 * @throws JsonSyntaxException
	 * @throws JsonParseException
	 * @throws ArithmeticException
	 */
	private void validateLastPagePagination(String urlPagination1, int totalSize)
			throws JsonSyntaxException, JsonParseException, ArithmeticException {
		int maxPage = 0, lastPageNoOfEle = 0;
		String urlWithMaxPage1 = null, urlWithMaxPage2 = null;

		if (totalSize == 0)
			log.info("Total size of a response is : 0");
		else if (totalSize > 0 && totalSize <= 99) {
			maxPage = 1;
			lastPageNoOfEle = totalSize;
		} else if (totalSize > 99) {
			if (totalSize % 99 == 0) {
				maxPage = totalSize / 99;
				lastPageNoOfEle = 99;
			} else {
				maxPage = (totalSize / 99) + 1;
				lastPageNoOfEle = totalSize % 99;
			}
		} else
			log.info("Total size is of response is null or invalid");
		log.info("Max page : " + maxPage);
		log.info("Expected No. of Elements on last Page : " + lastPageNoOfEle);

		if (urlPagination1.contains("?")) {
			urlWithMaxPage1 = urlPagination1.concat("&page_start=").concat(String.valueOf(maxPage));
			urlWithMaxPage2 = urlPagination1.concat("&page_start=").concat(String.valueOf(maxPage + 1));
		} else {
			urlWithMaxPage1 = urlPagination1.concat("?page_start=").concat(String.valueOf(maxPage));
			urlWithMaxPage2 = urlPagination1.concat("?page_start=").concat(String.valueOf(maxPage + 1));
		}

		log.info("URL with Max Page :- " + urlWithMaxPage1);
		Response resp1 = hitURL(urlWithMaxPage1);
		assertStatus200(resp1);
		// System.out.println(resp1.prettyPrint());

		// Validate no. of Elements present on last Page.
		if (resp1.getBody().asString().equals("\"\"")) {
			log.info("Getting null response for this URL :- " + urlWithMaxPage1);
			Assert.fail("Testcase failed in validating Pagination...");
		} else {
			String data = resp1.jsonPath().get("body.$").toString();
			JsonParser jsonParser = new JsonParser();
			JsonArray jsonArray = (JsonArray) jsonParser.parse(data);
			log.info("Actual No. of Elements on last page : " + jsonArray.size());
			Assert.assertEquals(lastPageNoOfEle, jsonArray.size());
			log.info("No of Elements on last page is validated...");

			// Validate Error thrown when hit URL with parameter pageStart 'Max+1'
			Response resp2 = hitURL(urlWithMaxPage2);
			assertStatus404(resp2);
			Assert.assertTrue(resp2.jsonPath().get("error.message").toString()
					.contains("Records not available for the requested page"));
		}
	}

	protected String expandBody(String body) {
		try {
			String dni = findLegalDocument("DNI");
			return amendLegalDocumentValue(body, dni);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return body;
	}

	protected String expandUrl(String url) {
		Map<String,String> mapaSesiones = session().asMap();
		return StringSubstitutor.replace(url, mapaSesiones);
	}

	protected void assertAllTheJSONFields(String key, JsonNode node) {
		if (node.isValueNode()) {
			assertJsonNode(getResponse(), key, node.textValue(), true);
		} else {
			if (node.isArray()) {
				int i = 0;
				for (JsonNode value : node) {
					assertAllTheJSONFields(key + "[" + Integer.toString(i) + "]", value);
					i++;
				}
			} else {
				Iterator<Map.Entry<String, JsonNode>> iter = node.fields();
				while (iter.hasNext()) {
					Map.Entry<String, JsonNode> entry = iter.next();
					if (key.isEmpty()) {
						assertAllTheJSONFields(entry.getKey(), entry.getValue());
					} else {
						assertAllTheJSONFields(key + "." + entry.getKey(), entry.getValue());
					}
				}
			}
		}
	}

	/**
	 * This method is use to generate DNI document number for Argentina based on the
	 * logic provided by functional team
	 **/

	public String generateDNI_AR(String gender) throws IOException {
		String expectedvalue = gender;
		String strUpdatedDNI;
		if (!expectedvalue.isEmpty()) {
			strUpdatedDNI = expectedvalue + RandomNumberGenerator.random7DigitGenerator();
		}
		else {
			strUpdatedDNI = String.valueOf(RandomNumberGenerator.random8DigitGenerator());
		}
		session().set("DNIAR", strUpdatedDNI);
		return strUpdatedDNI;
	}

	public String amendDNIValue(String request, String DNINumber) throws Exception {
		String newrequest = request.replace("ChangeDNINumber", DNINumber);
		return newrequest;
	}

	public String generatelegalId_AR(String gender) throws IOException {
		String dniNumber;
		if (!Serenity.getCurrentSession().containsKey("DNIAR")) {
			dniNumber = String.valueOf(RandomNumberGenerator.random8DigitGenerator());
		}
		else {
			dniNumber = Serenity.getCurrentSession().get("DNIAR").toString();
			dniNumber = dniNumber.replace("F", "0").replace("M", "0");
		}
		if (gender.equals("M")){
			dniNumber = "20" + dniNumber;
		}
		if (gender.equals("F")) {
			dniNumber = "27" + dniNumber;
		}
		if (gender.isEmpty()) {
			dniNumber = "23" + dniNumber;
		}
		String multipliers = "5432765432";
		long calculo = 0;
		int digit = 0;
		for (char number : multipliers.toCharArray()) {
			calculo += Character.getNumericValue(number) * Character.getNumericValue(dniNumber.charAt(digit));
			digit++;
		}
		calculo = 11 - (calculo % 11);
		if (calculo == 11) {
			calculo = 0;
			return dniNumber + calculo;
		}
		else if (calculo == 10) {
			gender = "";
			return generatelegalId_AR(gender);
		}
		else {
			return dniNumber + calculo;
		}
	}

	public JsonPointer convertJsonPathToJsonPointer(String jsonPath) {
		JsonPointer pointer = JsonPointer.valueOf("/" + jsonPath.replace(".", "/").replace("[", "/").replace("]", ""));
		return pointer;
	}

	@SuppressWarnings("unchecked")
	protected void assertIBAN(String iban) {
		
		Assert.assertThat(iban.substring(0, 2), AnyOf.anyOf(Is.is("ES")));
		Assert.assertThat(iban.substring(4, 8), Is.is("0073"));
		Assert.assertThat(iban.substring(8, 12), AnyOf.anyOf(Is.is("0100")));
		int cd = Integer.parseInt(iban.substring(12, 14));

		// iban control code
		String ibancd = iban.substring(2, 4);

		// Algorithm for Check Digit 1
		int bankNumberCalculation = Integer.parseInt(iban.substring(4, 5)) * 4
				+ Integer.parseInt(iban.substring(5, 6)) * 8 + Integer.parseInt(iban.substring(6, 7)) * 5
				+ Integer.parseInt(iban.substring(7, 8)) * 10;
		int branchNumberCalculation = Integer.parseInt(iban.substring(8, 9)) * 9
				+ Integer.parseInt(iban.substring(9, 10)) * 7 + Integer.parseInt(iban.substring(10, 11)) * 3
				+ Integer.parseInt(iban.substring(11, 12)) * 6;
		int mod_1 = (bankNumberCalculation + branchNumberCalculation) % 11;
		int cd_1 = 11 - mod_1;
		if (cd_1 == 11) {
			cd_1 = 0;
		} 
		else if (cd_1 == 10) {
			cd_1 = 1;
		}

		// Algorithm for Check Digit 2

		// ccc

		String ccc = iban.substring(14, 24);

		int accountNumberCalculation = Integer.parseInt(ccc.substring(0, 1)) * 1
				+ Integer.parseInt(ccc.substring(1, 2)) * 2 + Integer.parseInt(ccc.substring(2, 3)) * 4
				+ Integer.parseInt(ccc.substring(3, 4)) * 8 + Integer.parseInt(ccc.substring(4, 5)) * 5
				+ Integer.parseInt(ccc.substring(5, 6)) * 10 + Integer.parseInt(ccc.substring(6, 7)) * 9
				+ Integer.parseInt(ccc.substring(7, 8)) * 7 + Integer.parseInt(ccc.substring(8, 9)) * 3
				+ Integer.parseInt(ccc.substring(9, 10)) * 6;
		int mod_2 = accountNumberCalculation % 11;
		int cd_2 = 11 - mod_2;
		if (cd_2 == 11) {
			cd_2 = 0;
		} 
		else if (cd_2 == 10) {
			cd_2 = 1;
		}
		String ibanWithoutIbanCheckDigit = iban.substring(4, 24);

		Map<String,String> mapCountryCode = new HashMap<>();
		mapCountryCode.put("A","10");
		mapCountryCode.put("B","11");
		mapCountryCode.put("C","12");
		mapCountryCode.put("D","13");
		mapCountryCode.put("E","14");
		mapCountryCode.put("F","15");
		mapCountryCode.put("G","16");
		mapCountryCode.put("H","17");
		mapCountryCode.put("I","18");
		mapCountryCode.put("J","19");
		mapCountryCode.put("K","20");
		mapCountryCode.put("L","21");
		mapCountryCode.put("M","22");
		mapCountryCode.put("N","23");
		mapCountryCode.put("O","24");
		mapCountryCode.put("P","25");
		mapCountryCode.put("Q","26");
		mapCountryCode.put("R","27");
		mapCountryCode.put("S","28");
		mapCountryCode.put("T","29");
		mapCountryCode.put("U","30");
		mapCountryCode.put("V","31");
		mapCountryCode.put("W","32");
		mapCountryCode.put("X","33");
		mapCountryCode.put("Y","34");
		mapCountryCode.put("Z","35");

		BigInteger ibanCheckDigit = new BigInteger("98").subtract(new BigInteger(ibanWithoutIbanCheckDigit + mapCountryCode.get(iban.substring(0,1)) + mapCountryCode.get(iban.substring(1,2)) + "00").mod(new BigInteger("97")));
		int ibanRest = new BigInteger(ibanWithoutIbanCheckDigit + mapCountryCode.get(iban.substring(0,1)) + mapCountryCode.get(iban.substring(1,2)) + ibancd).mod(new BigInteger("97")).intValue();

		String ibanCheckDigitString = ibanCheckDigit.toString();
		if (ibanCheckDigitString.length() == 1) {
			ibanCheckDigitString = "0" + ibanCheckDigitString;
		}
		
		Assert.assertThat(Integer.toString(cd), Is.is(Integer.toString(cd_1) + Integer.toString(cd_2)));
		Assert.assertThat(ibancd, Is.is(ibanCheckDigitString));
		Assert.assertThat(1, Is.is(ibanRest));
	}

	protected BigInteger generateRandomNumber(int length) {
		BigInteger valueInitial = new BigInteger("10").pow(length - 1);
		BigInteger valueEnd = new BigInteger("10").pow(length).subtract(new BigInteger("1"));
		BigInteger bigInteger = valueEnd.subtract(valueInitial);
		Random randNum = new Random();
		int len = valueEnd.bitLength();
		BigInteger res = new BigInteger(len, randNum);
		if (res.compareTo(valueInitial) < 0)
			res = res.add(valueInitial);
		if (res.compareTo(bigInteger) >= 0)
			res = res.mod(bigInteger).add(valueInitial);
		
		return res;
	}

	protected String generateRandomWord(int length, String exceptions, boolean alphanumeric) {
		String letters = "abcdefghijklmnñopqrstuvwxyz";
		letters = letters.replace(exceptions, "");
		Random random = new Random();
		char[] word = new char[length];
        for(int i = 0; i < word.length; i++)
        {
        	word[i] = letters.charAt(random.nextInt(letters.length()));
        	if (alphanumeric) {
        		String numbers = "0123456789";
        		if (random.nextInt(2) == 1) {
        			word[i] = numbers.charAt(random.nextInt(numbers.length()));
				}
			}
		}
		return new String(word);
	}


	protected String generatePassportPortugal_ES() {
		return generateRandomNumber(8).toString();
	}

	protected String generatePassportDeutschland_ES() {
		return generateRandomNumber(10).toString();
	}

	protected String generatePassportNetherlands_ES() {
		String passport = generateRandomWord(2, "o", false).toUpperCase();
		passport += generateRandomWord(6, "o", true).toUpperCase();
		passport += generateRandomNumber(1);
		return passport;
	}
}
