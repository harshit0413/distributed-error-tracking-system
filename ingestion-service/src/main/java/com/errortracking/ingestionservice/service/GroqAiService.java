package com.errortracking.ingestionservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class GroqAiService {

    private static final Logger log = LoggerFactory.getLogger(GroqAiService.class);
    private final WebClient webClient;

    // application.properties se API key utha rahe hain
    @Value("${groq.api.key}")
    private String groqApiKey;

    public GroqAiService(WebClient.Builder webClientBuilder) {
        // Base URL set kar diya
        this.webClient = webClientBuilder.baseUrl("https://api.groq.com/openai/v1").build();
    }

    public String generateRootCauseSummary(String errorType, String errorMessage, String stackTrace) {
        // 1. AI ke liye strict instruction (Prompt)
        String prompt = String.format(
                "You are a senior backend engineer. Be concise. 2 sentences max. " +
                        "Generate a root cause hypothesis for this error spike:\nType: %s\nMessage: %s\nStack Trace top frames: %s",
                errorType, errorMessage, stackTrace.substring(0, Math.min(stackTrace.length(), 1000))
        );

        log.info("Sending request to Groq AI for root cause analysis...");

        try {
            // 2. HTTP POST Request to Groq API
            Map response = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + groqApiKey)
                    .bodyValue(Map.of(
                            "model", "openai/gpt-oss-20b", // Free tier ka superfast model
                            "messages", List.of(Map.of("role", "user", "content", prompt)),
                            "max_tokens", 200, // 2 sentence ke liye 200 tokens kaafi hain
                            "temperature", 0.3 // Kam temperature = Zyada technical aur less creative answer
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(); // Thread ko tab tak roko jab tak AI reply na kare

            // 3. Complex JSON se sirf actual answer (content) nikalna
            List choices = (List) response.get("choices");
            Map firstChoice = (Map) choices.get(0);
            Map message = (Map) firstChoice.get("message");

            return (String) message.get("content");

        } catch (Exception e) {
            log.error("Failed to generate AI summary from Groq", e);
            return "AI Root Cause summary unavailable due to Groq API error.";
        }
    }
}