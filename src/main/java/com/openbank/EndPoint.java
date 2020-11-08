package com.openbank;

import com.openbank.util.VariablesExpander;

public enum EndPoint {
    BASE(getBase());

    private String url;

	EndPoint(String url) {
        this.url = url;
    }

	private static String getBase() {
		String exp = exp("${" + exp("${country}.${env}.baseUrl") + "}");
        return exp;
    }

    private static String exp(String exp) {
        return VariablesExpander.get().replace(exp);
    }

	public String getUrl() {
        return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
