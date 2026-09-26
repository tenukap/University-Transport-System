-- Extends the team V1/V2 lineage. Feedback and Users remain team-owned.
CREATE TABLE dbo.invoice (
    invoice_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    billing_month INT NOT NULL,
    billing_year INT NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    CONSTRAINT chk_invoice_month CHECK (billing_month BETWEEN 1 AND 12),
    CONSTRAINT chk_invoice_year CHECK (billing_year >= 2020),
    CONSTRAINT chk_invoice_amount CHECK (total_amount >= 0),
    CONSTRAINT chk_invoice_dates CHECK (due_date >= issue_date)
);

CREATE TABLE dbo.payment (
    payment_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_date DATE NOT NULL,
    payment_status NVARCHAR(50) NOT NULL,
    CONSTRAINT chk_payment_amount CHECK (amount > 0),
    CONSTRAINT chk_payment_status CHECK (payment_status IN (N'PAID', N'PENDING', N'FAILED')),
    CONSTRAINT fk_payment_invoice FOREIGN KEY (invoice_id)
        REFERENCES dbo.invoice(invoice_id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE INDEX ix_payment_invoice_id ON dbo.payment(invoice_id);
