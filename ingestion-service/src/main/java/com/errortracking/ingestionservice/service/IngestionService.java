package com.errortracking.ingestionservice.service;

import com.errortracking.ingestionservice.dto.ErrorPayload;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class IngestionService {

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Constructor Injection (Strictly use this, avoid @Autowired on fields)
    public IngestionService(StringRedisTemplate redisTemplate, KafkaTemplate<String, Object> kafkaTemplate) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void processError(String apiKey, ErrorPayload payload) {
        // Step 1: Validate API Key from Redis cache (O(1) lookup)
        String tenantId = redisTemplate.opsForValue().get("tenant:" + apiKey + ":apikey");

        if (tenantId == null) {
            // TODO: Agar Redis mein nahi mila, toh DB se check karke Redis mein daalna (Cache Aside Pattern)
            // Abhi ke liye hum exception throw kar rahe hain
            throw new IllegalArgumentException("Invalid API Key");
        }

        // Step 2: TODO - Rate Limiting (1000 requests/min check)

        // Step 3: TODO - Publish payload to Kafka 'error-events' topic
    }
}