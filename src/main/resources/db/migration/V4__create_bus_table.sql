CREATE TABLE bus (
    bus_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    registration_number NVARCHAR(50) NOT NULL UNIQUE,
    passenger_capacity INT NOT NULL,
    status NVARCHAR(50) DEFAULT 'Available'
);
