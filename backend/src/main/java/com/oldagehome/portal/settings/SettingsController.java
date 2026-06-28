package com.oldagehome.portal.settings;

import com.oldagehome.portal.auth.User;
import com.oldagehome.portal.auth.UserRepository;
import com.oldagehome.portal.resident.ResidentRepository;
import com.oldagehome.portal.donor.DonorRepository;
import com.oldagehome.portal.inventory.InventoryRepository;
import com.oldagehome.portal.settings.dto.ProfileDTO;
import com.oldagehome.portal.settings.dto.PasswordChangeDTO;
import com.oldagehome.portal.settings.dto.PreferencesDTO;
import com.oldagehome.portal.settings.dto.OfficeInfoDTO;
import com.oldagehome.portal.settings.entity.UserPreference;
import com.oldagehome.portal.settings.service.SettingsService;
import com.oldagehome.portal.excel.ResidentExcelExporter;
import com.oldagehome.portal.excel.DonorExcelExporter;
import com.oldagehome.portal.excel.InventoryExcelExporter;
import com.oldagehome.portal.excel.DonationReportExporter;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootVersion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/settings")
@PreAuthorize("hasRole('ADMIN')")
public class SettingsController {

    private final SettingsService settingsService;
    private final UserRepository userRepository;
    private final ResidentRepository residentRepository;
    private final DonorRepository donorRepository;
    private final InventoryRepository inventoryRepository;

    @Autowired
    public SettingsController(SettingsService settingsService,
                              UserRepository userRepository,
                              ResidentRepository residentRepository,
                              DonorRepository donorRepository,
                              InventoryRepository inventoryRepository) {
        this.settingsService = settingsService;
        this.userRepository = userRepository;
        this.residentRepository = residentRepository;
        this.donorRepository = donorRepository;
        this.inventoryRepository = inventoryRepository;
    }

    // Common helper to fetch current user
    private User getCurrentUser(Principal principal) {
        if (principal == null) return null;
        return userRepository.findByUsername(principal.getName()).orElse(null);
    }

    @GetMapping
    public String dashboard(Model model, Principal principal) {
        model.addAttribute("activePage", "settings");
        model.addAttribute("title", "Settings Dashboard");
        
        User user = getCurrentUser(principal);
        if (user != null) {
            model.addAttribute("currentUser", user);
            model.addAttribute("preferences", settingsService.getPreferences(user.getId()));
        }
        
        model.addAttribute("officeInfo", settingsService.getOfficeInfo());
        
        // System information
        model.addAttribute("javaVersion", System.getProperty("java.version"));
        model.addAttribute("springBootVersion", SpringBootVersion.getVersion());
        model.addAttribute("serverTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalResidents", residentRepository.count());
        model.addAttribute("totalDonors", donorRepository.count());
        model.addAttribute("totalInventoryItems", inventoryRepository.count());
        model.addAttribute("totalDonations", donorRepository.count());
        
        return "settings/settings";
    }

    @GetMapping("/profile")
    public String profilePage(Model model, Principal principal) {
        model.addAttribute("activePage", "settings");
        model.addAttribute("title", "Profile Settings");
        
        User user = getCurrentUser(principal);
        if (user != null) {
            ProfileDTO profileDTO = new ProfileDTO();
            profileDTO.setFullName(user.getFullName());
            profileDTO.setEmail(user.getEmail());
            profileDTO.setMobile(user.getMobile());
            profileDTO.setUsername(user.getUsername());
            model.addAttribute("profileDTO", profileDTO);
            model.addAttribute("profilePicture", user.getProfilePicture());
        }
        return "settings/profile-settings";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileDTO") ProfileDTO profileDTO,
                                BindingResult bindingResult,
                                @RequestParam(value = "profilePhoto", required = false) MultipartFile profilePhoto,
                                Principal principal,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activePage", "settings");
            model.addAttribute("title", "Profile Settings");
            User user = getCurrentUser(principal);
            if (user != null) {
                model.addAttribute("profilePicture", user.getProfilePicture());
            }
            return "settings/profile-settings";
        }

        User user = getCurrentUser(principal);
        if (user != null) {
            try {
                settingsService.updateProfile(user.getId(), profileDTO, profilePhoto);
                redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
            } catch (Exception e) {
                bindingResult.rejectValue("username", "error.profileDTO", e.getMessage());
                model.addAttribute("activePage", "settings");
                model.addAttribute("title", "Profile Settings");
                model.addAttribute("profilePicture", user.getProfilePicture());
                return "settings/profile-settings";
            }
        }
        return "redirect:/settings/profile";
    }

    @GetMapping("/password")
    public String passwordPage(Model model) {
        model.addAttribute("activePage", "settings");
        model.addAttribute("title", "Change Password");
        model.addAttribute("passwordDTO", new PasswordChangeDTO());
        return "settings/security-settings";
    }

    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute("passwordDTO") PasswordChangeDTO passwordDTO,
                                 BindingResult bindingResult,
                                 Principal principal,
                                 jakarta.servlet.http.HttpServletRequest request,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activePage", "settings");
            model.addAttribute("title", "Change Password");
            return "settings/security-settings";
        }

        if (!passwordDTO.getNewPassword().equals(passwordDTO.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.passwordDTO", "Passwords do not match");
            model.addAttribute("activePage", "settings");
            model.addAttribute("title", "Change Password");
            return "settings/security-settings";
        }

        User user = getCurrentUser(principal);
        if (user != null) {
            try {
                settingsService.changePassword(user.getId(), passwordDTO);
                // Invalidate session (logout) after password change as requested
                SecurityContextHolder.clearContext();
                request.getSession().invalidate();
                return "redirect:/login?passwordChanged=true";
            } catch (Exception e) {
                bindingResult.rejectValue("currentPassword", "error.passwordDTO", e.getMessage());
                model.addAttribute("activePage", "settings");
                model.addAttribute("title", "Change Password");
                return "settings/security-settings";
            }
        }
        return "redirect:/settings/password";
    }

    @GetMapping("/system")
    public String systemPage(Model model) {
        model.addAttribute("activePage", "settings");
        model.addAttribute("title", "Office & System Information");
        model.addAttribute("officeInfoDTO", settingsService.getOfficeInfo());
        return "settings/system-settings";
    }

    @PostMapping("/system")
    public String updateSystem(@Valid @ModelAttribute("officeInfoDTO") OfficeInfoDTO officeInfoDTO,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activePage", "settings");
            model.addAttribute("title", "Office & System Information");
            return "settings/system-settings";
        }

        settingsService.updateOfficeInfo(officeInfoDTO);
        redirectAttributes.addFlashAttribute("successMessage", "Office Information updated successfully!");
        return "redirect:/settings/system";
    }

    @GetMapping("/preferences")
    public String preferencesPage(Model model, Principal principal) {
        model.addAttribute("activePage", "settings");
        model.addAttribute("title", "User Preferences");
        
        User user = getCurrentUser(principal);
        if (user != null) {
            UserPreference pref = settingsService.getPreferences(user.getId());
            PreferencesDTO dto = new PreferencesDTO();
            dto.setTheme(pref.getTheme());
            dto.setSidebarCollapsed(pref.isSidebarCollapsed());
            dto.setLanguage(pref.getLanguage());
            dto.setEmailNotifications(pref.isEmailNotifications());
            dto.setSmsNotifications(pref.isSmsNotifications());
            dto.setBrowserNotifications(pref.isBrowserNotifications());
            dto.setItemsPerPage(pref.getItemsPerPage());
            dto.setDefaultDashboardPage(pref.getDefaultDashboardPage());
            dto.setFontSize(pref.getFontSize());
            model.addAttribute("preferencesDTO", dto);
        }
        return "settings/preferences";
    }

    @PostMapping("/preferences")
    public String updatePreferences(@Valid @ModelAttribute("preferencesDTO") PreferencesDTO preferencesDTO,
                                    BindingResult bindingResult,
                                    Principal principal,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activePage", "settings");
            model.addAttribute("title", "User Preferences");
            return "settings/preferences";
        }

        User user = getCurrentUser(principal);
        if (user != null) {
            settingsService.updatePreferences(user.getId(), preferencesDTO);
            redirectAttributes.addFlashAttribute("successMessage", "User Preferences updated successfully!");
        }
        return "redirect:/settings/preferences";
    }

    @GetMapping("/backup")
    public String backupPage(Model model) {
        model.addAttribute("activePage", "settings");
        model.addAttribute("title", "Database Backup & Export");
        return "settings/backup";
    }

    @GetMapping("/backup/export")
    public ResponseEntity<byte[]> exportSql() {
        try {
            byte[] backup = settingsService.exportBackup();
            String filename = "backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(backup);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/backup/restore")
    public String restoreSql(@RequestParam("backupFile") MultipartFile backupFile,
                             RedirectAttributes redirectAttributes) {
        try {
            settingsService.restoreBackup(backupFile);
            redirectAttributes.addFlashAttribute("successMessage", "Database restored successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to restore database: " + e.getMessage());
        }
        return "redirect:/settings/backup";
    }

    // Export Excel buttons
    @GetMapping("/backup/export/residents")
    public ResponseEntity<byte[]> exportResidentsExcel() throws IOException {
        byte[] data = ResidentExcelExporter.exportResidents(residentRepository.findAll());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=residents_export.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping("/backup/export/donors")
    public ResponseEntity<byte[]> exportDonorsExcel() throws IOException {
        byte[] data = DonorExcelExporter.exportDonors(donorRepository.findAll());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=donors_export.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping("/backup/export/inventory")
    public ResponseEntity<byte[]> exportInventoryExcel() throws IOException {
        byte[] data = InventoryExcelExporter.exportInventory(inventoryRepository.findAll());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory_export.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping("/backup/export/donations")
    public ResponseEntity<byte[]> exportDonationsExcel() throws IOException {
        byte[] data = DonationReportExporter.exportExcel(donorRepository.findAll(), "Donation Records Report");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=donations_export.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping("/about")
    public String aboutPage(Model model) {
        model.addAttribute("activePage", "settings");
        model.addAttribute("title", "About Application");
        model.addAttribute("appName", "Smart Old Age Home Management System");
        model.addAttribute("appVersion", "1.0.0");
        model.addAttribute("developer", "GTU Societal Internship Team");
        model.addAttribute("techStack", "Spring Boot, Spring Security, JPA, Thymeleaf, Bootstrap 5, MySQL");
        model.addAttribute("license", "Apache License 2.0");
        model.addAttribute("lastUpdated", "June 2026");
        return "settings/about";
    }
}
