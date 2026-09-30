-- V1__users_and_roles.sql
-- Users, student, driver tables (FK-safe creation order).
-- Constraint prefixes: PK_ UQ_ CK_ DF_ FK_
-- Every FK column has a nonclustered index.

-- ============================================================
-- 1. Users
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Users')
BEGIN
    CREATE TABLE Users (
        UserId        INT IDENTITY NOT NULL,
        FullName      VARCHAR(100) NOT NULL,
        Email         VARCHAR(150) NOT NULL,
        PasswordHash  VARCHAR(255) NOT NULL,
        Phone         VARCHAR(20) NULL,
        RoleName      VARCHAR(50) NOT NULL,
        AccountStatus VARCHAR(20) NULL CONSTRAINT DF_Users_AccountStatus DEFAULT 'Active',
        CreatedAt     DATETIME2 NULL CONSTRAINT DF_Users_CreatedAt DEFAULT GETDATE(),
        UpdatedAt     DATETIME2 NULL CONSTRAINT DF_Users_UpdatedAt DEFAULT GETDATE(),
        CONSTRAINT PK_Users PRIMARY KEY (UserId),
        CONSTRAINT UQ_Users_Email UNIQUE (Email)
    );
END;

-- ============================================================
-- 2. student
--    PK = user_id (no surrogate id column).
--    No full_name, no phone columns.
--    semester TINYINT NULL added.
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'student')
BEGIN
    CREATE TABLE student (
        user_id       INT NOT NULL,
        student_index VARCHAR(50) NULL,
        semester      TINYINT NULL,
        CONSTRAINT PK_student PRIMARY KEY (user_id),
        CONSTRAINT CK_student_semester CHECK (semester IS NULL OR semester BETWEEN 1 AND 8),
        CONSTRAINT FK_student_user FOREIGN KEY (user_id)
            REFERENCES Users(UserId) ON DELETE CASCADE
    );
END;

-- Filtered unique index: two students cannot share a non-null index; NULLs (unset) are allowed.
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'UQ_student_index_filtered'
               AND object_id = OBJECT_ID('student'))
    CREATE UNIQUE NONCLUSTERED INDEX UQ_student_index_filtered
        ON student(student_index) WHERE student_index IS NOT NULL;

-- ============================================================
-- 3. driver
--    PK = user_id (shares the Users row).
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'driver')
BEGIN
    CREATE TABLE driver (
        user_id        INT NOT NULL,
        license_number NVARCHAR(100) NOT NULL,
        dob            DATE NULL,
        status         NVARCHAR(50) NULL CONSTRAINT DF_driver_status DEFAULT 'Available',
        CONSTRAINT PK_driver PRIMARY KEY (user_id),
        CONSTRAINT UQ_driver_license UNIQUE (license_number),
        CONSTRAINT FK_driver_user FOREIGN KEY (user_id)
            REFERENCES Users(UserId) ON DELETE CASCADE
    );
END;

-- ============================================================
-- 4. admin
--    PK = user_id. No ON DELETE CASCADE — Java deletes child
--    row explicitly before deleting the parent Users row.
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'admin')
BEGIN
    CREATE TABLE admin (
        user_id      INT NOT NULL,
        employee_id  VARCHAR(20) NOT NULL,
        department   VARCHAR(100) NULL,
        access_level VARCHAR(20) NOT NULL CONSTRAINT DF_admin_access_level DEFAULT 'STANDARD',
        CONSTRAINT PK_admin PRIMARY KEY (user_id),
        CONSTRAINT UQ_admin_employee_id UNIQUE (employee_id),
        CONSTRAINT CK_admin_access_level CHECK (access_level IN ('STANDARD','SUPER')),
        CONSTRAINT FK_admin_user FOREIGN KEY (user_id) REFERENCES Users(UserId) ON DELETE CASCADE
    );
END;

-- ============================================================
-- 5. finance_officer
--    PK = user_id. No ON DELETE CASCADE.
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'finance_officer')
BEGIN
    CREATE TABLE finance_officer (
        user_id        INT NOT NULL,
        employee_id    VARCHAR(20) NOT NULL,
        approval_limit DECIMAL(10,2) NULL,
        hire_date      DATE NULL,
        CONSTRAINT PK_finance_officer PRIMARY KEY (user_id),
        CONSTRAINT UQ_finance_officer_employee_id UNIQUE (employee_id),
        CONSTRAINT CK_finance_officer_limit CHECK (approval_limit IS NULL OR approval_limit >= 0),
        CONSTRAINT FK_finance_officer_user FOREIGN KEY (user_id) REFERENCES Users(UserId) ON DELETE CASCADE
    );
END;

-- ============================================================
-- 6. transport_officer
--    PK = user_id. No ON DELETE CASCADE.
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'transport_officer')
BEGIN
    CREATE TABLE transport_officer (
        user_id     INT NOT NULL,
        employee_id VARCHAR(20) NOT NULL,
        depot       VARCHAR(100) NULL,
        hire_date   DATE NULL,
        CONSTRAINT PK_transport_officer PRIMARY KEY (user_id),
        CONSTRAINT UQ_transport_officer_employee_id UNIQUE (employee_id),
        CONSTRAINT FK_transport_officer_user FOREIGN KEY (user_id) REFERENCES Users(UserId) ON DELETE CASCADE
    );
END;
