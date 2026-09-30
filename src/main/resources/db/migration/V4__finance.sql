-- V4__finance.sql
-- invoice, payment, paymentcancellation.
-- Depends on: V1 (student), V3 (booking — not directly referenced here, but ordering safe).

-- ============================================================
-- 1. invoice
--    Added: student_user_id INT NULL → student(user_id).
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'invoice')
BEGIN
    CREATE TABLE invoice (
        invoice_id      BIGINT IDENTITY(1,1) NOT NULL,
        billing_month   INT NOT NULL,
        billing_year    INT NOT NULL,
        issue_date      DATE NOT NULL,
        due_date        DATE NOT NULL,
        total_amount    DECIMAL(10,2) NOT NULL,
        student_user_id INT NULL,
        CONSTRAINT PK_invoice PRIMARY KEY (invoice_id),
        CONSTRAINT chk_invoice_month CHECK (billing_month BETWEEN 1 AND 12),
        CONSTRAINT chk_invoice_year CHECK (billing_year >= 2020),
        CONSTRAINT chk_invoice_amount CHECK (total_amount >= 0),
        CONSTRAINT chk_invoice_dates CHECK (due_date >= issue_date),
        CONSTRAINT FK_invoice_student FOREIGN KEY (student_user_id)
            REFERENCES student(user_id)
    );
END;

-- Index: invoice.student_user_id
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_invoice_student_user_id'
               AND object_id = OBJECT_ID('invoice'))
    CREATE NONCLUSTERED INDEX IX_invoice_student_user_id ON invoice(student_user_id);

-- ============================================================
-- 2. payment
--    status CHECK includes CANCELLED from the start (baked in; no ALTER needed).
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'payment')
BEGIN
    CREATE TABLE payment (
        payment_id     BIGINT IDENTITY(1,1) NOT NULL,
        invoice_id     BIGINT NOT NULL,
        amount         DECIMAL(10,2) NOT NULL,
        payment_date   DATE NOT NULL,
        payment_status NVARCHAR(50) NOT NULL,
        CONSTRAINT PK_payment PRIMARY KEY (payment_id),
        CONSTRAINT chk_payment_amount CHECK (amount > 0),
        CONSTRAINT chk_payment_status CHECK (
            payment_status IN (N'PAID', N'PENDING', N'FAILED', N'CANCELLED')
        ),
        CONSTRAINT fk_payment_invoice FOREIGN KEY (invoice_id)
            REFERENCES dbo.invoice(invoice_id) ON DELETE NO ACTION ON UPDATE NO ACTION
    );
END;

-- Index: payment.invoice_id
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'ix_payment_invoice_id'
               AND object_id = OBJECT_ID('dbo.payment'))
    CREATE NONCLUSTERED INDEX ix_payment_invoice_id ON dbo.payment(invoice_id);

-- ============================================================
-- 3. paymentcancellation
--    PaymentId is BIGINT (matches payment.payment_id BIGINT IDENTITY).
--    FK → payment(payment_id) added.
-- ============================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'paymentcancellation')
BEGIN
    CREATE TABLE paymentcancellation (
        CancellationId INT IDENTITY(1,1) NOT NULL,
        PaymentId      BIGINT NOT NULL,
        Reason         NVARCHAR(255) NULL,
        CancelledAt    DATETIME NULL CONSTRAINT DF_paycancel_CancelledAt DEFAULT GETDATE(),
        CONSTRAINT PK_paymentcancellation PRIMARY KEY (CancellationId),
        CONSTRAINT FK_paycancel_payment FOREIGN KEY (PaymentId)
            REFERENCES payment(payment_id)
    );
END;

-- Index: paymentcancellation.PaymentId
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_paycancel_PaymentId'
               AND object_id = OBJECT_ID('paymentcancellation'))
    CREATE NONCLUSTERED INDEX IX_paycancel_PaymentId ON paymentcancellation(PaymentId);
