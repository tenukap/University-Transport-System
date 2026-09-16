CREATE TABLE Crash_Incident (
                                Incident_ID INT IDENTITY(1,1) PRIMARY KEY,
                                Bus_No INT,
                                Driver_No INT,
                                Location_Coordinates VARCHAR(255) NOT NULL,
                                Severity_Level VARCHAR(50) NOT NULL,
                                Description NVARCHAR(MAX),
                                Timestamp DATETIME DEFAULT GETDATE(),
                                Status VARCHAR(50) DEFAULT 'Reported'
);