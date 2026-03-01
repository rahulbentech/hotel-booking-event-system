package com.ben.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingCreatedEvent {
    private UUID bookingId;
    private String hotelId;
    private String userId;
    private BigDecimal amount;
    private LocalDate checkIn;
    private LocalDate checkOut;
    // other fields as needed
}