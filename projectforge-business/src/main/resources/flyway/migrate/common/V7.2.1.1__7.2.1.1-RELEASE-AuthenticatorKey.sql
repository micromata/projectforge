-- Authenticator key is used for 2FA apps, such as Microsoft or Google authenticator.
ALTER TABLE T_PF_USER_AUTHENTICATIONS ADD COLUMN authenticator_key VARCHAR(1000);
-- Mobile phone may used for 2FA.
ALTER TABLE T_PF_USER ADD COLUMN mobile_phone VARCHAR(255);
