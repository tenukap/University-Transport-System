CREATE TABLE location (
    LocationId INT IDENTITY(1,1) NOT NULL,
    LocationName NVARCHAR(100) NOT NULL,
    Latitude DECIMAL(10,8) DEFAULT NULL,
    Longitude DECIMAL(11,8) DEFAULT NULL,
    PRIMARY KEY (LocationId)
);
INSERT INTO location (LocationName, Latitude, Longitude) VALUES
('SLIIT', 6.91470000, 79.97290000),
('Kaduwela', 6.93500000, 79.98000000),
('Malabe', 6.90970000, 79.96350000),
('Colombo Fort', 6.93548000, 79.84868000);
