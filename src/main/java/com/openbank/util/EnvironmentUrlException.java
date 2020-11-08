package com.openbank.util;

public class EnvironmentUrlException extends RuntimeException{
    private static final long serialVersionUID = 1610534256156703554L;
    public EnvironmentUrlException() {
        super("The environment data in Jira test execution is empty or there are fields missing");
    }
}