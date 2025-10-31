package com.ticketing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {

    @NotNull(message = "이벤트 ID는 필수입니다")
    private Long eventId;

    @NotNull(message = "사용자 ID는 필수입니다")
    private String userId;

    @NotNull(message = "수량은 필수입니다")
    @Min(value = 1, message = "최소 1장 이상 예매해야 합니다")
    private Integer quantity;
}
