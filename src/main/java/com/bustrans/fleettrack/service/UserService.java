package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.dto.UserDto.UserRequest;
import com.bustrans.fleettrack.dto.UserDto.UserResponse;
import com.bustrans.fleettrack.entity.Student;
import com.bustrans.fleettrack.entity.User;
import com.bustrans.fleettrack.repository.StudentRepository;
import com.bustrans.fleettrack.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    // Needed to BCrypt-hash passwords on create/update (AuthService verifies with BCrypt).
    private final PasswordEncoder passwordEncoder;
    // Used to auto-create a Student profile when a STUDENT-role user is added.
    private final StudentRepository studentRepository;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       StudentRepository studentRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.studentRepository = studentRepository;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return UserResponse.fromEntity(user);
    }

    // Transactional so the User and its auto-created Student profile are committed together;
    // if the Student save fails the whole registration is rolled back (FK stays consistent).
    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (request.email() != null && userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email already in use: " + request.email());
        }

        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                // TEMP: storing plaintext for testing only — REVERT to passwordEncoder.encode() before production.
                .passwordHash(request.password())
                .phone(request.phone())
                .roleName(request.roleName() != null ? request.roleName().toUpperCase() : null)
                .accountStatus("Active")
                .createdAt(now)
                .updatedAt(now)
                .build();
        User savedUser = userRepository.save(user);

        // Option B: a STUDENT-role user needs a matching Student row, otherwise
        // StudentService.getStudentByUserId() throws "Student not found" on login.
        if ("STUDENT".equals(savedUser.getRoleName())) {
            try {
                Student student = new Student();
                student.setUser(savedUser); // FK link to Users.UserId
                // student_index is NOT NULL + UNIQUE in the schema. No index is sent on
                // creation yet, so seed a unique placeholder derived from the user id.
                // TODO: capture the real student index from the registration request.
                student.setStudentIndex("PENDING-" + savedUser.getUserId());
                // full_name is NOT NULL in the schema; fall back to the email if missing.
                student.setFullName(savedUser.getFullName() != null
                        ? savedUser.getFullName()
                        : savedUser.getEmail());
                student.setPhone(savedUser.getPhone());
                studentRepository.save(student);
            } catch (RuntimeException ex) {
                // Rolls back the transaction (including the User) to keep the tables in sync.
                throw new RuntimeException("Failed to create student profile", ex);
            }
        }

        return UserResponse.fromEntity(savedUser);
    }

    public UserResponse updateUser(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.email() != null) {
            userRepository.findByEmail(request.email())
                    .filter(existing -> !existing.getUserId().equals(id))
                    .ifPresent(existing -> { throw new RuntimeException("Email already in use"); });
            user.setEmail(request.email());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.roleName() != null) {
            user.setRoleName(request.roleName().toUpperCase());
        }
        user.setUpdatedAt(LocalDateTime.now());
        return UserResponse.fromEntity(userRepository.save(user));
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
