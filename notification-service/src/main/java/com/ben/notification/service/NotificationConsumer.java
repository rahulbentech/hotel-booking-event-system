package com.ben.notification.service;

import com.ben.notification.dto.BookingCreatedEvent;
import com.ben.notification.dto.PaymentProcessedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;

import jakarta.annotation.PostConstruct;

@SuppressWarnings("ALL")
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void consume() {
        kafkaReceiver.receive()
                .flatMap(this::processEvent)
                .doOnError(e -> log.error("Error consuming event", e))
                .retry()
                .subscribe();
    }

    private Mono<Void> processEvent(ReceiverRecord<String, String> record) {
        String topic = record.topic();
        String value = record.value();
        log.info("Received event from topic {}: {}", topic, value);

        try {
            if ("booking-events".equals(topic)) {
                BookingCreatedEvent event = objectMapper.readValue(value, BookingCreatedEvent.class);
                return handleBookingCreated(event, record);
            } else if ("payment-events".equals(topic)) {
                PaymentProcessedEvent event = objectMapper.readValue(value, PaymentProcessedEvent.class);
                return handlePaymentProcessed(event, record);
            } else {
                log.warn("Unknown topic: {}", topic);
                record.receiverOffset().acknowledge();
                return Mono.empty();
            }
        } catch (Exception e) {
            log.error("Failed to parse event from topic {}", topic, e);
            record.receiverOffset().acknowledge(); // or send to DLQ
            return Mono.empty();
        }
    }

    private Mono<Void> handleBookingCreated(BookingCreatedEvent event, ReceiverRecord<String, String> record) {
        // Simulate sending email/SMS
        log.info("Sending booking confirmation email to user {} for booking {} at hotel {}",
                event.getUserId(), event.getBookingId(), event.getHotelId());
        // Here you would call an external email/SMS service reactively
        record.receiverOffset().acknowledge();
        return Mono.empty();
    }

    private Mono<Void> handlePaymentProcessed(PaymentProcessedEvent event, ReceiverRecord<String, String> record) {
        if ("SUCCESS".equals(event.getStatus())) {
            log.info("Sending payment success notification for booking {}", event.getBookingId());
        } else {
            log.info("Sending payment failure notification for booking {}", event.getBookingId());
        }
        record.receiverOffset().acknowledge();
        return Mono.empty();
    }
}