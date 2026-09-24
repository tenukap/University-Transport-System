-- V2__schema.sql
-- Clean consolidated schema (SQL Server / T-SQL).
-- Tables are created in foreign-key dependency order.
-- Role is stored as a string in Users.RoleName (no separate Roles table).
-- Every FK to Users(UserId) uses ON DELETE CASCADE.

-- 1. Users
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Users')
BEGIN
    CREATE TABLE Users (
        UserId        INT IDENTITY PRIMARY KEY,
        FullName      VARCHAR(100) NOT NULL,
        Email         VARCHAR(150) NOT NULL UNIQUE,
        PasswordHash  VARCHAR(255) NOT NULL,
        Phone         VARCHAR(20) NULL,
        RoleName      VARCHAR(50) NOT NULL,
        AccountStatus VARCHAR(20) DEFAULT 'Active',
        CreatedAt     DATETIME2 DEFAULT GETDATE(),
        UpdatedAt     DATETIME2 DEFAULT GETDATE()
    );
END;

-- 2. student
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'student')
BEGIN
    CREATE TABLE student (
        id            BIGINT IDENTITY PRIMARY KEY,
        user_id       INT NOT NULL UNIQUE,
        student_index VARCHAR(50) NOT NULL UNIQUE,
        full_name     VARCHAR(100) NOT NULL,
        phone         VARCHAR(20) NULL,
        CONSTRAINT FK_student_user FOREIGN KEY (user_id)
            REFERENCES Users(UserId) ON DELETE CASCADE
    );
END;

-- 3. location
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'location')
BEGIN
    CREATE TABLE location (
        LocationId   INT IDENTITY PRIMARY KEY,
        LocationName VARCHAR(100) NOT NULL,
        Latitude     DECIMAL(9,6) NULL,
        Longitude    DECIMAL(9,6) NULL
    );
END;

-- 4. bus
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'bus')
BEGIN
    CREATE TABLE bus (
        bus_id              BIGINT IDENTITY PRIMARY KEY,
        registration_number VARCHAR(50) NOT NULL UNIQUE,
        passenger_capacity  INT NOT NULL,
        status              VARCHAR(20) DEFAULT 'Active'
    );
END;

-- 5. bustrip
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'bustrip')
BEGIN
    CREATE TABLE bustrip (
        TripId           INT IDENTITY PRIMARY KEY,
        bus_id           BIGINT NOT NULL,
        TripDate         DATE NOT NULL,
        StartTime        TIME NOT NULL,
        ETA              TIME NOT NULL,
        PickupLocationId INT NULL,
        DropLocationId   INT NULL,
        TripStatus       VARCHAR(20) DEFAULT 'Scheduled',
        OperatingCost    DECIMAL(10,2) DEFAULT 0.00,
        CONSTRAINT FK_bustrip_bus FOREIGN KEY (bus_id)
            REFERENCES bus(bus_id),
        CONSTRAINT FK_bustrip_pickup FOREIGN KEY (PickupLocationId)
            REFERENCES location(LocationId),
        CONSTRAINT FK_bustrip_drop FOREIGN KEY (DropLocationId)
            REFERENCES location(LocationId)
    );
END;

-- 6. booking
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'booking')
BEGIN
    CREATE TABLE booking (
        id             BIGINT IDENTITY PRIMARY KEY,
        user_id        INT NOT NULL,
        trip_id        INT NOT NULL,
        pickup_loc_id  INT NULL,
        dropoff_loc_id INT NULL,
        seat_number    INT NULL,
        fare_amount    DECIMAL(10,2) NULL,
        status         VARCHAR(20) DEFAULT 'PENDING',
        created_at     DATETIME2 DEFAULT GETDATE(),
        CONSTRAINT FK_booking_user FOREIGN KEY (user_id)
            REFERENCES Users(UserId) ON DELETE CASCADE,
        CONSTRAINT FK_booking_trip FOREIGN KEY (trip_id)
            REFERENCES bustrip(TripId),
        CONSTRAINT FK_booking_pickup FOREIGN KEY (pickup_loc_id)
            REFERENCES location(LocationId),
        CONSTRAINT FK_booking_dropoff FOREIGN KEY (dropoff_loc_id)
            REFERENCES location(LocationId)
    );
END;

-- 7. seat_reservation
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'seat_reservation')
BEGIN
    CREATE TABLE seat_reservation (
        id          BIGINT IDENTITY PRIMARY KEY,
        booking_id  BIGINT NOT NULL,
        bus_id      BIGINT NOT NULL,
        seat_number INT NOT NULL,
        reserved_at DATETIME2 DEFAULT GETDATE(),
        CONSTRAINT FK_seatres_booking FOREIGN KEY (booking_id)
            REFERENCES booking(id),
        CONSTRAINT FK_seatres_bus FOREIGN KEY (bus_id)
            REFERENCES bus(bus_id)
    );
END;

-- 8. SavedReports
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'SavedReports')
BEGIN
    CREATE TABLE SavedReports (
        ReportId          INT IDENTITY PRIMARY KEY,
        ReportTitle       VARCHAR(150) NOT NULL,
        ReportType        VARCHAR(50) NOT NULL,
        StartDate         DATE NOT NULL,
        EndDate           DATE NOT NULL,
        GeneratedByUserId INT NOT NULL,
        SummaryNotes      NVARCHAR(MAX) NULL,
        GeneratedAt       DATETIME2 DEFAULT GETDATE(),
        CONSTRAINT FK_savedreports_user FOREIGN KEY (GeneratedByUserId)
            REFERENCES Users(UserId) ON DELETE CASCADE
    );
END;

-- 9. Announcements
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Announcements')
BEGIN
    CREATE TABLE Announcements (
        AnnouncementId INT IDENTITY PRIMARY KEY,
        Title          VARCHAR(200) NOT NULL,
        Content        NVARCHAR(MAX) NOT NULL,
        PostedByUserId INT NOT NULL,
        CreatedAt      DATETIME2 DEFAULT GETDATE(),
        CONSTRAINT FK_announcements_user FOREIGN KEY (PostedByUserId)
            REFERENCES Users(UserId) ON DELETE CASCADE
    );
END;

-- 10. Feedback
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Feedback')
BEGIN
    CREATE TABLE Feedback (
        FeedbackId  INT IDENTITY PRIMARY KEY,
        UserId      INT NOT NULL,
        Subject     VARCHAR(150) NOT NULL,
        Message     NVARCHAR(MAX) NOT NULL,
        Status      VARCHAR(20) DEFAULT 'Pending',
        SubmittedAt DATETIME2 DEFAULT GETDATE(),
        CONSTRAINT FK_feedback_user FOREIGN KEY (UserId)
            REFERENCES Users(UserId) ON DELETE CASCADE
    );
END;

-- 11. AdminAuditLogs
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'AdminAuditLogs')
BEGIN
    CREATE TABLE AdminAuditLogs (
        LogId           INT IDENTITY PRIMARY KEY,
        AdminUserId     INT NOT NULL,
        ActionPerformed VARCHAR(100) NOT NULL,
        TargetUserId    INT NULL,
        Timestamp       DATETIME2 DEFAULT GETDATE(),
        CONSTRAINT FK_auditlogs_user FOREIGN KEY (AdminUserId)
            REFERENCES Users(UserId) ON DELETE CASCADE
    );
END;
