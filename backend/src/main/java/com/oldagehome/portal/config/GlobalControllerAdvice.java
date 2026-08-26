package com.oldagehome.portal.config;

import com.oldagehome.portal.auth.User;
import com.oldagehome.portal.auth.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UserRepository userRepository;

    @Autowired
    public GlobalControllerAdvice(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute("currentUserInfo")
    public User getCurrentUserInfo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.oldagehome.portal.auth.CustomUserDetails) {
            return ((com.oldagehome.portal.auth.CustomUserDetails) principal).getUser();
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }
}
