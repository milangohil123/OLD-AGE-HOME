package com.oldagehome.portal.settings.service.impl;

import com.oldagehome.portal.settings.entity.SystemSetting;
import com.oldagehome.portal.settings.entity.UserPreference;
import com.oldagehome.portal.settings.repository.SystemSettingRepository;
import com.oldagehome.portal.settings.repository.UserPreferenceRepository;
import com.oldagehome.portal.settings.dto.ProfileDTO;
import com.oldagehome.portal.settings.dto.PasswordChangeDTO;
import com.oldagehome.portal.settings.dto.PreferencesDTO;
import com.oldagehome.portal.settings.dto.OfficeInfoDTO;
import com.oldagehome.portal.settings.service.SettingsService;
import com.oldagehome.portal.auth.User;
import com.oldagehome.portal.auth.UserRepository;
import com.oldagehome.portal.audit.AuditService;
import com.oldagehome.portal.audit.AuditModule;
import com.oldagehome.portal.audit.AuditAction;
import com.oldagehome.portal.utils.FileUploadUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class SettingsServiceImpl implements SettingsService {

    private final SystemSettingRepository systemSettingRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final FileUploadUtility fileUploadUtility;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public SettingsServiceImpl(SystemSettingRepository systemSettingRepository,
                               UserPreferenceRepository userPreferenceRepository,
                               UserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               AuditService auditService,
                               FileUploadUtility fileUploadUtility,
                               JdbcTemplate jdbcTemplate) {
        this.systemSettingRepository = systemSettingRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.fileUploadUtility = fileUploadUtility;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemSetting> getSettings() {
        return systemSettingRepository.findAll();
    }

    @Override
    public void saveSettings(Map<String, String> settings) {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            SystemSetting setting = systemSettingRepository.findBySettingKey(entry.getKey())
                    .orElse(new SystemSetting());
            setting.setSettingKey(entry.getKey());
            setting.setSettingValue(entry.getValue());
            systemSettingRepository.save(setting);
        }
        auditService.logActivity(AuditModule.SETTINGS, AuditAction.UPDATE, "Updated system settings", "SystemSetting", null, true, null);
    }

    @Override
    public void updateProfile(Long userId, ProfileDTO profileDTO, MultipartFile profilePhoto) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate username uniqueness if changed
        if (!user.getUsername().equals(profileDTO.getUsername()) && userRepository.existsByUsername(profileDTO.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        // Validate email uniqueness if changed
        if (!user.getEmail().equals(profileDTO.getEmail()) && userRepository.existsByEmail(profileDTO.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        user.setFullName(profileDTO.getFullName());
        user.setEmail(profileDTO.getEmail());
        user.setMobile(profileDTO.getMobile());
        user.setUsername(profileDTO.getUsername());

        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            String path = fileUploadUtility.saveFile("profiles", profilePhoto);
            user.setProfilePicture(path);
        }

        userRepository.save(user);
        
        // Refresh SecurityContext with new username
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth != null) {
            org.springframework.security.core.userdetails.User newPrincipal = 
                new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPassword(),
                    currentAuth.getAuthorities()
                );
            UsernamePasswordAuthenticationToken newAuth = 
                new UsernamePasswordAuthenticationToken(newPrincipal, currentAuth.getCredentials(), currentAuth.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        }

        auditService.logActivity(AuditModule.SETTINGS, AuditAction.UPDATE, "Updated profile for user: " + user.getUsername(), "User", user.getId(), true, null);
    }

    @Override
    public void changePassword(Long userId, PasswordChangeDTO passwordDTO) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(passwordDTO.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Incorrect current password");
        }

        if (!passwordDTO.getNewPassword().equals(passwordDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        user.setPassword(passwordEncoder.encode(passwordDTO.getNewPassword()));
        userRepository.save(user);
        auditService.logActivity(AuditModule.SETTINGS, AuditAction.UPDATE, "Changed password for user: " + user.getUsername(), "User", user.getId(), true, null);
    }

    @Override
    public byte[] exportBackup() throws Exception {
        StringBuilder sqlDump = new StringBuilder();
        sqlDump.append("-- Smart Old Age Home System SQL Backup\n");
        sqlDump.append("-- Generated on ").append(LocalDateTime.now()).append("\n\n");
        sqlDump.append("SET FOREIGN_KEY_CHECKS = 0;\n\n");

        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();
            try (ResultSet tables = metaData.getTables(catalog, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    // Skip Spring Session or temporary tables if any
                    if (tableName.startsWith("SPRING_SESSION")) continue;

                    sqlDump.append("-- Table: ").append(tableName).append("\n");
                    sqlDump.append("DROP TABLE IF EXISTS `").append(tableName).append("`;\n");

                    // Get CREATE TABLE statement if possible or recreate schema
                    // For simpler restore, we can rely on existing ddl-auto. So we only dump INSERT statements!
                    sqlDump.append("TRUNCATE TABLE `").append(tableName).append("`;\n");

                    try (Statement stmt = conn.createStatement();
                         ResultSet rows = stmt.executeQuery("SELECT * FROM `" + tableName + "`")) {
                        int columnCount = rows.getMetaData().getColumnCount();
                        while (rows.next()) {
                            sqlDump.append("INSERT INTO `").append(tableName).append("` VALUES (");
                            for (int i = 1; i <= columnCount; i++) {
                                Object val = rows.getObject(i);
                                if (val == null) {
                                    sqlDump.append("NULL");
                                } else if (val instanceof Number || val instanceof Boolean) {
                                    sqlDump.append(val);
                                } else {
                                    String escapedVal = val.toString().replace("'", "\\'");
                                    sqlDump.append("'").append(escapedVal).append("'");
                                }
                                if (i < columnCount) sqlDump.append(", ");
                            }
                            sqlDump.append(");\n");
                        }
                    }
                    sqlDump.append("\n");
                }
            }
        }

        sqlDump.append("SET FOREIGN_KEY_CHECKS = 1;\n");
        auditService.logActivity(AuditModule.SYSTEM, AuditAction.EXPORT, "Exported database SQL backup", "Database", null, true, null);
        return sqlDump.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void restoreBackup(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Backup file is empty");
        }

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        String[] sqlStatements = content.split(";");

        try (Connection conn = jdbcTemplate.getDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            conn.setAutoCommit(false);
            try {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
                for (String sql : sqlStatements) {
                    if (sql.trim().isEmpty()) continue;
                    stmt.execute(sql);
                }
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                auditService.logActivity(AuditModule.SYSTEM, AuditAction.IMPORT, "Failed to restore database backup", "Database", null, false, e.getMessage());
                throw e;
            }
        }
        auditService.logActivity(AuditModule.SYSTEM, AuditAction.IMPORT, "Restored database backup", "Database", null, true, null);
    }

    @Override
    public UserPreference getPreferences(Long userId) {
        return userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserPreference pref = new UserPreference();
                    pref.setUserId(userId);
                    return userPreferenceRepository.save(pref);
                });
    }

    @Override
    public void updatePreferences(Long userId, PreferencesDTO preferencesDTO) {
        UserPreference pref = getPreferences(userId);
        pref.setTheme(preferencesDTO.getTheme());
        pref.setSidebarCollapsed(preferencesDTO.isSidebarCollapsed());
        pref.setLanguage(preferencesDTO.getLanguage());
        pref.setEmailNotifications(preferencesDTO.isEmailNotifications());
        pref.setSmsNotifications(preferencesDTO.isSmsNotifications());
        pref.setBrowserNotifications(preferencesDTO.isBrowserNotifications());
        pref.setItemsPerPage(preferencesDTO.getItemsPerPage());
        pref.setDefaultDashboardPage(preferencesDTO.getDefaultDashboardPage());
        pref.setFontSize(preferencesDTO.getFontSize());
        userPreferenceRepository.save(pref);
        auditService.logActivity(AuditModule.SETTINGS, AuditAction.UPDATE, "Updated user UI preferences", "UserPreference", pref.getId(), true, null);
    }

    @Override
    public OfficeInfoDTO getOfficeInfo() {
        OfficeInfoDTO info = new OfficeInfoDTO();
        info.setHomeName(getSettingValue("office.name", "Smart Old Age Home"));
        info.setAddress(getSettingValue("office.address", "123, Care Street, Greenfield"));
        info.setPhone(getSettingValue("office.phone", "+1234567890"));
        info.setEmail(getSettingValue("office.email", "info@smartoldagehome.com"));
        info.setWebsite(getSettingValue("office.website", "www.smartoldagehome.com"));
        info.setTrustName(getSettingValue("office.trust_name", "Greenfield Hope Foundation"));
        info.setRegistrationNumber(getSettingValue("office.registration_number", "REG-2026-9901"));
        info.setWorkingHours(getSettingValue("office.working_hours", "09:00 AM - 06:00 PM"));
        return info;
    }

    @Override
    public void updateOfficeInfo(OfficeInfoDTO officeInfoDTO) {
        Map<String, String> map = new HashMap<>();
        map.put("office.name", officeInfoDTO.getHomeName());
        map.put("office.address", officeInfoDTO.getAddress());
        map.put("office.phone", officeInfoDTO.getPhone());
        map.put("office.email", officeInfoDTO.getEmail());
        map.put("office.website", officeInfoDTO.getWebsite());
        map.put("office.trust_name", officeInfoDTO.getTrustName());
        map.put("office.registration_number", officeInfoDTO.getRegistrationNumber());
        map.put("office.working_hours", officeInfoDTO.getWorkingHours());
        saveSettings(map);
    }

    private String getSettingValue(String key, String defaultValue) {
        return systemSettingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .orElse(defaultValue);
    }
}
