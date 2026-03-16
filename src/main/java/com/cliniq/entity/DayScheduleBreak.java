package com.cliniq.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "day_schedule_breaks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DayScheduleBreak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_schedule_id", nullable = false)
    private DaySchedule daySchedule;

    @Column(name = "break_start", nullable = false)
    private LocalTime breakStart;

    @Column(name = "break_end", nullable = false)
    private LocalTime breakEnd;

    @Column(name = "label")
    private String label;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
