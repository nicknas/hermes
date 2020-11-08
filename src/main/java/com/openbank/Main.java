package com.openbank;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.openbank.util.DataSource;
import com.openbank.util.EnvironmentUrl;
import com.openbank.util.EnvironmentUrlException;
import com.openbank.util.JiraXrayApi;
import com.openbank.util.aws_api.ParameterStoreAPI;
import com.openbank.util.aws_api.SecretsManagerAPI;
import com.openbank.util.VariablesExpander;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.util.EnvironmentVariables;

import org.apache.commons.io.FileUtils;
import org.junit.runner.JUnitCore;

public class Main {

    public static void main(String[] args) throws RuntimeException {
        if (args.length == 1) {
            JsonNode mongoCredentials = SecretsManagerAPI.getSecret("mongoAPITestingCredentials");
            String jiraAuth = ParameterStoreAPI.getJiraAuthorization();
            EnvironmentVariables env = Injectors.getInjector()
                    .getInstance(EnvironmentVariables.class);
            String server = mongoCredentials.get("host").asText() + ":" + mongoCredentials.get("port").asText();
            env.setProperty("mongo.server", server);
            System.setProperty("mongo.server", server);
            env.setProperty("mongo.username", mongoCredentials.get("username").asText());
            System.setProperty("mongo.username", mongoCredentials.get("username").asText());
            env.setProperty("mongo.password", mongoCredentials.get("password").asText());
            System.setProperty("mongo.password", mongoCredentials.get("password").asText());
            env.setProperty("jira.auth", jiraAuth);
            System.setProperty("jira.auth", jiraAuth);
            VariablesExpander.INSTANCE = null;
            String[] jiraTicket = args[0].split("=");
            if (jiraTicket.length != 2) {
                throw new RuntimeException(
                        "The format of the argument must be plan=ticket or execution=ticket where ticket is the jira ticket id");
            }
            String jiraTicketType = jiraTicket[0];
            String jiraTicketValue = jiraTicket[1];
            if (jiraTicketType.equalsIgnoreCase("plan")) {
                env.setProperty("jira.plan", jiraTicketValue);
                System.setProperty("jira.plan", jiraTicketValue);
                VariablesExpander.INSTANCE = null;
                List<String> testExecutions = JiraXrayApi.getTestExecutionsFromPlan();
                for (String testExecution : testExecutions) {
                    env.setProperty("jira.execution", testExecution);
                    System.setProperty("jira.execution", testExecution);
                    VariablesExpander.INSTANCE = null;
                    runTests();
                }
            } else if (jiraTicketType.equalsIgnoreCase("execution")) {
                env.setProperty("jira.execution", jiraTicketValue);
                System.setProperty("jira.execution", jiraTicketValue);
                VariablesExpander.INSTANCE = null;
                runTests();
            } else {
                throw new RuntimeException(
                        "The format of the argument must be plan=ticket or execution=ticket where ticket is the jira ticket id");
            }
        } else {
            throw new RuntimeException("Main class only accepts one argument");
        }
    }

    private static void runTests() {
        EnvironmentVariables env = Injectors.getInjector()
                .getInstance(EnvironmentVariables.class);
        try {
            JiraXrayApi.setEnvironmentData(expand("${jira.execution}"));
            env.setProperty("country", EnvironmentUrl.COUNTRY);
            env.setProperty("env", EnvironmentUrl.ENVIRONMENT);
            System.setProperty("country", EnvironmentUrl.COUNTRY);
            System.setProperty("env", EnvironmentUrl.ENVIRONMENT);
            VariablesExpander.INSTANCE = null;
            env.setProperty(expand("${country}.${env}.baseUrl"), EnvironmentUrl.URL);
            System.setProperty(expand("${country}.${env}.baseUrl"), EnvironmentUrl.URL);
            VariablesExpander.INSTANCE = null;
            EndPoint.BASE.setUrl(EnvironmentUrl.URL);
        } catch (EnvironmentUrlException e) {
            e.printStackTrace();
        }
        String scenarioTags = getScenarioTags();

        List<String> testCollection = Arrays.stream(scenarioTags.split(",")).filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toList());
        for (String test : testCollection) {
            try {
                FileUtils.forceMkdir(new File(DataSource.propertyReadSerenity("serenity.tempFeaturesDirectory")));
                JiraXrayApi.downloadScenarioFromJira(test);
                JUnitCore.runClasses(CucumberTestSuite.class);  
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private static String expand(String str) {
        return VariablesExpander.get().replace(str);
    }

    private static String getScenarioTags() {
        return String.join(",", JiraXrayApi.getTestIds());
    }
}
