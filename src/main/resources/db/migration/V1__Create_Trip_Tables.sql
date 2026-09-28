-- Table 1: location
CREATE TABLE location (
    LocationId INT IDENTITY(1,1) NOT NULL,
    LocationName NVARCHAR(100) NOT NULL,
    Latitude DECIMAL(10,8) DEFAULT NULL,
    Longitude DECIMAL(11,8) DEFAULT NULL,
    PRIMARY KEY (LocationId)
);

-- Table 2: bustrip
CREATE TABLE bustrip (
    TripId INT IDENTITY(1,1) NOT NULL,
    TripDate DATE NOT NULL,
    StartTime TIME NOT NULL,
    ETA TIME DEFAULT NULL,
    PickupLocationId INT DEFAULT NULL,
    DropLocationId INT DEFAULT NULL,
    Status VARCHAR(50) DEFAULT 'Scheduled',
    PRIMARY KEY (TripId),
    CONSTRAINT FK_BusTrip_PickupLocation FOREIGN KEY (PickupLocationId) REFERENCES location(LocationId),
    CONSTRAINT FK_BusTrip_DropLocation FOREIGN KEY (DropLocationId) REFERENCES location(LocationId)
);

-- Table 3: busroute
CREATE TABLE busroute (
    RouteId INT IDENTITY(1,1) NOT NULL,
    RouteName NVARCHAR(100) NOT NULL,
    StartPoint NVARCHAR(100) NOT NULL,
    EndPoint NVARCHAR(100) NOT NULL,
    PRIMARY KEY (RouteId)
);

-- Table 4: destination
CREATE TABLE destination (
    DestinationId INT IDENTITY(1,1) NOT NULL,
    DestinationName NVARCHAR(100) NOT NULL,
    Location NVARCHAR(255) NOT NULL,
    RouteId INT NOT NULL,
    PRIMARY KEY (DestinationId),
    CONSTRAINT FK_Destination_BusRoute FOREIGN KEY (RouteId) REFERENCES busroute(RouteId)
);

-- Table 5: locationupdate
CREATE TABLE locationupdate (
    LocationUpdateId INT IDENTITY(1,1) NOT NULL,
    TripId INT NOT NULL,
    Latitude DECIMAL(10,8) NOT NULL,
    Longitude DECIMAL(11,8) NOT NULL,
    RecordedAt DATETIME NOT NULL,
    PRIMARY KEY (LocationUpdateId),
    CONSTRAINT FK_LocationUpdate_BusTrip FOREIGN KEY (TripId) REFERENCES bustrip(TripId)
);

-- Table 6: tripcancellation
CREATE TABLE tripcancellation (
    CancellationId INT IDENTITY(1,1) NOT NULL,
    TripId INT NOT NULL,
    Reason NVARCHAR(255) DEFAULT NULL,
    CancelledAt DATETIME DEFAULT GETDATE(),
    PRIMARY KEY (CancellationId),
    CONSTRAINT FK_TripCancellation_BusTrip FOREIGN KEY (TripId) REFERENCES bustrip(TripId)
);

-- Table 7: payment
CREATE TABLE payment (
    PaymentId INT IDENTITY(1,1) NOT NULL,
    StudentId INT NOT NULL,
    Amount DECIMAL(10,2) NOT NULL,
    Status VARCHAR(50) DEFAULT 'Completed',
    PRIMARY KEY (PaymentId)
);

-- Table 8: paymentcancellation
CREATE TABLE paymentcancellation (
    CancellationId INT IDENTITY(1,1) NOT NULL,
    PaymentId INT NOT NULL,
    Reason NVARCHAR(255) DEFAULT NULL,
    CancelledAt DATETIME DEFAULT GETDATE(),
    PRIMARY KEY (CancellationId),
    CONSTRAINT FK_PaymentCancellation_Payment FOREIGN KEY (PaymentId) REFERENCES payment(PaymentId)
);