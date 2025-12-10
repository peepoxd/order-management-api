package com.delivery.order_management.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

@Component
public class OrderNumberGenerator {

    private static final AtomicInteger sequence = new AtomicInteger(0);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Generate unique order number
     * Format: ORD-YYYYMMDDHHMMSS-XXXX
     * Example: ORD-20241203143025-0001
     */
    public String generate() {
        String timestamp = LocalDateTime.now().format(formatter);
        int seq = sequence.incrementAndGet() % 10000; // Reset after 9999

        return String.format("ORD-%s-%04d", timestamp, seq);
    }

    /**
     * Alternative: Generate shorter order number
     * Format: ORD-TIMESTAMP-RANDOM
     * Example: ORD-1701609625-A3F2
     */
    public String generateShort() {
        long timestamp = System.currentTimeMillis() / 1000; // Unix timestamp
        String random = generateRandomString(4);

        return String.format("ORD-%d-%s", timestamp, random);
    }

    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            result.append(chars.charAt(index));
        }

        return result.toString();
    }
}
