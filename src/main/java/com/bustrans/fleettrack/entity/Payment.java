package com.bustrans.fleettrack.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Nationalized
    @Column(name = "payment_status", nullable = false, length = 50)
    private String paymentStatus;

    // ── Slip upload fields (nullable; absent on manually created payments) ──

    /** Server-generated UUID filename stored on disk — never the original upload name. */
    @Column(name = "slip_file_name", length = 100)
    private String slipFileName;

    /** Original filename as supplied by the student, kept for display only. */
    @Column(name = "slip_original_name", length = 255)
    private String slipOriginalName;

    /** Student who submitted the slip. */
    @Column(name = "submitted_by_user_id")
    private Integer submittedByUserId;

    // ── Review fields (filled when a finance officer approves or rejects) ──

    @Column(name = "reviewed_by_user_id")
    private Integer reviewedByUserId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", length = 255)
    private String reviewNote;

    public Payment() {
    }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getSlipFileName() { return slipFileName; }
    public void setSlipFileName(String slipFileName) { this.slipFileName = slipFileName; }

    public String getSlipOriginalName() { return slipOriginalName; }
    public void setSlipOriginalName(String slipOriginalName) { this.slipOriginalName = slipOriginalName; }

    public Integer getSubmittedByUserId() { return submittedByUserId; }
    public void setSubmittedByUserId(Integer submittedByUserId) { this.submittedByUserId = submittedByUserId; }

    public Integer getReviewedByUserId() { return reviewedByUserId; }
    public void setReviewedByUserId(Integer reviewedByUserId) { this.reviewedByUserId = reviewedByUserId; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
}
