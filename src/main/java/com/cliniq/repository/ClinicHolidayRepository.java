package com.cliniq.repository;

import com.cliniq.entity.ClinicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClinicHolidayRepository extends JpaRepository<ClinicHoliday, Long> {

    Optional<ClinicHoliday> findByHolidayDate(LocalDate holidayDate);

    boolean existsByHolidayDate(LocalDate holidayDate);

    List<ClinicHoliday> findAllByOrderByHolidayDateAsc();

    List<ClinicHoliday> findByHolidayDateBetweenOrderByHolidayDateAsc(LocalDate startDate, LocalDate endDate);
}
