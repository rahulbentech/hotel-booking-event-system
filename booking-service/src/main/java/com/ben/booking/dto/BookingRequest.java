package com.ben.booking.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingRequest(
        @NotBlank String hotelId,
        @NotBlank String userId,
        @NotNull LocalDate checkIn,
        @NotNull LocalDate checkOut,
        @NotNull @Positive BigDecimal amount
) {}