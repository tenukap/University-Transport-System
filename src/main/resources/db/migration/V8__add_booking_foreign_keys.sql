ALTER TABLE booking
    ADD CONSTRAINT fk_booking_trip
        FOREIGN KEY (trip_id) REFERENCES bustrip(TripId);
ALTER TABLE booking
    ADD CONSTRAINT fk_booking_pickup
        FOREIGN KEY (pickup_loc_id) REFERENCES location(LocationId);
ALTER TABLE booking
    ADD CONSTRAINT fk_booking_dropoff
        FOREIGN KEY (dropoff_loc_id) REFERENCES location(LocationId);
