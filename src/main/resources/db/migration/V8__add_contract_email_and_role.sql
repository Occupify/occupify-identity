ALTER TABLE contracts ADD COLUMN IF NOT EXISTS freelancer_email VARCHAR(255);
ALTER TABLE contracts ADD COLUMN IF NOT EXISTS role VARCHAR(100);
ALTER TABLE contracts ADD COLUMN IF NOT EXISTS title VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_identity_contracts_email ON contracts(freelancer_email);
