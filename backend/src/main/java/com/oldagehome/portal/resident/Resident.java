package com.oldagehome.portal.resident;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Entity
@Table(name = "residents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Resident ID is required")
    @Column(name = "resident_id", unique = true, nullable = false, length = 50)
    private String residentId;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @NotBlank(message = "Gender is required")
    @Column(nullable = false, length = 10)
    private String gender; // MALE, FEMALE, OTHER

    @NotNull(message = "Date of Birth is required")
    @Past(message = "Date of Birth must be in the past")
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
    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResidentStatus status = ResidentStatus.ACTIVE;

    @Column(name = "guardian_name", length = 100)
    private String guardianName;

    @Pattern(regexp = "^$|[0-9]{10,15}$", message = "Guardian phone must be between 10 and 15 digits")
    @Column(name = "guardian_phone", length = 15)
    private String guardianPhone;

    @Column(name = "medical_notes", columnDefinition = "TEXT")
    private String medicalNotes;

    @Column(length = 255)
    private String photo; // Filepath of the uploaded image

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Hook to calculate age automatically before persist/update
    @PrePersist
    @PreUpdate
    public void calculateAge() {
        if (dateOfBirth != null) {
            this.age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        }
    }
}
