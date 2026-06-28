package com.oldagehome.portal.settings.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "user_preferences")
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "theme", length = 20)
    private String theme = "Blue";

    @Column(name = "sidebar_collapsed")
    private boolean sidebarCollapsed = false;

    @Column(name = "language", length = 10)
    private String language = "en";

    @Column(name = "email_notifications")
    private boolean emailNotifications = true;

    @Column(name = "sms_notifications")
    private boolean smsNotifications = false;

    @Column(name = "browser_notifications")
    private boolean browserNotifications = true;

    @Column(name = "items_per_page")
    private int itemsPerPage = 10;

    @Column(name = "default_dashboard_page", length = 50)
    private String defaultDashboardPage = "Dashboard";

    @Column(name = "font_size", length = 20)
    private String fontSize = "Medium";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public boolean isSidebarCollapsed() { return sidebarCollapsed; }
    public void setSidebarCollapsed(boolean sidebarCollapsed) { this.sidebarCollapsed = sidebarCollapsed; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public boolean isEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(boolean emailNotifications) { this.emailNotifications = emailNotifications; }
    public boolean isSmsNotifications() { return smsNotifications; }
    public void setSmsNotifications(boolean smsNotifications) { this.smsNotifications = smsNotifications; }
    public boolean isBrowserNotifications() { return browserNotifications; }
    public void setBrowserNotifications(boolean browserNotifications) { this.browserNotifications = browserNotifications; }
    public int getItemsPerPage() { return itemsPerPage; }
    public void setItemsPerPage(int itemsPerPage) { this.itemsPerPage = itemsPerPage; }
    public String getDefaultDashboardPage() { return defaultDashboardPage; }
    public void setDefaultDashboardPage(String defaultDashboardPage) { this.defaultDashboardPage = defaultDashboardPage; }
    public String getFontSize() { return fontSize; }
    public void setFontSize(String fontSize) { this.fontSize = fontSize; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
