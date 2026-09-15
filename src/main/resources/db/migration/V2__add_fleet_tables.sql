-- Create Bus Table
CREATE TABLE bus (
                     bus_id BIGINT IDENTITY(1,1) PRIMARY KEY,
                     registration_number NVARCHAR(50) NOT NULL UNIQUE,
                     passenger_capacity INT NOT NULL,
                     status NVARCHAR(50) DEFAULT 'Available'
);

-- Create Driver Table (Linked to  users table)
CREATE TABLE driver (
                        user_id BIGINT PRIMARY KEY,
                        license_number NVARCHAR(100) NOT NULL UNIQUE,
                        dob DATE,
                        status NVARCHAR(50) DEFAULT 'Available',
                        CONSTRAINT fk_driver_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);