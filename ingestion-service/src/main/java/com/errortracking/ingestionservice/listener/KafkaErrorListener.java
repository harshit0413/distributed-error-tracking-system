package com.errortracking.ingestionservice.listener;

import com.errortracking.ingestionservice.dto.ErrorPayload;
import com.errortracking.ingestionservice.entity.ErrorEvent;
import com.errortracking.ingestionservice.entity.ErrorGroup;
import com.errortracking.ingestionservice.repository.ErrorEventRepository;
import com.errortracking.ingestionservice.repository.ErrorGroupRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class KafkaErrorListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorListener.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 1. Repositories Inject kar rahe hain
    private final ErrorEventRepository errorEventRepository;
    private final ErrorGroupRepository errorGroupRepository;
    private final StringRedisTemplate redisTemplate;

    public KafkaErrorListener(ErrorEventRepository errorEventRepository,
                              ErrorGroupRepository errorGroupRepository,
                              StringRedisTemplate redisTemplate) {
        this.errorEventRepository = errorEventRepository;
        this.errorGroupRepository = errorGroupRepository;
        this.redisTemplate = redisTemplate;
    }

    // @Transactional zaroori hai kyunki hum 2 tables mein likh rahe hain
    @Transactional
    @KafkaListener(topics = "error-events", groupId = "fast-worker-group")
    public void consumeErrorEvent(
            @Header(KafkaHeaders.RECEIVED_KEY) String tenantIdStr, // Kafka se 'Key' uthayi
            String payloadJSON) {

        try {
            UUID tenantId = UUID.fromString(tenantIdStr);
            ErrorPayload payload = objectMapper.readValue(payloadJSON, ErrorPayload.class);

            // Normalize & Fingerprint
            String normalizedTrace = normalizeStackTrace(payload.getStackTrace());
            String fingerprint = generateSHA256Fingerprint(normalizedTrace);

            // Step A: Hypertable mein Raw Error Insert karna (Timeline ke liye)
            ErrorEvent rawEvent = new ErrorEvent();
            rawEvent.setTenantId(tenantId);
            rawEvent.setFingerprint(fingerprint);
            rawEvent.setErrorType(payload.getErrorType());
            rawEvent.setErrorMessage(payload.getErrorMessage());
            rawEvent.setStackTrace(payload.getStackTrace());
            rawEvent.setServiceName(payload.getServiceName());
            rawEvent.setEnvironment(payload.getEnvironment());
            rawEvent.setAppVersion(payload.getAppVersion());
            rawEvent.setHostName(payload.getHostName());
            rawEvent.setUserId(payload.getUserId());
            // ZonedDateTime default abhi ka current time lega
            rawEvent.setOccurredAt(ZonedDateTime.now());
            errorEventRepository.save(rawEvent);

            // Step B: Error Groups mein Upsert (Deduplication Logic)
            Optional<ErrorGroup> existingGroupOpt = errorGroupRepository.findById(fingerprint);

            if (existingGroupOpt.isPresent()) {
                // Purana error mila -> Sirf Count aur LastSeen Update karo
                ErrorGroup existingGroup = existingGroupOpt.get();
                existingGroup.setOccurrenceCount(existingGroup.getOccurrenceCount() + 1);
                existingGroup.setLastSeen(ZonedDateTime.now());
                errorGroupRepository.save(existingGroup);
                log.info("Updated existing error group. Fingerprint: {}, New Count: {}", fingerprint, existingGroup.getOccurrenceCount());
            } else {
                // Naya error aaya -> Naya Record Banao
                ErrorGroup newGroup = new ErrorGroup();
                newGroup.setFingerprint(fingerprint);
                newGroup.setTenantId(tenantId);
                newGroup.setErrorType(payload.getErrorType());
                newGroup.setErrorMessage(payload.getErrorMessage());
                newGroup.setFirstSeen(ZonedDateTime.now());
                newGroup.setLastSeen(ZonedDateTime.now());
                newGroup.setOccurrenceCount(1L);
                errorGroupRepository.save(newGroup);
                log.info("Created NEW error group! Fingerprint: {}", fingerprint);
            }
            // Step C: Redis Spike Detection Counter
            String redisCounterKey = "error:" + fingerprint + ":count";
            Long currentSpikeCount = redisTemplate.opsForValue().increment(redisCounterKey);

            // Agar yeh is 5-minute window ka pehla error hai, toh Timer (TTL) laga do
            if (currentSpikeCount != null && currentSpikeCount == 1L) {
                redisTemplate.expire(redisCounterKey, Duration.ofSeconds(300));
            }
            log.info("Redis Spike Counter for {} is now: {}", fingerprint, currentSpikeCount);

        } catch (Exception e) {
            log.error("Failed to process and save error event from Kafka", e);
        }
    }

    // --- Helper Methods ---
    private String normalizeStackTrace(String stackTrace) {
        if (stackTrace == null) return "";
        return stackTrace.replaceAll(":[0-9]+", "");
    }

    private String generateSHA256Fingerprint(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}