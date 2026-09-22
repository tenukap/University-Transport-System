-- ============================================================================
-- CAMPUS TRANSPORT MANAGEMENT SYSTEM - SECTION 6.6 DATABASE SCHEMA
-- Student: Doane A.J.J (IT25103292)
-- Role Focus: Administrative Dashboard & Financial Reporting
-- ============================================================================

USE CampusTransportDB;
GO

-- Clean up existing tables if re-running (Optional)
-- DROP TABLE IF EXISTS AdminAuditLogs, SavedReports, Feedback, Announcements, Bookings, Trips, Routes, Buses, Users, StudentGroups, Roles;

-- ============================================================================
-- 1. ROLE & USER MANAGEMENT TABLES
-- ============================================================================

CREATE TABLE Roles (
    RoleId INT IDENTITY(1,1) PRIMARY KEY,
    RoleName VARCHAR(50) NOT NULL UNIQUE, -- 'Admin', 'FinanceOfficer', 'Student', 'Driver'
    Description VARCHAR(255) NULL
);
GO

CREATE TABLE StudentGroups (
    GroupId INT IDENTITY(1,1) PRIMARY KEY,
    GroupName VARCHAR(100) NOT NULL UNIQUE, -- e.g., 'IT Batch 2026', 'Engineering Y2'
    Faculty VARCHAR(100) NULL
);
GO

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
);
GO

-- ============================================================================
-- 2. TRANSPORT & TRIP MANAGEMENT TABLES
-- ============================================================================

CREATE TABLE Buses (
    BusId INT IDENTITY(1,1) PRIMARY KEY,
    BusNumber VARCHAR(50) NOT NULL UNIQUE, -- e.g., 'NC-5542'
    Capacity INT NOT NULL,
    Status VARCHAR(20) DEFAULT 'Active' -- 'Active', 'Maintenance', 'Inactive'
);
GO

CREATE TABLE Routes (
    RouteId INT IDENTITY(1,1) PRIMARY KEY,
    RouteName VARCHAR(100) NOT NULL, -- e.g., 'Main Campus to City Express'
    StartLocation VARCHAR(100) NOT NULL,
    EndLocation VARCHAR(100) NOT NULL,
    BaseFare DECIMAL(10,2) NOT NULL DEFAULT 0.00
);
GO

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
);
GO

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
);
GO

-- ============================================================================
-- 3. FINANCIAL REPORTS, FEEDBACK & ANNOUNCEMENTS TABLES
-- ============================================================================

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
);
GO

CREATE TABLE Announcements (
    AnnouncementId INT IDENTITY(1,1) PRIMARY KEY,
    Title VARCHAR(200) NOT NULL,
    Content NVARCHAR(MAX) NOT NULL,
    PostedByUserId INT NOT NULL,
    CreatedAt DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT FK_Announcements_Users FOREIGN KEY (PostedByUserId) REFERENCES Users(UserId)
);
GO

CREATE TABLE Feedback (
    FeedbackId INT IDENTITY(1,1) PRIMARY KEY,
    UserId INT NOT NULL,
    Subject VARCHAR(150) NOT NULL,
    Message NVARCHAR(MAX) NOT NULL,
    Status VARCHAR(20) DEFAULT 'Pending', -- 'Pending', 'Reviewed', 'Resolved'
    SubmittedAt DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT FK_Feedback_Users FOREIGN KEY (UserId) REFERENCES Users(UserId)
);
GO

-- Audit log for Admin actions (Creating, updating, suspending accounts)
CREATE TABLE AdminAuditLogs (
    LogId INT IDENTITY(1,1) PRIMARY KEY,
    AdminUserId INT NOT NULL,
    ActionPerformed VARCHAR(100) NOT NULL,
    TargetUserId INT NULL,
    Timestamp DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT FK_Audit_Admin FOREIGN KEY (AdminUserId) REFERENCES Users(UserId)
);
GO

-- ============================================================================
-- 4. SEED SAMPLE DATA (For Project Presentation & Testing)
-- ============================================================================

-- Roles
INSERT INTO Roles (RoleName, Description) VALUES 
('Admin', 'Full administrative access across all modules'),
('FinanceOfficer', 'Access to financial insights, trip cost calculations, and reports'),
('Student', 'General user making bookings'),
('Driver', 'Bus operator');

-- Groups
INSERT INTO StudentGroups (GroupName, Faculty) VALUES 
('IT Batch 2026', 'Faculty of Computing'),
('Business Admin Y1', 'Faculty of Management');

-- Users (1 Admin, 1 Finance Officer, 2 Students)
INSERT INTO Users (FullName, Email, PasswordHash, Phone, RoleId, GroupId, AccountStatus) VALUES 
('Doane Admin', 'admin@campus.edu', '$2a$12$eImiTXuWVxfM37uY4...hash', '0771234567', 1, NULL, 'Active'),
('Sarah Finance', 'finance@campus.edu', '$2a$12$eImiTXuWVxfM37uY4...hash', '0777654321', 2, NULL, 'Active'),
('Alex Smith', 'alex@student.campus.edu', '$2a$12$eImiTXuWVxfM37uY4...hash', '0711112222', 3, 1, 'Active'),
('Jane Doe', 'jane@student.campus.edu', '$2a$12$eImiTXuWVxfM37uY4...hash', '0722223333', 3, 2, 'Suspended');

-- Buses & Routes
INSERT INTO Buses (BusNumber, Capacity, Status) VALUES ('NA-1001', 50, 'Active'), ('NA-2002', 40, 'Active');
INSERT INTO Routes (RouteName, StartLocation, EndLocation, BaseFare) VALUES 
('Route A - Express', 'Main Gate', 'City Center', 150.00),
('Route B - Hostel Shuttle', 'Hostel Complex', 'Faculty Quad', 50.00);

-- Trips & Bookings
INSERT INTO Trips (RouteId, BusId, DepartureTime, ArrivalTime, TripStatus, OperatingCost) 
VALUES (1, 1, DATEADD(hour, 2, GETDATE()), DATEADD(hour, 3, GETDATE()), 'Scheduled', 2500.00);

INSERT INTO Bookings (TripId, UserId, SeatNumber, FareAmount, BookingStatus) 
VALUES (1, 3, 12, 150.00, 'Confirmed');

-- Sample Announcement & Feedback
INSERT INTO Announcements (Title, Content, PostedByUserId) 
VALUES ('New Bus Route Added', 'Hostel Shuttle schedule has been updated for exam season.', 1);

INSERT INTO Feedback (UserId, Subject, Message, Status) 
VALUES (3, 'AC Issue', 'Bus NA-1001 AC was not working during morning trip.', 'Pending');
GO
USE CampusTransportDB;
GO

-- 1. Fetch Key Metrics for Admin & Finance Dashboard
CREATE PROCEDURE sp_GetDashboardMetrics
AS
BEGIN
    SELECT 
        (SELECT COUNT(*) FROM Bookings WHERE BookingStatus = 'Confirmed') AS TotalActiveBookings,
        (SELECT COUNT(*) FROM Trips WHERE DepartureTime > GETDATE() AND TripStatus = 'Scheduled') AS UpcomingTrips,
        (SELECT COUNT(*) FROM Users WHERE AccountStatus = 'Active') AS TotalActiveUsers,
        (SELECT ISNULL(SUM(FareAmount), 0) FROM Bookings WHERE BookingStatus = 'Confirmed') AS TotalRevenue;
END;
GO

-- 2. Financial Summary Report Procedure (Used by Finance Officer)
CREATE PROCEDURE sp_GetFinancialReport
    @StartDate DATE,
    @EndDate DATE
AS
BEGIN
    SELECT 
        R.RouteName,
        COUNT(Bk.BookingId) AS TotalBookings,
        SUM(Bk.FareAmount) AS TotalRevenue,
        SUM(T.OperatingCost) AS TotalOperatingCost,
        (SUM(Bk.FareAmount) - SUM(T.OperatingCost)) AS NetProfitLoss
    FROM Bookings Bk
    JOIN Trips T ON Bk.TripId = T.TripId
    JOIN Routes R ON T.RouteId = R.RouteId
    WHERE CAST(Bk.BookingDate AS DATE) BETWEEN @StartDate AND @EndDate
    GROUP BY R.RouteName;
END;
GO
USE CampusTransportDB;
GO

-- View Roles & Groups
SELECT * FROM Roles;
SELECT * FROM StudentGroups;

-- View Users with joined Role & Group names
SELECT 
    U.UserId, 
    U.FullName, 
    U.Email, 
    R.RoleName, 
    G.GroupName, 
    U.AccountStatus 
FROM Users U
LEFT JOIN Roles R ON U.RoleId = R.RoleId
LEFT JOIN StudentGroups G ON U.GroupId = G.GroupId;

