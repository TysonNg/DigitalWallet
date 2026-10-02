ALTER TABLE users
    ADD password VARCHAR(255);

ALTER TABLE users
    ALTER COLUMN password SET NOT NULL;

ALTER TABLE users
ALTER
COLUMN phone_number TYPE VARCHAR(13) USING (phone_number::VARCHAR(13));