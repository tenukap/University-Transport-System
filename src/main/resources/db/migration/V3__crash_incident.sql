
CREATE TABLE Crash_Incident (
    Incident_ID INT IDENTITY(1,1) PRIMARY KEY,
    bus_id BIGINT,  -- Changed from Bus_No
    user_id INT,    -- Changed from Driver_No (assuming admin/driver is a user)
    Location_Coordinates VARCHAR(255) NOT NULL,
    Severity_Level VARCHAR(50) NOT NULL,
    Description NVARCHAR(MAX),
    Timestamp DATETIME DEFAULT GETDATE(),
    Status VARCHAR(50) DEFAULT 'Reported',
    
    CONSTRAINT FK_Crash_Bus FOREIGN KEY (bus_id)
        REFERENCES bus(bus_id),
    CONSTRAINT FK_Crash_User FOREIGN KEY (user_id)
        REFERENCES Users(UserId) ON DELETE CASCADE
);