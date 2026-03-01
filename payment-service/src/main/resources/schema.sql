CREATE TABLE IF NOT EXISTS payments (
                                        id UUID PRIMARY KEY,
                                        booking_id UUID NOT NULL UNIQUE,
                                        amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL
    );