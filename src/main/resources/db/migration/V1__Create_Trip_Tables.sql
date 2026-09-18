-- 1. Create location table
CREATE TABLE location (
    LocationId INT IDENTITY(1,1) NOT NULL,
    LocationName NVARCHAR(100) NOT NULL,
    Latitude DECIMAL(10,8) DEFAULT NULL,
    Longitude DECIMAL(11,8) DEFAULT NULL,
    PRIMARY KEY (LocationId)
);

-- 2. Create bustrip table
CREATE TABLE bustrip (
    TripId INT IDENTITY(1,1) NOT NULL,
    TripDate DATE NOT NULL,
    StartTime TIME NOT NULL,
    ETA TIME DEFAULT NULL,
    PickupLocationId INT DEFAULT NULL,
    DropLocationId INT DEFAULT NULL,
    PRIMARY KEY (TripId),
    CONSTRAINT FK_BusTrip_PickupLocation FOREIGN KEY (PickupLocationId) REFERENCES location(LocationId),
    CONSTRAINT FK_BusTrip_DropLocation FOREIGN KEY (DropLocationId) REFERENCES location(LocationId)
);

-- 3. Create locationupdate table
CREATE TABLE locationupdate (
    LocationUpdateId INT IDENTITY(1,1) NOT NULL,
    TripId INT NOT NULL,
    Latitude DECIMAL(10,8) NOT NULL,
    Longitude DECIMAL(11,8) NOT NULL,
    RecordedAt DATETIME NOT NULL,
    PRIMARY KEY (LocationUpdateId),
    CONSTRAINT FK_LocationUpdate_BusTrip FOREIGN KEY (TripId) REFERENCES bustrip(TripId)
);