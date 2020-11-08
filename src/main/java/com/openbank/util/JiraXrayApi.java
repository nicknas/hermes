package com.openbank.util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.thucydides.core.model.TestResult;

public class JiraXrayApi {

    private static final String auth;

    static {
        auth = expand("${jira.auth}");
        assertProperties();
    }

    /**
     * assert required jira configuration properties
     */
    private static void assertProperties() {
        assertProperty("jira.url.xrayApi");
        assertProperty("jira.auth");
    }

    private static void assertProperty(String key) {
        String val = expand("${" + key + "}");
        if (val == null || val.isEmpty())
            throw new AssertionError(String.format("property '%s' required", key));
    }

    public static String expand(String str) {
        return VariablesExpander.get().replace(str);
    }


    public static void updateTestStatus(TestResult result, String testIdResult) {
        try {
            String runId = getTestRunId(testIdResult);
            String status = converToJiraStatus(result);
            updateTestRunStatus(runId, status);
            Logger.getLogger(JiraXrayApi.class.getName()).info(
                    String.format("JIRA-XRAY: status for test with id '%s' updated to '%s' ", testIdResult, status));

        } catch (Exception ex) {
            Logger.getLogger(JiraXrayApi.class.getName()).log(Level.WARNING,
                    String.format("JIRA-XRAY: error while updating results for test %s", testIdResult), ex);
        }
    }

    /**
     * build rest client with headers
     *
     * @return RequestSpecification
     */
    private static RequestSpecification client() {
        return RestAssured.given()
                // .log().method().log().uri().log().body()
                .header("Authorization", auth).contentType(ContentType.JSON).accept(ContentType.JSON);
    }

    /**
     * update the run status
     *
     * @param runId  test run id
     * @param status test status
     */
    private static void updateTestRunStatus(String runId, String status) {
        client().put(expand("${jira.url.xrayApi}/testrun/" + runId + "/status?status=executing"));
        client().put(expand("${jira.url.xrayApi}/testrun/" + runId + "/status?status=" + status)).then().log()
                .ifError();
    }

    public static void updateDescriptionWithS3Report(String issueID, String url) {
        String putUrl = expand("${jira.url.api}/issue/" + issueID);
        client().body("{\"fields\": { \"description\": \"Latest Report\\n" + url + "\" } }").log().all().put(putUrl)
                .then().statusCode(204).log().all();
    }

    public static void downloadScenarioFromJira(String issueID) {
        String getUrl = expand("${jira.url.api}/issue/" + issueID);
        Response resp = client().queryParam("fields", "summary,customfield_10413").log().all().get(getUrl);
        resp.then().statusCode(200);
        JsonPath scenario = resp.body().jsonPath();
        String featureName = scenario.getString("fields.summary");
        String scenarioBody = scenario.getString("fields.customfield_10413");
        File featureFile;
        try {
            featureFile = new File(DataSource.propertyReadSerenity("serenity.tempFeaturesDirectory") + "/"
                    + EnvironmentUrl.COUNTRY + "/" + EnvironmentUrl.ENVIRONMENT + "/" + issueID + ".feature");
            FileUtils.writeStringToFile(featureFile,
                    "Feature: " + featureName + "\n" + "@" + issueID + "\n" + scenarioBody, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setEnvironmentData(String jiraExecutionId) throws EnvironmentUrlException {
        String getUrl = expand("${jira.url.api}/issue/" + jiraExecutionId);
        JsonPath environmentData = client().queryParam("fields", "customfield_11201,customfield_10903,environment")
                .get(getUrl).body().jsonPath();
        if (environmentData == null) {
            throw new EnvironmentUrlException();
        }

        String environment = environmentData.getString("fields.environment");
        String country = environmentData.getString("fields.customfield_10903");
        String url = environmentData.getString("fields.customfield_11201");

        if (environment.trim().isEmpty() || country.trim().isEmpty() || url.trim().isEmpty()) {
            throw new EnvironmentUrlException();
        }

        if (country.equalsIgnoreCase("argentina") || country.equalsIgnoreCase("arg")) {
            EnvironmentUrl.COUNTRY = "arg";
        } else if (country.equalsIgnoreCase("spain") || country.equalsIgnoreCase("españa")
                || country.equalsIgnoreCase("es")) {
            EnvironmentUrl.COUNTRY = "es";
        }

        EnvironmentUrl.ENVIRONMENT = environment.toLowerCase();
        EnvironmentUrl.URL = url;
    }


    private static String getTestRunId(String testId) {
        return String.valueOf(client()
                .get(expand("${jira.url.xrayApi}/testrun?testExecIssueKey=${jira.execution}&testIssueKey=" + testId))
                .then().log().ifError().extract().body().jsonPath().getInt("id"));
    }

    /**
     * Get test ids inside given execution id
     *
     * @return test id list
     */
    public static List<String> getTestIds() {
        return client().get(expand("${jira.url.xrayApi}/testexec/${jira.execution}/test")).then().log().ifError()
                .extract().body().jsonPath().getList("key");
    }

    public static List<String> getTestExecutionsFromPlan() {
        return client().get(expand("${jira.url.xrayApi}/testplan/${jira.plan}/testexecution")).then().log().ifError()
                .extract().body().jsonPath().getList("key");
    }

    private static String converToJiraStatus(TestResult status) {
        switch (status) {
        case SUCCESS:
            return "pass";
        case PENDING:
        case SKIPPED:
        case UNDEFINED:
            return "todo";
        case IGNORED:
        default:
            return "fail";
        }
    }

}
