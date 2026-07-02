    package com.oldagehome.portal.settings.entity;

    import jakarta.persistence.*;
    import java.time.LocalDateTime;
    import org.hibernate.annotations.CreationTimestamp;
    import org.hibernate.annotations.UpdateTimestamp;

    /**
     * Entity representing a configurable system setting stored as key/value pair.
     */
    @Entity
    @Table(name = "system_setting", uniqueConstraints = {@UniqueConstraint(columnNames = "setting_key")})
    public class SystemSetting {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "setting_key", nullable = false, length = 100)
        private String settingKey;

        @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
        private String settingValue;

        @Column(length = 255)
        private String description;

        @CreationTimestamp
        @Column(name = "created_at", updatable = false)
        private LocalDateTime createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at")
        private LocalDateTime updatedAt;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getSettingKey() { return settingKey; }
        public void setSettingKey(String settingKey) { this.settingKey = settingKey; }
        public String getSettingValue() { return settingValue; }
        public void setSettingValue(String settingValue) { this.settingValue = settingValue; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
