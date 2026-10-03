-- V9__trip_link_for_reports.sql
-- Additive migration. To be merged into the base migration later.
-- Adds a nullable trip_id FK to Emergency_Report and Crash_Incident.
-- Existing rows stay valid (NULL). Column naming matches lowercase-underscore style used in V5.

-- ============================================================
-- 1. Emergency_Report.trip_id
-- ============================================================
IF NOT EXISTS (
    SELECT * FROM sys.columns
    WHERE object_id = OBJECT_ID('Emergency_Report') AND name = 'trip_id'
)
    ALTER TABLE Emergency_Report ADD trip_id INT NULL;

IF NOT EXISTS (
    SELECT * FROM sys.foreign_keys WHERE name = 'FK_EmgReport_Trip'
)
    ALTER TABLE Emergency_Report
        ADD CONSTRAINT FK_EmgReport_Trip
            FOREIGN KEY (trip_id) REFERENCES bustrip(TripId);

IF NOT EXISTS (
    SELECT * FROM sys.indexes
    WHERE name = 'IX_EmgReport_trip_id' AND object_id = OBJECT_ID('Emergency_Report')
)
    CREATE NONCLUSTERED INDEX IX_EmgReport_trip_id ON Emergency_Report(trip_id);

-- ============================================================
-- 2. Crash_Incident.trip_id
-- ============================================================
IF NOT EXISTS (
    SELECT * FROM sys.columns
    WHERE object_id = OBJECT_ID('Crash_Incident') AND name = 'trip_id'
)
    ALTER TABLE Crash_Incident ADD trip_id INT NULL;

IF NOT EXISTS (
    SELECT * FROM sys.foreign_keys WHERE name = 'FK_CrashInc_Trip'
)
    ALTER TABLE Crash_Incident
        ADD CONSTRAINT FK_CrashInc_Trip
            FOREIGN KEY (trip_id) REFERENCES bustrip(TripId);

IF NOT EXISTS (
    SELECT * FROM sys.indexes
    WHERE name = 'IX_CrashInc_trip_id' AND object_id = OBJECT_ID('Crash_Incident')
)
    CREATE NONCLUSTERED INDEX IX_CrashInc_trip_id ON Crash_Incident(trip_id);
