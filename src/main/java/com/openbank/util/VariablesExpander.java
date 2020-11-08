/*
  * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package com.openbank.util;


import net.thucydides.core.util.SystemEnvironmentVariables;

import java.io.Serializable;
import java.util.Properties;

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


}