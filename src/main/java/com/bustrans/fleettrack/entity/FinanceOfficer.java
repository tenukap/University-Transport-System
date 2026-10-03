package com.bustrans.fleettrack.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "finance_officer")
public class FinanceOfficer {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    // Optional — not collected at creation time; can be set later.
    @Column(name = "approval_limit")
    private BigDecimal approvalLimit;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public BigDecimal getApprovalLimit() { return approvalLimit; }
    public void setApprovalLimit(BigDecimal approvalLimit) { this.approvalLimit = approvalLimit; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
}
