package com.openbank.util.aws_api;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Base64;

public class SecretsManagerAPI {
    private static final String region = "eu-west-1";
    private static final AWSSecretsManager client = AWSSecretsManagerClientBuilder.standard().withRegion(region).build();

    public static JsonNode getSecret(String secretName) {

        // mongoAPITestingCredentials

        // In this sample we only handle the specific exceptions for the
        // 'GetSecretValue' API.
        // See
        // https://docs.aws.amazon.com/secretsmanager/latest/apireference/API_GetSecretValue.html   
        // We rethrow the exception by default.

        String secret, decodedBinarySecret;
        GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest().withSecretId(secretName);
        GetSecretValueResult getSecretValueResult;

        getSecretValueResult = client.getSecretValue(getSecretValueRequest);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = objectMapper.createObjectNode();
        // Decrypts secret using the associated KMS CMK.
        // Depending on whether the secret is a string or binary, one of these fields
        // will be populated.
        if (getSecretValueResult.getSecretString() != null) {
            secret = getSecretValueResult.getSecretString();
            try {
                node = objectMapper.readTree(secret);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            decodedBinarySecret = new String(Base64.getDecoder().decode(getSecretValueResult.getSecretBinary()).array());
            try {
                node = objectMapper.readTree(decodedBinarySecret);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return node;
    }
}