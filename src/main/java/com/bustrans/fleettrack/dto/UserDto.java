package com.bustrans.fleettrack.dto;

import com.bustrans.fleettrack.entity.User;

public final class UserDto {
    private UserDto() {}

    public record UserResponse(
            Long userId,
            String fullName,
            String email,
            String phone,
            String roleName,
            String accountStatus
    ) {
        public static UserResponse fromEntity(User user) {
            return new UserResponse(
                    user.getUserId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getRoleName(),
                    user.getAccountStatus());
        }
    }

    public record UserRequest(
            String fullName,
            String email,
            String password,
            String phone,
            String roleName,
            String accountStatus,
            // Driver-only fields; null for all other roles.
            String licenseNumber,
            String dob,
            // Staff-role fields (ADMIN / FINANCE_OFFICER / TRANSPORT_OFFICER); null for other roles.
            String employeeId
    ) {}

    /** Student self-service: only fullName and phone may be changed. */
    public record UserSelfUpdateRequest(String fullName, String phone) {}
}
