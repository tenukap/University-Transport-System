-- ============================================================================
-- Admin schema — role/user management, transport, financial reporting.
-- Converted for Flyway from Doane A.J.J (IT25103292) Section 6.6 schema:
--   GO separators + USE statements removed; stored procedures and verification
--   SELECTs omitted (logic lives in Java). Each CREATE TABLE and each seed
--   INSERT is guarded so the migration is safe to re-run.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Role & user management
-- ---------------------------------------------------------------------------
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Roles')
BEGIN
    CREATE TABLE Roles (
        RoleId INT IDENTITY(1,1) PRIMARY KEY,
        RoleName VARCHAR(50) NOT NULL UNIQUE, -- 'Admin', 'FinanceOfficer', 'Student', 'Driver'
        Description VARCHAR(255) NULL
    )
END

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='StudentGroups')
BEGIN
    CREATE TABLE StudentGroups (
        GroupId INT IDENTITY(1,1) PRIMARY KEY,
        GroupName VARCHAR(100) NOT NULL UNIQUE, -- e.g., 'IT Batch 2026', 'Engineering Y2'
        Faculty VARCHAR(100) NULL
    )
END

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Users')
BEGIN
    CREATE TABLE Users (
        UserId INT IDENTITY(1,1) PRIMARY KEY,
        FullName VARCHAR(100) NOT NULL,
        Email VARCHAR(150) NOT NULL UNIQUE,
        PasswordHash VARCHAR(255) NOT NULL,
        Phone VARCHAR(20) NULL,
        RoleId INT NOT NULL,
        GroupId INT NULL, -- Applies to students
        AccountStatus VARCHAR(20) NOT NULL DEFAULT 'Active', -- 'Active', 'Suspended', 'Deactivated'
        CreatedAt DATETIME2 DEFAULT GETDATE(),
        UpdatedAt DATETIME2 DEFAULT GETDATE(),
        CONSTRAINT FK_Users_Roles FOREIGN KEY (RoleId) REFERENCES Roles(RoleId),
        CONSTRAINT FK_Users_Groups FOREIGN KEY (GroupId) REFERENCES StudentGroups(GroupId)
    )
END

-- ---------------------------------------------------------------------------
-- 2. Transport & trip management
-- ---------------------------------------------------------------------------
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Buses')
BEGIN
    CREATE TABLE Buses (
        BusId INT IDENTITY(1,1) PRIMARY KEY,
        BusNumber VARCHAR(50) NOT NULL UNIQUE, -- e.g., 'NC-5542'
        Capacity INT NOT NULL,
        Status VARCHAR(20) DEFAULT 'Active' -- 'Active', 'Maintenance', 'Inactive'
    )
END

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Routes')
BEGIN
    CREATE TABLE Routes (
        RouteId INT IDENTITY(1,1) PRIMARY KEY,
        RouteName VARCHAR(100) NOT NULL, -- e.g., 'Main Campus to City Express'
        StartLocation VARCHAR(100) NOT NULL,
        EndLocation VARCHAR(100) NOT NULL,
        BaseFare DECIMAL(10,2) NOT NULL DEFAULT 0.00
    )
END

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Trips')
BEGIN
    CREATE TABLE Trips (
        TripId INT IDENTITY(1,1) PRIMARY KEY,
        RouteId INT NOT NULL,
        BusId INT NOT NULL,
        DepartureTime DATETIME2 NOT NULL,
        ArrivalTime DATETIME2 NOT NULL,
        TripStatus VARCHAR(20) DEFAULT 'Scheduled', -- 'Scheduled', 'In-Transit', 'Completed', 'Cancelled'
        OperatingCost DECIMAL(10,2) NOT NULL DEFAULT 0.00, -- Financial metric
        CONSTRAINT FK_Trips_Routes FOREIGN KEY (RouteId) REFERENCES Routes(RouteId),
        CONSTRAINT FK_Trips_Buses FOREIGN KEY (BusId) REFERENCES Buses(BusId)
    )
END

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Bookings')
BEGIN
    CREATE TABLE Bookings (
        BookingId INT IDENTITY(1,1) PRIMARY KEY,
        TripId INT NOT NULL,
        UserId INT NOT NULL,
        BookingDate DATETIME2 DEFAULT GETDATE(),
        SeatNumber INT NULL,
        FareAmount DECIMAL(10,2) NOT NULL,
        BookingStatus VARCHAR(20) DEFAULT 'Confirmed', -- 'Confirmed', 'Cancelled', 'Completed'
        CONSTRAINT FK_Bookings_Trips FOREIGN KEY (TripId) REFERENCES Trips(TripId),
        CONSTRAINT FK_Bookings_Users FOREIGN KEY (UserId) REFERENCES Users(UserId)
    )
END

-- ---------------------------------------------------------------------------
-- 3. Financial reports, feedback & announcements
-- ---------------------------------------------------------------------------
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='SavedReports')
BEGIN
    CREATE TABLE SavedReports (
        ReportId INT IDENTITY(1,1) PRIMARY KEY,
        ReportTitle VARCHAR(150) NOT NULL,
        ReportType VARCHAR(50) NOT NULL, -- 'TransportUsage', 'CostCalculation', 'BookingVolume'
        StartDate DATE NOT NULL,
        EndDate DATE NOT NULL,
        GeneratedByUserId INT NOT NULL,
        SummaryNotes NVARCHAR(MAX) NULL,
        GeneratedAt DATETIME2 DEFAULT GETDATE(),
        CONSTRAINT FK_Reports_Users FOREIGN KEY (GeneratedByUserId) REFERENCES Users(UserId)
    )
END

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Announcements')
BEGIN
    CREATE TABLE Announcements (
        AnnouncementId INT IDENTITY(1,1) PRIMARY KEY,
        Title VARCHAR(200) NOT NULL,
        Content NVARCHAR(MAX) NOT NULL,
        PostedByUserId INT NOT NULL,
        CreatedAt DATETIME2 DEFAULT GETDATE(),
        CONSTRAINT FK_Announcements_Users FOREIGN KEY (PostedByUserId) REFERENCES Users(UserId)
    )
END

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Feedback')
BEGIN
    CREATE TABLE Feedback (
        FeedbackId INT IDENTITY(1,1) PRIMARY KEY,
        UserId INT NOT NULL,
        Subject VARCHAR(150) NOT NULL,
        Message NVARCHAR(MAX) NOT NULL,
        Status VARCHAR(20) DEFAULT 'Pending', -- 'Pending', 'Reviewed', 'Resolved'
        SubmittedAt DATETIME2 DEFAULT GETDATE(),
        CONSTRAINT FK_Feedback_Users FOREIGN KEY (UserId) REFERENCES Users(UserId)
    )
END

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='AdminAuditLogs')
BEGIN
    CREATE TABLE AdminAuditLogs (
        LogId INT IDENTITY(1,1) PRIMARY KEY,
        AdminUserId INT NOT NULL,
        ActionPerformed VARCHAR(100) NOT NULL,
        TargetUserId INT NULL,
        Timestamp DATETIME2 DEFAULT GETDATE(),
        CONSTRAINT FK_Audit_Admin FOREIGN KEY (AdminUserId) REFERENCES Users(UserId)
    )
END

-- ---------------------------------------------------------------------------
-- 4. Seed sample data (guarded so re-running does not duplicate rows)
-- ---------------------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM Roles)
BEGIN
    INSERT INTO Roles (RoleName, Description) VALUES
    ('Admin', 'Full administrative access across all modules'),
    ('FinanceOfficer', 'Access to financial insights, trip cost calculations, and reports'),
    ('Student', 'General user making bookings'),
    ('Driver', 'Bus operator');
END

IF NOT EXISTS (SELECT 1 FROM StudentGroups)
BEGIN
    INSERT INTO StudentGroups (GroupName, Faculty) VALUES
    ('IT Batch 2026', 'Faculty of Computing'),
    ('Business Admin Y1', 'Faculty of Management');
END

IF NOT EXISTS (SELECT 1 FROM Users)
BEGIN
    INSERT INTO Users (FullName, Email, PasswordHash, Phone, RoleId, GroupId, AccountStatus) VALUES
    ('Doane Admin', 'admin@campus.edu', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMueqIleBne9T1wXpFc5ZNJSK2', '0771234567', 1, NULL, 'Active'),
    ('Sarah Finance', 'finance@campus.edu', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMueqIleBne9T1wXpFc5ZNJSK2', '0777654321', 2, NULL, 'Active'),
    ('Alex Smith', 'alex@student.campus.edu', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMueqIleBne9T1wXpFc5ZNJSK2', '0711112222', 3, 1, 'Active'),
    ('Jane Doe', 'jane@student.campus.edu', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMueqIleBne9T1wXpFc5ZNJSK2', '0722223333', 3, 2, 'Suspended');
END

IF NOT EXISTS (SELECT 1 FROM Buses)
BEGIN
    INSERT INTO Buses (BusNumber, Capacity, Status) VALUES ('NA-1001', 50, 'Active'), ('NA-2002', 40, 'Active');
END

IF NOT EXISTS (SELECT 1 FROM Routes)
BEGIN
    INSERT INTO Routes (RouteName, StartLocation, EndLocation, BaseFare) VALUES
    ('Route A - Express', 'Main Gate', 'City Center', 150.00),
    ('Route B - Hostel Shuttle', 'Hostel Complex', 'Faculty Quad', 50.00);
END

IF NOT EXISTS (SELECT 1 FROM Trips)
BEGIN
    INSERT INTO Trips (RouteId, BusId, DepartureTime, ArrivalTime, TripStatus, OperatingCost)
    VALUES (1, 1, DATEADD(hour, 2, GETDATE()), DATEADD(hour, 3, GETDATE()), 'Scheduled', 2500.00);
END

IF NOT EXISTS (SELECT 1 FROM Bookings)
BEGIN
    INSERT INTO Bookings (TripId, UserId, SeatNumber, FareAmount, BookingStatus)
    VALUES (1, 3, 12, 150.00, 'Confirmed');
END

IF NOT EXISTS (SELECT 1 FROM Announcements)
BEGIN
    INSERT INTO Announcements (Title, Content, PostedByUserId)
    VALUES ('New Bus Route Added', 'Hostel Shuttle schedule has been updated for exam season.', 1);
END

IF NOT EXISTS (SELECT 1 FROM Feedback)
BEGIN
    INSERT INTO Feedback (UserId, Subject, Message, Status)
    VALUES (3, 'AC Issue', 'Bus NA-1001 AC was not working during morning trip.', 'Pending');
END
