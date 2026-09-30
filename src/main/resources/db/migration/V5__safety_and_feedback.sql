-- V5__safety_and_feedback.sql
-- Emergency_Report, Crash_Incident, Feedback.
-- Depends on: V1 (Users, driver), V2 (bus), V3 (booking).

-- ============================================================
-- 1. Emergency_Report
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Emergency_Report')
BEGIN
    CREATE TABLE Emergency_Report (
        Report_ID         INT IDENTITY(1,1) NOT NULL,
        Report_Title      VARCHAR(255) NOT NULL,
        Emergency_Type    VARCHAR(100) NOT NULL,
        Description       NVARCHAR(MAX) NOT NULL,
        Timestamp         DATETIME NULL CONSTRAINT DF_EmgReport_Timestamp DEFAULT GETDATE(),
        Resolution_Status VARCHAR(50) NULL CONSTRAINT DF_EmgReport_Status DEFAULT 'Pending',
        user_id           INT NULL,
        CONSTRAINT PK_Emergency_Report PRIMARY KEY (Report_ID),
        CONSTRAINT FK_Emergency_User FOREIGN KEY (user_id)
            REFERENCES Users(UserId) ON DELETE CASCADE
    );
END;

-- Index: Emergency_Report.user_id
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_EmgReport_user_id'
               AND object_id = OBJECT_ID('Emergency_Report'))
    CREATE NONCLUSTERED INDEX IX_EmgReport_user_id ON Emergency_Report(user_id);

-- ============================================================
-- 2. Crash_Incident
--    Column name is driver_user_id (not user_id).
--    FK → driver(user_id) (not Users).
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Crash_Incident')
BEGIN
    CREATE TABLE Crash_Incident (
        Incident_ID          INT IDENTITY(1,1) NOT NULL,
        bus_id               BIGINT NULL,
        driver_user_id       INT NULL,
        Location_Coordinates VARCHAR(255) NOT NULL,
        Severity_Level       VARCHAR(50) NOT NULL,
        Description          NVARCHAR(MAX) NULL,
        Timestamp            DATETIME NULL CONSTRAINT DF_CrashInc_Timestamp DEFAULT GETDATE(),
        Status               VARCHAR(50) NULL CONSTRAINT DF_CrashInc_Status DEFAULT 'Reported',
        CONSTRAINT PK_Crash_Incident PRIMARY KEY (Incident_ID),
        CONSTRAINT FK_Crash_Bus FOREIGN KEY (bus_id)
            REFERENCES bus(bus_id),
        CONSTRAINT FK_Crash_Driver FOREIGN KEY (driver_user_id)
            REFERENCES driver(user_id)
    );
END;

-- Index: Crash_Incident.bus_id
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_CrashInc_bus_id'
               AND object_id = OBJECT_ID('Crash_Incident'))
    CREATE NONCLUSTERED INDEX IX_CrashInc_bus_id ON Crash_Incident(bus_id);

-- Index: Crash_Incident.driver_user_id
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_CrashInc_driver_user_id'
               AND object_id = OBJECT_ID('Crash_Incident'))
    CREATE NONCLUSTERED INDEX IX_CrashInc_driver_user_id ON Crash_Incident(driver_user_id);

-- ============================================================
-- 3. Feedback
--    Rating INT NULL CHECK (NULL OR 1-5).
--    BookingId BIGINT NULL FK → booking(id).
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Feedback')
BEGIN
    CREATE TABLE Feedback (
        FeedbackId  INT IDENTITY NOT NULL,
        UserId      INT NOT NULL,
        BookingId   BIGINT NULL,
        Subject     VARCHAR(150) NOT NULL,
        Message     NVARCHAR(MAX) NOT NULL,
        Rating      INT NULL,
        Status      VARCHAR(20) NULL CONSTRAINT DF_Feedback_Status DEFAULT 'Pending',
        SubmittedAt DATETIME2 NULL CONSTRAINT DF_Feedback_SubmittedAt DEFAULT GETDATE(),
        CONSTRAINT PK_Feedback PRIMARY KEY (FeedbackId),
        CONSTRAINT FK_Feedback_User FOREIGN KEY (UserId)
            REFERENCES Users(UserId) ON DELETE CASCADE,
        CONSTRAINT FK_Feedback_Booking FOREIGN KEY (BookingId)
            REFERENCES booking(id),
        CONSTRAINT CK_Feedback_Rating CHECK (Rating IS NULL OR (Rating BETWEEN 1 AND 5))
    );
END;

-- Index: Feedback.UserId
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_Feedback_UserId'
               AND object_id = OBJECT_ID('Feedback'))
    CREATE NONCLUSTERED INDEX IX_Feedback_UserId ON Feedback(UserId);

-- Index: Feedback.BookingId
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_Feedback_BookingId'
               AND object_id = OBJECT_ID('Feedback'))
    CREATE NONCLUSTERED INDEX IX_Feedback_BookingId ON Feedback(BookingId);
