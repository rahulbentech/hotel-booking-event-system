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
                .id(UUID.randomUUID().toString())
                .hotelId(request.hotelId())
                .userId(request.userId())
                .checkIn(request.checkIn())
                .checkOut(request.checkOut())
                .amount(request.amount())
                .status(BookingStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Save inside a transaction and only after successful save, publish to Kafka.
        // Then return the response.
        return bookingRepository.save(booking)
                .flatMap(savedBooking ->
                        publishBookingCreated(savedBooking)
                                .thenReturn(BookingResponse.fromEntity(savedBooking))
                )
                .doOnSuccess(response -> log.info("Booking created and event published: {}", response.id()));
    }

    private Mono<Void> publishBookingCreated(Booking booking) {
        return Mono.fromCallable(() -> {
                    try {
                        return objectMapper.writeValueAsString(Map.of(
                                "bookingId", booking.getId().toString(),
                                "hotelId", booking.getHotelId(),
                                "userId", booking.getUserId(),
                                "amount", booking.getAmount()
                        ));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to serialize booking", e);
                    }
                })
                .flatMap(value ->
                        kafkaSender.send(Mono.just(SenderRecord.create("booking-events", null, null,
                                        booking.getId().toString(), value, null)))
                                .next()  // take the first (and only) result
                                .then()
                )
                .doOnError(e -> log.error("Failed to send booking event", e));
    }

    public Mono<BookingResponse> getBooking(String id) {
        return bookingRepository.findById(id)
                .map(BookingResponse::fromEntity);
    }
}