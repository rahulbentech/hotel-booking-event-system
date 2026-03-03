package com.ben.booking.service;

import com.ben.booking.entity.BookingStatus;
import com.ben.booking.repository.BookingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final BookingRepository bookingRepository;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void consume() {
        log.info("PaymentEventConsumer started, subscribing to payment-events");
        kafkaReceiver.receive()
                .flatMap(this::processPaymentEvent)
                .doOnError(e -> log.error("Error consuming payment event", e))
                .retry()
                .subscribe();
    }

    private Mono<Void> processPaymentEvent(ReceiverRecord<String, String> record) {
        String value = record.value();
        log.info("Received payment event: {}", value);
        try {
            JsonNode json = objectMapper.readTree(value);
            String bookingId = json.get("bookingId").asText();
            String statusStr = json.get("status").asText(); // "SUCCESS" or "FAILED"
            BookingStatus newStatus = "SUCCESS".equals(statusStr) ? BookingStatus.CONFIRMED : BookingStatus.FAILED;

            return bookingRepository.findById(bookingId)
                    .flatMap(booking -> {
                        booking.setStatus(newStatus);
                        booking.setUpdatedAt(LocalDateTime.now());
                        return bookingRepository.save(booking);
                    })
                    .doOnSuccess(b -> {
                        record.receiverOffset().acknowledge();
                        log.info("Updated booking {} to {}", bookingId, newStatus);
                    })
                    .doOnError(e -> log.error("Failed to update booking {}", bookingId, e))
                    .then();
        } catch (Exception e) {
            log.error("Failed to parse payment event", e);
            record.receiverOffset().acknowledge(); // avoid poison pill; better send to DLQ
            return Mono.empty();
        }
    }
}