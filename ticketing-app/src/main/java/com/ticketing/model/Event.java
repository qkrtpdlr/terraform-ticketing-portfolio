package com.ticketing.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventName;

    @Column(nullable = false)
    private Integer totalSeats;

    @Column(nullable = false)
    private Integer availableSeats;

    @Column(nullable = false)
    private LocalDateTime eventDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (availableSeats == null) {
            availableSeats = totalSeats;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 좌석 차감 (동시성 제어는 Service 계층에서 Redis Lock 사용)
     */
    public void decreaseSeats(Integer quantity) {
        if (this.availableSeats < quantity) {
            throw new IllegalStateException("좌석이 부족합니다");
        }
        this.availableSeats -= quantity;
    }

    /**
     * 좌석 복구 (예매 취소 시)
     */
    public void increaseSeats(Integer quantity) {
        this.availableSeats += quantity;
        if (this.availableSeats > this.totalSeats) {
            this.availableSeats = this.totalSeats;
        }
    }
}
