-- PostgreSQL Migration Script for Smart Old Age Home Management Portal
-- This script matches the final entity structure of the enhanced Donor Module.

-- 1. Update the 'donors' table schema
ALTER TABLE donors ADD COLUMN IF NOT EXISTS donation_frequency VARCHAR(20) NOT NULL DEFAULT 'ONE_TIME';
ALTER TABLE donors ALTER COLUMN donation_amount DROP NOT NULL;

-- 2. Create the 'medicine_donation_items' table
CREATE TABLE IF NOT EXISTS medicine_donation_items (
    id BIGSERIAL PRIMARY KEY,
    donor_id BIGINT NOT NULL,
    medicine_name VARCHAR(200) NOT NULL,
    price DECIMAL(10, 2),
    expiry_date DATE,
    display_order INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_medicine_donations_donor FOREIGN KEY (donor_id) REFERENCES donors(id) ON DELETE CASCADE
);

-- 3. Create the 'food_donation_items' table
CREATE TABLE IF NOT EXISTS food_donation_items (
    id BIGSERIAL PRIMARY KEY,
    donor_id BIGINT NOT NULL,
    food_name VARCHAR(200) NOT NULL,
    quantity VARCHAR(100),
    display_order INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_food_donations_donor FOREIGN KEY (donor_id) REFERENCES donors(id) ON DELETE CASCADE
);

-- 4. Create Indexes for optimization
CREATE INDEX IF NOT EXISTS idx_medicine_donation_items_donor_id ON medicine_donation_items(donor_id);
CREATE INDEX IF NOT EXISTS idx_food_donation_items_donor_id ON food_donation_items(donor_id);
