package com.bustrans.fleettrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackRequestDTO {

    private Long bookingId;
    private Integer rating;
    private String comment;
}
