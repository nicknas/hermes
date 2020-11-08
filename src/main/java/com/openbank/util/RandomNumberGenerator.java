package com.openbank.util;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Random;

public class RandomNumberGenerator {

	public static String random7DigitGenerator() {
		Random rand = new Random();
		int n = (int) (1000000 + rand.nextFloat() * 9000000);
		return (String.valueOf(n));
	}

	public static int random8DigitGenerator() {
		Random rand = new Random();
		return (int) (10000000 + rand.nextFloat() * 90000000);
	}

	public static String random19DigitGenerator() {
		return "CRD" + RandomStringUtils.random(16, false, true);
	}

	public static String random11DigitGenerator() {
		return RandomStringUtils.random(11, false, true);
	}

}
