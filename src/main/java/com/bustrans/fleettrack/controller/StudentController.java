package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.dto.StudentResponseDTO;
import com.bustrans.fleettrack.dto.StudentUpdateDTO;
import com.bustrans.fleettrack.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/{id}")
    public StudentResponseDTO getStudentById(@PathVariable Long id) {
        return studentService.getStudent(id);
    }

    @GetMapping("/user/{userId}")
    public StudentResponseDTO getStudentByUserId(@PathVariable Long userId) {
        return studentService.getStudentByUserId(userId);
    }

    @PutMapping("/{id}")
    public StudentResponseDTO updateStudent(@PathVariable Long id,
                                            @RequestBody StudentUpdateDTO updateDTO,
                                            Authentication authentication) {
        enforceStudentSelfOnly(id, authentication);
        return studentService.updateStudent(id, updateDTO);
    }

    /**
     * A STUDENT may only update their own student row (path id must match their JWT userId).
     * ADMIN bypasses this check.
     */
    private void enforceStudentSelfOnly(Long id, Authentication authentication) {
        if (authentication == null) return;
        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        if (!isStudent) return;
        try {
            Long callerId = Long.parseLong(authentication.getName());
            if (!callerId.equals(id)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Students may only update their own profile");
            }
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token");
        }
    }
}
