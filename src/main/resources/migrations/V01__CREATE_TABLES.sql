CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE country_code AS ENUM ('44', '234', '1');

CREATE TYPE user_type AS ENUM ('ADMIN', 'CUSTOMER', 'DOCTOR');

CREATE TABLE email (
  id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
  email_address VARCHAR(255) NOT NULL,
  verified BOOLEAN NOT NULL,
  user_type user_type NOT NULL,
  connected BOOLEAN NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE contact_number (
  id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
  country_code country_code NOT NULL,
  digits VARCHAR(20) NOT NULL,
  connected BOOLEAN NOT NULL,
  user_type user_type NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE login (
  id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
  password VARCHAR(255) NOT NULL,
  blocked BOOLEAN NOT NULL,
  user_type user_type NOT NULL,
  email_id UUID NOT NULL,
  contact_number_id UUID NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- PROCEDURES for created_at and updated_at
CREATE OR REPLACE FUNCTION update_dates()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    NEW.created_at = OLD.created_at;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION insert_dates()
RETURNS TRIGGER AS $$
BEGIN
    NEW.created_at = now();
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


DROP TRIGGER IF EXISTS email_update_dates ON email;
CREATE TRIGGER email_update_dates
BEFORE UPDATE ON email
FOR EACH ROW EXECUTE PROCEDURE update_dates();

DROP TRIGGER IF EXISTS email_insert_dates ON email;
CREATE TRIGGER email_insert_dates
BEFORE INSERT ON email
FOR EACH ROW EXECUTE PROCEDURE insert_dates();

DROP TRIGGER IF EXISTS contact_number_update_dates ON contact_number;
CREATE TRIGGER contact_number_update_dates
BEFORE UPDATE ON contact_number
FOR EACH ROW EXECUTE PROCEDURE update_dates();

DROP TRIGGER IF EXISTS contact_number_insert_dates ON contact_number;
CREATE TRIGGER contact_number_insert_dates
BEFORE INSERT ON contact_number
FOR EACH ROW EXECUTE PROCEDURE insert_dates();

DROP TRIGGER IF EXISTS login_update_dates ON login;
CREATE TRIGGER login_update_dates
BEFORE UPDATE ON login
FOR EACH ROW EXECUTE PROCEDURE update_dates();

DROP TRIGGER IF EXISTS login_insert_dates ON login;
CREATE TRIGGER login_insert_dates
BEFORE INSERT ON login
FOR EACH ROW EXECUTE PROCEDURE insert_dates();


