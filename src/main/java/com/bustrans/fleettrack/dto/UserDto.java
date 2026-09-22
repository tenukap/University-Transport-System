package com.bustrans.fleettrack.dto;

import com.bustrans.fleettrack.repository.UserRepository.UserRecord;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

public final class UserDto {
    private UserDto() {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UserResponse(
            long id,
            String fullName,
            String name,
            String email,
            String roleName,
            String role,
            String accountStatus,
            String status,
            OffsetDateTime createdAt
    ) {
        public static UserResponse fromRecord(UserRecord record) {
            return new UserResponse(
                    record.userId(),
                    record.fullName(),
                    record.fullName(),
                    record.email(),
                    record.roleName(),
                    record.roleName(),
                    record.accountStatus(),
                    record.accountStatus(),
                    record.createdAt()
            );
        }
    }

    public record CreateUserRequest(
            String fullName,
            String name,
            String email,
            String password,
            String roleName,
            String role
    ) {
        public String getEffectiveName() {
            if (fullName != null && !fullName.isBlank()) return fullName.trim();
            if (name != null && !name.isBlank()) return name.trim();
            return "";
        }

        public String getEffectiveRole() {
            if (roleName != null && !roleName.isBlank()) return roleName.trim();
            if (role != null && !role.isBlank()) return role.trim();
            return "";
        }
    }

    public record UpdateUserRequest(
            String fullName,
            String name,
            String roleName,
            String role,
            String accountStatus,
            String status
    ) {
        public String getEffectiveName() {
            if (fullName != null && !fullName.isBlank()) return fullName.trim();
            if (name != null && !name.isBlank()) return name.trim();
            return null;
        }

        public String getEffectiveRole() {
            if (roleName != null && !roleName.isBlank()) return roleName.trim();
            if (role != null && !role.isBlank()) return role.trim();
            return null;
        }

        public String getEffectiveStatus() {
            if (accountStatus != null && !accountStatus.isBlank()) return accountStatus.trim();
            if (status != null && !status.isBlank()) return status.trim();
            return null;
        }
    }
}
