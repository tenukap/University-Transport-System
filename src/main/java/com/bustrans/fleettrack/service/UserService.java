package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.dto.UserDto.UserRequest;
import com.bustrans.fleettrack.dto.UserDto.UserResponse;
import com.bustrans.fleettrack.dto.UserDto.UserSelfUpdateRequest;
import com.bustrans.fleettrack.entity.*;
import com.bustrans.fleettrack.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private static final Set<String> ALLOWED_ACCOUNT_STATUSES = Set.of("Active", "Suspended", "Deactivated");

    private final UserRepository userRepository;
    // Needed to BCrypt-hash passwords on create/update (AuthService verifies with BCrypt).
    private final PasswordEncoder passwordEncoder;
    private final StudentRepository studentRepository;
    private final DriverRepository driverRepository;
    private final AdminRepository adminRepository;
    private final FinanceOfficerRepository financeOfficerRepository;
    private final TransportOfficerRepository transportOfficerRepository;
    // Used by deleteUser: a student with bookings cannot be hard-deleted.
    private final BookingRepository bookingRepository;
    // Used by deleteUser / setUserStatus: trips guard driver deletion and deactivation.
    private final BusTripRepository busTripRepository;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       StudentRepository studentRepository,
                       DriverRepository driverRepository,
                       AdminRepository adminRepository,
                       FinanceOfficerRepository financeOfficerRepository,
                       TransportOfficerRepository transportOfficerRepository,
                       BookingRepository bookingRepository,
                       BusTripRepository busTripRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.studentRepository = studentRepository;
        this.driverRepository = driverRepository;
        this.adminRepository = adminRepository;
        this.financeOfficerRepository = financeOfficerRepository;
        this.transportOfficerRepository = transportOfficerRepository;
        this.bookingRepository = bookingRepository;
        this.busTripRepository = busTripRepository;
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

    // Transactional so the User and its child profile row are committed together;
    // if the profile save fails the whole registration is rolled back (FK stays consistent).
    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (request.email() != null && userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email already in use: " + request.email());
        }

        String upperRole = request.roleName() != null ? request.roleName().toUpperCase() : "";

        // Validate role-specific fields BEFORE saving the user so no orphan user row is left on error.
        if ("DRIVER".equals(upperRole)) {
            validateLicense(request);
        } else if ("ADMIN".equals(upperRole) || "FINANCE_OFFICER".equals(upperRole) || "TRANSPORT_OFFICER".equals(upperRole)) {
            validateEmployeeId(upperRole, request);
        }

        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                // TODO(security): plaintext for dev only — switch back to passwordEncoder.encode() before final submission
                .passwordHash(request.password())
                .phone(request.phone())
                .roleName(request.roleName() != null ? request.roleName().toUpperCase() : null)
                .accountStatus("Active")
                .createdAt(now)
                .updatedAt(now)
                .build();
        User savedUser = userRepository.save(user);

        // Create the matching child row for each role that requires one.
        try {
            switch (savedUser.getRoleName()) {
                case "STUDENT" -> {
                    Student student = new Student();
                    student.setUserId(savedUser.getUserId());
                    // student_index is nullable; set later via PUT /api/students/{id}
                    studentRepository.save(student);
                }
                case "DRIVER" -> {
                    Driver driver = new Driver();
                    driver.setUserId(savedUser.getUserId());
                    driver.setLicenseNumber(request.licenseNumber().trim());
                    if (request.dob() != null && !request.dob().isBlank()) {
                        try {
                            driver.setDob(LocalDate.parse(request.dob()));
                        } catch (Exception e) {
                            throw new RuntimeException("Date of birth must be in YYYY-MM-DD format");
                        }
                    }
                    driverRepository.save(driver);
                }
                case "ADMIN" -> {
                    Admin admin = new Admin();
                    admin.setUserId(savedUser.getUserId());
                    admin.setEmployeeId(request.employeeId().trim());
                    // access_level defaults to STANDARD as per the schema DEFAULT
                    admin.setAccessLevel("STANDARD");
                    adminRepository.save(admin);
                }
                case "FINANCE_OFFICER" -> {
                    FinanceOfficer fo = new FinanceOfficer();
                    fo.setUserId(savedUser.getUserId());
                    fo.setEmployeeId(request.employeeId().trim());
                    financeOfficerRepository.save(fo);
                }
                case "TRANSPORT_OFFICER" -> {
                    TransportOfficer to = new TransportOfficer();
                    to.setUserId(savedUser.getUserId());
                    to.setEmployeeId(request.employeeId().trim());
                    transportOfficerRepository.save(to);
                }
                // No child table for roles not listed above.
            }
        } catch (RuntimeException ex) {
            // Re-throw so @Transactional rolls back the user row too.
            throw new RuntimeException("Failed to create role profile: " + ex.getMessage(), ex);
        }

        return UserResponse.fromEntity(savedUser);
    }

    private void validateLicense(UserRequest request) {
        String lic = request.licenseNumber() != null ? request.licenseNumber().trim() : "";
        if (lic.isEmpty()) {
            throw new RuntimeException("License number is required for Driver accounts");
        }
        // GlobalExceptionHandler maps "already in use" → 409 Conflict.
        if (driverRepository.existsByLicenseNumber(lic)) {
            throw new RuntimeException("License number already in use: " + lic);
        }
    }

    private void validateEmployeeId(String role, UserRequest request) {
        String empId = request.employeeId() != null ? request.employeeId().trim() : "";
        if (empId.isEmpty()) {
            throw new RuntimeException("Employee ID is required for " + role + " accounts");
        }
        if (empId.length() > 20) {
            throw new RuntimeException("Employee ID must be 20 characters or fewer");
        }
        // Each role's table has its own UNIQUE constraint; check only the relevant table.
        boolean taken = switch (role) {
            case "ADMIN" -> adminRepository.existsByEmployeeId(empId);
            case "FINANCE_OFFICER" -> financeOfficerRepository.existsByEmployeeId(empId);
            case "TRANSPORT_OFFICER" -> transportOfficerRepository.existsByEmployeeId(empId);
            default -> false;
        };
        if (taken) {
            throw new RuntimeException("Employee ID already in use: " + empId);
        }
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
            // TODO(security): plaintext for dev only — switch back to passwordEncoder.encode() before final submission
            user.setPasswordHash(request.password());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.roleName() != null) {
            user.setRoleName(request.roleName().toUpperCase());
        }
        if (request.accountStatus() != null) {
            if (!ALLOWED_ACCOUNT_STATUSES.contains(request.accountStatus())) {
                throw new IllegalArgumentException(
                        "Invalid account status '" + request.accountStatus()
                        + "'. Allowed values: Active, Suspended, Deactivated");
            }
            user.setAccountStatus(request.accountStatus());
        }
        user.setUpdatedAt(LocalDateTime.now());
        return UserResponse.fromEntity(userRepository.save(user));
    }

    /**
     * Activates or deactivates a user. Never touches bookings, trips or any other history rows.
     * Business guards: admin cannot deactivate self; last active admin cannot be deactivated;
     * a driver with upcoming non-cancelled trips cannot be deactivated.
     */
    public UserResponse setUserStatus(Long targetId, boolean activate, Long callerId) {
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + targetId));

        if (!activate) {
            if (targetId.equals(callerId)) {
                throw new RuntimeException("An admin cannot deactivate their own account");
            }
            if ("ADMIN".equals(target.getRoleName())) {
                // Count how many admins are still active; block if this is the last one.
                long activeAdmins = userRepository.countByRoleNameAndAccountStatus("ADMIN", "Active");
                if (activeAdmins <= 1) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Cannot deactivate the last active administrator");
                }
            }
            if ("DRIVER".equals(target.getRoleName())) {
                long upcoming = busTripRepository.countUpcomingTripsByDriver(targetId.intValue(), LocalDate.now());
                if (upcoming > 0) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Cannot deactivate driver: they are assigned to " + upcoming
                            + " upcoming trip(s). Reassign those trips first.");
                }
            }
            target.setAccountStatus("Deactivated");
        } else {
            target.setAccountStatus("Active");
        }

        target.setUpdatedAt(LocalDateTime.now());
        return UserResponse.fromEntity(userRepository.save(target));
    }

    /**
     * Hard-deletes a user. Only allowed when the user has NO history.
     * A student with any bookings, or a driver assigned to any trip, must be deactivated instead.
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        String role = user.getRoleName();

        // Prevent silent data loss: if there is any linked history, the caller must deactivate.
        if ("STUDENT".equals(role) && bookingRepository.existsByUser_UserId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This user has history. Deactivate instead.");
        }
        if ("DRIVER".equals(role) && busTripRepository.existsByDriverUserId(id.intValue())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This user has history. Deactivate instead.");
        }

        // All child profile tables (student, driver, admin, …) have ON DELETE CASCADE, so
        // deleting the Users row is sufficient when there is no booking/trip history.
        userRepository.deleteById(id);
    }

    /** Student self-service update — only fullName and phone are applied; all other fields ignored. */
    public UserResponse updateUserSelf(Long id, UserSelfUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        if (request.fullName() != null) user.setFullName(request.fullName());
        if (request.phone() != null)    user.setPhone(request.phone());
        user.setUpdatedAt(LocalDateTime.now());
        return UserResponse.fromEntity(userRepository.save(user));
    }
}
