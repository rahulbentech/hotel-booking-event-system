package com.ben.booking.dto;

import com.ben.booking.entity.Booking;
import com.ben.booking.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        String hotelId,
        String userId,
        LocalDate checkIn,
        LocalDate checkOut,
        BigDecimal amount,
        BookingStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static BookingResponse fromEntity(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getHotelId(),
                booking.getUserId(),
                booking.getCheckIn(),
                booking.getCheckOut(),
                booking.getAmount(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}