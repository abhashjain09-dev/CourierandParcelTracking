package com.courier.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class TrackingNumberGenerator {
    private static final String PREFIX = "CTS";
    private static final Random RANDOM = new Random();

    private TrackingNumberGenerator() {}

    public static String generate() {
        String date   = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = alphaNum(6);
        return PREFIX + "-" + date + "-" + suffix;
    }

    private static String alphaNum(int len) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        return sb.toString();
    }
}
