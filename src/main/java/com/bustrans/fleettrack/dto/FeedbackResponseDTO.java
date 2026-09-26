package com.bustrans.fleettrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackResponseDTO {

    private Integer id;
    private Long bookingId;
    private Long userId;
    private Integer rating;
    private String comment;
    private String status;
    private LocalDateTime submittedAt;
}
