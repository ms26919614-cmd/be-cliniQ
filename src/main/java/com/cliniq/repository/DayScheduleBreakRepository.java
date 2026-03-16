package com.cliniq.repository;

import com.cliniq.entity.DayScheduleBreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DayScheduleBreakRepository extends JpaRepository<DayScheduleBreak, Long> {

    List<DayScheduleBreak> findByDayScheduleIdOrderByBreakStartAsc(Long dayScheduleId);

    void deleteByDayScheduleId(Long dayScheduleId);
}
