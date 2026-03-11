-- Module 8: Branch geolocation for nearest-branch finder

ALTER TABLE branches ADD COLUMN IF NOT EXISTS latitude DECIMAL(10,8);
ALTER TABLE branches ADD COLUMN IF NOT EXISTS longitude DECIMAL(11,8);
ALTER TABLE branches ADD COLUMN IF NOT EXISTS city VARCHAR(100);
ALTER TABLE branches ADD COLUMN IF NOT EXISTS country VARCHAR(100);
ALTER TABLE branches ADD COLUMN IF NOT EXISTS address_line VARCHAR(255);
ALTER TABLE branches ADD COLUMN IF NOT EXISTS phone VARCHAR(30);
ALTER TABLE branches ADD COLUMN IF NOT EXISTS opens_at TIME;
ALTER TABLE branches ADD COLUMN IF NOT EXISTS closes_at TIME;
ALTER TABLE branches ADD COLUMN IF NOT EXISTS services TEXT;

-- PostgreSQL: geo index (optional - uncomment when using PostgreSQL with earthdistance)
-- CREATE EXTENSION IF NOT EXISTS cube;
-- CREATE EXTENSION IF NOT EXISTS earthdistance;
-- CREATE INDEX IF NOT EXISTS idx_branches_location ON branches USING GIST(ll_to_earth(latitude, longitude));
