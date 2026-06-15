package com.senctraiq.shifts;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class ShiftService {

    @Autowired
    private ShiftRepository shiftRepository;

    public Shift createNewShift(ShiftDto shiftDto) {
        try {
            Shift shift = new Shift();
            shift.setShiftName(shiftDto.getShiftName());
            shift.setCreatedBy(shiftDto.getCreatedBy());
            shift.setCreationTime(LocalDateTime.now());
            shift.setStartTime(shiftDto.getStartTime());
            shift.setEndTime(shiftDto.getEndTime());
            shift.setDeleted(false);

            return shiftRepository.save(shift);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create shift: " + e.getMessage());
        }
    }

    private boolean isShiftActiveAtTime(Shift shift, LocalTime time) {
        LocalTime start = shift.getStartTime();
        LocalTime end = shift.getEndTime();

        if (start == null || end == null || time == null) {
            return false;
        }

        if (!start.isAfter(end)) {
            return !time.isBefore(start) && !time.isAfter(end);
        } else {
            return !time.isBefore(start) || !time.isAfter(end);
        }
    }

    public List<Shift> getActiveShiftsNow() {
        try {
            LocalTime now = LocalTime.now();

            return shiftRepository.findByDeletedFalse().stream()
                    .filter(shift -> isShiftActiveAtTime(shift, now))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve active shifts: " + e.getMessage());
        }
    }

    public String deleteShift(Long id) {
        try {
            Shift shift = (Shift) shiftRepository.findByIdAndDeletedFalse(id)
                    .orElseThrow(() -> new RuntimeException("Shift not found"));

            shift.setDeleted(true);
            shiftRepository.save(shift);

            return "Shift deleted successfully";
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete shift: " + e.getMessage());
        }
    }

    public Shift getActiveShiftNow() {
        try {
            LocalTime now = LocalTime.now();
            return getActiveShiftByTime(now);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve active shift: " + e.getMessage());
        }
    }

    public Shift getActiveShiftByTime(LocalTime time) {
        try {
            List<Shift> activeShifts = shiftRepository.findByDeletedFalse().stream()
                    .filter(shift -> isShiftActiveAtTime(shift, time))
                    .toList();

            if (activeShifts.isEmpty()) {
                throw new RuntimeException("No active shift found for time: " + time);
            }

            if (activeShifts.size() > 1) {
                throw new RuntimeException("Multiple active shifts found. Shift configuration overlaps.");
            }

            return activeShifts.get(0);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve shift by time: " + e.getMessage());
        }
    }
}