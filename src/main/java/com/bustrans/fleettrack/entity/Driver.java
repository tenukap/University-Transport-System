package com.bustrans.fleettrack.entity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "driver")
public class Driver {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "license_number", nullable = false, unique = true)
    private String licenseNumber;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "status")
    private String status = "Available";

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
