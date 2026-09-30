package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.dto.StudentResponseDTO;
import com.bustrans.fleettrack.dto.StudentUpdateDTO;
import com.bustrans.fleettrack.entity.Student;
import com.bustrans.fleettrack.entity.User;
import com.bustrans.fleettrack.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentResponseDTO getStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        return mapToDTO(student);
    }

    public StudentResponseDTO getStudentByUserId(Long userId) {
        Student student = studentRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException("Student not found with userId: " + userId));
        return mapToDTO(student);
    }

    public StudentResponseDTO updateStudent(Long id, StudentUpdateDTO updateDTO) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));

        if (updateDTO.getSemester() != null) {
            student.setSemester(updateDTO.getSemester());
        }

        if (updateDTO.getStudentIndex() != null) {
            String trimmed = updateDTO.getStudentIndex().trim();
            if (trimmed.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student index cannot be blank");
            }
            student.setStudentIndex(trimmed);
        }

        try {
            return mapToDTO(studentRepository.save(student));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student index is already in use");
        }
    }

    private StudentResponseDTO mapToDTO(Student student) {
        User u = student.getUser();
        return StudentResponseDTO.builder()
                .id(student.getUserId())
                .studentIndex(student.getStudentIndex())
                .fullName(u != null ? u.getFullName() : null)
                .phone(u != null ? u.getPhone() : null)
                .email(u != null ? u.getEmail() : null)
                .semester(student.getSemester())
                .build();
    }
}
