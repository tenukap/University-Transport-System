package com.transport.uni_transport_system.entity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "driver")
public class Driver {
    @Id
    private Long userId; // Linked to  Users table

    @Column(nullable = false, unique = true)
    private String licenseNumber;

    private LocalDate dob;

    private String status = "Available";

    //  Getters and Setters


    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}