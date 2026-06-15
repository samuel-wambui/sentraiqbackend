package com.senctraiq.shifts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {

    List<Shift> findAllByOrderByStartTimeAsc();

    List<Shift> findByIsActiveTrue();
    Optional<Shift> findByIdAndIsActiveTrue(Long id);

    Optional<Object> findByIdAndDeletedFalse(Long id);

    List<Shift>findByDeletedFalse();

    long countByDeletedFalseAndIsActiveTrue();

    Optional<Shift> findByShiftNameAndDeletedFalse(String shiftName);
}

