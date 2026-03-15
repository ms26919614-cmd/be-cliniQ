package com.cliniq.repository;

import com.cliniq.entity.DayOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DayOverrideRepository extends JpaRepository<DayOverride, Long> {

    Optional<DayOverride> findByOverrideDate(LocalDate overrideDate);

    boolean existsByOverrideDate(LocalDate overrideDate);

    List<DayOverride> findAllByOrderByOverrideDateAsc();

    List<DayOverride> findByOverrideDateBetweenOrderByOverrideDateAsc(LocalDate start, LocalDate end);
}
