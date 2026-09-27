
CREATE TABLE Trip_Status (
    Status_Id INT IDENTITY(1,1) PRIMARY KEY,
    TripId INT NOT NULL,  -- Changed from Trip_Id
    Status_Type VARCHAR(100) NOT NULL,
    Updated_At DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT FK_TripStatus_Trip FOREIGN KEY (TripId)
        REFERENCES bustrip(TripId)
);

CREATE TABLE Location_Update (
    Location_Update_Id INT IDENTITY(1,1) PRIMARY KEY,
    TripId INT NOT NULL,  -- Changed from Trip_Id
    Latitude DECIMAL(10, 8) NOT NULL,
    Longitude DECIMAL(11, 8) NOT NULL,
    Recorded_At DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT FK_LocationUpdate_Trip FOREIGN KEY (TripId)
        REFERENCES bustrip(TripId)
);