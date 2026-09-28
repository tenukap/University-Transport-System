-- V8: Transport Officer module tables

-- paymentcancellation: log of cancelled payments
-- Matches PaymentCancelation.java (@Table("paymentcancellation"))
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'paymentcancellation')
BEGIN
CREATE TABLE paymentcancellation (
    CancellationId INT IDENTITY(1,1) NOT NULL,
    PaymentId      INT NOT NULL,
    Reason         NVARCHAR(255) NULL,
    CancelledAt    DATETIME DEFAULT GETDATE(),
    PRIMARY KEY (CancellationId)
);
END;

-- tripcancellation (double-L): log of cancelled trips
-- Matches TripCancellation.java (@Table("tripcancellation"))
-- Note: V1 already has a separate "TripCancelation" (single-L) table; this is a different table.
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'tripcancellation')
BEGIN
CREATE TABLE tripcancellation (
    CancellationId INT IDENTITY(1,1) NOT NULL,
    TripId         INT NOT NULL,
    Reason         NVARCHAR(255) NULL,
    CancelledAt    DATETIME DEFAULT GETDATE(),
    PRIMARY KEY (CancellationId),
    CONSTRAINT FK_tripcancellation_bustrip FOREIGN KEY (TripId)
        REFERENCES bustrip(TripId)
);
END;

-- Extend the payment status constraint to allow 'CANCELLED'
-- (required by PaymentService.cancelPayment which sets status = 'CANCELLED')
IF EXISTS (SELECT * FROM sys.check_constraints
           WHERE name = 'chk_payment_status'
             AND parent_object_id = OBJECT_ID('payment'))
    ALTER TABLE payment DROP CONSTRAINT chk_payment_status;
ALTER TABLE payment ADD CONSTRAINT chk_payment_status
    CHECK (payment_status IN (N'PAID', N'PENDING', N'FAILED', N'CANCELLED'));
