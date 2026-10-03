-- V8__payment_slip_and_invoice_unique.sql
-- Additive migration. To be merged into the base migration later.
-- Adds slip-upload and review columns to payment; adds a filtered unique index
-- on invoice so each student can have at most one invoice per billing month.
-- All new columns are nullable so existing rows stay valid without flyway:clean.

-- ============================================================
-- 1. payment — slip upload and officer review columns
-- ============================================================

IF NOT EXISTS (SELECT * FROM sys.columns
               WHERE object_id = OBJECT_ID('dbo.payment') AND name = 'slip_file_name')
    ALTER TABLE dbo.payment ADD slip_file_name VARCHAR(100) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns
               WHERE object_id = OBJECT_ID('dbo.payment') AND name = 'slip_original_name')
    ALTER TABLE dbo.payment ADD slip_original_name VARCHAR(255) NULL;

IF NOT EXISTS (SELECT * FROM sys.columns
               WHERE object_id = OBJECT_ID('dbo.payment') AND name = 'submitted_by_user_id')
    ALTER TABLE dbo.payment ADD submitted_by_user_id INT NULL;

IF NOT EXISTS (SELECT * FROM sys.columns
               WHERE object_id = OBJECT_ID('dbo.payment') AND name = 'reviewed_by_user_id')
    ALTER TABLE dbo.payment ADD reviewed_by_user_id INT NULL;

IF NOT EXISTS (SELECT * FROM sys.columns
               WHERE object_id = OBJECT_ID('dbo.payment') AND name = 'reviewed_at')
    ALTER TABLE dbo.payment ADD reviewed_at DATETIME2 NULL;

IF NOT EXISTS (SELECT * FROM sys.columns
               WHERE object_id = OBJECT_ID('dbo.payment') AND name = 'review_note')
    ALTER TABLE dbo.payment ADD review_note VARCHAR(255) NULL;

-- FKs to Users (only safe to add after columns exist)
IF NOT EXISTS (SELECT * FROM sys.foreign_keys
               WHERE name = 'FK_payment_submitted_by' AND parent_object_id = OBJECT_ID('dbo.payment'))
    ALTER TABLE dbo.payment
        ADD CONSTRAINT FK_payment_submitted_by
            FOREIGN KEY (submitted_by_user_id) REFERENCES dbo.Users(UserId);

IF NOT EXISTS (SELECT * FROM sys.foreign_keys
               WHERE name = 'FK_payment_reviewed_by' AND parent_object_id = OBJECT_ID('dbo.payment'))
    ALTER TABLE dbo.payment
        ADD CONSTRAINT FK_payment_reviewed_by
            FOREIGN KEY (reviewed_by_user_id) REFERENCES dbo.Users(UserId);

-- ============================================================
-- 2. invoice — filtered unique index
--    Allows at most one invoice per (student, billing_month, billing_year).
--    WHERE student_user_id IS NOT NULL excludes manually created invoices
--    (student_user_id NULL) so they do not trigger uniqueness conflicts.
-- ============================================================

IF NOT EXISTS (SELECT * FROM sys.indexes
               WHERE name = 'UX_invoice_student_month_year'
               AND object_id = OBJECT_ID('dbo.invoice'))
    CREATE UNIQUE NONCLUSTERED INDEX UX_invoice_student_month_year
        ON dbo.invoice (student_user_id, billing_month, billing_year)
        WHERE student_user_id IS NOT NULL;
