CREATE TABLE seat_reservation (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,  -- UNIQUE enforces one-to-one with booking
    seat_id BIGINT NOT NULL,
    reserved_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT fk_reservation_booking
        FOREIGN KEY (booking_id) REFERENCES booking(id) ON DELETE CASCADE,
    CONSTRAINT fk_reservation_seat
        FOREIGN KEY (seat_id) REFERENCES seat(id)
);