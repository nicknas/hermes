/*
  * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package com.openbank.util;


import net.thucydides.core.util.SystemEnvironmentVariables;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

/**
 * This class is used to expand variables in the format <code>${variable}</code>$, using values from
 * {@link System#getenv()}, {@link System#getProperties()} and the <code>Properties</code> object specified in the
 * constructor (in inverse order; first match is accepted).
 *
 * @author Luigi R. Viggiano
 */
public class VariablesExpander implements Serializable {

    private static final long serialVersionUID = 1L;
    public static VariablesExpander INSTANCE;
    private final StrSubstitutor substitutor;


    private VariablesExpander() {
        Properties variables = new Properties();
        variables.putAll(SystemEnvironmentVariables.createEnvironmentVariables().getProperties());
        variables.putAll(System.getenv());
        variables.putAll(System.getProperties());
        setDefaultProperty(variables,"mongo.server", "localhost:27017");
        //**
        setDefaultProperty(variables,"mongo.db", "config");
        setDefaultProperty(variables,"country", "es");
        setDefaultProperty(variables,"env", "qa");
        substitutor = new StrSubstitutor(variables);
    }

    private void setDefaultProperty(Properties existing,String key, String value) {
        if (!existing.containsKey(key)) {
            existing.put(key, value);
        }
    }

    public static VariablesExpander get() {
        if (INSTANCE == null)
            INSTANCE = new VariablesExpander();
        return INSTANCE;
    }

    private static String expandUserHome(String text) {
        if (text.equals("~")) {
            return System.getProperty("user.home");
        } else if (text.indexOf("~/") != 0 && text.indexOf("file:~/") != 0 && text.indexOf("jar:file:~/") != 0) {
            return text.indexOf("~\\") != 0 && text.indexOf("file:~\\") != 0 && text.indexOf("jar:file:~\\") != 0 ?
                    text : text.replaceFirst("~\\\\", fixBackslashForRegex(System.getProperty("user.home")) + "\\\\");
        } else {
            return text.replaceFirst("~/", fixBackslashForRegex(System.getProperty("user.home")) + "/");
        }
    }

    private static String fixBackslashForRegex(String text) {
        return text.replace("\\", "\\\\");
    }

    public String expand(String path) {
        String expanded = expandUserHome(path);
        return replace(expanded);
    }
    public String replace(String str) {
        return substitutor.replace(str);
    }
    
    //Method to create invalid Date of Birth
    public static String randomInvalidDOB() {
    	Random rand = new Random(); 
    	int value = rand.nextInt(50) + 13; 
    	return  "1990" + value + "09";
    }
    
    //Method to create invalid Date of Birth
    public static String randomInvalidresidenceTCode() {
    	
    	final String alphabet = "BCDE";
        final int N = alphabet.length();

        Random r = new Random();
        String str = null;

        for (int i = 0; i < 4; i++) {
            str = str + alphabet.charAt(r.nextInt(N));
        }
		return str;
    }
    
    
    
    //Method to create invalid 3 digit code
    public static String randomInvalidTCode() {
    	Random rand = new Random(); 
    	int value = rand.nextInt(99) + 100; 
    	return  value + "";
    }
    
  //Method to create invalid 12 digit code
    public static String randomInvalidLegalDocumentID() {
    	Random rand = new Random(); 
    	int value = rand.nextInt(99) + 100; 
    	return  value + "qawsseqeeqeqeqeqeqeqewwassawwq";
    }
        
    //Method to create invalid 4 char code
    public static String randomInvalidLegalDocumentType() {
    	Random rand = new Random(); 
    	int value = rand.nextInt(5000) + 177; 
    	return  value + "tttwrrwrfafrqadaddafvsdjjdkkdkdkdhh";
    }
    
  //Method to create Invalid DepositId of 10 digit
    public static String randonInvalidDepositId() {
       String invalidDepositId = "200" + RandomStringUtils.random(6, true, true) + "@";
       return invalidDepositId;
    }
    
    //Method to fetch system date and return in format of YYYY-MM-DD
    public static String systemDate() {
    	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
//		System.out.println(dateFormat.format(date));
		
    	return dateFormat.format(date).toString();
    }
    
    //Method to add months in current system date
    public static String addMonthToSystemDate(int noOfMonths) {
    	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		
        cal.setTime(date);
        cal.add(Calendar.MONTH, noOfMonths);
        System.out.println(dateFormat.format(cal.getTime()));
    	
    	return dateFormat.format(cal.getTime()).toString();
    }
    
    
        
}