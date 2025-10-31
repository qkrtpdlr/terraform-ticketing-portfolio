package com.ticketing.dto;

import com.ticketing.model.Reservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

    private Long id;
    private Long eventId;
    private String userId;
    private Integer quantity;
    private String status;
    private LocalDateTime createdAt;

    public static ReservationResponse from(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .eventId(reservation.getEventId())
                .userId(reservation.getUserId())
                .quantity(reservation.getQuantity())
                .status(reservation.getStatus().name())
                .createdAt(reservation.getCreatedAt())
                .build();
    }

    public static ReservationResponse success(Reservation reservation) {
        return from(reservation);
    }
}
