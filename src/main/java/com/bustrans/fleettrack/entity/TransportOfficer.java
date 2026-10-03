package com.bustrans.fleettrack.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "transport_officer")
public class TransportOfficer {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    // Optional — not collected at creation time; can be set later.
    @Column(name = "depot")
    private String depot;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getDepot() { return depot; }
    public void setDepot(String depot) { this.depot = depot; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
}
