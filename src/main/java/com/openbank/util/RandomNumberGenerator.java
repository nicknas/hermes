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
		int n = (int) (10000000 + rand.nextFloat() * 90000000);
		return n;
	}

	public static String random19DigitGenerator() {
		String cardId = "CRD" + RandomStringUtils.random(16, false, true);
		return cardId;
	}

	public static String random9DigitGenerator() {
		String cmsAccountId = RandomStringUtils.random(9, false, true);
		return cmsAccountId;
	}
	public static String random11DigitGenerator() {
		String cmsAccountId = RandomStringUtils.random(11, false, true);
		return cmsAccountId;
	}

}
