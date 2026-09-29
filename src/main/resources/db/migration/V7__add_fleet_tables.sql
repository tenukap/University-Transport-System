-- bus table already created by V1; do NOT recreate it.

-- Create Driver Table (linked to Users table)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'driver')
BEGIN
    CREATE TABLE driver (
        user_id        INT PRIMARY KEY,
        license_number NVARCHAR(100) NOT NULL UNIQUE,
        dob            DATE,
        status         NVARCHAR(50) DEFAULT 'Available',
        CONSTRAINT fk_driver_user FOREIGN KEY (user_id)
            REFERENCES Users(UserId) ON DELETE CASCADE
    );
END;
