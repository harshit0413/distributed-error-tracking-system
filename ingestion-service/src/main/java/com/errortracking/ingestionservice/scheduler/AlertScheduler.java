package com.errortracking.ingestionservice.scheduler;

import com.errortracking.ingestionservice.entity.ErrorEvent;
import com.errortracking.ingestionservice.repository.ErrorEventRepository;
import com.errortracking.ingestionservice.service.GroqAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlertScheduler.class);
    private final StringRedisTemplate redisTemplate;
    private final ErrorEventRepository errorEventRepository;

    // 1. AI Service ko add kiya
    private final GroqAiService groqAiService;

    // Constructor update kiya
    public AlertScheduler(StringRedisTemplate redisTemplate, ErrorEventRepository errorEventRepository, GroqAiService groqAiService) {
        this.redisTemplate = redisTemplate;
        this.errorEventRepository = errorEventRepository;
        this.groqAiService = groqAiService;
    }

    @Scheduled(fixedRate = 60000)
    public void checkForErrorSpikes() {
        log.info("Manager checking for spikes...");

        Set<String> activeKeys = redisTemplate.keys("error:*:count");
        if (activeKeys == null || activeKeys.isEmpty()) {
            return;
        }

        for (String key : activeKeys) {
            String fingerprint = key.split(":")[1];

            String countStr = redisTemplate.opsForValue().get(key);
            long currentCount = countStr != null ? Long.parseLong(countStr) : 0;

            Double historicalAvg = errorEventRepository.getHistoricalHourlyAverage(fingerprint);
            double avg = historicalAvg != null ? historicalAvg : 0.0;

            boolean isSpike = (avg > 0 && currentCount > (3 * avg)) || (avg == 0 && currentCount > 5);
            if (isSpike) {
                // 2. Database se is error ka ek sample mangwao
                ErrorEvent sampleEvent = errorEventRepository.findFirstByFingerprint(fingerprint);

                String aiSummary = "No sample found.";
                if (sampleEvent != null) {
                    // 3. AI (Groq) ko call lagao
                    aiSummary = groqAiService.generateRootCauseSummary(
                            sampleEvent.getErrorType(),
                            sampleEvent.getErrorMessage(),
                            sampleEvent.getStackTrace()
                    );
                }

                // 4. Final Alert Print karo with AI Intelligence!
                log.error("🚨🚨 SPIKE DETECTED! Fingerprint: {} | Count: {} | Historical Avg: {}", fingerprint, currentCount, avg);
                log.error("🤖 AI Root Cause Summary: {}", aiSummary);
                log.error("=========================================================");

            } else {
                log.info("Normal traffic for {}. Count: {}, Avg: {}", fingerprint, currentCount, avg);
            }
        }
    }
}