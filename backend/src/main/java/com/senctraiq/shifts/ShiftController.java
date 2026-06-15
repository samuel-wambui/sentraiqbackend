package com.senctraiq.shifts;

import com.senctraiq.ApiResponse.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@CrossOrigin(origins = "http://localhost:8080")
public class ShiftController {

    @Autowired
    private ShiftService shiftService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Shift>> createShift(@RequestBody ShiftDto shiftDto) {
        ApiResponse<Shift> response = new ApiResponse<>();

        try {
            Shift shift = shiftService.createNewShift(shiftDto);

            response.setEntity(shift);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Shift created successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to create shift: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/active-now")
    public ResponseEntity<ApiResponse<Shift>> getActiveShiftNow() {
        ApiResponse<Shift> response = new ApiResponse<>();

        try {
            Shift shift = shiftService.getActiveShiftNow();

            response.setEntity(shift);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Active shift retrieved successfully");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.setStatusCode(HttpStatus.NOT_FOUND.value());
            response.setMessage(e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Internal server error");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/active-by-time")
    public ResponseEntity<ApiResponse<Shift>> getActiveShiftByTime(@RequestParam LocalTime time) {
        ApiResponse<Shift> response = new ApiResponse<>();

        try {
            Shift shift = shiftService.getActiveShiftByTime(time);

            response.setEntity(shift);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Active shift retrieved successfully");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.setStatusCode(HttpStatus.NOT_FOUND.value());
            response.setMessage(e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Internal server error");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/active-list")
    public ResponseEntity<ApiResponse<List<Shift>>> getActiveShiftsNow() {
        ApiResponse<List<Shift>> response = new ApiResponse<>();

        try {
            List<Shift> shifts = shiftService.getActiveShiftsNow();

            if (shifts.isEmpty()) {
                response.setStatusCode(HttpStatus.NOT_FOUND.value());
                response.setMessage("No active shifts found");
                response.setEntity(null);

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            response.setEntity(shifts);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Active shifts retrieved successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Internal server error");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<String>> deleteShift(@PathVariable Long id) {
        ApiResponse<String> response = new ApiResponse<>();

        try {
            String message = shiftService.deleteShift(id);

            response.setEntity(message);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage(message);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.setStatusCode(HttpStatus.NOT_FOUND.value());
            response.setMessage(e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Internal server error");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}