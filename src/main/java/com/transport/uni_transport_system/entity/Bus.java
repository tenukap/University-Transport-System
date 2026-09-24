package com.transport.uni_transport_system.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "bus")
public class Bus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long busId;

    @Column(nullable = false, unique = true)
    private String registrationNumber;

    @Column(nullable = false)
    private int passengerCapacity;

    private String status = "Available";

    //  Getters and Setters

    public Long getBusId() { return busId; }
    public void setBusId(Long busId) { this.busId = busId; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public int getPassengerCapacity() { return passengerCapacity; }
    public void setPassengerCapacity(int passengerCapacity) { this.passengerCapacity = passengerCapacity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }


}