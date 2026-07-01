package com.oldagehome.portal.common;

public final class AppConstants {

    // Private constructor to prevent instantiation
    private AppConstants() {}

    // Security Roles
    public static final class Roles {
        public static final String ADMIN = "ADMIN";
        public static final String MANAGER = "MANAGER";
        public static final String STAFF = "STAFF";
        
        // Authority formats (for Spring Security hasAuthority/hasRole checks)
        public static final String ROLE_ADMIN = "ROLE_ADMIN";
        public static final String ROLE_MANAGER = "ROLE_MANAGER";
        public static final String ROLE_STAFF = "ROLE_STAFF";
    }

    // System Routes/Endpoints
    public static final class Routes {
        public static final String LOGIN = "/login";
        public static final String LOGOUT = "/logout";
        public static final String DASHBOARD = "/dashboard";
        public static final String RESIDENTS = "/residents";
        public static final String DONORS = "/donors";
        public static final String DONATIONS = "/donations";
        public static final String INVENTORY = "/inventory";
        public static final String SCHEMES = "/schemes";
        public static final String POCKET_MONEY = "/pocketmoney";
        public static final String REPORTS = "/reports";
        public static final String SETTINGS = "/settings";
        public static final String IMPORTS = "/imports";
        public static final String AUDIT = "/admin/audit";
    }

    // Page Default Configuration
    public static final class Pagination {
        public static final String DEFAULT_PAGE_NUMBER = "0";
        public static final String DEFAULT_PAGE_SIZE = "10";
        public static final String DEFAULT_SORT_BY = "id";
        public static final String DEFAULT_SORT_DIRECTION = "desc";
    }

    // Storage Paths and Keys
    public static final class Uploads {
        public static final String RESIDENTS_DIR = "residents";
        public static final String DONORS_DIR = "donors";
        public static final String DOCUMENTS_DIR = "documents";
        public static final String RECEIPTS_DIR = "receipts";
        public static final String TEMP_DIR = "temp";
    }

    // Standard Alerts Messages
    public static final class Messages {
        public static final String SUCCESS_SAVE = "Record saved successfully.";
        public static final String SUCCESS_UPDATE = "Record updated successfully.";
        public static final String SUCCESS_DELETE = "Record deleted successfully.";
        public static final String ERROR_NOT_FOUND = "Requested record could not be found.";
        public static final String ERROR_GENERIC = "An unexpected error occurred. Please try again.";
        public static final String ERROR_DUPLICATE = "A record with matching unique values already exists.";
    }
}
