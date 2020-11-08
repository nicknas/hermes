package com.openbank.stepdefinitions;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.junit.Assert;

import java.math.BigInteger;
import java.util.*;

public class MasterStepDefinitions {

	private static class FormatWord {
		private final String word;
		private final int initWord;
		private final int endWord;
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

			} catch (io.restassured.path.json.exception.JsonPathException ignored) {
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

	public void assertListContains(Response resp, String jsonPath, Object expected) {
		resp.then().body(jsonPath, Matchers.hasItem(expected));
	}

	/**
	 * This method is use to generate PASSPORT/REDCARD document number based on the
	 * logic provided by functional team
	 **/
	protected String getLegalDocID() {
		return RandomNumberGenerator.random7DigitGenerator();
	}

	protected String getNIE() {
		return generateNIE();
	}

	/**
	 * This method is use to generate DNI document number based on the logic
	 * provided by functional team
	 **/
	protected String getDNI() {
		return generateDNI();
	}

	/**
	 * This method is use to generate NIF document number based on the logic
	 * provided by functional team
	 **/
	protected String getKNIF() {
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
		StringBuilder formatWord = new StringBuilder();
		List<FormatWord> formatWords = new ArrayList<>();
		for (int i = 0; i < totalSize; i++){
			if (initWord == -1 && format.charAt(i) != '?'){
				initWord = i;
				formatWord.append(format.charAt(i));
			}
			else if (initWord > -1 && format.charAt(i) != '?'){
				formatWord.append(format.charAt(i));
			}
			else if (format.charAt(i) == '?' && (formatWord.length() > 0)){
				formatWords.add(new FormatWord(formatWord.toString(), initWord, initWord + formatWord.length() - 1));
				formatWord = new StringBuilder();
				initWord = -1;
			}
		}
		StringBuilder pattern = new StringBuilder();
		FormatWord lastFormatWord = null;
		for (FormatWord word : formatWords) {
			if (pattern.length() == 0) {
				pattern.append("^.{").append(word.getInitWord()).append("}(").append(word.getWord()).append("){1}");
			}
			else {
				assert lastFormatWord != null;
				pattern.append(".{").append((word.getInitWord() - lastFormatWord.getEndWord() - 1)).append("}(").append(word.getWord()).append("){1}");
			}
			lastFormatWord = word;
		}

		assert lastFormatWord != null;
		pattern.append(".{").append(((format.length() - 1) - lastFormatWord.getEndWord())).append("}$");

		if (shouldExist) {
			resp.then().body(node, MatchesPattern.matchesPattern(pattern.toString()));
		}
		else {
			resp.then().body(node, Matchers.not(MatchesPattern.matchesPattern(pattern.toString())));
		}
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

	public String findLegalDocument(String legalDocument) {
		switch (legalDocument) {
		case "PASSPORT":
			case "REDCARD":
				legalDocument = getLegalDocID();
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

	private String generateNIE() {

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
		return "X" + NIEUpdatedNo + expectedvalue;
	}

	private String generateDNI() {
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
		return IntDNI + expectedvalue;
	}

	public String amendLegalDocumentValue(String request, String legalDocument) {
		return request.replace("legalDocIDchange", legalDocument);
	}

	private String generateKNIF() {
		String updatedKNIF = RandomNumberGenerator.random7DigitGenerator();

		String[] Numbers = updatedKNIF.split("(?!^)");
		int addEven = 0, addOdd = 0, remaining;
		for (int i = 1; i < Numbers.length; i++) {
			int evenNumber = Integer.parseInt(Numbers[i]);
			addEven = addEven + evenNumber;
			i = i + 1;
		}
		int[] n = new int[4];
		for (int i = 0; i < Numbers.length; i++) {
			int oddNumber = Integer.parseInt(Numbers[i]) * 2;
			n[addOdd] = oddNumber;
			i = i + 1;
			addOdd = addOdd + 1;
		}
		String[] oddNumber1 = String.valueOf(n[0]).split("(?!^)");
		String[] oddNumber2 = String.valueOf(n[1]).split("(?!^)");
		String[] oddNumber3 = String.valueOf(n[2]).split("(?!^)");
		String[] oddNumber4 = String.valueOf(n[3]).split("(?!^)");
		int firstvalue = 0, secondvalue = 0, thirdvalue = 0, fourthvalue = 0;
		for (String s : oddNumber1) {
			firstvalue = firstvalue + Integer.parseInt(s);
		}
		for (String s : oddNumber2) {
			secondvalue = secondvalue + Integer.parseInt(s);
		}
		for (String s : oddNumber3) {
			thirdvalue = thirdvalue + Integer.parseInt(s);
		}
		for (String s : oddNumber4) {
			fourthvalue = fourthvalue + Integer.parseInt(s);
		}
		int totalodd = firstvalue + secondvalue + thirdvalue + fourthvalue;
		int totalvalue = addEven + totalodd;
		String[] splittotal = String.valueOf(totalvalue).split("(?!^)");
		int size = splittotal.length;
		if (splittotal[size - 1].equals("0")) {
			remaining = 0;
		} else {
			remaining = 10 - Integer.parseInt(splittotal[size - 1]);
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

	protected String expandBody(String body) {
		try {
			String dni = findLegalDocument("DNI");
			return amendLegalDocumentValue(body, dni);
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
					assertAllTheJSONFields(key + "[" + i + "]", value);
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

	public String generateDNI_AR(String gender) {
		String strUpdatedDNI;
		if (!gender.isEmpty()) {
			strUpdatedDNI = gender + RandomNumberGenerator.random7DigitGenerator();
		}
		else {
			strUpdatedDNI = String.valueOf(RandomNumberGenerator.random8DigitGenerator());
		}
		session().set("DNIAR", strUpdatedDNI);
		return strUpdatedDNI;
	}

	public String generatelegalId_AR(String gender) {
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
		return JsonPointer.valueOf("/" + jsonPath.replace(".", "/").replace("[", "/").replace("]", ""));
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

		int accountNumberCalculation = Integer.parseInt(ccc.substring(0, 1))
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
		
		Assert.assertThat(Integer.toString(cd), Is.is(cd_1 + Integer.toString(cd_2)));
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
