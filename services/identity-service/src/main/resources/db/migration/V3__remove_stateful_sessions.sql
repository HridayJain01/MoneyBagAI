-- Authentication is now stateless JWT. Existing session rows cannot be used as
-- credentials after this deployment, so the obsolete session store is removed.
DROP TABLE IF EXISTS user_sessions;
