package com.openbank.util.aws_api;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.HashMap;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;

public class ParameterStoreAPI {
    private static final String region = "eu-west-1";
    private static final AWSSimpleSystemsManagement client = AWSSimpleSystemsManagementClientBuilder.standard().withRegion(region).build();

    public static String getJiraAuthorization() {
        List<String> credentials = new ArrayList<>();
        credentials.add("/config/testing/jira_user");
        credentials.add("/config/testing/jira_password");
        
        GetParametersRequest parametersRequest = new GetParametersRequest().withNames(credentials).withWithDecryption(true);
        GetParametersResult response = client.getParameters(parametersRequest);
        List<Parameter> parameters = response.getParameters();
        return "Basic " + Base64.getEncoder().encodeToString((parameters.get(1).getValue() + ":" + parameters.get(0).getValue()).getBytes());
    }

    public static HashMap<String, String> getObmCredentials(String country) {
        List<String> credentials = new ArrayList<>();
        credentials.add("/config/testing/obm_user_" + country);
        credentials.add("/config/testing/obm_password_" + country);
        
        GetParametersRequest parametersRequest = new GetParametersRequest().withNames(credentials).withWithDecryption(true);
        GetParametersResult response = client.getParameters(parametersRequest);
        List<Parameter> parameters = response.getParameters();
        HashMap<String, String> obmCredentials = new HashMap<>();
        obmCredentials.put("user", parameters.get(1).getValue());
        obmCredentials.put("password", parameters.get(0).getValue());
        return obmCredentials;
    }
}