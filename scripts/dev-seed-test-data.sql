-- =============================================================================
-- scripts/dev-seed-test-data.sql
-- Dev-only seed for utms_db.  Run in DBeaver; DO NOT add to Flyway migrations.
--
-- Re-runnable: every INSERT is guarded by IF/WHERE NOT EXISTS on a natural key.
-- IDs are never hard-coded; they are resolved by SELECT after each section.
-- Password convention: plaintext (PortalController uses .equals(), not BCrypt).
-- Bus status: 'Available' to match V1 + V7 DEFAULT.
-- Trip dates: relative to GETDATE() so upcoming trips always exist.
-- =============================================================================
USE utms_db;
GO

SET XACT_ABORT ON;
BEGIN TRANSACTION;
BEGIN TRY

-- =============================================================================
-- 0.  RELATIVE DATE HELPERS
-- =============================================================================
DECLARE @today      DATE = CAST(GETDATE() AS DATE);
DECLARE @tomorrow   DATE = DATEADD(day,  1, @today);
DECLARE @dayPlus2   DATE = DATEADD(day,  2, @today);
DECLARE @yesterday  DATE = DATEADD(day, -1, @today);
DECLARE @dayMinus2  DATE = DATEADD(day, -2, @today);

-- =============================================================================
-- 1.  USERS  (never touch existing rows; guard by email)
--     Roles: ADMIN | STUDENT | DRIVER | FINANCE_OFFICER | TRANSPORT_OFFICER
--     AccountStatus: Active | Suspended | Deactivated
--     Password: plaintext — PortalController compares with .equals()
-- =============================================================================
-- Existing from V4 seed: admin@campus.edu  (ADMIN, 'admin1234') — left untouched.

-- Existing users mentioned — ensure they exist with correct columns:
IF NOT EXISTS (SELECT 1 FROM Users WHERE Email = 'john@gmail.com')
    INSERT INTO Users (FullName, Email, PasswordHash, Phone, RoleName, AccountStatus, CreatedAt, UpdatedAt)
    VALUES ('John Silva', 'john@gmail.com', '123456', '0771234567', 'STUDENT', 'Active', GETDATE(), GETDATE());

IF NOT EXISTS (SELECT 1 FROM Users WHERE Email = 'pep@gmail.com')
    INSERT INTO Users (FullName, Email, PasswordHash, Phone, RoleName, AccountStatus, CreatedAt, UpdatedAt)
    VALUES ('Pep Fernando', 'pep@gmail.com', '123456', '0779876543', 'DRIVER', 'Active', GETDATE(), GETDATE());

IF NOT EXISTS (SELECT 1 FROM Users WHERE Email = 'jame@gmail.com')
    INSERT INTO Users (FullName, Email, PasswordHash, Phone, RoleName, AccountStatus, CreatedAt, UpdatedAt)
    VALUES ('James Perera', 'jame@gmail.com', '123456', '0765551234', 'FINANCE_OFFICER', 'Active', GETDATE(), GETDATE());

-- 2 additional students:
IF NOT EXISTS (SELECT 1 FROM Users WHERE Email = 'sara@student.campus.lk')
    INSERT INTO Users (FullName, Email, PasswordHash, Phone, RoleName, AccountStatus, CreatedAt, UpdatedAt)
    VALUES ('Sara Wickramasinghe', 'sara@student.campus.lk', '123456', '0712345678', 'STUDENT', 'Active', GETDATE(), GETDATE());

IF NOT EXISTS (SELECT 1 FROM Users WHERE Email = 'kamal@student.campus.lk')
    INSERT INTO Users (FullName, Email, PasswordHash, Phone, RoleName, AccountStatus, CreatedAt, UpdatedAt)
    VALUES ('Kamal Bandara', 'kamal@student.campus.lk', '123456', '0723456789', 'STUDENT', 'Active', GETDATE(), GETDATE());

-- 1 additional driver:
IF NOT EXISTS (SELECT 1 FROM Users WHERE Email = 'nimal@driver.campus.lk')
    INSERT INTO Users (FullName, Email, PasswordHash, Phone, RoleName, AccountStatus, CreatedAt, UpdatedAt)
    VALUES ('Nimal Jayasinghe', 'nimal@driver.campus.lk', '123456', '0734567890', 'DRIVER', 'Active', GETDATE(), GETDATE());

-- 1 transport officer:
IF NOT EXISTS (SELECT 1 FROM Users WHERE Email = 'transport@campus.lk')
    INSERT INTO Users (FullName, Email, PasswordHash, Phone, RoleName, AccountStatus, CreatedAt, UpdatedAt)
    VALUES ('Suresh Kumara', 'transport@campus.lk', '123456', '0745678901', 'TRANSPORT_OFFICER', 'Active', GETDATE(), GETDATE());

-- =============================================================================
-- 2.  STUDENT rows  (one per STUDENT user, guarded by student_index UNIQUE)
-- =============================================================================
IF NOT EXISTS (SELECT 1 FROM student WHERE student_index = 'CSE/2021/001')
    INSERT INTO student (user_id, student_index, full_name, phone)
    SELECT UserId, 'CSE/2021/001', 'John Silva', '0771234567'
    FROM Users WHERE Email = 'john@gmail.com';

IF NOT EXISTS (SELECT 1 FROM student WHERE student_index = 'CSE/2021/002')
    INSERT INTO student (user_id, student_index, full_name, phone)
    SELECT UserId, 'CSE/2021/002', 'Sara Wickramasinghe', '0712345678'
    FROM Users WHERE Email = 'sara@student.campus.lk';

IF NOT EXISTS (SELECT 1 FROM student WHERE student_index = 'CSE/2021/003')
    INSERT INTO student (user_id, student_index, full_name, phone)
    SELECT UserId, 'CSE/2021/003', 'Kamal Bandara', '0723456789'
    FROM Users WHERE Email = 'kamal@student.campus.lk';

-- =============================================================================
-- 3.  DRIVER rows  (one per DRIVER user, guarded by license_number UNIQUE)
-- =============================================================================
IF NOT EXISTS (SELECT 1 FROM driver WHERE license_number = 'B1234567')
    INSERT INTO driver (user_id, license_number, dob, status)
    SELECT UserId, 'B1234567', '1985-03-15', 'Available'
    FROM Users WHERE Email = 'pep@gmail.com';

IF NOT EXISTS (SELECT 1 FROM driver WHERE license_number = 'C7654321')
    INSERT INTO driver (user_id, license_number, dob, status)
    SELECT UserId, 'C7654321', '1990-07-22', 'Available'
    FROM Users WHERE Email = 'nimal@driver.campus.lk';

-- =============================================================================
-- 4.  LOCATION  (12 Sri Lankan stops; guarded by LocationName)
-- =============================================================================
DECLARE @locs TABLE (Name VARCHAR(100), Lat DECIMAL(9,6), Lng DECIMAL(9,6));
INSERT INTO @locs VALUES
    ('Main Gate',    6.821900, 80.039900),
    ('City Center',  6.935500, 79.850200),
    ('Malabe Town',  6.904700, 79.973700),
    ('Kaduwela',     6.940400, 79.987200),
    ('Kottawa',      6.838400, 79.975200),
    ('Athurugiriya', 6.873000, 79.984200),
    ('Battaramulla', 6.915500, 79.919300),
    ('Rajagiriya',   6.905600, 79.899300),
    ('Nugegoda',     6.878000, 79.890000),
    ('Maharagama',   6.848300, 79.926500),
    ('Homagama',     6.847400, 80.007700),
    ('Kadawatha',    7.002800, 79.958700);

INSERT INTO location (LocationName, Latitude, Longitude)
SELECT l.Name, l.Lat, l.Lng
FROM   @locs l
WHERE  NOT EXISTS (SELECT 1 FROM location WHERE LocationName = l.Name);

-- =============================================================================
-- 5.  BUS  (NA-1001 may already exist from V4 seed; guarded by registration_number)
-- =============================================================================
DECLARE @buses TABLE (RegNo VARCHAR(50), Cap INT);
INSERT INTO @buses VALUES
    ('NA-1001', 50), ('NA-1002', 50), ('NA-1003', 45),
    ('NA-1004', 50), ('NA-1005', 40);

INSERT INTO bus (registration_number, passenger_capacity, status)
SELECT b.RegNo, b.Cap, 'Available'
FROM   @buses b
WHERE  NOT EXISTS (SELECT 1 FROM bus WHERE registration_number = b.RegNo);

-- =============================================================================
-- ID LOOKUPS — location, bus, user  (used by every FK below)
-- =============================================================================
DECLARE @locMainGate   INT = (SELECT LocationId FROM location WHERE LocationName = 'Main Gate');
DECLARE @locCityCenter INT = (SELECT LocationId FROM location WHERE LocationName = 'City Center');
DECLARE @locMalabe     INT = (SELECT LocationId FROM location WHERE LocationName = 'Malabe Town');
DECLARE @locKaduwela   INT = (SELECT LocationId FROM location WHERE LocationName = 'Kaduwela');
DECLARE @locKottawa    INT = (SELECT LocationId FROM location WHERE LocationName = 'Kottawa');
DECLARE @locAthu       INT = (SELECT LocationId FROM location WHERE LocationName = 'Athurugiriya');
DECLARE @locBatta      INT = (SELECT LocationId FROM location WHERE LocationName = 'Battaramulla');
DECLARE @locRaja       INT = (SELECT LocationId FROM location WHERE LocationName = 'Rajagiriya');
DECLARE @locNugegoda   INT = (SELECT LocationId FROM location WHERE LocationName = 'Nugegoda');
DECLARE @locMahara     INT = (SELECT LocationId FROM location WHERE LocationName = 'Maharagama');
DECLARE @locHomagama   INT = (SELECT LocationId FROM location WHERE LocationName = 'Homagama');
DECLARE @locKadawatha  INT = (SELECT LocationId FROM location WHERE LocationName = 'Kadawatha');

DECLARE @busId1 BIGINT = (SELECT bus_id FROM bus WHERE registration_number = 'NA-1001');
DECLARE @busId2 BIGINT = (SELECT bus_id FROM bus WHERE registration_number = 'NA-1002');
DECLARE @busId3 BIGINT = (SELECT bus_id FROM bus WHERE registration_number = 'NA-1003');
DECLARE @busId4 BIGINT = (SELECT bus_id FROM bus WHERE registration_number = 'NA-1004');
DECLARE @busId5 BIGINT = (SELECT bus_id FROM bus WHERE registration_number = 'NA-1005');

DECLARE @uAdmin     INT = (SELECT UserId FROM Users WHERE Email = 'admin@campus.edu');
DECLARE @uJohn      INT = (SELECT UserId FROM Users WHERE Email = 'john@gmail.com');
DECLARE @uSara      INT = (SELECT UserId FROM Users WHERE Email = 'sara@student.campus.lk');
DECLARE @uKamal     INT = (SELECT UserId FROM Users WHERE Email = 'kamal@student.campus.lk');
DECLARE @uPep       INT = (SELECT UserId FROM Users WHERE Email = 'pep@gmail.com');
DECLARE @uNimal     INT = (SELECT UserId FROM Users WHERE Email = 'nimal@driver.campus.lk');
DECLARE @uJame      INT = (SELECT UserId FROM Users WHERE Email = 'jame@gmail.com');
DECLARE @uTransport INT = (SELECT UserId FROM Users WHERE Email = 'transport@campus.lk');

-- =============================================================================
-- 6.  BUSROUTE  (guarded by RouteName; RouteService checks for duplicates too)
-- =============================================================================
IF NOT EXISTS (SELECT 1 FROM BusRoute WHERE RouteName = 'Malabe Morning Express')
    INSERT INTO BusRoute (RouteName, StartPoint, EndPoint)
    VALUES ('Malabe Morning Express', 'Malabe Town', 'Main Gate');

IF NOT EXISTS (SELECT 1 FROM BusRoute WHERE RouteName = 'Malabe Evening Return')
    INSERT INTO BusRoute (RouteName, StartPoint, EndPoint)
    VALUES ('Malabe Evening Return', 'Main Gate', 'Malabe Town');

IF NOT EXISTS (SELECT 1 FROM BusRoute WHERE RouteName = 'Nugegoda Morning Express')
    INSERT INTO BusRoute (RouteName, StartPoint, EndPoint)
    VALUES ('Nugegoda Morning Express', 'Nugegoda', 'Main Gate');

IF NOT EXISTS (SELECT 1 FROM BusRoute WHERE RouteName = 'Kaduwela Evening Service')
    INSERT INTO BusRoute (RouteName, StartPoint, EndPoint)
    VALUES ('Kaduwela Evening Service', 'Main Gate', 'Kaduwela');

IF NOT EXISTS (SELECT 1 FROM BusRoute WHERE RouteName = 'Battaramulla Shuttle')
    INSERT INTO BusRoute (RouteName, StartPoint, EndPoint)
    VALUES ('Battaramulla Shuttle', 'Battaramulla', 'Main Gate');

IF NOT EXISTS (SELECT 1 FROM BusRoute WHERE RouteName = 'Homagama Morning Service')
    INSERT INTO BusRoute (RouteName, StartPoint, EndPoint)
    VALUES ('Homagama Morning Service', 'Homagama', 'Main Gate');

DECLARE @rMalab  INT = (SELECT RouteId FROM BusRoute WHERE RouteName = 'Malabe Morning Express');
DECLARE @rMalabE INT = (SELECT RouteId FROM BusRoute WHERE RouteName = 'Malabe Evening Return');
DECLARE @rNuge   INT = (SELECT RouteId FROM BusRoute WHERE RouteName = 'Nugegoda Morning Express');
DECLARE @rKadu   INT = (SELECT RouteId FROM BusRoute WHERE RouteName = 'Kaduwela Evening Service');
DECLARE @rBatta  INT = (SELECT RouteId FROM BusRoute WHERE RouteName = 'Battaramulla Shuttle');
DECLARE @rHoma   INT = (SELECT RouteId FROM BusRoute WHERE RouteName = 'Homagama Morning Service');

-- =============================================================================
-- 7.  DESTINATION  (guarded by DestinationName + RouteId)
-- =============================================================================
IF NOT EXISTS (SELECT 1 FROM Destination WHERE DestinationName = 'Malabe Town Centre' AND RouteId = @rMalab)
    INSERT INTO Destination (DestinationName, Location, RouteId)
    VALUES ('Malabe Town Centre', 'Malabe Town, Western Province', @rMalab);

IF NOT EXISTS (SELECT 1 FROM Destination WHERE DestinationName = 'Campus Main Entrance (Malabe)' AND RouteId = @rMalab)
    INSERT INTO Destination (DestinationName, Location, RouteId)
    VALUES ('Campus Main Entrance (Malabe)', 'Main Gate, Homagama', @rMalab);

IF NOT EXISTS (SELECT 1 FROM Destination WHERE DestinationName = 'Nugegoda Junction' AND RouteId = @rNuge)
    INSERT INTO Destination (DestinationName, Location, RouteId)
    VALUES ('Nugegoda Junction', 'Nugegoda, Western Province', @rNuge);

IF NOT EXISTS (SELECT 1 FROM Destination WHERE DestinationName = 'Campus Main Entrance (Nugegoda)' AND RouteId = @rNuge)
    INSERT INTO Destination (DestinationName, Location, RouteId)
    VALUES ('Campus Main Entrance (Nugegoda)', 'Main Gate, Homagama', @rNuge);

IF NOT EXISTS (SELECT 1 FROM Destination WHERE DestinationName = 'Battaramulla Town Stop' AND RouteId = @rBatta)
    INSERT INTO Destination (DestinationName, Location, RouteId)
    VALUES ('Battaramulla Town Stop', 'Battaramulla, Western Province', @rBatta);

IF NOT EXISTS (SELECT 1 FROM Destination WHERE DestinationName = 'Homagama Bus Stand' AND RouteId = @rHoma)
    INSERT INTO Destination (DestinationName, Location, RouteId)
    VALUES ('Homagama Bus Stand', 'Homagama, Western Province', @rHoma);

-- =============================================================================
-- 8.  BUSTRIP
--     TripStatus values used by the code:
--       'Scheduled'  — BookingService allows bookings; FleetTrackService counts these
--       'Completed'  — TransportController.completeTrip() writes this
--       'Cancelled'  — TransportController.cancelTrip() writes this
--     Natural key: (bus_id, TripDate, StartTime)
-- =============================================================================

-- ── TODAY  (4 Scheduled + 1 Cancelled) ──────────────────────────────────────
IF NOT EXISTS (SELECT 1 FROM bustrip WHERE bus_id = @busId1 AND TripDate = @today AND StartTime = '07:00')
    INSERT INTO bustrip (bus_id, TripDate, StartTime, ETA, PickupLocationId, DropLocationId, TripStatus, OperatingCost)
    VALUES (@busId1, @today, '07:00', '08:30', @locMalabe,   @locMainGate, 'Scheduled', 2500.00);

IF NOT EXISTS (SELECT 1 FROM bustrip WHERE bus_id = @busId2 AND TripDate = @today AND StartTime = '07:30')
    INSERT INTO bustrip (bus_id, TripDate, StartTime, ETA, PickupLocationId, DropLocationId, TripStatus, OperatingCost)
    VALUES (@busId2, @today, '07:30', '09:00', @locNugegoda, @locMainGate, 'Scheduled', 2800.00);

IF NOT EXISTS (SELECT 1 FROM bustrip WHERE bus_id = @busId1 AND TripDate = @today AND StartTime = '17:00')
    INSERT INTO bustrip (bus_id, TripDate, StartTime, ETA, PickupLocationId, DropLocationId, TripStatus, OperatingCost)
    VALUES (@busId1, @today, '17:00', '18:30', @locMainGate, @locMalabe,   'Scheduled', 2500.00);

IF NOT EXISTS (SELECT 1 FROM bustrip WHERE bus_id = @busId2 AND TripDate = @today AND StartTime = '17:30')
    INSERT INTO bustrip (bus_id, TripDate, StartTime, ETA, PickupLocationId, DropLocationId, TripStatus, OperatingCost)
    VALUES (@busId2, @today, '17:30', '19:00', @locMainGate, @locKaduwela, 'Scheduled', 3000.00);

-- Today cancelled trip (generates a tripcancellation row later):
IF NOT EXISTS (SELECT 1 FROM bustrip WHERE bus_id = @busId3 AND TripDate = @today AND StartTime = '09:00')
    INSERT INTO bustrip (bus_id, TripDate, StartTime, ETA, PickupLocationId, DropLocationId, TripStatus, OperatingCost)
    VALUES (@busId3, @today, '09:00', '10:30', @locKaduwela, @locMainGate, 'Cancelled', 0.00);

-- ── TOMORROW  (2 Scheduled) ──────────────────────────────────────────────────
IF NOT EXISTS (SELECT 1 FROM bustrip WHERE bus_id = @busId3 AND TripDate = @tomorrow AND StartTime = '07:00')
    INSERT INTO bustrip (bus_id, TripDate, StartTime, ETA, PickupLocationId, DropLocationId, TripStatus, OperatingCost)
    VALUES (@busId3, @tomorrow, '07:00', '08:30', @locBatta,    @locMainGate, 'Scheduled', 2200.00);

IF NOT EXISTS (SELECT 1 FROM bustrip WHERE bus_id = @busId4 AND TripDate = @tomorrow AND StartTime = '17:00')
    INSERT INTO bustrip (bus_id, TripDate, StartTime, ETA, PickupLocationId, DropLocationId, TripStatus, OperatingCost)
    VALUES (@busId4, @tomorrow, '17:00', '18:30', @locMainGate, @locBatta,    'Scheduled', 2200.00);

-- ── DAY +2  (1 Scheduled) ────────────────────────────────────────────────────
IF NOT EXISTS (SELECT 1 FROM bustrip WHERE bus_id = @busId5 AND TripDate = @dayPlus2 AND StartTime = '07:00')
    INSERT INTO bustrip (bus_id, TripDate, StartTime, ETA, PickupLocationId, DropLocationId, TripStatus, OperatingCost)
    VALUES (@busId5, @dayPlus2, '07:00', '08:30', @locHomagama, @locMainGate, 'Scheduled', 1800.00);

-- ── YESTERDAY  (2 Completed — for booking history and financial report) ──────
IF NOT EXISTS (SELECT 1 FROM bustrip WHERE bus_id = @busId1 AND TripDate = @yesterday AND StartTime = '07:00')
    INSERT INTO bustrip (bus_id, TripDate, StartTime, ETA, PickupLocationId, DropLocationId, TripStatus, OperatingCost)
    VALUES (@busId1, @yesterday, '07:00', '08:30', @locMalabe,   @locMainGate, 'Completed', 2500.00);

IF NOT EXISTS (SELECT 1 FROM bustrip WHERE bus_id = @busId1 AND TripDate = @yesterday AND StartTime = '17:00')
    INSERT INTO bustrip (bus_id, TripDate, StartTime, ETA, PickupLocationId, DropLocationId, TripStatus, OperatingCost)
    VALUES (@busId1, @yesterday, '17:00', '18:30', @locMainGate, @locMalabe,   'Completed', 2500.00);

-- ── DAY -2  (1 Completed) ────────────────────────────────────────────────────
IF NOT EXISTS (SELECT 1 FROM bustrip WHERE bus_id = @busId2 AND TripDate = @dayMinus2 AND StartTime = '07:00')
    INSERT INTO bustrip (bus_id, TripDate, StartTime, ETA, PickupLocationId, DropLocationId, TripStatus, OperatingCost)
    VALUES (@busId2, @dayMinus2, '07:00', '08:30', @locNugegoda, @locMainGate, 'Completed', 2800.00);

-- ── TRIP ID LOOKUPS ──────────────────────────────────────────────────────────
-- T1  today 07:00 bus1 Malabe→MainGate       (Scheduled)
-- T2  today 07:30 bus2 Nugegoda→MainGate     (Scheduled)
-- T3  today 17:00 bus1 MainGate→Malabe       (Scheduled)
-- T4  today 17:30 bus2 MainGate→Kaduwela     (Scheduled)
-- T5  today 09:00 bus3 Kaduwela→MainGate     (Cancelled)
-- T6  tomorrow 07:00 bus3 Batta→MainGate     (Scheduled)
-- T7  tomorrow 17:00 bus4 MainGate→Batta     (Scheduled)
-- T8  dayPlus2 07:00 bus5 Homagama→MainGate  (Scheduled)
-- T9  yesterday 07:00 bus1 Malabe→MainGate   (Completed)
-- T10 yesterday 17:00 bus1 MainGate→Malabe   (Completed)
-- T11 dayMinus2 07:00 bus2 Nugegoda→MainGate (Completed)
DECLARE @tripT1  INT = (SELECT TripId FROM bustrip WHERE bus_id = @busId1 AND TripDate = @today      AND StartTime = '07:00');
DECLARE @tripT2  INT = (SELECT TripId FROM bustrip WHERE bus_id = @busId2 AND TripDate = @today      AND StartTime = '07:30');
DECLARE @tripT3  INT = (SELECT TripId FROM bustrip WHERE bus_id = @busId1 AND TripDate = @today      AND StartTime = '17:00');
DECLARE @tripT4  INT = (SELECT TripId FROM bustrip WHERE bus_id = @busId2 AND TripDate = @today      AND StartTime = '17:30');
DECLARE @tripT5  INT = (SELECT TripId FROM bustrip WHERE bus_id = @busId3 AND TripDate = @today      AND StartTime = '09:00');
DECLARE @tripT6  INT = (SELECT TripId FROM bustrip WHERE bus_id = @busId3 AND TripDate = @tomorrow   AND StartTime = '07:00');
DECLARE @tripT7  INT = (SELECT TripId FROM bustrip WHERE bus_id = @busId4 AND TripDate = @tomorrow   AND StartTime = '17:00');
DECLARE @tripT8  INT = (SELECT TripId FROM bustrip WHERE bus_id = @busId5 AND TripDate = @dayPlus2   AND StartTime = '07:00');
DECLARE @tripT9  INT = (SELECT TripId FROM bustrip WHERE bus_id = @busId1 AND TripDate = @yesterday  AND StartTime = '07:00');
DECLARE @tripT10 INT = (SELECT TripId FROM bustrip WHERE bus_id = @busId1 AND TripDate = @yesterday  AND StartTime = '17:00');
DECLARE @tripT11 INT = (SELECT TripId FROM bustrip WHERE bus_id = @busId2 AND TripDate = @dayMinus2  AND StartTime = '07:00');

-- =============================================================================
-- 9.  BOOKING
--     Statuses: PENDING | CONFIRMED | CANCELLED
--     Natural key: (user_id, trip_id)
--     fare_amount: DECIMAL(10,2) — no CHECK constraint, any non-null value is fine
--     BookingService.createBooking() checks TripStatus = 'Scheduled' — only the
--     seed script directly inserts bookings on Completed trips (for history).
-- =============================================================================

-- Today's Scheduled trips:
IF NOT EXISTS (SELECT 1 FROM booking WHERE user_id = @uJohn  AND trip_id = @tripT1)
    INSERT INTO booking (user_id, trip_id, pickup_loc_id, dropoff_loc_id, seat_number, fare_amount, status, created_at)
    VALUES (@uJohn,  @tripT1, @locMalabe,   @locMainGate, 5,  250.00, 'PENDING',    GETDATE());

IF NOT EXISTS (SELECT 1 FROM booking WHERE user_id = @uJohn  AND trip_id = @tripT3)
    INSERT INTO booking (user_id, trip_id, pickup_loc_id, dropoff_loc_id, seat_number, fare_amount, status, created_at)
    VALUES (@uJohn,  @tripT3, @locMainGate, @locMalabe,   5,  250.00, 'CONFIRMED',  GETDATE());

IF NOT EXISTS (SELECT 1 FROM booking WHERE user_id = @uSara  AND trip_id = @tripT2)
    INSERT INTO booking (user_id, trip_id, pickup_loc_id, dropoff_loc_id, seat_number, fare_amount, status, created_at)
    VALUES (@uSara,  @tripT2, @locNugegoda, @locMainGate, 8,  300.00, 'PENDING',    GETDATE());

IF NOT EXISTS (SELECT 1 FROM booking WHERE user_id = @uSara  AND trip_id = @tripT4)
    INSERT INTO booking (user_id, trip_id, pickup_loc_id, dropoff_loc_id, seat_number, fare_amount, status, created_at)
    VALUES (@uSara,  @tripT4, @locMainGate, @locKaduwela, 8,  350.00, 'PENDING',    GETDATE());

IF NOT EXISTS (SELECT 1 FROM booking WHERE user_id = @uKamal AND trip_id = @tripT1)
    INSERT INTO booking (user_id, trip_id, pickup_loc_id, dropoff_loc_id, seat_number, fare_amount, status, created_at)
    VALUES (@uKamal, @tripT1, @locMalabe,   @locMainGate, 12, 250.00, 'CONFIRMED',  GETDATE());

IF NOT EXISTS (SELECT 1 FROM booking WHERE user_id = @uKamal AND trip_id = @tripT2)
    INSERT INTO booking (user_id, trip_id, pickup_loc_id, dropoff_loc_id, seat_number, fare_amount, status, created_at)
    VALUES (@uKamal, @tripT2, @locNugegoda, @locMainGate, 15, 300.00, 'CANCELLED',  GETDATE());

-- Tomorrow:
IF NOT EXISTS (SELECT 1 FROM booking WHERE user_id = @uJohn  AND trip_id = @tripT6)
    INSERT INTO booking (user_id, trip_id, pickup_loc_id, dropoff_loc_id, seat_number, fare_amount, status, created_at)
    VALUES (@uJohn,  @tripT6, @locBatta,    @locMainGate, 5,  220.00, 'PENDING',    GETDATE());

IF NOT EXISTS (SELECT 1 FROM booking WHERE user_id = @uSara  AND trip_id = @tripT6)
    INSERT INTO booking (user_id, trip_id, pickup_loc_id, dropoff_loc_id, seat_number, fare_amount, status, created_at)
    VALUES (@uSara,  @tripT6, @locBatta,    @locMainGate, 10, 220.00, 'PENDING',    GETDATE());

-- Past completed trips (history for financial report + bookings page):
IF NOT EXISTS (SELECT 1 FROM booking WHERE user_id = @uJohn  AND trip_id = @tripT9)
    INSERT INTO booking (user_id, trip_id, pickup_loc_id, dropoff_loc_id, seat_number, fare_amount, status, created_at)
    VALUES (@uJohn,  @tripT9,  @locMalabe,   @locMainGate, 5, 250.00, 'CONFIRMED',
            DATEADD(day, -1, CAST(GETDATE() AS DATETIME2)));

IF NOT EXISTS (SELECT 1 FROM booking WHERE user_id = @uSara  AND trip_id = @tripT11)
    INSERT INTO booking (user_id, trip_id, pickup_loc_id, dropoff_loc_id, seat_number, fare_amount, status, created_at)
    VALUES (@uSara,  @tripT11, @locNugegoda, @locMainGate, 8, 300.00, 'CONFIRMED',
            DATEADD(day, -2, CAST(GETDATE() AS DATETIME2)));

IF NOT EXISTS (SELECT 1 FROM booking WHERE user_id = @uKamal AND trip_id = @tripT11)
    INSERT INTO booking (user_id, trip_id, pickup_loc_id, dropoff_loc_id, seat_number, fare_amount, status, created_at)
    VALUES (@uKamal, @tripT11, @locNugegoda, @locMainGate, 18, 300.00, 'CONFIRMED',
            DATEADD(day, -2, CAST(GETDATE() AS DATETIME2)));

-- ── BOOKING ID LOOKUPS ───────────────────────────────────────────────────────
DECLARE @bkJohnT1    BIGINT = (SELECT id FROM booking WHERE user_id = @uJohn  AND trip_id = @tripT1);
DECLARE @bkJohnT3    BIGINT = (SELECT id FROM booking WHERE user_id = @uJohn  AND trip_id = @tripT3);
DECLARE @bkSaraT2    BIGINT = (SELECT id FROM booking WHERE user_id = @uSara  AND trip_id = @tripT2);
DECLARE @bkSaraT4    BIGINT = (SELECT id FROM booking WHERE user_id = @uSara  AND trip_id = @tripT4);
DECLARE @bkKamalT1   BIGINT = (SELECT id FROM booking WHERE user_id = @uKamal AND trip_id = @tripT1);
DECLARE @bkJohnT9    BIGINT = (SELECT id FROM booking WHERE user_id = @uJohn  AND trip_id = @tripT9);
DECLARE @bkSaraT11   BIGINT = (SELECT id FROM booking WHERE user_id = @uSara  AND trip_id = @tripT11);
DECLARE @bkKamalT11  BIGINT = (SELECT id FROM booking WHERE user_id = @uKamal AND trip_id = @tripT11);

-- =============================================================================
-- 10. SEAT_RESERVATION  (PENDING + CONFIRMED bookings only; guarded by booking_id)
--     seat_number is unique per (bus, trip) by application convention.
--     Seats: bus1 cap 50, bus2 cap 50, bus3 cap 45, bus4 cap 50, bus5 cap 40.
-- =============================================================================
IF @bkJohnT1  IS NOT NULL AND NOT EXISTS (SELECT 1 FROM seat_reservation WHERE booking_id = @bkJohnT1)
    INSERT INTO seat_reservation (booking_id, bus_id, seat_number, reserved_at)
    VALUES (@bkJohnT1,  @busId1, 5,  GETDATE());

IF @bkJohnT3  IS NOT NULL AND NOT EXISTS (SELECT 1 FROM seat_reservation WHERE booking_id = @bkJohnT3)
    INSERT INTO seat_reservation (booking_id, bus_id, seat_number, reserved_at)
    VALUES (@bkJohnT3,  @busId1, 5,  GETDATE());

IF @bkSaraT2  IS NOT NULL AND NOT EXISTS (SELECT 1 FROM seat_reservation WHERE booking_id = @bkSaraT2)
    INSERT INTO seat_reservation (booking_id, bus_id, seat_number, reserved_at)
    VALUES (@bkSaraT2,  @busId2, 8,  GETDATE());

IF @bkSaraT4  IS NOT NULL AND NOT EXISTS (SELECT 1 FROM seat_reservation WHERE booking_id = @bkSaraT4)
    INSERT INTO seat_reservation (booking_id, bus_id, seat_number, reserved_at)
    VALUES (@bkSaraT4,  @busId2, 8,  GETDATE());

IF @bkKamalT1 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM seat_reservation WHERE booking_id = @bkKamalT1)
    INSERT INTO seat_reservation (booking_id, bus_id, seat_number, reserved_at)
    VALUES (@bkKamalT1, @busId1, 12, GETDATE());

-- Past completed-trip reservations (historical):
IF @bkJohnT9  IS NOT NULL AND NOT EXISTS (SELECT 1 FROM seat_reservation WHERE booking_id = @bkJohnT9)
    INSERT INTO seat_reservation (booking_id, bus_id, seat_number, reserved_at)
    VALUES (@bkJohnT9,  @busId1, 5,  DATEADD(day, -1, GETDATE()));

IF @bkSaraT11 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM seat_reservation WHERE booking_id = @bkSaraT11)
    INSERT INTO seat_reservation (booking_id, bus_id, seat_number, reserved_at)
    VALUES (@bkSaraT11, @busId2, 8,  DATEADD(day, -2, GETDATE()));

IF @bkKamalT11 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM seat_reservation WHERE booking_id = @bkKamalT11)
    INSERT INTO seat_reservation (booking_id, bus_id, seat_number, reserved_at)
    VALUES (@bkKamalT11, @busId2, 18, DATEADD(day, -2, GETDATE()));

-- =============================================================================
-- 11. ANNOUNCEMENTS  (guarded by Title)
-- =============================================================================
IF NOT EXISTS (SELECT 1 FROM Announcements WHERE Title = 'Welcome to UniTransport System')
    INSERT INTO Announcements (Title, Content, PostedByUserId, CreatedAt)
    VALUES ('Welcome to UniTransport System',
            N'The new campus transportation management system is now live. Book your seats online and track trips in real time.',
            @uAdmin, DATEADD(day, -30, GETDATE()));

IF NOT EXISTS (SELECT 1 FROM Announcements WHERE Title = 'Route Schedule Update – October 2025')
    INSERT INTO Announcements (Title, Content, PostedByUserId, CreatedAt)
    VALUES ('Route Schedule Update – October 2025',
            N'Morning express routes from Malabe and Nugegoda now depart 15 minutes earlier. Please check the updated timetable.',
            @uAdmin, DATEADD(day, -10, GETDATE()));

IF NOT EXISTS (SELECT 1 FROM Announcements WHERE Title = 'System Maintenance Notice')
    INSERT INTO Announcements (Title, Content, PostedByUserId, CreatedAt)
    VALUES ('System Maintenance Notice',
            N'The transport booking portal will be unavailable on Sunday 10 PM – 11 PM for scheduled maintenance. Please plan accordingly.',
            @uAdmin, DATEADD(day, -2, GETDATE()));

-- =============================================================================
-- 12. FEEDBACK
--     Columns (V1): FeedbackId, UserId INT FK, BookingId BIGINT NULL FK,
--                   Subject, Message, Rating INT NULL CHECK(NULL or 1-5),
--                   Status, SubmittedAt
--     Guarded by: (UserId, Subject)
-- =============================================================================
IF NOT EXISTS (SELECT 1 FROM Feedback WHERE UserId = @uJohn AND Subject = 'Great service on morning route')
    INSERT INTO Feedback (UserId, BookingId, Subject, Message, Rating, Status, SubmittedAt)
    VALUES (@uJohn, @bkJohnT9,
            'Great service on morning route',
            N'The morning Malabe express was on time and comfortable. Driver was very professional.',
            5, 'Reviewed', DATEADD(day, -1, GETDATE()));

IF NOT EXISTS (SELECT 1 FROM Feedback WHERE UserId = @uSara AND Subject = 'Bus was late by 20 minutes')
    INSERT INTO Feedback (UserId, BookingId, Subject, Message, Rating, Status, SubmittedAt)
    VALUES (@uSara, NULL,
            'Bus was late by 20 minutes',
            N'The Nugegoda route bus was significantly delayed this morning. Please improve punctuality.',
            3, 'Pending', DATEADD(day, -3, GETDATE()));

IF NOT EXISTS (SELECT 1 FROM Feedback WHERE UserId = @uKamal AND Subject = 'Seat reservation system works well')
    INSERT INTO Feedback (UserId, BookingId, Subject, Message, Rating, Status, SubmittedAt)
    VALUES (@uKamal, @bkKamalT11,
            'Seat reservation system works well',
            N'Easy to reserve seats online. The digital booking confirmation is very convenient for daily commute.',
            4, 'Pending', DATEADD(day, -2, GETDATE()));

-- =============================================================================
-- 13. SAVEDREPORTS  (guarded by ReportTitle)
-- =============================================================================
IF NOT EXISTS (SELECT 1 FROM SavedReports WHERE ReportTitle = 'Monthly Operations Summary – Sep 2025')
    INSERT INTO SavedReports (ReportTitle, ReportType, StartDate, EndDate, GeneratedByUserId, SummaryNotes, GeneratedAt)
    VALUES ('Monthly Operations Summary – Sep 2025', 'Operations',
            '2025-09-01', '2025-09-30', @uAdmin,
            N'Total 42 trips completed. Average occupancy 78%. Malabe route highest demand.',
            DATEADD(day, -29, GETDATE()));

IF NOT EXISTS (SELECT 1 FROM SavedReports WHERE ReportTitle = 'Quarterly Financial Report Q3 2025')
    INSERT INTO SavedReports (ReportTitle, ReportType, StartDate, EndDate, GeneratedByUserId, SummaryNotes, GeneratedAt)
    VALUES ('Quarterly Financial Report Q3 2025', 'Financial',
            '2025-07-01', '2025-09-30', @uJame,
            N'Gross revenue LKR 1,245,000. Operating cost LKR 780,000. Net surplus LKR 465,000.',
            DATEADD(day, -28, GETDATE()));

-- =============================================================================
-- 14. ADMINAUDITLOGS  (no UNIQUE constraint — guarded by (AdminUserId, ActionPerformed, TargetUserId))
-- =============================================================================
IF NOT EXISTS (SELECT 1 FROM AdminAuditLogs WHERE AdminUserId = @uAdmin AND ActionPerformed = 'USER_CREATED' AND TargetUserId = @uSara)
    INSERT INTO AdminAuditLogs (AdminUserId, ActionPerformed, TargetUserId, Timestamp)
    VALUES (@uAdmin, 'USER_CREATED', @uSara,  DATEADD(day, -5, GETDATE()));

IF NOT EXISTS (SELECT 1 FROM AdminAuditLogs WHERE AdminUserId = @uAdmin AND ActionPerformed = 'USER_CREATED' AND TargetUserId = @uKamal)
    INSERT INTO AdminAuditLogs (AdminUserId, ActionPerformed, TargetUserId, Timestamp)
    VALUES (@uAdmin, 'USER_CREATED', @uKamal, DATEADD(day, -4, GETDATE()));

IF NOT EXISTS (SELECT 1 FROM AdminAuditLogs WHERE AdminUserId = @uAdmin AND ActionPerformed = 'USER_CREATED' AND TargetUserId = @uTransport)
    INSERT INTO AdminAuditLogs (AdminUserId, ActionPerformed, TargetUserId, Timestamp)
    VALUES (@uAdmin, 'USER_CREATED', @uTransport, DATEADD(day, -3, GETDATE()));

IF NOT EXISTS (SELECT 1 FROM AdminAuditLogs WHERE AdminUserId = @uAdmin AND ActionPerformed = 'BUS_FLEET_UPDATED' AND TargetUserId IS NULL)
    INSERT INTO AdminAuditLogs (AdminUserId, ActionPerformed, TargetUserId, Timestamp)
    VALUES (@uAdmin, 'BUS_FLEET_UPDATED', NULL, DATEADD(day, -10, GETDATE()));

IF NOT EXISTS (SELECT 1 FROM AdminAuditLogs WHERE AdminUserId = @uAdmin AND ActionPerformed = 'ROUTES_CONFIGURED' AND TargetUserId IS NULL)
    INSERT INTO AdminAuditLogs (AdminUserId, ActionPerformed, TargetUserId, Timestamp)
    VALUES (@uAdmin, 'ROUTES_CONFIGURED', NULL, DATEADD(day, -3, GETDATE()));

-- =============================================================================
-- 15. EMERGENCY_REPORT  (guarded by Report_Title)
--     Columns (V2): Report_ID, Report_Title, Emergency_Type, Description,
--                   Timestamp (DEFAULT GETDATE()), Resolution_Status, user_id
-- =============================================================================
IF NOT EXISTS (SELECT 1 FROM Emergency_Report WHERE Report_Title = 'Bus Breakdown – NA-1003 on Kaduwela Route')
    INSERT INTO Emergency_Report (Report_Title, Emergency_Type, Description, Resolution_Status, user_id)
    VALUES ('Bus Breakdown – NA-1003 on Kaduwela Route',
            'Mechanical Failure',
            N'Bus NA-1003 experienced engine failure near Kaduwela Junction at 09:15. Passengers safely evacuated. Replacement bus dispatched. Trip cancelled.',
            'Resolved', @uTransport);

IF NOT EXISTS (SELECT 1 FROM Emergency_Report WHERE Report_Title = 'Medical Emergency on Bus NA-1001')
    INSERT INTO Emergency_Report (Report_Title, Emergency_Type, Description, Resolution_Status, user_id)
    VALUES ('Medical Emergency on Bus NA-1001',
            'Medical Emergency',
            N'A student passenger reported feeling unwell during the morning Malabe route. Bus diverted to nearest clinic. Passenger received first aid.',
            'Pending', @uJohn);

-- =============================================================================
-- 16. CRASH_INCIDENT  (guarded by (Location_Coordinates, Status))
--     Columns (V3): Incident_ID, bus_id BIGINT FK, user_id INT FK,
--                   Location_Coordinates, Severity_Level, Description,
--                   Timestamp (DEFAULT GETDATE()), Status
-- =============================================================================
IF NOT EXISTS (SELECT 1 FROM Crash_Incident WHERE Location_Coordinates = '6.9047,79.9737' AND Status = 'Resolved')
    INSERT INTO Crash_Incident (bus_id, user_id, Location_Coordinates, Severity_Level, Description, Status)
    VALUES (@busId3, @uPep,
            '6.9047,79.9737',
            'Minor',
            N'Minor fender collision at Malabe Town intersection. No injuries. Police report filed. Bus resumed service after 45 minutes.',
            'Resolved');

IF NOT EXISTS (SELECT 1 FROM Crash_Incident WHERE Location_Coordinates = '6.8780,79.8900' AND Status = 'Under Investigation')
    INSERT INTO Crash_Incident (bus_id, user_id, Location_Coordinates, Severity_Level, Description, Status)
    VALUES (@busId2, @uNimal,
            '6.8780,79.8900',
            'Moderate',
            N'Bus sideswiped a parked vehicle near Nugegoda town. Driver and all passengers unharmed. Vehicle damage under assessment.',
            'Under Investigation');

-- =============================================================================
-- 17. TRIP_STATUS  (for today's active trips; guarded by (TripId, Status_Type))
--     Column name in V5 table is TripId (NOT Trip_Id — see V5 migration comment)
-- =============================================================================
IF NOT EXISTS (SELECT 1 FROM Trip_Status WHERE TripId = @tripT1 AND Status_Type = 'Scheduled')
    INSERT INTO Trip_Status (TripId, Status_Type, Updated_At)
    VALUES (@tripT1, 'Scheduled', DATEADD(hour, -2, GETDATE()));

IF NOT EXISTS (SELECT 1 FROM Trip_Status WHERE TripId = @tripT1 AND Status_Type = 'Departed')
    INSERT INTO Trip_Status (TripId, Status_Type, Updated_At)
    VALUES (@tripT1, 'Departed',  DATEADD(minute, -90, GETDATE()));

IF NOT EXISTS (SELECT 1 FROM Trip_Status WHERE TripId = @tripT1 AND Status_Type = 'En Route')
    INSERT INTO Trip_Status (TripId, Status_Type, Updated_At)
    VALUES (@tripT1, 'En Route',  DATEADD(minute, -45, GETDATE()));

IF NOT EXISTS (SELECT 1 FROM Trip_Status WHERE TripId = @tripT2 AND Status_Type = 'Scheduled')
    INSERT INTO Trip_Status (TripId, Status_Type, Updated_At)
    VALUES (@tripT2, 'Scheduled', DATEADD(hour, -2, GETDATE()));

IF NOT EXISTS (SELECT 1 FROM Trip_Status WHERE TripId = @tripT5 AND Status_Type = 'Cancelled')
    INSERT INTO Trip_Status (TripId, Status_Type, Updated_At)
    VALUES (@tripT5, 'Cancelled', DATEADD(hour, -1, GETDATE()));

-- =============================================================================
-- 18. LOCATION_UPDATE  (GPS breadcrumb trail for today's trip T1 in progress)
--     Column name in V5 table is TripId (NOT Trip_Id)
--     Guarded by (TripId, Latitude, Longitude) — unique ping per position
-- =============================================================================
IF NOT EXISTS (SELECT 1 FROM Location_Update WHERE TripId = @tripT1 AND Latitude = 6.90470000 AND Longitude = 79.97370000)
    INSERT INTO Location_Update (TripId, Latitude, Longitude, Recorded_At)
    VALUES (@tripT1, 6.90470000, 79.97370000, DATEADD(minute, -90, GETDATE()));  -- Malabe Town (start)

IF NOT EXISTS (SELECT 1 FROM Location_Update WHERE TripId = @tripT1 AND Latitude = 6.89500000 AND Longitude = 79.96200000)
    INSERT INTO Location_Update (TripId, Latitude, Longitude, Recorded_At)
    VALUES (@tripT1, 6.89500000, 79.96200000, DATEADD(minute, -70, GETDATE()));  -- Athurugiriya approach

IF NOT EXISTS (SELECT 1 FROM Location_Update WHERE TripId = @tripT1 AND Latitude = 6.87300000 AND Longitude = 79.98420000)
    INSERT INTO Location_Update (TripId, Latitude, Longitude, Recorded_At)
    VALUES (@tripT1, 6.87300000, 79.98420000, DATEADD(minute, -50, GETDATE()));  -- Near Athurugiriya

IF NOT EXISTS (SELECT 1 FROM Location_Update WHERE TripId = @tripT1 AND Latitude = 6.84740000 AND Longitude = 80.00770000)
    INSERT INTO Location_Update (TripId, Latitude, Longitude, Recorded_At)
    VALUES (@tripT1, 6.84740000, 80.00770000, DATEADD(minute, -20, GETDATE()));  -- Homagama area

IF NOT EXISTS (SELECT 1 FROM Location_Update WHERE TripId = @tripT1 AND Latitude = 6.82190000 AND Longitude = 80.03990000)
    INSERT INTO Location_Update (TripId, Latitude, Longitude, Recorded_At)
    VALUES (@tripT1, 6.82190000, 80.03990000, DATEADD(minute,  -5, GETDATE()));  -- Approaching Main Gate

IF NOT EXISTS (SELECT 1 FROM Location_Update WHERE TripId = @tripT2 AND Latitude = 6.87800000 AND Longitude = 79.89000000)
    INSERT INTO Location_Update (TripId, Latitude, Longitude, Recorded_At)
    VALUES (@tripT2, 6.87800000, 79.89000000, DATEADD(minute, -60, GETDATE()));  -- Nugegoda (start)

IF NOT EXISTS (SELECT 1 FROM Location_Update WHERE TripId = @tripT2 AND Latitude = 6.87560000 AND Longitude = 79.92000000)
    INSERT INTO Location_Update (TripId, Latitude, Longitude, Recorded_At)
    VALUES (@tripT2, 6.87560000, 79.92000000, DATEADD(minute, -30, GETDATE()));  -- Maharagama area

-- =============================================================================
-- 19. INVOICE
--     Checks: billing_month 1-12, billing_year >= 2020,
--             total_amount >= 0, due_date >= issue_date
--     Guarded by (billing_month, billing_year, issue_date)
-- =============================================================================
IF NOT EXISTS (SELECT 1 FROM invoice WHERE billing_month = 9  AND billing_year = 2025 AND issue_date = '2025-09-01')
    INSERT INTO invoice (billing_month, billing_year, issue_date, due_date, total_amount)
    VALUES (9,  2025, '2025-09-01', '2025-09-30', 12500.00);

IF NOT EXISTS (SELECT 1 FROM invoice WHERE billing_month = 10 AND billing_year = 2025 AND issue_date = '2025-10-01')
    INSERT INTO invoice (billing_month, billing_year, issue_date, due_date, total_amount)
    VALUES (10, 2025, '2025-10-01', '2025-10-31', 15000.00);

IF NOT EXISTS (SELECT 1 FROM invoice WHERE billing_month = 11 AND billing_year = 2025 AND issue_date = '2025-11-01')
    INSERT INTO invoice (billing_month, billing_year, issue_date, due_date, total_amount)
    VALUES (11, 2025, '2025-11-01', '2025-11-30', 18000.00);

DECLARE @invSep BIGINT = (SELECT invoice_id FROM invoice WHERE billing_month = 9  AND billing_year = 2025 AND issue_date = '2025-09-01');
DECLARE @invOct BIGINT = (SELECT invoice_id FROM invoice WHERE billing_month = 10 AND billing_year = 2025 AND issue_date = '2025-10-01');
DECLARE @invNov BIGINT = (SELECT invoice_id FROM invoice WHERE billing_month = 11 AND billing_year = 2025 AND issue_date = '2025-11-01');

-- =============================================================================
-- 20. PAYMENT
--     Checks: amount > 0,
--             payment_status IN ('PAID','PENDING','FAILED','CANCELLED')  [V8 extended]
--     Guarded by (invoice_id, payment_date, payment_status)
-- =============================================================================
IF NOT EXISTS (SELECT 1 FROM payment WHERE invoice_id = @invSep AND payment_date = '2025-09-20' AND payment_status = N'PAID')
    INSERT INTO payment (invoice_id, amount, payment_date, payment_status)
    VALUES (@invSep, 12500.00, '2025-09-20', N'PAID');

IF NOT EXISTS (SELECT 1 FROM payment WHERE invoice_id = @invOct AND payment_date = '2025-10-15' AND payment_status = N'PAID')
    INSERT INTO payment (invoice_id, amount, payment_date, payment_status)
    VALUES (@invOct, 15000.00, '2025-10-15', N'PAID');

IF NOT EXISTS (SELECT 1 FROM payment WHERE invoice_id = @invNov AND payment_date = '2025-11-15' AND payment_status = N'PENDING')
    INSERT INTO payment (invoice_id, amount, payment_date, payment_status)
    VALUES (@invNov, 10000.00, '2025-11-15', N'PENDING');

IF NOT EXISTS (SELECT 1 FROM payment WHERE invoice_id = @invNov AND payment_date = '2025-11-10' AND payment_status = N'FAILED')
    INSERT INTO payment (invoice_id, amount, payment_date, payment_status)
    VALUES (@invNov,  5000.00, '2025-11-10', N'FAILED');

-- Cancelled payment (PaymentService.cancelPayment writes 'CANCELLED'; V8 added it to CHECK):
IF NOT EXISTS (SELECT 1 FROM payment WHERE invoice_id = @invOct AND payment_date = '2025-10-05' AND payment_status = N'CANCELLED')
    INSERT INTO payment (invoice_id, amount, payment_date, payment_status)
    VALUES (@invOct,  8000.00, '2025-10-05', N'CANCELLED');

DECLARE @pmtCancelled BIGINT = (
    SELECT payment_id FROM payment
    WHERE invoice_id = @invOct AND payment_date = '2025-10-05' AND payment_status = N'CANCELLED'
);

-- =============================================================================
-- 21. PAYMENTCANCELLATION  (one row per cancelled payment; guarded by PaymentId)
-- =============================================================================
IF @pmtCancelled IS NOT NULL AND NOT EXISTS (SELECT 1 FROM paymentcancellation WHERE PaymentId = @pmtCancelled)
    INSERT INTO paymentcancellation (PaymentId, Reason, CancelledAt)
    VALUES (@pmtCancelled,
            N'Payment disputed by finance department pending invoice re-verification.',
            GETDATE());

-- =============================================================================
-- 22. TRIPCANCELLATION  (one row per Cancelled trip; guarded by TripId)
-- =============================================================================
IF NOT EXISTS (SELECT 1 FROM tripcancellation WHERE TripId = @tripT5)
    INSERT INTO tripcancellation (TripId, Reason, CancelledAt)
    VALUES (@tripT5,
            N'Bus NA-1003 unavailable due to unscheduled maintenance. Passengers notified via SMS. Service resumes tomorrow.',
            GETDATE());

-- =============================================================================
COMMIT TRANSACTION;
PRINT 'dev-seed-test-data.sql committed successfully.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    DECLARE @msg NVARCHAR(4000) = ERROR_MESSAGE();
    DECLARE @sev INT             = ERROR_SEVERITY();
    DECLARE @sta INT             = ERROR_STATE();
    PRINT 'ROLLBACK — ' + @msg;
    RAISERROR(@msg, @sev, @sta);
END CATCH;
GO

-- =============================================================================
-- VERIFICATION 1: row count per table
-- =============================================================================
SELECT TableName, RowCount
FROM (
    SELECT 'Users'              AS TableName, COUNT(*) AS RowCount FROM Users              UNION ALL
    SELECT 'student',                         COUNT(*)             FROM student             UNION ALL
    SELECT 'driver',                          COUNT(*)             FROM driver              UNION ALL
    SELECT 'location',                        COUNT(*)             FROM location            UNION ALL
    SELECT 'bus',                             COUNT(*)             FROM bus                 UNION ALL
    SELECT 'BusRoute',                        COUNT(*)             FROM BusRoute            UNION ALL
    SELECT 'Destination',                     COUNT(*)             FROM Destination         UNION ALL
    SELECT 'bustrip',                         COUNT(*)             FROM bustrip             UNION ALL
    SELECT 'booking',                         COUNT(*)             FROM booking             UNION ALL
    SELECT 'seat_reservation',                COUNT(*)             FROM seat_reservation    UNION ALL
    SELECT 'Announcements',                   COUNT(*)             FROM Announcements       UNION ALL
    SELECT 'Feedback',                        COUNT(*)             FROM Feedback            UNION ALL
    SELECT 'SavedReports',                    COUNT(*)             FROM SavedReports        UNION ALL
    SELECT 'AdminAuditLogs',                  COUNT(*)             FROM AdminAuditLogs      UNION ALL
    SELECT 'Emergency_Report',                COUNT(*)             FROM Emergency_Report    UNION ALL
    SELECT 'Crash_Incident',                  COUNT(*)             FROM Crash_Incident      UNION ALL
    SELECT 'Trip_Status',                     COUNT(*)             FROM Trip_Status         UNION ALL
    SELECT 'Location_Update',                 COUNT(*)             FROM Location_Update     UNION ALL
    SELECT 'invoice',                         COUNT(*)             FROM invoice             UNION ALL
    SELECT 'payment',                         COUNT(*)             FROM payment             UNION ALL
    SELECT 'paymentcancellation',             COUNT(*)             FROM paymentcancellation UNION ALL
    SELECT 'tripcancellation',                COUNT(*)             FROM tripcancellation
) AS counts
ORDER BY TableName;
GO

-- =============================================================================
-- VERIFICATION 2: today's trips — bus, route, and booked seats
-- =============================================================================
SELECT
    bt.TripId,
    bt.TripDate,
    CONVERT(VARCHAR(5), bt.StartTime, 108)     AS Departs,
    CONVERT(VARCHAR(5), bt.ETA,       108)     AS ETA,
    bt.TripStatus,
    b.registration_number                      AS Bus,
    b.passenger_capacity                       AS Capacity,
    pl.LocationName                            AS From_Location,
    dl.LocationName                            AS To_Location,
    COUNT(bk.id)                               AS TotalBookings,
    SUM(CASE WHEN bk.status IN ('PENDING','CONFIRMED') THEN 1 ELSE 0 END) AS ActiveSeats,
    b.passenger_capacity
        - SUM(CASE WHEN bk.status IN ('PENDING','CONFIRMED') THEN 1 ELSE 0 END) AS SeatsRemaining
FROM       bustrip  bt
JOIN       bus      b   ON bt.bus_id           = b.bus_id
JOIN       location pl  ON bt.PickupLocationId = pl.LocationId
JOIN       location dl  ON bt.DropLocationId   = dl.LocationId
LEFT JOIN  booking  bk  ON bk.trip_id          = bt.TripId
WHERE bt.TripDate = CAST(GETDATE() AS DATE)
GROUP BY
    bt.TripId, bt.TripDate, bt.StartTime, bt.ETA, bt.TripStatus,
    b.registration_number, b.passenger_capacity,
    pl.LocationName, dl.LocationName
ORDER BY bt.StartTime;
GO
