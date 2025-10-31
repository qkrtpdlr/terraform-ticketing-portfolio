package com.ticketing.dto;

import com.ticketing.model.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    private Long id;
    private String eventName;
    private Integer totalSeats;
    private Integer availableSeats;
    private LocalDateTime eventDate;
    private LocalDateTime createdAt;

    public static EventResponse from(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .eventName(event.getEventName())
                .totalSeats(event.getTotalSeats())
                .availableSeats(event.getAvailableSeats())
                .eventDate(event.getEventDate())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
