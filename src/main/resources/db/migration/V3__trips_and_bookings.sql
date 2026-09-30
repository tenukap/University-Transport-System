-- V3__trips_and_bookings.sql
-- bustrip, booking, seat_reservation, Trip_Status, Location_Update, tripcancellation.
-- Depends on: V1 (Users, student, driver), V2 (location, bus).

-- ============================================================
-- 1. bustrip
--    Added: driver_user_id INT NULL → driver(user_id).
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'bustrip')
BEGIN
    CREATE TABLE bustrip (
        TripId           INT IDENTITY NOT NULL,
        bus_id           BIGINT NOT NULL,
        driver_user_id   INT NULL,
        TripDate         DATE NOT NULL,
        StartTime        TIME NOT NULL,
        ETA              TIME NOT NULL,
        PickupLocationId INT NULL,
        DropLocationId   INT NULL,
        TripStatus       VARCHAR(20) NULL CONSTRAINT DF_bustrip_TripStatus DEFAULT 'Scheduled',
        OperatingCost    DECIMAL(10,2) NULL CONSTRAINT DF_bustrip_OperatingCost DEFAULT 0.00,
        CONSTRAINT PK_bustrip PRIMARY KEY (TripId),
        CONSTRAINT FK_bustrip_bus FOREIGN KEY (bus_id)
            REFERENCES bus(bus_id),
        CONSTRAINT FK_bustrip_driver FOREIGN KEY (driver_user_id)
            REFERENCES driver(user_id),
        CONSTRAINT FK_bustrip_pickup FOREIGN KEY (PickupLocationId)
            REFERENCES location(LocationId),
        CONSTRAINT FK_bustrip_drop FOREIGN KEY (DropLocationId)
            REFERENCES location(LocationId)
    );
END;

-- Index: bustrip.bus_id
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_bustrip_bus_id'
               AND object_id = OBJECT_ID('bustrip'))
    CREATE NONCLUSTERED INDEX IX_bustrip_bus_id ON bustrip(bus_id);

-- Index: bustrip.driver_user_id
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_bustrip_driver_user_id'
               AND object_id = OBJECT_ID('bustrip'))
    CREATE NONCLUSTERED INDEX IX_bustrip_driver_user_id ON bustrip(driver_user_id);

-- ============================================================
-- 2. booking
--    user_id FK → student(user_id) (no ON DELETE rule).
--    Filtered unique index on (trip_id, seat_number) for non-cancelled active rows.
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'booking')
BEGIN
    CREATE TABLE booking (
        id             BIGINT IDENTITY NOT NULL,
        user_id        INT NOT NULL,
        trip_id        INT NOT NULL,
        pickup_loc_id  INT NULL,
        dropoff_loc_id INT NULL,
        seat_number    INT NULL,
        fare_amount    DECIMAL(10,2) NULL,
        status         VARCHAR(20) NULL CONSTRAINT DF_booking_status DEFAULT 'PENDING',
        created_at     DATETIME2 NULL CONSTRAINT DF_booking_created_at DEFAULT GETDATE(),
        CONSTRAINT PK_booking PRIMARY KEY (id),
        CONSTRAINT FK_booking_student FOREIGN KEY (user_id)
            REFERENCES student(user_id),
        CONSTRAINT FK_booking_trip FOREIGN KEY (trip_id)
            REFERENCES bustrip(TripId),
        CONSTRAINT FK_booking_pickup FOREIGN KEY (pickup_loc_id)
            REFERENCES location(LocationId),
        CONSTRAINT FK_booking_dropoff FOREIGN KEY (dropoff_loc_id)
            REFERENCES location(LocationId)
    );
END;

-- Index: booking.user_id
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_booking_user_id'
               AND object_id = OBJECT_ID('booking'))
    CREATE NONCLUSTERED INDEX IX_booking_user_id ON booking(user_id);

-- Index: booking.trip_id
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_booking_trip_id'
               AND object_id = OBJECT_ID('booking'))
    CREATE NONCLUSTERED INDEX IX_booking_trip_id ON booking(trip_id);

-- Filtered unique index: prevent double-booking of the same seat on the same trip
-- (only active, non-cancelled bookings where a seat is assigned count).
-- NOTE: the literal must be VARCHAR (no N prefix) to match booking.status.
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'UQ_booking_trip_seat_active'
               AND object_id = OBJECT_ID('booking'))
    CREATE UNIQUE NONCLUSTERED INDEX UQ_booking_trip_seat_active
        ON booking(trip_id, seat_number)
        WHERE status <> 'CANCELLED' AND seat_number IS NOT NULL;

-- ============================================================
-- 3. seat_reservation
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'seat_reservation')
BEGIN
    CREATE TABLE seat_reservation (
        id          BIGINT IDENTITY NOT NULL,
        booking_id  BIGINT NOT NULL,
        bus_id      BIGINT NOT NULL,
        seat_number INT NOT NULL,
        reserved_at DATETIME2 NULL CONSTRAINT DF_seatres_reserved_at DEFAULT GETDATE(),
        CONSTRAINT PK_seat_reservation PRIMARY KEY (id),
        CONSTRAINT FK_seatres_booking FOREIGN KEY (booking_id)
            REFERENCES booking(id),
        CONSTRAINT FK_seatres_bus FOREIGN KEY (bus_id)
            REFERENCES bus(bus_id)
    );
END;

-- Index: seat_reservation.booking_id
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_seatres_booking_id'
               AND object_id = OBJECT_ID('seat_reservation'))
    CREATE NONCLUSTERED INDEX IX_seatres_booking_id ON seat_reservation(booking_id);

-- ============================================================
-- 4. Trip_Status
--    Status_Type VARCHAR(100) NOT NULL (source-of-truth: old V5).
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Trip_Status')
BEGIN
    CREATE TABLE Trip_Status (
        Status_Id   INT IDENTITY(1,1) NOT NULL,
        TripId      INT NOT NULL,
        Status_Type VARCHAR(100) NOT NULL,
        Updated_At  DATETIME NULL CONSTRAINT DF_TripStatus_Updated_At DEFAULT GETDATE(),
        CONSTRAINT PK_Trip_Status PRIMARY KEY (Status_Id),
        CONSTRAINT FK_TripStatus_Trip FOREIGN KEY (TripId)
            REFERENCES bustrip(TripId)
    );
END;

-- Index: Trip_Status.TripId
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_TripStatus_TripId'
               AND object_id = OBJECT_ID('Trip_Status'))
    CREATE NONCLUSTERED INDEX IX_TripStatus_TripId ON Trip_Status(TripId);

-- ============================================================
-- 5. Location_Update
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Location_Update')
BEGIN
    CREATE TABLE Location_Update (
        Location_Update_Id INT IDENTITY(1,1) NOT NULL,
        TripId             INT NOT NULL,
        Latitude           DECIMAL(10,8) NOT NULL,
        Longitude          DECIMAL(11,8) NOT NULL,
        Recorded_At        DATETIME NULL CONSTRAINT DF_LocUpdate_Recorded_At DEFAULT GETDATE(),
        CONSTRAINT PK_Location_Update PRIMARY KEY (Location_Update_Id),
        CONSTRAINT FK_LocationUpdate_Trip FOREIGN KEY (TripId)
            REFERENCES bustrip(TripId)
    );
END;

-- Index: Location_Update.TripId
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_LocUpdate_TripId'
               AND object_id = OBJECT_ID('Location_Update'))
    CREATE NONCLUSTERED INDEX IX_LocUpdate_TripId ON Location_Update(TripId);

-- ============================================================
-- 6. tripcancellation
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'tripcancellation')
BEGIN
    CREATE TABLE tripcancellation (
        CancellationId INT IDENTITY(1,1) NOT NULL,
        TripId         INT NOT NULL,
        Reason         NVARCHAR(255) NULL,
        CancelledAt    DATETIME NULL CONSTRAINT DF_tripcancellation_CancelledAt DEFAULT GETDATE(),
        CONSTRAINT PK_tripcancellation PRIMARY KEY (CancellationId),
        CONSTRAINT FK_tripcancellation_bustrip FOREIGN KEY (TripId)
            REFERENCES bustrip(TripId)
    );
END;

-- Index: tripcancellation.TripId
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_tripcancellation_TripId'
               AND object_id = OBJECT_ID('tripcancellation'))
    CREATE NONCLUSTERED INDEX IX_tripcancellation_TripId ON tripcancellation(TripId);