package com.openbank.stepdefinitions;

import cucumber.api.Scenario;
import cucumber.api.java.Before;
import net.serenitybdd.core.Serenity;

public class SetupStepDefinitions {
    @Before
    public void addTags(Scenario scenario) {
        scenario.getSourceTagNames().stream().map(t -> t.replaceFirst("@", "")).forEach(tagname -> {
            String[] parts = tagname.split(":");
            String key = parts[0];
            String value = parts.length > 1 ? parts[1] : "";
            Serenity.getCurrentSession().addMetaData(key, value);
        });
    }
}
