package com.oldagehome.portal.settings.service;

import com.oldagehome.portal.settings.entity.SystemSetting;
import com.oldagehome.portal.settings.entity.UserPreference;
import com.oldagehome.portal.settings.dto.ProfileDTO;
import com.oldagehome.portal.settings.dto.PasswordChangeDTO;
import com.oldagehome.portal.settings.dto.PreferencesDTO;
import com.oldagehome.portal.settings.dto.OfficeInfoDTO;
import com.oldagehome.portal.auth.User;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface SettingsService {
    
    List<SystemSetting> getSettings();
    
    void saveSettings(Map<String, String> settings);
    
    void updateProfile(Long userId, ProfileDTO profileDTO, MultipartFile profilePhoto) throws Exception;
    
    void changePassword(Long userId, PasswordChangeDTO passwordDTO) throws Exception;
    
    byte[] exportBackup() throws Exception;
    
    void restoreBackup(MultipartFile file) throws Exception;
    
    UserPreference getPreferences(Long userId);
    
    void updatePreferences(Long userId, PreferencesDTO preferencesDTO);

    OfficeInfoDTO getOfficeInfo();

    void updateOfficeInfo(OfficeInfoDTO officeInfoDTO);
}
