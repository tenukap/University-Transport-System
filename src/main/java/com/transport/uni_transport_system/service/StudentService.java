package com.transport.uni_transport_system.service;

import com.transport.uni_transport_system.dto.StudentResponseDTO;
import com.transport.uni_transport_system.dto.StudentUpdateDTO;
import com.transport.uni_transport_system.entity.Student;
import com.transport.uni_transport_system.repository.StudentRepository;
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
