package com.ben.payment.repository;

import com.ben.payment.entity.Payment;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@SuppressWarnings("ALL")
@Repository
public interface PaymentRepository extends R2dbcRepository<Payment, UUID> {
    Mono<Payment> findByBookingId(UUID bookingId);
}