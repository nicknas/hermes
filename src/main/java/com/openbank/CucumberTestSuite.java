//package com.openbank;
//
//import cucumber.api.CucumberOptions;
//import net.serenitybdd.cucumber.CucumberWithSerenity;
//
//import org.junit.runner.JUnitCore;
//import org.junit.runner.RunWith;
//
//@RunWith(CucumberWithSerenity.class)
//
//
//@CucumberOptions(strict=false, tags= {""}, features="classpath:features/Argentina",
//plugin = { "pretty", "json:target/cucumber-json-report.json","html:target/cucumber-html-report"})
//
//public class CucumberTestSuite {  
//	
//
//	public static void main(String[] args) {
//
//		JUnitCore.main(CucumberTestSuite.class.getName());
//	}
//}  
// 

package com.openbank;

import cucumber.api.CucumberOptions;
import net.serenitybdd.cucumber.CucumberWithSerenity;

import org.junit.runner.RunWith;

@RunWith(CucumberWithSerenity.class)
@CucumberOptions(strict = false, features = "features/",
			plugin = { "pretty", "json:target/cucumber-json-report.json", "html:target/cucumber-html-report" })
public class CucumberTestSuite {}