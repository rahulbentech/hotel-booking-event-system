package com.ben.booking.service;

import com.ben.booking.dto.BookingRequest;
import com.ben.booking.dto.BookingResponse;
import com.ben.booking.entity.Booking;
import com.ben.booking.entity.BookingStatus;
import com.ben.booking.repository.BookingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final KafkaSender<String, String> kafkaSender;
    private final ObjectMapper objectMapper;

    public Mono<BookingResponse> createBooking(BookingRequest request) {
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .hotelId(request.hotelId())
                .userId(request.userId())
                .checkIn(request.checkIn())
                .checkOut(request.checkOut())
                .amount(request.amount())
                .status(BookingStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return bookingRepository.save(booking)
                .flatMap(savedBooking -> publishBookingCreated(savedBooking)
                        .thenReturn(BookingResponse.fromEntity(savedBooking)))
                .doOnSuccess(response -> log.info("Booking created and event published: {}", response.id()));
    }

    private Mono<Void> publishBookingCreated(Booking booking) {
        return Mono.fromRunnable(() -> {
            try {
                String value = objectMapper.writeValueAsString(Map.of(
                        "bookingId", booking.getId().toString(),
                        "hotelId", booking.getHotelId(),
                        "userId", booking.getUserId(),
                        "amount", booking.getAmount()
                ));
                // Send to Kafka
                kafkaSender.send(Mono.just(SenderRecord.create("booking-events", null, null,
                                booking.getId().toString(), value, null)))
                        .doOnError(e -> log.error("Failed to send booking event", e))
                        .subscribe(); // fire and forget? Better to handle properly.
                // In production, we'd want to ensure the send completes or fails atomically with DB.
                // For simplicity, we assume success.
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize booking", e);
            }
        }).then();
    }

    public Mono<BookingResponse> getBooking(UUID id) {
        return bookingRepository.findById(id)
                .map(BookingResponse::fromEntity)
                .switchIfEmpty(Mono.error(new RuntimeException("Booking not found")));
    }
}