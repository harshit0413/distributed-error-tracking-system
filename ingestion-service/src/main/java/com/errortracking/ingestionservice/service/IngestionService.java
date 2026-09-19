package com.errortracking.ingestionservice.service;

import com.errortracking.ingestionservice.dto.ErrorPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class IngestionService {

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    // Constructor Injection (Strictly use this, avoid @Autowired on fields)
    public IngestionService(StringRedisTemplate redisTemplate, KafkaTemplate<String, Object> kafkaTemplate) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void processError(String apiKey, ErrorPayload payload) {
        // Step 1: Validate API Key from Redis cache (O(1) lookup)
        String tenantId = redisTemplate.opsForValue().get("tenant:" + apiKey + ":apikey");

        if (tenantId == null) {
            throw new IllegalArgumentException("Invalid API Key");
        }

        // Step 2: Rate Limiting - Fixed Window Algorithm (Limit: 100 requests per minute)
        String currentMinute = String.valueOf(System.currentTimeMillis() / 60000);
        String rateLimitKey = "rate_limit:" + tenantId + ":" + currentMinute;

        // Redis mein count +1 karo (Agar key nahi hai toh 1 ban jayegi)
        Long currentCount = redisTemplate.opsForValue().increment(rateLimitKey);

        // Agar pehli request hai is minute ki, toh 1 minute ka Timer (TTL) laga do
        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(rateLimitKey, java.time.Duration.ofMinutes(1));
        }

        // Agar 1 minute mein 100 se zyada errors aa gaye, toh block kar do
        // Agar 1 minute mein 100 se zyada errors aa gaye, toh block kar do
        if (currentCount != null && currentCount > 100) {
            // 1. Internal Logging (Sirf humein console mein dikhega)
            log.warn("Rate limit breached for tenantId: {}. Blocking request.", tenantId);

            // 2. External Generic Response (Jo client ko jayega)
            throw new RuntimeException("Rate limit exceeded. Please try again later.");
        }

        // Step 3: Publish payload to Kafka 'error-events' topic
        // tenantId ko hum Kafka 'Key' ki tarah bhej rahe hain
        kafkaTemplate.send("error-events", tenantId, payload);
    }
}