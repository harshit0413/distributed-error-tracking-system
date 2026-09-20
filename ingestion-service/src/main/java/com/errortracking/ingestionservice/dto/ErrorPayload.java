package com.errortracking.ingestionservice.dto;

import lombok.Data;

@Data
public class ErrorPayload {

    private String errorType;
    private String errorMessage;
    private String stackTrace;
    private String serviceName;
    private String environment;
    private String appVersion;
    private String hostName;
    private String userId;
    private String timestamp;
}