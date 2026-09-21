-- ============================================================================
-- FIX: replace placeholder "...hash" strings with REAL bcrypt hashes
-- Run this against CampusTransportDB after SQLQuery6.sql
-- ============================================================================
USE CampusTransportDB;
GO

-- admin@campus.edu      -> password: Admin@123
UPDATE Users SET PasswordHash = '$2b$10$kPwaigVHsH2DEEAvpEqcbO9xyACuaIr5wp/P75C8p6xRa.7VPp/me'
WHERE Email = 'admin@campus.edu';

-- finance@campus.edu    -> password: Finance@123
UPDATE Users SET PasswordHash = '$2b$10$rKsSoi0DdNTk/Tb8gY36oecrAcQtca9pLPsuP7Z2M5pkZZreEexnO'
WHERE Email = 'finance@campus.edu';

-- alex@student.campus.edu -> password: Student@123
UPDATE Users SET PasswordHash = '$2b$10$TDTET/k1bgndil/mf7DoT./xgEDQET6VbFt7BI46DfX3O93cjoQdq'
WHERE Email = 'alex@student.campus.edu';

-- jane@student.campus.edu -> password: Student@123 (account is Suspended, login should still be rejected)
UPDATE Users SET PasswordHash = '$2b$10$p4lS624dJDD13Iy3HuRY3.fLg9AjTL7hAYGpyY82q2Wqrow4SQkEG'
WHERE Email = 'jane@student.campus.edu';
GO

-- Verify
SELECT UserId, FullName, Email, LEFT(PasswordHash, 10) AS HashPreview, AccountStatus FROM Users;
GO
