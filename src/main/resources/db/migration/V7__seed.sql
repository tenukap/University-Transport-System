-- V7__seed.sql
-- Seed data: admin user only.
-- Location and bus rows removed (they were test data, now in dev-seed-test-data.sql).
-- Depends on: V1 (Users).

-- Admin user (password stored as plaintext for dev; switch to BCrypt before production).
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

-- admin profile row for admin@campus.edu.
-- The SELECT form safely resolves the UserId without hard-coding it.
IF NOT EXISTS (
    SELECT 1 FROM admin
    WHERE user_id = (SELECT UserId FROM Users WHERE Email = 'admin@campus.edu')
)
BEGIN
    INSERT INTO admin (user_id, employee_id, department, access_level)
    SELECT UserId, 'ADM001', 'IT', 'SUPER'
    FROM Users WHERE Email = 'admin@campus.edu';
END;
