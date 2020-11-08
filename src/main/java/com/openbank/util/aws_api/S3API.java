package com.openbank.util.aws_api;

import java.io.File;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

public class S3API {
    private static final String region = "eu-west-1";
    private static final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(region).build();

    public static void uploadReport(String key, String bucketName, File reportFile) {
        try {
            s3.putObject(bucketName, key, reportFile);
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
        }
    }
}