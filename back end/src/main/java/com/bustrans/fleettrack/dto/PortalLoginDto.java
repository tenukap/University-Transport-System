package com.bustrans.fleettrack.dto;

import com.bustrans.fleettrack.dto.UserDto.UserResponse;

public final class PortalLoginDto {
    private PortalLoginDto() {}

    public record PortalLoginRequest(
            String email,
            String username,
            String password
    ) {
        public String getEffectiveEmail() {
            if (email != null && !email.isBlank()) return email.trim();
            if (username != null && !username.isBlank()) return username.trim();
            return "";
        }
    }

    public record PortalLoginResponse(
            String token,
            UserResponse user
    ) {}

    public record PortalMessageResponse(
            String message
    ) {}
}
