package com.oldagehome.portal.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Intercepts all generic exceptions, logs the details, and redirects back to the referring page with an error message.
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, jakarta.servlet.http.HttpServletRequest request, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        logger.error("Global Exception Handler caught an unhandled exception at URL {}: ", request.getRequestURI(), ex);
        
        String requestUrl = request.getRequestURI();
        
        // Define fallback URL based on context
        String fallbackUrl = "/dashboard";
        if (requestUrl != null) {
            if (requestUrl.startsWith("/residents")) {
                fallbackUrl = "/residents";
            } else if (requestUrl.startsWith("/donors")) {
                fallbackUrl = "/donors";
            } else if (requestUrl.startsWith("/reports")) {
                fallbackUrl = "/reports";
            } else if (requestUrl.startsWith("/settings")) {
                fallbackUrl = "/settings";
            } else if (requestUrl.startsWith("/inventory")) {
                fallbackUrl = "/inventory";
            }
        }
        
        redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while processing your request. The issue has been logged safely.");
        
        return "redirect:" + fallbackUrl;
    }
}
