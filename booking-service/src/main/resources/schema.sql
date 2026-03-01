CREATE TABLE IF NOT EXISTS bookings (
                                        id UUID PRIMARY KEY,
                                        hotel_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
    );