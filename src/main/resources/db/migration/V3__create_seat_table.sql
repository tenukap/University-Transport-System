CREATE TABLE seat (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    seat_number NVARCHAR(10) NOT NULL,  -- e.g. A1, A2, B1...
    -- bus_id FK will be added once Dinuja confirms his bus table PK
    created_at DATETIME2 DEFAULT GETDATE()
);