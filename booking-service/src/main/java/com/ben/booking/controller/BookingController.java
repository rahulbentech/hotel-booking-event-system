package com.ben.booking.controller;

import com.ben.booking.dto.BookingRequest;
import com.ben.booking.dto.BookingResponse;
import com.ben.booking.repository.BookingRepository;
import com.ben.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        return bookingService.createBooking(request);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<BookingResponse>> getBooking(@PathVariable String id) {
        return bookingService.getBooking(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/health-db")
    public Mono<String> checkDbHealth() {
        return bookingRepository.count()
                .map(count -> "DB connected. Records: " + count)
                .onErrorReturn("DB connection failed");
    }
}