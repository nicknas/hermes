package com.openbank.util;

import java.util.LinkedList;
import java.util.List;

import io.restassured.http.Header;
import io.restassured.http.Headers;

public class CreateHeaders {

	static public Headers getHeaders() {
		List<Header> headerList = new LinkedList<>();

		headerList.add(new Header("Content-Type", "application/json"));
		return new Headers(headerList);
	}
}
