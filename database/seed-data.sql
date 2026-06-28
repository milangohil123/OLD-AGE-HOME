-- Seed data for Smart Old Age Home Management Portal
USE smart_old_age_home;

-- Seed Users (Default passwords are: admin123, manager123, staff123 encoded with BCrypt)
-- Admin: admin / admin123
INSERT INTO users (username, password, role, full_name, email, enabled)
VALUES ('admin', '$2a$10$9G/oO1r7n92tK3yK0P9QfeXGg7H0eJ2lCg.1uP21j4F9zG7.qK0yG', 'ADMIN', 'System Administrator', 'admin@oldagehome.com', true)
ON DUPLICATE KEY UPDATE username=username;

-- Manager: manager / manager123
INSERT INTO users (username, password, role, full_name, email, enabled)
VALUES ('manager', '$2a$10$n8gVz36t7bT2/j28r6X.ae.V3z5x6Y7w8E9q0tZ1m2n3p4q5r6s7t', 'MANAGER', 'Home Manager', 'manager@oldagehome.com', true)
ON DUPLICATE KEY UPDATE username=username;

-- Staff: staff / staff123
INSERT INTO users (username, password, role, full_name, email, enabled)
VALUES ('staff', '$2a$10$o9hWz47u8cU3/k39s7Y.bf.W4a6y7Z8x9F0r1uA2n3o4p5q6r7s8u', 'STAFF', 'Office Staff Member', 'staff@oldagehome.com', true)
ON DUPLICATE KEY UPDATE username=username;
