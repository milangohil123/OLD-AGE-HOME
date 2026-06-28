package com.oldagehome.portal.settings.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class PreferencesDTO {

    private String theme = "Blue";
    private boolean sidebarCollapsed;
    private String language = "en";
    private boolean emailNotifications;
    private boolean smsNotifications;
    private boolean browserNotifications;

    @Min(value = 5, message = "Items per page must be at least 5")
    @Max(value = 100, message = "Items per page must not exceed 100")
    private int itemsPerPage = 10;

    @NotBlank(message = "Default dashboard page is required")
    private String defaultDashboardPage = "Dashboard";

    private String fontSize = "Medium";

    // Getters and Setters
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
}
