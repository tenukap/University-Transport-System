-- V3__seed_data.sql
-- Reference seed data (SQL Server / T-SQL).
-- No seed users (no password hashes stored here).
-- All inserts guarded with IF NOT EXISTS to stay idempotent.

-- Locations
IF NOT EXISTS (SELECT * FROM location WHERE LocationName = 'Main Gate')
BEGIN
    INSERT INTO location (LocationName) VALUES ('Main Gate');
END;

IF NOT EXISTS (SELECT * FROM location WHERE LocationName = 'City Center')
BEGIN
    INSERT INTO location (LocationName) VALUES ('City Center');
END;

-- Bus
IF NOT EXISTS (SELECT * FROM bus WHERE registration_number = 'NA-1001')
BEGIN
    INSERT INTO bus (registration_number, passenger_capacity, status)
    VALUES ('NA-1001', 50, 'Active');
END;
-- Admin user
-- Password stored as plaintext: admin1234
IF NOT EXISTS (SELECT * FROM Users WHERE Email = 'admin@campus.edu')
BEGIN
    INSERT INTO Users (FullName, Email, PasswordHash, RoleName, AccountStatus, CreatedAt)
    VALUES (
        'Admin User',
        'admin@campus.edu',
        'admin1234',
        'ADMIN',
        'Active',
        GETDATE()
    );
END;
