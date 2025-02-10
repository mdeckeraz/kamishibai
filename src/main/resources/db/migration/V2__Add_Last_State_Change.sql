-- Add last_state_change column to cards table
ALTER TABLE cards
ADD COLUMN last_state_change TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Update existing cards to have their last_state_change set to created_at
UPDATE cards SET last_state_change = created_at;
