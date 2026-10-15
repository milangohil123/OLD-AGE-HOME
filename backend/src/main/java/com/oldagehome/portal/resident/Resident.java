package com.oldagehome.portal.resident;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Entity
@Table(name = "residents")
@Data
public class Resident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @NotBlank(message = "Gender is required")
    @Column(nullable = false, length = 10)
    private String gender; // MALE, FEMALE, OTHER

    @NotNull(message = "Date of Birth is required")
    @Past(message = "Date of Birth must be in the past")
    @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Min(value = 0, message = "Age cannot be negative")
    private Integer age;

    @Column(name = "blood_group", length = 5)
    private String bloodGroup;

    @Pattern(regexp = "^$|[0-9]{10,15}$", message = "Mobile number must be between 10 and 15 digits")
    @Column(length = 15)
    private String mobile;

    @Email(message = "Please provide a valid email address")
    @Column(length = 100)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @NotNull(message = "Joining date is required")
    @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ResidentStatus status = ResidentStatus.ACTIVE;

    @NotBlank(message = "Guardian name is required")
    @Column(name = "guardian_name", length = 100)
    private String guardianName;

    @Pattern(regexp = "^$|[0-9]{10,15}$", message = "Guardian phone must be between 10 and 15 digits")
    @Column(name = "guardian_phone", length = 15)
    private String guardianPhone;

    @Email(message = "Please provide a valid guardian email address")
    @Column(name = "guardian_email", length = 100)
    private String guardianEmail;

    @Column(name = "guardian_address", columnDefinition = "TEXT")
    private String guardianAddress;

    @Column(name = "medical_notes", columnDefinition = "TEXT")
    private String medicalNotes;

    @Column(name = "medical_prescription", columnDefinition = "TEXT")
    private String medicalPrescription;

    @NotBlank(message = "Room number is required")
    @Column(name = "room_number", length = 20)
    private String roomNumber;

    @Column(name = "occupation", length = 100)
    private String occupation;

    @Column(name = "disability", length = 100)
    private String disability;

    @Pattern(regexp = "^$|[0-9]{12}$", message = "Aadhaar number must be exactly 12 digits")
    @Column(name = "aadhaar_number", length = 12)
    private String aadhaarNumber;

    @Column(length = 255)
    private String photo; // Filepath of the uploaded image

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Resident() {
    }

    @Builder
    public Resident(Long id, String fullName, String gender, LocalDate dateOfBirth, Integer age,
                    String bloodGroup, String mobile, String email, String address, LocalDate joiningDate,
                    ResidentStatus status, String guardianName, String guardianPhone, String guardianEmail,
                    String guardianAddress, String medicalNotes, String medicalPrescription, String roomNumber,
                    String occupation, String disability, String aadhaarNumber, String photo,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.fullName = fullName;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.age = age;
        this.bloodGroup = bloodGroup;
        this.mobile = mobile;
        this.email = email;
        this.address = address;
        this.joiningDate = joiningDate;
        this.status = status != null ? status : ResidentStatus.ACTIVE;
        this.guardianName = guardianName;
        this.guardianPhone = guardianPhone;
        this.guardianEmail = guardianEmail;
        this.guardianAddress = guardianAddress;
        this.medicalNotes = medicalNotes;
        this.medicalPrescription = medicalPrescription;
        this.roomNumber = roomNumber;
        this.occupation = occupation;
        this.disability = disability;
        this.aadhaarNumber = aadhaarNumber;
        this.photo = photo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Hook to calculate age automatically before persist/update
    @PrePersist
    @PreUpdate
    public void calculateAge() {
        if (dateOfBirth != null) {
            this.age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        }
    }
}
