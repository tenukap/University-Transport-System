package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.dto.StudentResponseDTO;
import com.bustrans.fleettrack.dto.StudentUpdateDTO;
import com.bustrans.fleettrack.entity.Student;
import com.bustrans.fleettrack.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentResponseDTO getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return mapToResponseDTO(student);
    }

    public StudentResponseDTO updateStudent(Long id, StudentUpdateDTO updateDTO) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (updateDTO.getFullName() != null) {
            student.setFullName(updateDTO.getFullName());
        }
        if (updateDTO.getPhone() != null) {
            student.setPhone(updateDTO.getPhone());
        }

        student = studentRepository.save(student);
        return mapToResponseDTO(student);
    }

    private StudentResponseDTO mapToResponseDTO(Student student) {
        return StudentResponseDTO.builder()
                .id(student.getId())
                .studentIndex(student.getStudentIndex())
                .fullName(student.getFullName())
                .phone(student.getPhone())
                .email(student.getUser().getEmail())
                .build();
    }
}
