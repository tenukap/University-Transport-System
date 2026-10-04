-- Additive migration. To be merged into the base migration later.
-- Adds admin review fields and a per-booking uniqueness constraint to Feedback.

-- admin_response: the admin's written reply to a student's feedback
IF NOT EXISTS (SELECT * FROM sys.columns
               WHERE object_id = OBJECT_ID('dbo.Feedback') AND name = 'admin_response')
    ALTER TABLE dbo.Feedback ADD admin_response NVARCHAR(1000) NULL;

-- reviewed_at: timestamp when the admin first (or last) reviewed the feedback
IF NOT EXISTS (SELECT * FROM sys.columns
               WHERE object_id = OBJECT_ID('dbo.Feedback') AND name = 'reviewed_at')
    ALTER TABLE dbo.Feedback ADD reviewed_at DATETIME2 NULL;

-- reviewed_by_id: which admin user performed the review
IF NOT EXISTS (SELECT * FROM sys.columns
               WHERE object_id = OBJECT_ID('dbo.Feedback') AND name = 'reviewed_by_id')
BEGIN
    ALTER TABLE dbo.Feedback ADD reviewed_by_id INT NULL;
    ALTER TABLE dbo.Feedback ADD CONSTRAINT FK_Feedback_ReviewedBy
        FOREIGN KEY (reviewed_by_id) REFERENCES dbo.Users(UserId);
END;

-- Filtered unique index: one feedback per booking, but NULL bookingId (from old Thymeleaf path) is excluded.
IF NOT EXISTS (SELECT * FROM sys.indexes
               WHERE name = 'UIX_Feedback_User_Booking'
               AND object_id = OBJECT_ID('dbo.Feedback'))
    CREATE UNIQUE NONCLUSTERED INDEX UIX_Feedback_User_Booking
        ON dbo.Feedback(UserId, BookingId)
        WHERE BookingId IS NOT NULL;
