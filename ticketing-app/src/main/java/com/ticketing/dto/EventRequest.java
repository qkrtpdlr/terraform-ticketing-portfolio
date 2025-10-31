package com.ticketing.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {

    @NotBlank(message = "이벤트 이름은 필수입니다")
    private String eventName;

    @NotNull(message = "총 좌석 수는 필수입니다")
    @Min(value = 1, message = "최소 1석 이상이어야 합니다")
    private Integer totalSeats;

    @NotNull(message = "이벤트 날짜는 필수입니다")
    @Future(message = "이벤트 날짜는 미래여야 합니다")
    private LocalDateTime eventDate;
}
