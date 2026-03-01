package com.ben.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentProcessedEvent {
    private UUID bookingId;
    private String status; // "SUCCESS" or "FAILED"
}