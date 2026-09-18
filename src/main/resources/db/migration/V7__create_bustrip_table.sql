CREATE TABLE bustrip (
    TripId INT IDENTITY(1,1) NOT NULL,
    TripDate DATE NOT NULL,
    StartTime TIME NOT NULL,
    ETA TIME DEFAULT NULL,
    PickupLocationId INT DEFAULT NULL,
    DropLocationId INT DEFAULT NULL,
    PRIMARY KEY (TripId),
    CONSTRAINT FK_BusTrip_PickupLocation
        FOREIGN KEY (PickupLocationId) REFERENCES location(LocationId),
    CONSTRAINT FK_BusTrip_DropLocation
        FOREIGN KEY (DropLocationId) REFERENCES location(LocationId)
);
INSERT INTO bustrip (TripDate, StartTime, ETA, PickupLocationId, DropLocationId) VALUES
('2026-09-20', '07:00:00', '08:15:00', 2, 1),
('2026-09-20', '08:00:00', '09:00:00', 3, 1),
('2026-09-21', '07:30:00', '08:45:00', 4, 1);
