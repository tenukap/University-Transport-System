package com.transport.uni_transport_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Emergency_Report")
public class EmergencyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Report_ID")
    private Integer reportId;

    @Column(name = "Report_Title", nullable = false, length = 255)
    private String reportTitle;

    @Column(name = "Emergency_Type", nullable = false, length = 100)
    private String emergencyType;

    @Column(name = "Description", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "Timestamp", insertable = false)
    private LocalDateTime timestamp;

    @Column(name = "Resolution_Status", length = 50)
    private String resolutionStatus = "Pending";

    @Column(name = "Student_No")
    private Integer studentNo;

    @Column(name = "Officer_No")
    private Integer officerNo;

    public Integer getReportId() {
        return reportId;
    }

    public void setReportId(Integer reportId) {
        this.reportId = reportId;
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public String getEmergencyType() {
        return emergencyType;
    }

    public void setEmergencyType(String emergencyType) {
        this.emergencyType = emergencyType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getResolutionStatus() {
        return resolutionStatus;
    }

    public void setResolutionStatus(String resolutionStatus) {
        this.resolutionStatus = resolutionStatus;
    }

    public Integer getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(Integer studentNo) {
        this.studentNo = studentNo;
    }

    public Integer getOfficerNo() {
        return officerNo;
    }

    public void setOfficerNo(Integer officerNo) {
        this.officerNo = officerNo;
    }
}