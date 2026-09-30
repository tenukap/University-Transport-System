-- V6__admin.sql
-- SavedReports, Announcements, AdminAuditLogs.
-- Depends on: V1 (Users).

-- ============================================================
-- 1. SavedReports
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'SavedReports')
BEGIN
    CREATE TABLE SavedReports (
        ReportId          INT IDENTITY NOT NULL,
        ReportTitle       VARCHAR(150) NOT NULL,
        ReportType        VARCHAR(50) NOT NULL,
        StartDate         DATE NOT NULL,
        EndDate           DATE NOT NULL,
        GeneratedByUserId INT NOT NULL,
        SummaryNotes      NVARCHAR(MAX) NULL,
        GeneratedAt       DATETIME2 NULL CONSTRAINT DF_SavedReports_GeneratedAt DEFAULT GETDATE(),
        CONSTRAINT PK_SavedReports PRIMARY KEY (ReportId),
        CONSTRAINT FK_SavedReports_User FOREIGN KEY (GeneratedByUserId)
            REFERENCES Users(UserId) ON DELETE CASCADE
    );
END;

-- ============================================================
-- 2. Announcements
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Announcements')
BEGIN
    CREATE TABLE Announcements (
        AnnouncementId INT IDENTITY NOT NULL,
        Title          VARCHAR(200) NOT NULL,
        Content        NVARCHAR(MAX) NOT NULL,
        PostedByUserId INT NOT NULL,
        CreatedAt      DATETIME2 NULL CONSTRAINT DF_Announcements_CreatedAt DEFAULT GETDATE(),
        CONSTRAINT PK_Announcements PRIMARY KEY (AnnouncementId),
        CONSTRAINT FK_Announcements_User FOREIGN KEY (PostedByUserId)
            REFERENCES Users(UserId) ON DELETE CASCADE
    );
END;

-- ============================================================
-- 3. AdminAuditLogs
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'AdminAuditLogs')
BEGIN
    CREATE TABLE AdminAuditLogs (
        LogId           INT IDENTITY NOT NULL,
        AdminUserId     INT NOT NULL,
        ActionPerformed VARCHAR(100) NOT NULL,
        TargetUserId    INT NULL,  -- No FK: audit logs must outlive deleted users; intentionally unlinked.
        Timestamp       DATETIME2 NULL CONSTRAINT DF_AdminAuditLogs_Timestamp DEFAULT GETDATE(),
        CONSTRAINT PK_AdminAuditLogs PRIMARY KEY (LogId),
        CONSTRAINT FK_AdminAuditLogs_User FOREIGN KEY (AdminUserId)
            REFERENCES Users(UserId) ON DELETE CASCADE
    );
END;
