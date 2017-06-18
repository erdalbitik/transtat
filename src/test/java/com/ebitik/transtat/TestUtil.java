package com.ebitik.transtat;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Random;

public class TestUtil {
	
	private static Random rand = new SecureRandom();

	public static long getRandomMinuteTime() {
		Instant instant = Instant.now();
		return instant.toEpochMilli()- rand.nextInt(60000);
	}
	
	public static double getRandomAmount() {
		return Math.random() * 50 + 1;
	}
	
}
