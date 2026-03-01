package com.ben.booking.repository;

import com.ben.booking.entity.Booking;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface BookingRepository extends R2dbcRepository<Booking, UUID> {
}
