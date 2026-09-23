package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.dto.StudentResponseDTO;
import com.bustrans.fleettrack.dto.StudentUpdateDTO;
import com.bustrans.fleettrack.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/{id}")
    public StudentResponseDTO getStudentById(@PathVariable Long id) {
        return studentService.getStudent(id);
    }

    @PutMapping("/{id}")
    public StudentResponseDTO updateStudent(@PathVariable Long id,
                                            @RequestBody StudentUpdateDTO updateDTO) {
        return studentService.updateStudent(id, updateDTO);
    }
}
