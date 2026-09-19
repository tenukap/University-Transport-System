CREATE TABLE Trip_Status (
                             Status_Id INT IDENTITY(1,1) PRIMARY KEY,
                             Trip_Id INT NOT NULL,
                             Status_Type VARCHAR(100) NOT NULL,
                             Updated_At DATETIME DEFAULT GETDATE()
);

CREATE TABLE Location_Update (
                                 Location_Update_Id INT IDENTITY(1,1) PRIMARY KEY,
                                 Trip_Id INT NOT NULL,
                                 Latitude DECIMAL(10, 8) NOT NULL,
                                 Longitude DECIMAL(11, 8) NOT NULL,
                                 Recorded_At DATETIME DEFAULT GETDATE()
);