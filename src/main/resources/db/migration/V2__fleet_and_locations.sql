-- V2__fleet_and_locations.sql
-- location, bus, BusRoute, Destination.
-- Depends on: nothing (no FKs to V1 tables in this file except Destination→BusRoute, same file).

-- ============================================================
-- 1. location
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'location')
BEGIN
    CREATE TABLE location (
        LocationId   INT IDENTITY NOT NULL,
        LocationName VARCHAR(100) NOT NULL,
        Latitude     DECIMAL(9,6) NULL,
        Longitude    DECIMAL(9,6) NULL,
        CONSTRAINT PK_location PRIMARY KEY (LocationId)
    );
END;

-- ============================================================
-- 2. bus
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'bus')
BEGIN
    CREATE TABLE bus (
        bus_id              BIGINT IDENTITY NOT NULL,
        registration_number VARCHAR(50) NOT NULL,
        passenger_capacity  INT NOT NULL,
        status              VARCHAR(20) NULL CONSTRAINT DF_bus_status DEFAULT 'Available',
        CONSTRAINT PK_bus PRIMARY KEY (bus_id),
        CONSTRAINT UQ_bus_registration UNIQUE (registration_number)
    );
END;

-- ============================================================
-- 3. BusRoute
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'BusRoute')
BEGIN
    CREATE TABLE BusRoute (
        RouteId    INT IDENTITY(1,1) NOT NULL,
        RouteName  NVARCHAR(100) NOT NULL,
        StartPoint NVARCHAR(100) NOT NULL,
        EndPoint   NVARCHAR(100) NOT NULL,
        CONSTRAINT PK_BusRoute PRIMARY KEY (RouteId)
    );
END;

-- ============================================================
-- 4. Destination
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Destination')
BEGIN
    CREATE TABLE Destination (
        DestinationId   INT IDENTITY(1,1) NOT NULL,
        DestinationName NVARCHAR(100) NOT NULL,
        Location        NVARCHAR(255) NOT NULL,
        RouteId         INT NOT NULL,
        CONSTRAINT PK_Destination PRIMARY KEY (DestinationId),
        CONSTRAINT FK_Destination_BusRoute FOREIGN KEY (RouteId)
            REFERENCES BusRoute(RouteId)
    );
END;

-- Index: Destination.RouteId
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_Destination_RouteId'
               AND object_id = OBJECT_ID('Destination'))
    CREATE NONCLUSTERED INDEX IX_Destination_RouteId ON Destination(RouteId);
