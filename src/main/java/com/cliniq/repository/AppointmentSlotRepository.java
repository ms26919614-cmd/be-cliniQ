package com.cliniq.repository;

import com.cliniq.entity.AppointmentSlot;
import com.cliniq.enums.DayOfWeekEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Long> {

    List<AppointmentSlot> findByDayOfWeekOrderByStartTimeAsc(DayOfWeekEnum dayOfWeek);

    List<AppointmentSlot> findByDayOfWeekAndActiveOrderByStartTimeAsc(DayOfWeekEnum dayOfWeek, boolean active);

    List<AppointmentSlot> findByActiveOrderByDayOfWeekAscStartTimeAsc(boolean active);

    List<AppointmentSlot> findAllByOrderByDayOfWeekAscStartTimeAsc();

    boolean existsByDayOfWeekAndStartTime(DayOfWeekEnum dayOfWeek, java.time.LocalTime startTime);
}
