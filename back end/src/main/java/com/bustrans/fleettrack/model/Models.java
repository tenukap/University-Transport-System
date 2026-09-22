package com.bustrans.fleettrack.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class Models {
    private Models() {
    }

    public record DashboardSummary(
            Metric activeBookings,
            Metric upcomingTrips,
            Metric busUtilisation,
            Metric activeUsers,
            List<User> recentUsers,
            List<Feedback> recentFeedback) {
    }

    public record Metric(long value, String displayValue, String trend, String trendDirection, String period) {
    }

    public record User(long id, String name, String email, String role, String status, OffsetDateTime createdAt) {
    }

        public record LoginRequest(@NotBlank String username, @NotBlank String password) {
        }

        public record LoginResponse(String token, User user) {
        }

    public record Profile(long id, String fullName, String email, String phone, String role,
                          String groupName, String accountStatus, OffsetDateTime memberSince) {
    }

    public record UpdateProfileRequest(
            @NotBlank String fullName,
            @NotBlank String email,
            String phone) {
    }

    public record CreateUserRequest(
            @NotBlank String name,
            @NotBlank String email,
            @NotBlank String role,
            @NotBlank String password) {
    }

    public record UpdateUserRequest(
            @NotBlank String role,
            @NotBlank String status) {
    }

    public record Booking(
            long id,
            String route,
            String passenger,
            LocalDate travelDate,
            String departureTime,
            String status,
            BigDecimal fare) {
    }

    public record CreateBookingRequest(
            @NotNull Long tripId,
            @NotNull Long userId,
            Integer seatNumber,
            @NotNull @Positive BigDecimal fare) {
    }

    public record Feedback(long id, String author, String message, OffsetDateTime createdAt) {
    }

    public record Announcement(
            long id,
            String title,
            String message,
            OffsetDateTime createdAt,
            String status) {
    }

    public record CreateAnnouncementRequest(
            @NotBlank String title,
            @NotBlank String message) {
    }

    public record FinancialReport(
            LocalDate startDate,
            LocalDate endDate,
            String route,
            String studentGroup,
            long bookingCount,
            BigDecimal grossRevenue,
            List<Booking> bookings) {
    }
}
