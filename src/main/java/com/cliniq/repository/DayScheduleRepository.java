package com.cliniq.repository;

import com.cliniq.entity.DaySchedule;
import com.cliniq.enums.DayOfWeekEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DayScheduleRepository extends JpaRepository<DaySchedule, Long> {

    Optional<DaySchedule> findByDayOfWeek(DayOfWeekEnum dayOfWeek);

    List<DaySchedule> findAllByOrderByDayOfWeekAsc();

    List<DaySchedule> findByWorkingDayOrderByDayOfWeekAsc(boolean workingDay);
}
