CREATE TABLE seat_reservation (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    bus_id BIGINT NOT NULL,
    seat_number INT NOT NULL,
    reserved_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT fk_reservation_booking
        FOREIGN KEY (booking_id) REFERENCES booking(id) ON DELETE CASCADE,
    CONSTRAINT fk_reservation_bus
        FOREIGN KEY (bus_id) REFERENCES bus(bus_id)
);
