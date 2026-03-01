package com.ben.payment.service;

import com.ben.payment.dto.BookingCreatedEvent;
import com.ben.payment.dto.PaymentProcessedEvent;
import com.ben.payment.entity.Payment;
import com.ben.payment.entity.PaymentStatus;
import com.ben.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentProcessor {

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final KafkaSender<String, String> kafkaSender;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void startProcessing() {
        kafkaReceiver.receive()
                .flatMap(this::processBookingEvent)
                .doOnError(e -> log.error("Error processing booking event", e))
                .retry()
                .subscribe();
    }

    private Mono<Void> processBookingEvent(ReceiverRecord<String, String> record) {
        String value = record.value();
        log.info("Received booking event: {}", value);
        try {
            BookingCreatedEvent event = objectMapper.readValue(value, BookingCreatedEvent.class);
            UUID bookingId = event.getBookingId();

            // Check if already processed (idempotency)
            return paymentRepository.findByBookingId(bookingId)
                    .flatMap(existing -> {
                        log.info("Payment already exists for booking {}, skipping", bookingId);
                        record.receiverOffset().acknowledge();
                        return Mono.empty();
                    })
                    .switchIfEmpty(processPayment(event, record))
                    .then();

        } catch (Exception e) {
            log.error("Failed to parse booking event", e);
            record.receiverOffset().acknowledge(); // or send to DLQ
            return Mono.empty();
        }
    }

    private Mono<Void> processPayment(BookingCreatedEvent event, ReceiverRecord<String, String> record) {
        // Simulate payment processing (random success/failure, delay)
        return Mono.fromCallable(() -> {
                    Thread.sleep((long) (Math.random() * 100)); // simulate work
                    return Math.random() > 0.2 ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
                })
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .flatMap(status -> {
                    Payment payment = Payment.builder()
                            .id(UUID.randomUUID())
                            .bookingId(event.getBookingId())
                            .amount(event.getAmount())
                            .status(status)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return paymentRepository.save(payment)
                            .flatMap(saved -> publishPaymentProcessed(event.getBookingId(), status, record));
                });
    }

    private Mono<Void> publishPaymentProcessed(UUID bookingId, PaymentStatus status, ReceiverRecord<String, String> record) {
        PaymentProcessedEvent event = new PaymentProcessedEvent(bookingId, status.name());
        try {
            String value = objectMapper.writeValueAsString(event);
            return kafkaSender.send(Mono.just(SenderRecord.create("payment-events", null, null,
                            bookingId.toString(), value, null)))
                    .doOnNext(result -> {
                        record.receiverOffset().acknowledge();
                        log.info("Published payment event for booking {} with status {}", bookingId, status);
                    })
                    .then();
        } catch (Exception e) {
            log.error("Failed to serialize payment event", e);
            return Mono.error(e);
        }
    }
}