export const env = {
    // Spring Boot Backend URL (runs on port 3006 in local development)
    SPRING_BOOT_URL: process.env.SPRING_BOOT_URL || "http://localhost:3006/api/v1",
    // Secret password for iron-session (AES-256 encryption, must be at least 32 characters)
    SESSION_SECRET:
        process.env.SESSION_SECRET ||
        "digital_wallet_secret_session_key_32_characters_minimum_length_required",
    NODE_ENV: process.env.NODE_ENV || "development",
};
