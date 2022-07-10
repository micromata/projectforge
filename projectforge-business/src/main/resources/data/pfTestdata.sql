-- We should increment the start values of the sequences, otherwise unique constraint violations will occur
-- after starting new data bases pre-filled with test data.
ALTER SEQUENCE hibernate_sequence RESTART WITH 10000;
ALTER SEQUENCE sq_base_ghistory_attr_data_pk RESTART WITH 10000;
ALTER SEQUENCE sq_base_ghistory_attr_pk RESTART WITH 10000;
ALTER SEQUENCE sq_base_ghistory_pk RESTART WITH 10000;

INSERT INTO t_address (pk, created, deleted, last_update, address_status, addresstext, birthday, business_phone, city,
                       comment, communication_language, contact_status, country, division, email, fax, fingerprint,
                       first_name, form, mobile_phone, name, organization, positiontext, postal_addresstext,
                       postal_city, postal_country, postal_state, postal_zip_code, private_addresstext, private_city,
                       private_country, private_email, private_mobile_phone, private_phone, private_state,
                       private_zip_code, public_key, state, title, website, zip_code, imagedata, image_data_preview,
                       uid)
VALUES (128, '2008-01-10 08:26:59.581', FALSE, '2017-11-23 12:56:36.393', 'UPTODATE', 'Marie-Calm-Str. 1-5', NULL,
        '+49 561316793-0', 'Kassel', NULL, NULL, 'ACTIVE',
        NULL,
        NULL,
        NULL,
        '+49 561316793-11',
        NULL,
        'Kai',
        'MISTER',
        NULL,
        'Reinhard',
        'Micromata GmbH', 'CEO', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '-----BEGIN PGP PUBLIC KEY BLOCK-----
      Version: GnuPG v1.0.6 (GNU/Linux)
      Comment: For info see http://www.gnupg.org

      mQGiBD1FLN4RBACUUKGce2+Q1jhzzHCZbR3S2JdKNJQfFSeC2zBrxVQh8MQOF/9U
      P6q4js6ai0u6kAI9pErFQcZxV7VQ5hTjcZ9JxFYiNgF5g1fkfJ/QbS4m4fdmZc3h
      tG+BVUqqg0XRL4KMI4uNkUdvBmqNCM55i2iLMqNb2xfrSyzbCSNNo3GPVwCgzrll
      Zw3sMwq6NYuYL08LVO7hWI0D/R/Qz1FSeftwW2YguJXwcgvT1U7wauOFrfyjLeAd
      lU7yNIxMIKnD/+o0yJylJIor5tqcOYavEBKiriwre0EwUBzjLGQmtju5nuneMRH6
      heRQBbQswhF4qt0ARVX16iqhYrCsmei/ihmtHRveZDGtzn3EW9HWFFu7DPnKwLuh
      DGCSA/998QLoLr2p59m7wn1KwSX+gOk2FFZZC9DLfUZLKnFgsn5F8IhxDsMG8nxo
      xvTG6UX46zHlcEWb246bg/RG1K6by3yQc8Jji4ZFauNg45KpnxaKrfh+ALPScTdi
      ZQ9dT3B8MvzVCHmXHF/229Xmn7dktf3pFcXdCD7FmT7s0ADPZ7QmS2FpIFJlaW5o
      YXJkIDxLLlJlaW5oYXJkQG1pY3JvbWF0YS5kZT6IVwQTEQIAFwUCPUUs3gULBwoD
      BAMVAwIDFgIBAheAAAoJEHUsb/ZREuGHx0cAn1WQMivZn2J4u43NnQZWvIMFray3
      AJsE2TyrLB4dFAdXD98SDuAeR521KbkBDQQ9RSzgEAQA8aSMv06/waBOU2qCzxzW
      hDt09fXkZzxIUh2EWmwlVnv3hnTwHIX1boJfNwMgNmb3wX+//PLvRAmCapUy6jAk
      Vdcm8nYZPGAd3HS4u27n1xEYcv139neYAlU80NPyHBrbXQ8x6ApvLDyi9noLCJop
      biP3643d8Ld8IEdR85jeipMAAwUD/iuS5z3i95A4N2CcTnIBUJguCjvOexg/HmOK
      SdQTCel3xByv1PlHi9jiaLeSwXQLbX7e+1axftWVOaG0geKnVb9fJqcCf1gerTni
      2lP+aAJnznKEFi3fni7XhqCo2MAzMHJDR/8ipzd+OTt0MEo35U6e4avGFC2zeMmX
      NI1CYOUGiEYEGBECAAYFAj1FLOAACgkQdSxv9lES4YcF/QCcCHbLlQ67J42ApCYc
      +vP8r2jysJkAoMX8fcms5VFPNEhV2I1owv+OR0at
      =SmDX
      -----END PGP PUBLIC KEY BLOCK-----
    ', NULL, 'Dipl.-Phys.', 'www.micromata.com', '34131', NULL, NULL, '38ff8f7b-beb6-42ef-bec0-31b920ec5afa');
INSERT INTO t_address (pk, created, deleted, last_update, address_status, addresstext, birthday, business_phone, city,
                       comment, communication_language, contact_status, country, division, email, fax, fingerprint,
                       first_name, form, mobile_phone, name, organization, positiontext, postal_addresstext,
                       postal_city, postal_country, postal_state, postal_zip_code, private_addresstext, private_city,
                       private_country, private_email, private_mobile_phone, private_phone, private_state,
                       private_zip_code, public_key, state, title, website, zip_code, imagedata, image_data_preview,
                       uid)
VALUES (129, '2010-04-21 22:09:18.543', FALSE, '2017-11-23 12:56:36.438', 'UPTODATE', NULL, '1968-05-07',
        '+49 1234 56789', NULL, NULL, NULL, 'ACTIVE', NULL, NULL,
        'h.martens@acme.com',
        NULL, NULL, 'Hugo',
        'MISTER',
        '+49 1234 56788',
        'Martens', 'ACME Inc.',
        'CIO', NULL,
        NULL, NULL,
        NULL, NULL,
        NULL, NULL,
        NULL, NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        'www.acme.com',
        NULL,
        NULL, NULL, '1ba2b809-fe72-4aaf-b8b5-130c5d5e172d');

-- USERS:
INSERT INTO t_pf_user (pk, created, deleted, last_update, authentication_token, date_format, deactivated, description,
                       email, excel_date_format, first_day_of_week, firstname, hr_planning, jira_username, lastlogin,
                       lastname, ldap_values, local_user, locale, loginfailures, organization,
                       personal_meb_identifiers, personal_phone_identifiers, restricted_user,
                       ssh_public_key, stay_logged_in_key, super_admin, time_notation, time_zone, username)
VALUES (16, '2013-04-07 15:49:57.114', FALSE, '2013-04-07 15:49:57.114', NULL, NULL, FALSE, 'Project manager',
        'a.ville@my-company.com', NULL, NULL, 'Ann', TRUE,
        NULL, NULL,
        'Ville',
        NULL, FALSE,
        NULL, 0,
        'My Company', NULL, NULL, FALSE, NULL, NULL, FALSE, NULL,
        'Europe/Berlin', 'ann');
INSERT INTO t_pf_user_password (pk, user_id, created, last_update, deleted, password_hash, password_salt)
VALUES(16, 16, '2013-04-07 15:49:57.114', '2013-04-07 15:49:57.114', false, 'SHA{4369A8892D05285B1E6BA56881B9A9B26D1DAC7D}', NULL);

INSERT INTO t_pf_user (pk, created, deleted, last_update, authentication_token, date_format, deactivated, description,
                       email, excel_date_format, first_day_of_week, firstname, hr_planning, jira_username, lastlogin,
                       lastname, ldap_values, local_user, locale, loginfailures, organization,
                       personal_meb_identifiers, personal_phone_identifiers, restricted_user,
                       ssh_public_key, stay_logged_in_key, super_admin, time_notation, time_zone, username)
VALUES (17, '2010-04-21 22:02:25.955', FALSE, '2013-04-07 18:04:45.05', '85L2ckM6g6PKtiEYjTCsPUKvCmk', NULL, FALSE,
        NULL, 'devnull@devnull.com', NULL, NULL, 'Kai',
        FALSE,
        NULL,
        '2013-04-07 18:04:45.05',
        'Reinhard',
        NULL,
        FALSE,
        NULL, 0,
        'Micromata GmbH', NULL, NULL, FALSE, NULL, NULL, FALSE,
        NULL, 'Europe/Berlin', 'kai');
INSERT INTO t_pf_user_password (pk, user_id, created, last_update, deleted, password_hash, password_salt)
VALUES(17, 17, '2013-04-07 18:04:45.05', '2013-04-07 18:04:45.05', false, 'SHA{4369A8892D05285B1E6BA56881B9A9B26D1DAC7D}', NULL);

INSERT INTO t_pf_user (pk, created, deleted, last_update, authentication_token, date_format, deactivated, description,
                       email, excel_date_format, first_day_of_week, firstname, hr_planning, jira_username, lastlogin,
                       lastname, ldap_values, local_user, locale, loginfailures, organization,
                       personal_meb_identifiers, personal_phone_identifiers, restricted_user,
                       ssh_public_key, stay_logged_in_key, super_admin, time_notation, time_zone, username)
VALUES (18, '2010-04-20 22:29:20.926', FALSE, '2013-04-27 15:56:13.735', 'HCOYtodmNK8lv_-bqnBEzHocBhg', NULL, FALSE,
        NULL, 'devnull@devnull.com', NULL, NULL, 'Demo',
        FALSE,
        NULL,
        '2013-04-27 15:56:13.731',
        'User',
        NULL,
        FALSE,
        NULL,
        0,
        NULL, NULL, NULL, FALSE, NULL, NULL, FALSE, NULL,
        'Europe/Berlin', 'demo');
INSERT INTO t_pf_user_password (pk, user_id, created, last_update, deleted, password_hash, password_salt)
VALUES(18, 18, '2013-04-27 15:56:13.731', '2013-04-27 15:56:13.731', false, 'SHA{4369A8892D05285B1E6BA56881B9A9B26D1DAC7D}', NULL);

INSERT INTO t_pf_user (pk, created, deleted, last_update, authentication_token, date_format, deactivated, description,
                       email, excel_date_format, first_day_of_week, firstname, hr_planning, jira_username, lastlogin,
                       lastname, ldap_values, local_user, locale, loginfailures, organization,
                       personal_meb_identifiers, personal_phone_identifiers, restricted_user,
                       ssh_public_key, stay_logged_in_key, super_admin, time_notation, time_zone, username)
VALUES (20, '2013-04-07 15:49:34.35', FALSE, '2013-04-07 15:49:34.35', NULL, NULL, FALSE, 'Project manager',
        'a.peters@my-company.com', NULL, NULL, 'Alex', TRUE,
        NULL, NULL,
        'Peters', NULL,
        FALSE, NULL, 0,
        'My Company', NULL, NULL, FALSE, NULL, NULL, FALSE, NULL,
        'Europe/Berlin', 'alex');
INSERT INTO t_pf_user_password (pk, user_id, created, last_update, deleted, password_hash, password_salt)
VALUES(20, 20, '2013-04-07 15:49:34.35', '2013-04-07 15:49:34.35', false, 'SHA{4369A8892D05285B1E6BA56881B9A9B26D1DAC7D}', NULL);

INSERT INTO t_pf_user (pk, created, deleted, last_update, authentication_token, date_format, deactivated, description,
                       email, excel_date_format, first_day_of_week, firstname, hr_planning, jira_username, lastlogin,
                       lastname, ldap_values, local_user, locale, loginfailures, organization,
                       personal_meb_identifiers, personal_phone_identifiers, restricted_user,
                       ssh_public_key, stay_logged_in_key, super_admin, time_notation, time_zone, username)
VALUES (21, '2013-04-07 15:51:04.237', FALSE, '2013-04-07 15:51:04.237', NULL, NULL, FALSE, 'Developer',
        'j.stone@my-company.com', NULL, NULL, 'Joe', TRUE, NULL,
        NULL, 'Stone',
        NULL, FALSE, NULL, 0,
        'My company', NULL, NULL, FALSE, NULL, NULL, FALSE, NULL,
        'Europe/Berlin', 'joe');
INSERT INTO t_pf_user_password (pk, user_id, created, last_update, deleted, password_hash, password_salt)
VALUES(21, 21, '2013-04-07 15:51:04.237', '2013-04-07 15:51:04.237', false, 'SHA{4369A8892D05285B1E6BA56881B9A9B26D1DAC7D}', NULL);

INSERT INTO t_pf_user (pk, created, deleted, last_update, authentication_token, date_format, deactivated, description,
                       email, excel_date_format, first_day_of_week, firstname, hr_planning, jira_username, lastlogin,
                       lastname, ldap_values, local_user, locale, loginfailures, organization,
                       personal_meb_identifiers, personal_phone_identifiers, restricted_user,
                       ssh_public_key, stay_logged_in_key, super_admin, time_notation, time_zone, username)
VALUES (22, '2013-04-07 15:52:12.89', FALSE, '2013-04-07 15:52:12.89', NULL, NULL, FALSE, 'Developer',
        'm.bach@my-company.de', NULL, NULL, 'Max', TRUE, NULL, NULL,
        'Bach', NULL, FALSE,
        NULL, 0, 'My company',
        NULL, NULL,
        FALSE, NULL,
        NULL, FALSE,
        NULL,
        'Europe/Berlin', 'max');
INSERT INTO t_pf_user_password (pk, user_id, created, last_update, deleted, password_hash, password_salt)
VALUES(22, 22, '2013-04-07 15:52:12.89', '2013-04-07 15:52:12.89', false, 'SHA{4369A8892D05285B1E6BA56881B9A9B26D1DAC7D}', NULL);

INSERT INTO t_pf_user (pk, created, deleted, last_update, authentication_token, date_format, deactivated, description,
                       email, excel_date_format, first_day_of_week, firstname, hr_planning, jira_username, lastlogin,
                       lastname, ldap_values, local_user, locale, loginfailures, organization,
                       personal_meb_identifiers, personal_phone_identifiers, restricted_user,
                       ssh_public_key, stay_logged_in_key, super_admin, time_notation, time_zone, username)
VALUES (23, '2013-04-07 15:51:41.389', FALSE, '2013-04-07 15:51:41.389', NULL, NULL, FALSE, 'Developer',
        'j.white@my-company.com', NULL, NULL, 'Julia', TRUE, NULL,
        NULL, 'White',
        NULL, FALSE, NULL, 0,
        'My company', NULL, NULL, FALSE, NULL, NULL, FALSE, NULL,
        'Europe/Berlin', 'julia');
INSERT INTO t_pf_user_password (pk, user_id, created, last_update, deleted, password_hash, password_salt)
VALUES(23, 23, '2013-04-07 15:51:41.389', '2013-04-07 15:51:41.389', false, 'SHA{4369A8892D05285B1E6BA56881B9A9B26D1DAC7D}', NULL);

INSERT INTO t_pf_user (pk, created, deleted, last_update, authentication_token, date_format, deactivated, description,
                       email, excel_date_format, first_day_of_week, firstname, hr_planning, jira_username, lastlogin,
                       lastname, ldap_values, local_user, locale, loginfailures, organization,
                       personal_meb_identifiers, personal_phone_identifiers, restricted_user,
                       ssh_public_key, stay_logged_in_key, super_admin, time_notation, time_zone, username)
VALUES (24, '2013-04-07 15:52:44.012', FALSE, '2013-04-07 15:52:44.012', NULL, NULL, FALSE, 'Developer',
        'm.evers@my-company.com', NULL, NULL, 'Michael', TRUE, NULL,
        NULL, 'Evers',
        NULL, FALSE, NULL, 0,
        'My company', NULL, NULL, FALSE, NULL, NULL, FALSE, NULL,
        'Europe/Berlin', 'michael');
INSERT INTO t_pf_user_password (pk, user_id, created, last_update, deleted, password_hash, password_salt)
VALUES(24, 24, '2013-04-07 15:52:44.012', '2013-04-07 15:52:44.012', false, 'SHA{4369A8892D05285B1E6BA56881B9A9B26D1DAC7D}', NULL);

INSERT INTO t_pf_user (pk, created, deleted, last_update, authentication_token, date_format, deactivated, description,
                       email, excel_date_format, first_day_of_week, firstname, hr_planning, jira_username, lastlogin,
                       lastname, ldap_values, local_user, locale, loginfailures, organization,
                        personal_meb_identifiers, personal_phone_identifiers, restricted_user,
                       ssh_public_key, stay_logged_in_key, super_admin, time_notation, time_zone, username)
VALUES (25, '2013-04-07 15:53:08.909', FALSE, '2013-04-07 15:53:08.909', NULL, NULL, FALSE, 'Developer',
        'm.nike@my-company.com', NULL, NULL, 'Mona', TRUE, NULL,
        NULL, 'Nike',
        NULL, FALSE, NULL, 0,
        'My company', NULL, NULL, FALSE, NULL, NULL, FALSE, NULL,
        'Europe/Berlin', 'mona');
INSERT INTO t_pf_user_password (pk, user_id, created, last_update, deleted, password_hash, password_salt)
VALUES(25, 25, '2013-04-07 15:53:08.909', '2013-04-07 15:53:08.909', false, 'SHA{4369A8892D05285B1E6BA56881B9A9B26D1DAC7D}', NULL);

INSERT INTO t_pf_user (pk, created, deleted, last_update, authentication_token, date_format, deactivated, description,
                       email, excel_date_format, first_day_of_week, firstname, hr_planning, jira_username, lastlogin,
                       lastname, ldap_values, local_user, locale, loginfailures, organization,
                       personal_meb_identifiers, personal_phone_identifiers, restricted_user,
                       ssh_public_key, stay_logged_in_key, super_admin, time_notation, time_zone, username)
VALUES (26, '2013-04-07 15:50:32.15', FALSE, '2013-04-07 15:50:32.15', NULL, NULL, FALSE, 'Developer',
        'c.clark@my-company.com', NULL, NULL, 'Chris', TRUE, NULL,
        NULL, 'Clark',
        NULL, FALSE, NULL, 0,
        'My company', NULL, NULL, FALSE, NULL, NULL, FALSE, NULL,
        'Europe/Berlin', 'chris');
INSERT INTO t_pf_user_password (pk, user_id, created, last_update, deleted, password_hash, password_salt)
VALUES(26, 26, '2013-04-07 15:50:32.15', '2013-04-07 15:50:32.15', false, 'SHA{4369A8892D05285B1E6BA56881B9A9B26D1DAC7D}', NULL);

INSERT INTO t_pf_user (pk, created, deleted, last_update, authentication_token, date_format, deactivated, description,
                       email, excel_date_format, first_day_of_week, firstname, hr_planning, jira_username, lastlogin,
                       lastname, ldap_values, local_user, locale, loginfailures, organization,
                       personal_meb_identifiers, personal_phone_identifiers, restricted_user,
                       ssh_public_key, stay_logged_in_key, super_admin, time_notation, time_zone, username)
VALUES (19, '2008-01-10 08:26:19.742', FALSE, '2014-09-02 12:26:04.944', NULL, NULL, FALSE, 'IT system administrators of ProjectForge. They have only access to finances, order book or the project manager''s view, if the groups are
      assigned.
    ', NULL, NULL, NULL, 'Admin', FALSE, NULL, '2014-09-02 12:34:47.785', 'ProjectForge Administrator', NULL,
        TRUE, 'en', 0, NULL,
        NULL,
        NULL, FALSE, NULL, NULL,
        TRUE, NULL, 'UTC', 'admin');
INSERT INTO t_pf_user_password (pk, user_id, created, last_update, deleted, password_hash, password_salt)
VALUES(19, 19, '2014-09-02 12:34:47.785', '2014-09-02 12:34:47.785', false, 'SHA{9CE7B48A16130787B61C52C2120193EBD525FFAA}', 'UXKTgc9ZnrtB7w==');

INSERT INTO t_addressbook_address (address_id, addressbook_id)
VALUES (128, 1);
INSERT INTO t_addressbook_address (address_id, addressbook_id)
VALUES (129, 1);

INSERT INTO t_task (pk, created, deleted, last_update, description, duration, end_date, gantt_type,
                    gantt_predecessor_offset, gantt_rel_type, kost2_black_white_list, kost2_is_black_list, max_hours,
                    old_kost2_id, priority, progress, protect_timesheets_until, protectionofprivacy, reference,
                    short_description, start_date, status, timesheet_booking_status, title, workpackage_code,
                    gantt_predecessor_fk, parent_task_id, responsible_user_id)
VALUES (47, '2008-01-10 08:26:28.361', FALSE, '2008-01-10 08:26:28.361', NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        FALSE, NULL, NULL, NULL, NULL, NULL, FALSE, NULL,
        'ProjectForge root task', NULL, 'N', 'INHERIT',
        'Root', NULL, NULL, NULL, NULL);
INSERT INTO t_task (pk, created, deleted, last_update, description, duration, end_date, gantt_type,
                    gantt_predecessor_offset, gantt_rel_type, kost2_black_white_list, kost2_is_black_list, max_hours,
                    old_kost2_id, priority, progress, protect_timesheets_until, protectionofprivacy, reference,
                    short_description, start_date, status, timesheet_booking_status, title, workpackage_code,
                    gantt_predecessor_fk, parent_task_id, responsible_user_id)
VALUES (48, '2010-04-21 22:03:03.31', FALSE, '2013-04-07 15:59:02.438', NULL, NULL, NULL, NULL, NULL, NULL, NULL, FALSE,
        NULL, NULL, NULL, NULL, NULL, FALSE, NULL,
        'Our customer (please browse to web portal, Release 1.0)',
        NULL, 'O', 'INHERIT', 'Yellow Logistics', NULL,
        NULL, 47, NULL);
INSERT INTO t_task (pk, created, deleted, last_update, description, duration, end_date, gantt_type,
                    gantt_predecessor_offset, gantt_rel_type, kost2_black_white_list, kost2_is_black_list, max_hours,
                    old_kost2_id, priority, progress, protect_timesheets_until, protectionofprivacy, reference,
                    short_description, start_date, status, timesheet_booking_status, title, workpackage_code,
                    gantt_predecessor_fk, parent_task_id, responsible_user_id)
VALUES (49, '2010-04-21 22:03:31.164', FALSE, '2013-04-07 15:57:08.629', NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        FALSE, NULL, NULL, NULL, NULL, NULL, FALSE, NULL,
        'web portal project', NULL, 'O', 'INHERIT',
        'Yellow web portal', NULL, NULL, 48, 16);
INSERT INTO t_task (pk, created, deleted, last_update, description, duration, end_date, gantt_type,
                    gantt_predecessor_offset, gantt_rel_type, kost2_black_white_list, kost2_is_black_list, max_hours,
                    old_kost2_id, priority, progress, protect_timesheets_until, protectionofprivacy, reference,
                    short_description, start_date, status, timesheet_booking_status, title, workpackage_code,
                    gantt_predecessor_fk, parent_task_id, responsible_user_id)
VALUES (50, '2010-04-21 22:03:50.818', FALSE, '2010-04-22 08:50:24.301', NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        FALSE, NULL, NULL, NULL, NULL, NULL, FALSE, NULL,
        NULL, NULL, 'O', 'ONLY_LEAFS', 'Release 1.0',
        NULL, NULL, 49, NULL);
INSERT INTO t_task (pk, created, deleted, last_update, description, duration, end_date, gantt_type,
                    gantt_predecessor_offset, gantt_rel_type, kost2_black_white_list, kost2_is_black_list, max_hours,
                    old_kost2_id, priority, progress, protect_timesheets_until, protectionofprivacy, reference,
                    short_description, start_date, status, timesheet_booking_status, title, workpackage_code,
                    gantt_predecessor_fk, parent_task_id, responsible_user_id)
VALUES (51, '2010-04-22 07:48:42.468', FALSE, '2010-11-08 21:37:09.616', NULL, 10.00, NULL, NULL, NULL, NULL, '00,10',
        FALSE, 24, NULL, NULL, 100, NULL, FALSE, NULL,
        'Out of budget', '2010-10-14 22:00:00', 'O',
        'OPENED', 'Akquise', NULL, NULL, 50, NULL);
INSERT INTO t_task (pk, created, deleted, last_update, description, duration, end_date, gantt_type,
                    gantt_predecessor_offset, gantt_rel_type, kost2_black_white_list, kost2_is_black_list, max_hours,
                    old_kost2_id, priority, progress, protect_timesheets_until, protectionofprivacy, reference,
                    short_description, start_date, status, timesheet_booking_status, title, workpackage_code,
                    gantt_predecessor_fk, parent_task_id, responsible_user_id)
VALUES (52, '2010-04-21 22:17:41.501', FALSE, '2010-08-27 17:42:12.835', NULL, 10.00, NULL, NULL, NULL, NULL, '00',
        TRUE, NULL, NULL, NULL, 90, NULL, FALSE, NULL,
        'Pflichtenheft / System Requirements Specification',
        NULL, 'O', 'INHERIT', 'WP 1 Specification', NULL,
        51, 50, NULL);
INSERT INTO t_task (pk, created, deleted, last_update, description, duration, end_date, gantt_type,
                    gantt_predecessor_offset, gantt_rel_type, kost2_black_white_list, kost2_is_black_list, max_hours,
                    old_kost2_id, priority, progress, protect_timesheets_until, protectionofprivacy, reference,
                    short_description, start_date, status, timesheet_booking_status, title, workpackage_code,
                    gantt_predecessor_fk, parent_task_id, responsible_user_id)
VALUES (53, '2010-04-21 22:17:59.652', FALSE, '2010-08-27 17:42:23.743', NULL, 5.00, NULL, NULL, 2, NULL, '00', TRUE,
        NULL, NULL, NULL, 20, NULL, FALSE, NULL, NULL,
        NULL, 'N', 'INHERIT',
        'WP 2 Realisierung / Realization', NULL, 52, 50, NULL);
INSERT INTO t_task (pk, created, deleted, last_update, description, duration, end_date, gantt_type,
                    gantt_predecessor_offset, gantt_rel_type, kost2_black_white_list, kost2_is_black_list, max_hours,
                    old_kost2_id, priority, progress, protect_timesheets_until, protectionofprivacy, reference,
                    short_description, start_date, status, timesheet_booking_status, title, workpackage_code,
                    gantt_predecessor_fk, parent_task_id, responsible_user_id)
VALUES (54, '2010-08-27 17:43:48.571', FALSE, '2010-08-27 17:58:46.093', NULL, 5.00, NULL, NULL, NULL, NULL, NULL,
        FALSE, NULL, NULL, NULL, NULL, NULL, FALSE, NULL,
        NULL, NULL, 'N', 'INHERIT',
        'User acceptance test', NULL, 53, 50, NULL);
INSERT INTO t_task (pk, created, deleted, last_update, description, duration, end_date, gantt_type,
                    gantt_predecessor_offset, gantt_rel_type, kost2_black_white_list, kost2_is_black_list, max_hours,
                    old_kost2_id, priority, progress, protect_timesheets_until, protectionofprivacy, reference,
                    short_description, start_date, status, timesheet_booking_status, title, workpackage_code,
                    gantt_predecessor_fk, parent_task_id, responsible_user_id)
VALUES (55, '2010-08-27 17:53:02.341', FALSE, '2010-08-27 17:58:54.929', NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        FALSE, NULL, NULL, NULL, NULL, NULL, FALSE, NULL,
        NULL, NULL, 'N', 'INHERIT', 'Acceptance', NULL,
        54, 50, NULL);
INSERT INTO t_task (pk, created, deleted, last_update, description, duration, end_date, gantt_type,
                    gantt_predecessor_offset, gantt_rel_type, kost2_black_white_list, kost2_is_black_list, max_hours,
                    old_kost2_id, priority, progress, protect_timesheets_until, protectionofprivacy, reference,
                    short_description, start_date, status, timesheet_booking_status, title, workpackage_code,
                    gantt_predecessor_fk, parent_task_id, responsible_user_id)
VALUES (56, '2010-04-21 22:18:11.748', FALSE, '2010-08-27 17:59:04.061', NULL, 15.00, NULL, NULL, 5, NULL, '00', TRUE,
        NULL, NULL, NULL, NULL, NULL, FALSE, NULL,
        NULL, NULL, 'N', 'INHERIT', 'WP 3 Installation',
        NULL, 55, 50, NULL);
INSERT INTO t_task (pk, created, deleted, last_update, description, duration, end_date, gantt_type,
                    gantt_predecessor_offset, gantt_rel_type, kost2_black_white_list, kost2_is_black_list, max_hours,
                    old_kost2_id, priority, progress, protect_timesheets_until, protectionofprivacy, reference,
                    short_description, start_date, status, timesheet_booking_status, title, workpackage_code,
                    gantt_predecessor_fk, parent_task_id, responsible_user_id)
VALUES (57, '2008-01-10 08:33:38.072', FALSE, '2013-04-07 16:01:00.055', NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        FALSE, NULL, NULL, NULL, NULL, NULL, FALSE, NULL,
        'Our famous Open Source project', NULL, 'O',
        'INHERIT', 'ProjectForge', NULL, NULL, 47, 17);
INSERT INTO t_task (pk, created, deleted, last_update, description, duration, end_date, gantt_type,
                    gantt_predecessor_offset, gantt_rel_type, kost2_black_white_list, kost2_is_black_list, max_hours,
                    old_kost2_id, priority, progress, protect_timesheets_until, protectionofprivacy, reference,
                    short_description, start_date, status, timesheet_booking_status, title, workpackage_code,
                    gantt_predecessor_fk, parent_task_id, responsible_user_id)
VALUES (58, '2010-04-21 22:04:06.152', FALSE, '2010-11-08 21:37:05.48', NULL, 30.00, NULL, NULL, NULL, NULL, NULL,
        FALSE, NULL, NULL, NULL, NULL, NULL, FALSE, NULL,
        NULL, '2011-01-14 23:00:00', 'N', 'NO_BOOKING',
        'Release 1.1', NULL, NULL, 49, NULL);
INSERT INTO t_task (pk, created, deleted, last_update, description, duration, end_date, gantt_type,
                    gantt_predecessor_offset, gantt_rel_type, kost2_black_white_list, kost2_is_black_list, max_hours,
                    old_kost2_id, priority, progress, protect_timesheets_until, protectionofprivacy, reference,
                    short_description, start_date, status, timesheet_booking_status, title, workpackage_code,
                    gantt_predecessor_fk, parent_task_id, responsible_user_id)
VALUES (59, '2008-01-10 08:34:18.759', FALSE, '2008-01-10 08:34:44.685', NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        FALSE, NULL, NULL, NULL, NULL, NULL, FALSE, NULL,
        NULL, NULL, 'O', 'INHERIT', 'Development', NULL,
        NULL, 57, NULL);
INSERT INTO t_task (pk, created, deleted, last_update, description, duration, end_date, gantt_type,
                    gantt_predecessor_offset, gantt_rel_type, kost2_black_white_list, kost2_is_black_list, max_hours,
                    old_kost2_id, priority, progress, protect_timesheets_until, protectionofprivacy, reference,
                    short_description, start_date, status, timesheet_booking_status, title, workpackage_code,
                    gantt_predecessor_fk, parent_task_id, responsible_user_id)
VALUES (60, '2008-01-10 08:34:34.72', FALSE, '2008-01-10 08:34:34.72', NULL, NULL, NULL, NULL, NULL, NULL, NULL, FALSE,
        NULL, NULL, NULL, NULL, NULL, FALSE, NULL,
        NULL, NULL, 'O', 'INHERIT', 'Database', NULL,
        NULL, 59, NULL);
INSERT INTO t_task (pk, created, deleted, last_update, description, duration, end_date, gantt_type,
                    gantt_predecessor_offset, gantt_rel_type, kost2_black_white_list, kost2_is_black_list, max_hours,
                    old_kost2_id, priority, progress, protect_timesheets_until, protectionofprivacy, reference,
                    short_description, start_date, status, timesheet_booking_status, title, workpackage_code,
                    gantt_predecessor_fk, parent_task_id, responsible_user_id)
VALUES (61, '2008-01-10 08:35:34.663', FALSE, '2008-01-10 08:35:34.663', NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        FALSE, NULL, NULL, NULL, NULL, NULL, FALSE, NULL,
        NULL, NULL, 'N', 'INHERIT', 'Web', NULL, NULL,
        59, NULL);
INSERT INTO t_task (pk, created, deleted, last_update, description, duration, end_date, gantt_type,
                    gantt_predecessor_offset, gantt_rel_type, kost2_black_white_list, kost2_is_black_list, max_hours,
                    old_kost2_id, priority, progress, protect_timesheets_until, protectionofprivacy, reference,
                    short_description, start_date, status, timesheet_booking_status, title, workpackage_code,
                    gantt_predecessor_fk, parent_task_id, responsible_user_id)
VALUES (62, '2008-01-10 08:35:54.151', FALSE, '2008-01-10 08:35:54.151', NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        FALSE, NULL, NULL, NULL, NULL, NULL, FALSE, NULL,
        NULL, NULL, 'O', 'INHERIT', 'Java', NULL, NULL,
        59, NULL);
INSERT INTO t_task (pk, created, deleted, last_update, description, duration, end_date, gantt_type,
                    gantt_predecessor_offset, gantt_rel_type, kost2_black_white_list, kost2_is_black_list, max_hours,
                    old_kost2_id, priority, progress, protect_timesheets_until, protectionofprivacy, reference,
                    short_description, start_date, status, timesheet_booking_status, title, workpackage_code,
                    gantt_predecessor_fk, parent_task_id, responsible_user_id)
VALUES (63, '2010-04-22 21:46:47.963', FALSE, '2013-04-07 15:57:27.338', NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        FALSE, NULL, NULL, NULL, NULL, NULL, FALSE, NULL,
        'Company stuff', NULL, 'O', 'INHERIT',
        'My Company', NULL, NULL, 47, NULL);
INSERT INTO t_task (pk, created, deleted, last_update, description, duration, end_date, gantt_type,
                    gantt_predecessor_offset, gantt_rel_type, kost2_black_white_list, kost2_is_black_list, max_hours,
                    old_kost2_id, priority, progress, protect_timesheets_until, protectionofprivacy, reference,
                    short_description, start_date, status, timesheet_booking_status, title, workpackage_code,
                    gantt_predecessor_fk, parent_task_id, responsible_user_id)
VALUES (64, '2010-04-22 21:47:11.168', FALSE, '2010-04-22 21:50:10.537', NULL, NULL, NULL, NULL, NULL, NULL,
        '7.000.00.99', FALSE, NULL, NULL, NULL, NULL, NULL,
        FALSE, NULL, NULL, NULL, 'O',
        'INHERIT', 'Urlaub / Holiday', NULL, NULL, 63, NULL);
INSERT INTO t_task (pk, created, deleted, last_update, description, duration, end_date, gantt_type,
                    gantt_predecessor_offset, gantt_rel_type, kost2_black_white_list, kost2_is_black_list, max_hours,
                    old_kost2_id, priority, progress, protect_timesheets_until, protectionofprivacy, reference,
                    short_description, start_date, status, timesheet_booking_status, title, workpackage_code,
                    gantt_predecessor_fk, parent_task_id, responsible_user_id)
VALUES (65, '2010-04-22 21:50:38.876', FALSE, '2010-04-22 21:50:38.876', NULL, NULL, NULL, NULL, NULL, NULL,
        '7.000.01.99', FALSE, NULL, NULL, NULL, NULL, NULL,
        FALSE, NULL, NULL, NULL, 'O',
        'INHERIT', 'Krankheit / ill', NULL, NULL, 63, NULL);
INSERT INTO t_task (pk, created, deleted, last_update, description, duration, end_date, gantt_type,
                    gantt_predecessor_offset, gantt_rel_type, kost2_black_white_list, kost2_is_black_list, max_hours,
                    old_kost2_id, priority, progress, protect_timesheets_until, protectionofprivacy, reference,
                    short_description, start_date, status, timesheet_booking_status, title, workpackage_code,
                    gantt_predecessor_fk, parent_task_id, responsible_user_id)
VALUES (66, '2010-08-27 17:57:51.115', FALSE, '2010-08-27 17:59:14.104', NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        FALSE, NULL, NULL, NULL, NULL, NULL, FALSE, NULL,
        NULL, NULL, 'N', 'INHERIT', 'Going live', NULL,
        56, 50, NULL);
INSERT INTO t_task (pk, created, deleted, last_update, description, duration, end_date, gantt_type,
                    gantt_predecessor_offset, gantt_rel_type, kost2_black_white_list, kost2_is_black_list, max_hours,
                    old_kost2_id, priority, progress, protect_timesheets_until, protectionofprivacy, reference,
                    short_description, start_date, status, timesheet_booking_status, title, workpackage_code,
                    gantt_predecessor_fk, parent_task_id, responsible_user_id)
VALUES (67, '2013-04-07 16:00:23.586', FALSE, '2013-04-07 16:00:23.586', NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        FALSE, NULL, NULL, NULL, NULL, NULL, FALSE, NULL,
        'track & trace project', NULL, 'O', 'INHERIT',
        'Yellow track & trace', NULL, NULL, 48, 16);

INSERT INTO t_book (pk, created, deleted, last_update, abstract_text, authors, comment, editor, isbn, keywords,
                    lend_out_comment, lend_out_date, publisher, signature, status, title, book_type, year_of_publishing,
                    lend_out_by)
VALUES (168, '2008-01-10 08:27:53.675', FALSE, '2011-01-23 17:23:34.283', NULL, 'Harnisch, Carsten', NULL, NULL, NULL,
        NULL, NULL, NULL, NULL, NULL, 'PRESENT',
        'eBay API', 'BOOK', '2007',
        NULL);
INSERT INTO t_book (pk, created, deleted, last_update, abstract_text, authors, comment, editor, isbn, keywords,
                    lend_out_comment, lend_out_date, publisher, signature, status, title, book_type, year_of_publishing,
                    lend_out_by)
VALUES (169, '2008-01-10 08:44:59.304', FALSE, '2011-01-23 17:23:29.555', NULL, 'Hart, Matthew u. Freeman, Robert G.',
        NULL, NULL, NULL, NULL, NULL, NULL,
        'OraclePress',
        NULL,
        'PRESENT',
        'Oracle Database 10g RMAN Backup & Recovery',
        'BOOK',
        '2007',
        NULL);
INSERT INTO t_book (pk, created, deleted, last_update, abstract_text, authors, comment, editor, isbn, keywords,
                    lend_out_comment, lend_out_date, publisher, signature, status, title, book_type, year_of_publishing,
                    lend_out_by)
VALUES (170, '2011-02-07 21:18:40.005', FALSE, '2011-02-07 21:18:40.005', NULL, 'Bien, Adam', NULL, NULL,
        '978-3-939084-24-2', 'Java EE5', NULL, NULL,
        'entwickler.press',
        NULL,
        'PRESENT',
        'Java EE5, Patterns und Idiome',
        'BOOK', '2007',
        NULL);
INSERT INTO t_book (pk, created, deleted, last_update, abstract_text, authors, comment, editor, isbn, keywords,
                    lend_out_comment, lend_out_date, publisher, signature, status, title, book_type, year_of_publishing,
                    lend_out_by)
VALUES (171, '2011-02-07 21:19:47.902', FALSE, '2011-02-07 21:19:47.902', NULL, 'Bien, Adam', NULL, NULL,
        '3-935042-99-X', NULL, NULL, NULL, 'entwickler.press',
        NULL, 'PRESENT',
        'Enterprise Architekturen. Leitfaden für effiziente Software-Entwicklung',
        'BOOK', '2006', NULL);

INSERT INTO t_configuration (pk, created, deleted, last_update, configurationtype, floatvalue, is_global, intvalue,
                             parameter, stringvalue)
VALUES (3, '2010-04-21 00:12:07.601', FALSE, '2011-02-07 21:22:26.455', 'STRING', NULL, FALSE, NULL, 'feedbackEMail',
        'admin@dev-null.com');
INSERT INTO t_configuration (pk, created, deleted, last_update, configurationtype, floatvalue, is_global, intvalue,
                             parameter, stringvalue)
VALUES (4, '2010-04-21 00:12:07.603', FALSE, '2010-04-21 22:07:27.722', 'PERCENT', 0.19000, FALSE, NULL,
        'fibu.defaultVAT', NULL);
INSERT INTO t_configuration (pk, created, deleted, last_update, configurationtype, floatvalue, is_global, intvalue,
                             parameter, stringvalue)
VALUES (7, '2010-04-21 00:12:07.607', FALSE, '2010-04-21 22:07:40.197', 'STRING', NULL, FALSE, NULL,
        'countryPhonePrefix', '+49');
INSERT INTO t_configuration (pk, created, deleted, last_update, configurationtype, floatvalue, is_global, intvalue,
                             parameter, stringvalue)
VALUES (8, '2010-07-13 17:39:34.685', FALSE, '2010-07-13 17:39:34.685', 'STRING', NULL, FALSE, NULL,
        'mebSMSReceivingPhoneNumber', NULL);
INSERT INTO t_configuration (pk, created, deleted, last_update, configurationtype, floatvalue, is_global, intvalue,
                             parameter, stringvalue)
VALUES (10, '2011-03-18 06:18:53.377', FALSE, '2011-03-18 06:18:53.377', 'STRING', NULL, FALSE, NULL, 'dateFormats',
        'dd/MM/yyyy;MM/dd/yyyy;dd.MM.yyyy;yyyy-MM-dd');
INSERT INTO t_configuration (pk, created, deleted, last_update, configurationtype, floatvalue, is_global, intvalue,
                             parameter, stringvalue)
VALUES (11, '2011-03-18 06:18:53.381', FALSE, '2011-03-18 06:18:53.381', 'STRING', NULL, FALSE, NULL,
        'excelDateFormats', 'DD/MM/YYYY;MM/DD/YYYY;DD.MM.YYYY');
INSERT INTO t_configuration (pk, created, deleted, last_update, configurationtype, floatvalue, is_global, intvalue,
                             parameter, stringvalue)
VALUES (12, '2011-03-18 06:18:53.383', FALSE, '2011-03-18 06:19:57.347', 'BOOLEAN', NULL, FALSE, NULL,
        'fibu.costConfigured', 'true');
INSERT INTO t_configuration (pk, created, deleted, last_update, configurationtype, floatvalue, is_global, intvalue,
                             parameter, stringvalue)
VALUES (13, '2012-05-02 22:42:13.084', FALSE, '2012-05-02 22:42:13.084', 'TEXT', NULL, FALSE, NULL, 'organization',
        NULL);
INSERT INTO t_configuration (pk, created, deleted, last_update, configurationtype, floatvalue, is_global, intvalue,
                             parameter, stringvalue)
VALUES (1, '2010-04-21 00:12:07.576', FALSE, '2011-02-07 21:22:17.164', 'STRING', NULL, TRUE, NULL,
        'systemAdministratorEMail', 'admin@dev-null.com');
INSERT INTO t_configuration (pk, created, deleted, last_update, configurationtype, floatvalue, is_global, intvalue,
                             parameter, stringvalue)
VALUES (2, '2010-04-21 00:12:07.599', FALSE, '2013-04-01 12:21:20.99', 'TEXT', NULL, TRUE, NULL, 'messageOfTheDay',
        'Please try user demo with password demo123. Have a lot of fun!');
INSERT INTO t_configuration (pk, created, deleted, last_update, configurationtype, floatvalue, is_global, intvalue,
                             parameter, stringvalue)
VALUES (514, '2017-11-23 12:56:36.808', FALSE, '2017-11-23 12:56:36.808', 'STRING', NULL, TRUE, NULL,
        'pluginsActivated', NULL);
INSERT INTO t_configuration (pk, created, deleted, last_update, configurationtype, floatvalue, is_global, intvalue,
                             parameter, stringvalue)
VALUES (15, '2016-09-30 12:03:23.48', FALSE, '2016-09-30 15:02:18.298', 'STRING', NULL, TRUE, NULL, 'hr.emailaddress',
        'hr@management.de');
INSERT INTO t_configuration (pk, created, deleted, last_update, configurationtype, floatvalue, is_global, intvalue,
                             parameter, stringvalue)
VALUES (542, '2017-11-23 12:56:36.888', FALSE, '2017-11-23 12:56:36.888', 'STRING', NULL, TRUE, NULL,
        'vacation.lastyear.enddate', '31.03.');
INSERT INTO t_configuration (pk, created, deleted, last_update, configurationtype, floatvalue, is_global, intvalue,
                             parameter, stringvalue)
VALUES (558, '2017-11-23 12:56:36.928', FALSE, '2017-11-23 12:56:36.928', 'INTEGER', NULL, TRUE, 10,
        'minPasswordLength', NULL);
INSERT INTO t_configuration (pk, created, deleted, last_update, configurationtype, floatvalue, is_global, intvalue,
                             parameter, stringvalue)
VALUES (574, '2017-11-23 12:56:36.968', FALSE, '2017-11-23 12:56:36.968', 'BOOLEAN', NULL, TRUE, NULL,
        'password.flag.checkChange', 'true');
INSERT INTO t_configuration (pk, created, deleted, last_update, configurationtype, floatvalue, is_global, intvalue,
                             parameter, stringvalue)
VALUES (9, '2011-01-23 17:21:33.597', FALSE, '2017-11-23 12:56:37.103', 'TIME_ZONE', NULL, FALSE, NULL, 'timezone',
        'UTC');
INSERT INTO t_configuration (pk, created, deleted, last_update, configurationtype, floatvalue, is_global, intvalue,
                             parameter, stringvalue)
VALUES (14, '2013-04-01 12:03:23.48', FALSE, '2017-11-23 12:56:37.154', 'STRING', NULL, FALSE, NULL, 'calendarDomain',
        'test');

INSERT INTO t_fibu_kost1 (pk, created, deleted, last_update, bereich, description, endziffer, kostentraegerstatus,
                          nummernkreis, teilbereich)
VALUES (80, '2010-04-22 07:58:19.39', FALSE, '2010-04-22 07:58:19.39', 0, 'Kai Reinhard', 0, 'ACTIVE', 3, 0);
INSERT INTO t_fibu_kost1 (pk, created, deleted, last_update, bereich, description, endziffer, kostentraegerstatus,
                          nummernkreis, teilbereich)
VALUES (81, '2010-04-22 07:58:46.207', FALSE, '2010-04-22 07:58:46.207', 0, 'Demo user', 0, 'ACTIVE', 3, 1);
INSERT INTO t_fibu_kost1 (pk, created, deleted, last_update, bereich, description, endziffer, kostentraegerstatus,
                          nummernkreis, teilbereich)
VALUES (82, '2010-04-23 21:11:27.507', FALSE, '2010-04-23 21:12:11.503', 999, 'Sonstiges / Misc', 99, 'ACTIVE', 9, 99);

INSERT INTO t_fibu_employee (pk, created, deleted, last_update, abteilung, account_holder, austritt, bic, birthday,
                             city, comment, country, eintritt, gender, iban, position_text, staffnumber, state,
                             employee_status, street, urlaubstage, weekly_working_hours, zipcode, kost1_id,
                             user_id)
VALUES (126, '2010-04-22 07:58:28.512', FALSE, '2017-11-23 12:56:36.593', 'Development', NULL, NULL, NULL, NULL, NULL,
        NULL, NULL, '2009-12-31 23:00:00', 0, NULL,
        NULL, NULL, NULL, 'FEST_ANGESTELLTER',
        NULL, 25, 40.00000, NULL, 80, 17);
INSERT INTO t_fibu_employee (pk, created, deleted, last_update, abteilung, account_holder, austritt, bic, birthday,
                             city, comment, country, eintritt, gender, iban, position_text, staffnumber, state,
                             employee_status, street, urlaubstage, weekly_working_hours, zipcode, kost1_id,
                             user_id)
VALUES (127, '2010-04-22 07:59:12.961', FALSE, '2017-11-23 12:56:36.679', NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        NULL, '2009-12-31 23:00:00', 0, NULL, NULL, NULL,
        NULL, 'FEST_ANGESTELLTER', NULL, 25, 40.00000,
        NULL, 81, 18);

INSERT INTO t_calendar (pk, created, deleted, last_update, description, ext_subscription,
                        ext_subscription_calendar_binary, ext_subscription_hash, ext_subscription_update_interval,
                        ext_subscription_url, full_access_group_ids, full_access_user_ids, minimal_access_group_ids,
                        minimal_access_user_ids, readonly_access_group_ids, readonly_access_user_ids, title,
                        owner_fk)
VALUES (157, '2013-04-07 16:15:42.141', FALSE, '2017-11-23 12:56:36.083', 'Kai''s business calendar', FALSE, NULL, NULL,
        NULL, NULL, '', '1,2', '', '', '40', '',
        'kai@work', 19);
INSERT INTO t_calendar (pk, created, deleted, last_update, description, ext_subscription,
                        ext_subscription_calendar_binary, ext_subscription_hash, ext_subscription_update_interval,
                        ext_subscription_url, full_access_group_ids, full_access_user_ids, minimal_access_group_ids,
                        minimal_access_user_ids, readonly_access_group_ids, readonly_access_user_ids, title,
                        owner_fk)
VALUES (158, '2013-04-07 16:14:34.257', FALSE, '2017-11-23 12:56:36.159', 'Team calendar', FALSE, NULL, NULL, NULL,
        NULL, '43', '2', '', '', '40', '',
        'Yellow web portal team', 19);
INSERT INTO t_calendar (pk, created, deleted, last_update, description, ext_subscription,
                        ext_subscription_calendar_binary, ext_subscription_hash, ext_subscription_update_interval,
                        ext_subscription_url, full_access_group_ids, full_access_user_ids, minimal_access_group_ids,
                        minimal_access_user_ids, readonly_access_group_ids, readonly_access_user_ids, title,
                        owner_fk)
VALUES (159, '2013-04-07 16:15:17.791', FALSE, '2017-11-23 12:56:36.228', 'Kai''s private calendar', FALSE, NULL, NULL,
        NULL, NULL, '', '2,1', '40', '', '', '',
        'kai@home', 19);
INSERT INTO t_calendar (pk, created, deleted, last_update, description, ext_subscription,
                        ext_subscription_calendar_binary, ext_subscription_hash, ext_subscription_update_interval,
                        ext_subscription_url, full_access_group_ids, full_access_user_ids, minimal_access_group_ids,
                        minimal_access_user_ids, readonly_access_group_ids, readonly_access_user_ids, title,
                        owner_fk)
VALUES (160, '2013-04-07 16:14:08.668', FALSE, '2017-11-23 12:56:36.282', 'Events, releases, meetings of the team etc.',
        FALSE, NULL, NULL, NULL, NULL, '35', '2',
        '', '', '41,40', '', 'ProjectForge team', 19);

INSERT INTO t_plugin_calendar_event (pk, created, deleted, last_update, all_day, end_date, external_uid, last_email,
                                     location, note, organizer, recurrence_date, recurrence_ex_date,
                                     recurrence_reference_id, recurrence_rule, recurrence_until, reminder_action_type,
                                     reminder_duration, reminder_duration_unit, sequence, start_date, subject,
                                     calendar_fk, uid, team_event_fk_creator, dt_stamp,
                                     organizer_additional_params, ownership)
VALUES (161, '2013-04-07 18:07:32.479', FALSE, '2013-04-07 18:07:32.479', FALSE, '2013-03-25 09:30:00', NULL, NULL,
        'Headquarter', 'Be well prepared, dude!', NULL,
        NULL, NULL, NULL, 'FREQ=WEEKLY;INTERVAL=1', NULL, 'MESSAGE_SOUND', 15, 'MINUTES', NULL, '2013-03-25 08:00:00',
        'Weekly meeting with my boss', 157,
        'testdata1', 19, NULL, NULL, NULL);
INSERT INTO t_plugin_calendar_event (pk, created, deleted, last_update, all_day, end_date, external_uid, last_email,
                                     location, note, organizer, recurrence_date, recurrence_ex_date,
                                     recurrence_reference_id, recurrence_rule, recurrence_until, reminder_action_type,
                                     reminder_duration, reminder_duration_unit, sequence, start_date, subject,
                                     calendar_fk, uid, team_event_fk_creator, dt_stamp,
                                     organizer_additional_params, ownership)
VALUES (162, '2013-04-07 18:09:35.002', FALSE, '2013-04-07 18:09:35.002', FALSE, '2013-03-28 08:30:00', NULL, NULL,
        'Micromata', NULL, NULL, NULL, NULL, NULL,
        'FREQ=WEEKLY;INTERVAL=1',
        NULL, NULL, 15,
        'MINUTES', NULL,
        '2013-03-28 08:00:00',
        'Legal affaires regular meeting', 157, 'testdata2', 19, NULL, NULL, NULL);
INSERT INTO t_plugin_calendar_event (pk, created, deleted, last_update, all_day, end_date, external_uid, last_email,
                                     location, note, organizer, recurrence_date, recurrence_ex_date,
                                     recurrence_reference_id, recurrence_rule, recurrence_until, reminder_action_type,
                                     reminder_duration, reminder_duration_unit, sequence, start_date, subject,
                                     calendar_fk, uid, team_event_fk_creator, dt_stamp,
                                     organizer_additional_params, ownership)
VALUES (163, '2013-04-07 18:10:39.758', FALSE, '2013-04-07 18:10:39.758', FALSE, '2013-04-02 08:15:00', NULL, NULL,
        'Headquarter', 'Every 2 weeks', NULL, NULL, NULL,
        NULL,
        'FREQ=WEEKLY;INTERVAL=2',
        NULL,
        'MESSAGE_SOUND',
        15,
        'MINUTES',
        NULL,
        '2013-04-02 07:00:00',
        'Team breakfast', 158, 'testdata3', 19, NULL, NULL, NULL);
INSERT INTO t_plugin_calendar_event (pk, created, deleted, last_update, all_day, end_date, external_uid, last_email,
                                     location, note, organizer, recurrence_date, recurrence_ex_date,
                                     recurrence_reference_id, recurrence_rule, recurrence_until, reminder_action_type,
                                     reminder_duration, reminder_duration_unit, sequence, start_date, subject,
                                     calendar_fk, uid, team_event_fk_creator, dt_stamp,
                                     organizer_additional_params, ownership)
VALUES (164, '2013-04-07 18:11:21.989', FALSE, '2013-04-07 18:11:21.989', TRUE, '2013-04-03 00:00:00', NULL, NULL,
        'Sweet home', NULL, NULL, NULL, NULL, NULL,
        'FREQ=WEEKLY;INTERVAL=1',
        NULL, NULL, 15,
        'MINUTES', NULL,
        '2013-04-03 00:00:00',
        'My home office day', 157, 'testdata4', 19, NULL, NULL, NULL);
INSERT INTO t_plugin_calendar_event (pk, created, deleted, last_update, all_day, end_date, external_uid, last_email,
                                     location, note, organizer, recurrence_date, recurrence_ex_date,
                                     recurrence_reference_id, recurrence_rule, recurrence_until, reminder_action_type,
                                     reminder_duration, reminder_duration_unit, sequence, start_date, subject,
                                     calendar_fk, uid, team_event_fk_creator, dt_stamp,
                                     organizer_additional_params, ownership)
VALUES (165, '2013-04-07 18:12:37.26', FALSE, '2013-04-07 18:12:37.26', FALSE, '2013-04-05 10:15:00', NULL, NULL,
        'Headquarter', NULL, NULL, NULL, NULL, NULL,
        'FREQ=WEEKLY;INTERVAL=1',
        NULL, NULL, 15,
        'MINUTES', NULL,
        '2013-04-05 08:45:00',
        'Weekly team-meeting', 158, 'testdata5', 19, NULL, NULL, NULL);
INSERT INTO t_plugin_calendar_event (pk, created, deleted, last_update, all_day, end_date, external_uid, last_email,
                                     location, note, organizer, recurrence_date, recurrence_ex_date,
                                     recurrence_reference_id, recurrence_rule, recurrence_until, reminder_action_type,
                                     reminder_duration, reminder_duration_unit, sequence, start_date, subject,
                                     calendar_fk, uid, team_event_fk_creator, dt_stamp,
                                     organizer_additional_params, ownership)
VALUES (166, '2013-04-07 18:14:07.579', FALSE, '2013-04-07 18:14:07.579', FALSE, '2013-04-04 13:15:00', NULL, NULL,
        'Gym', NULL, NULL, NULL, NULL, NULL,
        'FREQ=WEEKLY;INTERVAL=1',
        NULL, 'MESSAGE_SOUND', 1,
        'HOURS', NULL,
        '2013-04-04 12:00:00',
        'Volleyball training', 159, 'testdata6', 19, NULL, NULL, NULL);
INSERT INTO t_plugin_calendar_event (pk, created, deleted, last_update, all_day, end_date, external_uid, last_email,
                                     location, note, organizer, recurrence_date, recurrence_ex_date,
                                     recurrence_reference_id, recurrence_rule, recurrence_until, reminder_action_type,
                                     reminder_duration, reminder_duration_unit, sequence, start_date, subject,
                                     calendar_fk, uid, team_event_fk_creator, dt_stamp,
                                     organizer_additional_params, ownership)
VALUES (167, '2013-04-07 18:15:11.396', FALSE, '2013-04-07 18:15:11.396', FALSE, '2013-04-01 09:15:00', NULL, NULL,
        'Headquarter', 'weekly', NULL, NULL, NULL, NULL,
        'FREQ=WEEKLY;INTERVAL=1',
        NULL, NULL, 15,
        'MINUTES', NULL,
        '2013-04-01 08:00:00',
        'Code review meeting', 158, 'testdata7', 19, NULL, NULL, NULL);

INSERT INTO t_fibu_konto (pk, created, deleted, last_update, bezeichnung, description, nummer, status)
VALUES (46, '2014-09-02 12:27:50.484', FALSE, '2014-09-02 12:27:50.484', 'Yellow Logistics', NULL, 1000, 'ACTIVE');

INSERT INTO t_fibu_kunde (pk, created, deleted, last_update, description, division, identifier, name, status,
                          konto_id)
VALUES (100, '2010-04-21 22:11:17.078', FALSE, '2013-04-27 15:05:14.483', NULL, NULL, 'YellowLog', 'Yellow Logistics',
        'ACTIVE', NULL);
INSERT INTO t_fibu_kunde (pk, created, deleted, last_update, description, division, identifier, name, status,
                          konto_id)
VALUES (200, '2010-04-21 22:11:35.559', FALSE, '2010-04-21 22:11:35.559', NULL, NULL, 'MIC', 'Micromata', 'ACTIVE',
        NULL);

INSERT INTO t_group (pk, created, deleted, last_update, description, ldap_values, local_group, name, organization)
VALUES (36, '2009-01-05 23:00:42.43', FALSE, '2013-04-07 16:11:55.557', NULL, NULL, FALSE, 'PF_ProjectAssistant', NULL);
INSERT INTO t_group (pk, created, deleted, last_update, description, ldap_values, local_group, name, organization)
VALUES (37, '2016-11-14 23:00:42.43', FALSE, '2016-11-14 23:00:42.43', NULL, NULL, FALSE, 'PF_Organization', NULL);
INSERT INTO t_group (pk, created, deleted, last_update, description, ldap_values, local_group, name, organization)
VALUES (27, '2013-04-07 16:09:49.973', FALSE, '2017-11-23 12:56:34.574', 'project managers', NULL, FALSE,
        'Yellow web portal-managers', 'My company');
INSERT INTO t_group (pk, created, deleted, last_update, description, ldap_values, local_group, name, organization)
VALUES (28, '2010-04-21 22:05:39.677', TRUE, '2017-11-23 12:56:34.67', NULL, NULL, FALSE,
        'ProjectForge Projectmanagers', NULL);
INSERT INTO t_group (pk, created, deleted, last_update, description, ldap_values, local_group, name, organization)
VALUES (29, '2010-04-21 22:05:26.558', TRUE, '2017-11-23 12:56:34.783', NULL, NULL, FALSE, 'ACME Projectmanagers',
        NULL);
INSERT INTO t_group (pk, created, deleted, last_update, description, ldap_values, local_group, name, organization)
VALUES (30, '2008-01-10 08:26:19.863', FALSE, '2017-11-23 12:56:34.866', 'Administrators of ProjectForge', NULL, FALSE,
        'PF_Admin', NULL);
INSERT INTO t_group (pk, created, deleted, last_update, description, ldap_values, local_group, name, organization)
VALUES (31, '2008-01-10 08:26:19.863', FALSE, '2017-11-23 12:56:34.973',
        'Users for having full access to the companies finances.', NULL, FALSE, 'PF_Finance', NULL);
INSERT INTO t_group (pk, created, deleted, last_update, description, ldap_values, local_group, name, organization)
VALUES (32, '2009-01-05 22:59:34.047', FALSE, '2017-11-23 12:56:35.055',
        'Users for having read access to the companies finances.', NULL, FALSE, 'PF_Controlling',
        NULL);
INSERT INTO t_group (pk, created, deleted, last_update, description, ldap_values, local_group, name, organization)
VALUES (33, '2016-09-01 22:59:34.047', FALSE, '2017-11-23 12:56:35.132',
        'Users for having full access to the companies hr.', NULL, FALSE, 'PF_HR', NULL);
INSERT INTO t_group (pk, created, deleted, last_update, description, ldap_values, local_group, name, organization)
VALUES (34, '2009-01-05 23:00:17.99', FALSE, '2017-11-23 12:56:35.224',
        'Marketing users can download all addresses in excel format.', NULL, FALSE, 'PF_Marketing',
        NULL);
INSERT INTO t_group (pk, created, deleted, last_update, description, ldap_values, local_group, name, organization)
VALUES (35, '2009-01-05 23:00:42.43', FALSE, '2017-11-23 12:56:35.296', NULL, NULL, FALSE, 'PF_ProjectManager', NULL);
INSERT INTO t_group (pk, created, deleted, last_update, description, ldap_values, local_group, name, organization)
VALUES (38, '2010-04-21 22:04:50.192', TRUE, '2017-11-23 12:56:35.404', NULL, NULL, FALSE, 'ACME Developers', NULL);
INSERT INTO t_group (pk, created, deleted, last_update, description, ldap_values, local_group, name, organization)
VALUES (39, '2010-04-21 22:05:07.731', TRUE, '2017-11-23 12:56:35.48', NULL, NULL, FALSE, 'ProjectForge Developers',
        NULL);
INSERT INTO t_group (pk, created, deleted, last_update, description, ldap_values, local_group, name, organization)
VALUES (40, '2013-04-07 16:03:16.897', FALSE, '2017-11-23 12:56:35.552', 'All employees', NULL, FALSE, 'My Company',
        'My company');
INSERT INTO t_group (pk, created, deleted, last_update, description, ldap_values, local_group, name, organization)
VALUES (41, '2013-04-07 16:07:22.843', FALSE, '2017-11-23 12:56:35.662', 'ProjectForge team members', NULL, FALSE,
        'ProjectForge', 'www.projectforge.org');
INSERT INTO t_group (pk, created, deleted, last_update, description, ldap_values, local_group, name, organization)
VALUES (42, '2013-04-07 16:07:47.451', FALSE, '2017-11-23 12:56:35.737', 'ProjectForge project leaders', NULL, FALSE,
        'ProjectForge-managers',
        'www.projectforge.org');
INSERT INTO t_group (pk, created, deleted, last_update, description, ldap_values, local_group, name, organization)
VALUES (43, '2013-04-07 16:09:37.721', FALSE, '2017-11-23 12:56:35.821', 'project team', NULL, FALSE,
        'Yellow web portal', 'My company');
INSERT INTO t_group (pk, created, deleted, last_update, description, ldap_values, local_group, name, organization)
VALUES (44, '2013-04-07 16:10:21.364', FALSE, '2017-11-23 12:56:35.908', 'project team', NULL, FALSE,
        'Yellow track & trace', 'My company');
INSERT INTO t_group (pk, created, deleted, last_update, description, ldap_values, local_group, name, organization)
VALUES (45, '2013-04-07 16:10:58.345', FALSE, '2017-11-23 12:56:35.988', 'project managers', NULL, FALSE,
        'Yellow track & trace-managers', 'My company');

INSERT INTO t_fibu_projekt (pk, created, deleted, last_update, description, identifier, intern_kost2_4, name, nummer,
                            status, konto_id, kunde_id, projektmanager_group_fk, task_fk,
                            headofbusinessmanager_fk, salesmanager_fk, projectmanager_fk)
VALUES (68, '2010-04-21 22:12:13.11', FALSE, '2013-04-27 15:04:55.693', NULL, 'Yellow Logistics Web', NULL,
        'Yellow Logistics Webportal', 0, 'BUILD', NULL, 100,
        27, 49, NULL, NULL, NULL);
INSERT INTO t_fibu_projekt (pk, created, deleted, last_update, description, identifier, intern_kost2_4, name, nummer,
                            status, konto_id, kunde_id, projektmanager_group_fk, task_fk,
                            headofbusinessmanager_fk, salesmanager_fk, projectmanager_fk)
VALUES (69, '2010-04-21 22:15:33.36', FALSE, '2010-04-21 22:15:33.36', NULL, 'PF', NULL, 'ProjectForge', 0,
        'MAINTENANCE', NULL, 200, 28, 57, NULL, NULL, NULL);

INSERT INTO t_fibu_auftrag (pk, created, deleted, last_update, angebots_datum, status, beauftragungs_beschreibung,
                            beauftragungs_datum, bemerkung, bindungs_frist, kunde_text, nummer,
                            period_of_performance_begin, period_of_performance_end, referenz, status_beschreibung,
                            titel, ui_status_as_xml, contact_person_fk, kunde_fk, projekt_fk,
                            entscheidungs_datum, erfassungs_datum, probability_of_occurrence, headofbusinessmanager_fk,
                            salesmanager_fk, projectmanager_fk)
VALUES (104, '2010-04-21 22:20:17.581', FALSE, '2013-04-27 15:06:48.382', '2010-04-22', 'BEAUFTRAGT', NULL,
        '2010-03-29', NULL, '2010-04-20', NULL, 1, NULL, NULL,
        NULL, NULL,
        'Yellow Logistics Webportal Release 1.0',
        '<auftragUIStatus><closedPositions/></auftragUIStatus>',
        17, 100, 68,
        NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO t_fibu_auftrag (pk, created, deleted, last_update, angebots_datum, status, beauftragungs_beschreibung,
                            beauftragungs_datum, bemerkung, bindungs_frist, kunde_text, nummer,
                            period_of_performance_begin, period_of_performance_end, referenz, status_beschreibung,
                            titel, ui_status_as_xml, contact_person_fk, kunde_fk, projekt_fk,
                            entscheidungs_datum, erfassungs_datum, probability_of_occurrence, headofbusinessmanager_fk,
                            salesmanager_fk, projectmanager_fk)
VALUES (108, '2010-04-21 22:21:12.474', FALSE, '2013-04-27 15:06:38.601', '2010-04-22', 'GELEGT', NULL, NULL, NULL,
        NULL, NULL, 2, NULL, NULL, NULL, NULL,
        'Yellow Logistics Webportal Release 1.1',
        '<auftragUIStatus><closedPositions/></auftragUIStatus>',
        17, 100, 68, NULL, NULL,
        NULL, NULL, NULL, NULL);
INSERT INTO t_fibu_auftrag (pk, created, deleted, last_update, angebots_datum, status, beauftragungs_beschreibung,
                            beauftragungs_datum, bemerkung, bindungs_frist, kunde_text, nummer,
                            period_of_performance_begin, period_of_performance_end, referenz, status_beschreibung,
                            titel, ui_status_as_xml, contact_person_fk, kunde_fk, projekt_fk,
                            entscheidungs_datum, erfassungs_datum, probability_of_occurrence, headofbusinessmanager_fk,
                            salesmanager_fk, projectmanager_fk)
VALUES (110, '2010-04-21 22:22:19.462', FALSE, '2010-04-21 22:22:19.462', '2010-04-22', 'POTENZIAL', NULL, NULL, NULL,
        NULL, 'Micromata', 3, NULL, NULL, NULL, NULL,
        'Micromata Webportal',
        NULL, 17, NULL, NULL,
        NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO t_fibu_auftrag_position (pk, created, deleted, last_update, art, bemerkung, mode_of_payment_type,
                                     netto_summe, number, period_of_performance_begin, period_of_performance_end,
                                     period_of_performance_type, person_days, status, titel, vollstaendig_fakturiert,
                                     auftrag_fk, task_fk, paymenttype)
VALUES (105, NULL, FALSE, NULL, NULL, NULL, NULL, 40000.00, 1, NULL, NULL, NULL, 40.00, 'BEAUFTRAGT',
        'Pflichtenheft / System Requirements Specification', FALSE,
        104, 52, 'FESTPREISPAKET');
INSERT INTO t_fibu_auftrag_position (pk, created, deleted, last_update, art, bemerkung, mode_of_payment_type,
                                     netto_summe, number, period_of_performance_begin, period_of_performance_end,
                                     period_of_performance_type, person_days, status, titel, vollstaendig_fakturiert,
                                     auftrag_fk, task_fk, paymenttype)
VALUES (106, NULL, FALSE, NULL, NULL, NULL, NULL, 250000.00, 2, NULL, NULL, NULL, 250.00, 'BEAUFTRAGT',
        'Realisierung / Realization', FALSE, 104, 53,
        'FESTPREISPAKET');
INSERT INTO t_fibu_auftrag_position (pk, created, deleted, last_update, art, bemerkung, mode_of_payment_type,
                                     netto_summe, number, period_of_performance_begin, period_of_performance_end,
                                     period_of_performance_type, person_days, status, titel, vollstaendig_fakturiert,
                                     auftrag_fk, task_fk, paymenttype)
VALUES (107, NULL, FALSE, NULL, NULL, NULL, NULL, 10000.00, 3, NULL, NULL, NULL, 10.00, 'BEAUFTRAGT', 'Installation',
        FALSE, 104, 56, 'TIME_AND_MATERIALS');
INSERT INTO t_fibu_auftrag_position (pk, created, deleted, last_update, art, bemerkung, mode_of_payment_type,
                                     netto_summe, number, period_of_performance_begin, period_of_performance_end,
                                     period_of_performance_type, person_days, status, titel, vollstaendig_fakturiert,
                                     auftrag_fk, task_fk, paymenttype)
VALUES (109, NULL, FALSE, NULL, NULL, NULL, NULL, 120000.00, 1, NULL, NULL, NULL, 120.00, NULL, 'Release 1.1', FALSE,
        108, 58, 'FESTPREISPAKET');
INSERT INTO t_fibu_auftrag_position (pk, created, deleted, last_update, art, bemerkung, mode_of_payment_type,
                                     netto_summe, number, period_of_performance_begin, period_of_performance_end,
                                     period_of_performance_type, person_days, status, titel, vollstaendig_fakturiert,
                                     auftrag_fk, task_fk, paymenttype)
VALUES (111, NULL, FALSE, NULL, NULL, NULL, NULL, 15000.00, 1, NULL, NULL, NULL, 15.00, NULL, NULL, FALSE, 110, NULL,
        'TIME_AND_MATERIALS');

INSERT INTO t_fibu_kost2art (pk, created, deleted, last_update, description, fakturiert, name, projekt_standard,
                             work_fraction)
VALUES (2, '2010-04-21 22:12:56.974', FALSE, '2013-04-27 15:08:22.056', NULL, TRUE, 'Realization / Realisierung', TRUE,
        NULL);
INSERT INTO t_fibu_kost2art (pk, created, deleted, last_update, description, fakturiert, name, projekt_standard,
                             work_fraction)
VALUES (0, '2010-04-21 22:12:41.078', FALSE, '2013-04-27 15:08:12.714', NULL, FALSE, 'Acquisition / Akquise', TRUE,
        NULL);
INSERT INTO t_fibu_kost2art (pk, created, deleted, last_update, description, fakturiert, name, projekt_standard,
                             work_fraction)
VALUES (1, '2010-04-21 22:13:46.279', FALSE, '2010-04-21 22:13:46.279', NULL, TRUE, 'Meetings', TRUE, NULL);
INSERT INTO t_fibu_kost2art (pk, created, deleted, last_update, description, fakturiert, name, projekt_standard,
                             work_fraction)
VALUES (10, '2010-04-21 22:13:23.409', FALSE, '2013-04-27 15:08:30.955', NULL, FALSE, 'Travelling / Reise', TRUE,
        0.50000);
INSERT INTO t_fibu_kost2art (pk, created, deleted, last_update, description, fakturiert, name, projekt_standard,
                             work_fraction)
VALUES (11, '2010-04-21 22:14:01.379', FALSE, '2010-04-21 22:14:01.379', NULL, FALSE, 'Meetings', TRUE, NULL);
INSERT INTO t_fibu_kost2art (pk, created, deleted, last_update, description, fakturiert, name, projekt_standard,
                             work_fraction)
VALUES (99, '2010-04-22 21:49:28.01', FALSE, '2013-04-27 15:08:37.678', NULL, FALSE, 'Misc / Diverses', FALSE, NULL);

INSERT INTO t_fibu_kost2 (pk, created, deleted, last_update, bereich, comment, description, kostentraegerstatus,
                          nummernkreis, teilbereich, work_fraction, kost2_art_id, projekt_id)
VALUES (70, '2010-04-21 22:14:39.793', FALSE, '2010-04-21 22:14:39.793', 100, NULL, NULL, NULL, 5, 0, NULL, 2, 68);
INSERT INTO t_fibu_kost2 (pk, created, deleted, last_update, bereich, comment, description, kostentraegerstatus,
                          nummernkreis, teilbereich, work_fraction, kost2_art_id, projekt_id)
VALUES (71, '2010-04-21 22:14:39.736', FALSE, '2010-04-21 22:14:39.736', 100, NULL, NULL, NULL, 5, 0, NULL, 0, 68);
INSERT INTO t_fibu_kost2 (pk, created, deleted, last_update, bereich, comment, description, kostentraegerstatus,
                          nummernkreis, teilbereich, work_fraction, kost2_art_id, projekt_id)
VALUES (72, '2010-04-21 22:14:39.807', FALSE, '2010-04-21 22:14:39.807', 100, NULL, NULL, NULL, 5, 0, NULL, 10, 68);
INSERT INTO t_fibu_kost2 (pk, created, deleted, last_update, bereich, comment, description, kostentraegerstatus,
                          nummernkreis, teilbereich, work_fraction, kost2_art_id, projekt_id)
VALUES (73, '2010-04-21 22:15:33.397', FALSE, '2010-04-21 22:15:33.397', 200, NULL, NULL, NULL, 5, 0, NULL, 2, 69);
INSERT INTO t_fibu_kost2 (pk, created, deleted, last_update, bereich, comment, description, kostentraegerstatus,
                          nummernkreis, teilbereich, work_fraction, kost2_art_id, projekt_id)
VALUES (74, '2010-04-22 21:49:02.547', FALSE, '2010-04-22 21:49:46.26', 0, NULL, 'Krank / ill', 'ACTIVE', 7, 1, NULL,
        99, NULL);
INSERT INTO t_fibu_kost2 (pk, created, deleted, last_update, bereich, comment, description, kostentraegerstatus,
                          nummernkreis, teilbereich, work_fraction, kost2_art_id, projekt_id)
VALUES (75, '2010-04-21 22:14:39.78', FALSE, '2010-04-21 22:14:39.78', 100, NULL, NULL, NULL, 5, 0, NULL, 1, 68);
INSERT INTO t_fibu_kost2 (pk, created, deleted, last_update, bereich, comment, description, kostentraegerstatus,
                          nummernkreis, teilbereich, work_fraction, kost2_art_id, projekt_id)
VALUES (76, '2010-04-21 22:14:39.819', FALSE, '2010-04-21 22:14:39.819', 100, NULL, NULL, NULL, 5, 0, NULL, 11, 68);
INSERT INTO t_fibu_kost2 (pk, created, deleted, last_update, bereich, comment, description, kostentraegerstatus,
                          nummernkreis, teilbereich, work_fraction, kost2_art_id, projekt_id)
VALUES (77, '2010-04-21 22:15:33.379', FALSE, '2010-04-21 22:15:33.379', 200, NULL, NULL, NULL, 5, 0, NULL, 0, 69);
INSERT INTO t_fibu_kost2 (pk, created, deleted, last_update, bereich, comment, description, kostentraegerstatus,
                          nummernkreis, teilbereich, work_fraction, kost2_art_id, projekt_id)
VALUES (78, '2010-04-21 22:15:33.415', FALSE, '2010-04-21 22:15:33.415', 200, NULL, NULL, NULL, 5, 0, NULL, 11, 69);
INSERT INTO t_fibu_kost2 (pk, created, deleted, last_update, bereich, comment, description, kostentraegerstatus,
                          nummernkreis, teilbereich, work_fraction, kost2_art_id, projekt_id)
VALUES (79, '2010-04-22 21:47:33.859', FALSE, '2010-04-22 21:49:37.407', 0, NULL, 'Urlaub / Holiday', 'ACTIVE', 7, 0,
        NULL, 99, NULL);

INSERT INTO t_fibu_eingangsrechnung (pk, created, deleted, last_update, bemerkung, besonderheiten, betreff,
                                     bezahl_datum, datum, faelligkeit, ui_status_as_xml, zahl_betrag, kreditor,
                                     payment_type, referenz, konto_id, bic, iban, receiver, customernr,
                                     discountmaturity, discountpercent)
VALUES (123, '2010-04-23 21:13:09.313', FALSE, '2010-04-23 21:13:33.144', NULL, NULL, 'Broschures', NULL, '2010-04-23',
        '2010-06-22', NULL, NULL, 'Copy shop', NULL,
        'CS ADKE2345', NULL,
        NULL, NULL, NULL, NULL,
        NULL, NULL);
INSERT INTO t_fibu_eingangsrechnung (pk, created, deleted, last_update, bemerkung, besonderheiten, betreff,
                                     bezahl_datum, datum, faelligkeit, ui_status_as_xml, zahl_betrag, kreditor,
                                     payment_type, referenz, konto_id, bic, iban, receiver, customernr,
                                     discountmaturity, discountpercent)
VALUES (120, '2010-04-23 21:10:43.591', FALSE, '2014-09-02 12:28:28.3', NULL, NULL,
        'First layout of Yellow Logistic Webdesign', NULL, '2010-04-23', '2010-06-22',
        '<rechnungUIStatus><closedPositions/></rechnungUIStatus>', NULL, 'Webdesign Company', NULL, 'WD-2010-5634',
        46, NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO t_fibu_eingangsrechnung_position (pk, created, deleted, last_update, einzel_netto, menge, number, s_text,
                                              vat, eingangsrechnung_fk)
VALUES (121, NULL, FALSE, NULL, 60.00, 5.00000, 1, NULL, 0.19000, 120);
INSERT INTO t_fibu_eingangsrechnung_position (pk, created, deleted, last_update, einzel_netto, menge, number, s_text,
                                              vat, eingangsrechnung_fk)
VALUES (124, NULL, FALSE, NULL, 187.00, 1.00000, 1, NULL, 0.19000, 123);

INSERT INTO t_fibu_employee_timed (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, group_name,
                                   start_time, employee_id)
VALUES (484, '2017-11-23 12:56:36.593', '19', '2017-11-23 12:56:36.593', '19', 0, 'employeestatus',
        '2010-01-01 00:00:00', 126);
INSERT INTO t_fibu_employee_timed (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, group_name,
                                   start_time, employee_id)
VALUES (491, '2017-11-23 12:56:36.679', '19', '2017-11-23 12:56:36.679', '19', 0, 'employeestatus',
        '2010-01-01 00:00:00', 127);

INSERT INTO t_fibu_employee_timedattr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                                       propertyname, type, parent)
VALUES ('0', 485, '2017-11-23 12:56:36.593', '19', '2017-11-23 12:56:36.593', '19', 0,
        'fibu.employee.status.festAngestellter', 'status', 'V', 484);
INSERT INTO t_fibu_employee_timedattr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                                       propertyname, type, parent)
VALUES ('0', 492, '2017-11-23 12:56:36.679', '19', '2017-11-23 12:56:36.679', '19', 0,
        'fibu.employee.status.festAngestellter', 'status', 'V', 491);

INSERT INTO t_fibu_rechnung (pk, created, deleted, last_update, bemerkung, besonderheiten, betreff, bezahl_datum, datum,
                             faelligkeit, ui_status_as_xml, zahl_betrag, kunde_text, nummer, status, typ,
                             konto_id, kunde_id, projekt_id, bic, iban, receiver, discountmaturity, discountpercent,
                             period_of_performance_begin, period_of_performance_end, customerref1, customeraddress,
                             attachment)
VALUES (112, '2010-04-23 21:05:16.26', FALSE, '2010-04-23 21:09:30.714', NULL, NULL, 'Pflichtenheft / Specification',
        '2010-04-23', '2010-04-01', '2010-05-01', NULL,
        47600.00, NULL, 1000, 'BEZAHLT', 'RECHNUNG', NULL, 100, 68, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        NULL, NULL);
INSERT INTO t_fibu_rechnung (pk, created, deleted, last_update, bemerkung, besonderheiten, betreff, bezahl_datum, datum,
                             faelligkeit, ui_status_as_xml, zahl_betrag, kunde_text, nummer, status, typ,
                             konto_id, kunde_id, projekt_id, bic, iban, receiver, discountmaturity, discountpercent,
                             period_of_performance_begin, period_of_performance_end, customerref1, customeraddress,
                             attachment)
VALUES (116, '2010-04-23 21:07:43.813', FALSE, '2010-04-23 21:08:19.315', NULL, NULL, 'Realization First Milestone',
        NULL, '2010-04-23', '2010-05-23', NULL, NULL,
        NULL,
        1001,
        'GESTELLT',
        'RECHNUNG',
        NULL,
        100, 68,
        NULL,
        NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO t_fibu_rechnung_position (pk, created, deleted, last_update, einzel_netto, menge, number, s_text, vat,
                                      auftrags_position_fk, rechnung_fk, period_of_performance_begin,
                                      period_of_performance_end, period_of_performance_type)
VALUES (113, NULL, FALSE, NULL, 1000.00, 40.00000, 1, NULL, 0.19000, 105, 112, NULL, NULL, NULL);
INSERT INTO t_fibu_rechnung_position (pk, created, deleted, last_update, einzel_netto, menge, number, s_text, vat,
                                      auftrags_position_fk, rechnung_fk, period_of_performance_begin,
                                      period_of_performance_end, period_of_performance_type)
VALUES (117, NULL, FALSE, NULL, 1000.00, 10.00000, 1, NULL, 0.19000, 106, 116, NULL, NULL, NULL);

INSERT INTO t_fibu_kost_zuweisung (pk, created, deleted, last_update, comment, index, netto,
                                   eingangsrechnungs_pos_fk, employee_salary_fk, kost1_fk, kost2_fk, rechnungs_pos_fk)
VALUES (114, NULL, FALSE, NULL, NULL, 0, 20000.00, NULL, NULL, 80, 71, 113);
INSERT INTO t_fibu_kost_zuweisung (pk, created, deleted, last_update, comment, index, netto,
                                   eingangsrechnungs_pos_fk, employee_salary_fk, kost1_fk, kost2_fk, rechnungs_pos_fk)
VALUES (115, NULL, FALSE, NULL, NULL, 1, 20000.00, NULL, NULL, 81, 71, 113);
INSERT INTO t_fibu_kost_zuweisung (pk, created, deleted, last_update, comment, index, netto,
                                   eingangsrechnungs_pos_fk, employee_salary_fk, kost1_fk, kost2_fk, rechnungs_pos_fk)
VALUES (118, NULL, FALSE, NULL, NULL, 0, 5000.00, NULL, NULL, 81, 71, 117);
INSERT INTO t_fibu_kost_zuweisung (pk, created, deleted, last_update, comment, index, netto,
                                   eingangsrechnungs_pos_fk, employee_salary_fk, kost1_fk, kost2_fk, rechnungs_pos_fk)
VALUES (119, NULL, FALSE, NULL, NULL, 1, 5000.00, NULL, NULL, 81, 71, 117);
INSERT INTO t_fibu_kost_zuweisung (pk, created, deleted, last_update, comment, index, netto,
                                   eingangsrechnungs_pos_fk, employee_salary_fk, kost1_fk, kost2_fk, rechnungs_pos_fk)
VALUES (122, NULL, FALSE, NULL, NULL, 0, 300.00, 121, NULL, 82, 70, NULL);
INSERT INTO t_fibu_kost_zuweisung (pk, created, deleted, last_update, comment, index, netto,
                                   eingangsrechnungs_pos_fk, employee_salary_fk, kost1_fk, kost2_fk, rechnungs_pos_fk)
VALUES (125, NULL, FALSE, NULL, NULL, 0, 187.00, 124, NULL, 82, 71, NULL);

INSERT INTO t_gantt_chart (pk, created, deleted, last_update, gantt_objects_as_xml, name, read_access, settings_as_xml,
                           style_as_xml, write_access, owner_fk, task_fk)
VALUES (130, '2010-08-27 17:38:27.051', FALSE, '2013-04-27 15:21:02.568', '<ganttObject id="3"><children><ganttObject id="4" visible="true"><children><ganttObject
      id="5" visible="true"/><ganttObject id="6" visible="true"/><ganttObject id="7" visible="true"/><ganttObject
      id="8" visible="true"/><ganttObject id="9" visible="true"/><ganttObject id="10" visible="true"/><ganttObject
      id="20" visible="true"/></children></ganttObject><ganttObject id="12" visible="true"/></children></ganttObject>
    ', 'Yellow Logistics Webportal', 'ALL', '<ganttChartSettings><openNodes/></ganttChartSettings>',
        '<ganttChartStyle totalLabelWidth="240.0" xTicks="AUTO" width="1000"/>', 'OWNER', 19, 49);

INSERT INTO t_group_task_access (pk, created, deleted, last_update, description, recursive, group_id,
                                 task_id)
VALUES (172, '2010-04-21 22:05:58.058', TRUE, '2013-04-07 16:03:59.26', NULL, TRUE, 38, 48);
INSERT INTO t_group_task_access (pk, created, deleted, last_update, description, recursive, group_id,
                                 task_id)
VALUES (177, '2010-04-21 22:06:16.612', TRUE, '2013-04-07 16:04:15.33', NULL, TRUE, 29, 48);
INSERT INTO t_group_task_access (pk, created, deleted, last_update, description, recursive, group_id,
                                 task_id)
VALUES (182, '2010-04-21 22:06:49.685', TRUE, '2013-04-07 16:04:05.45', NULL, TRUE, 38, 57);
INSERT INTO t_group_task_access (pk, created, deleted, last_update, description, recursive, group_id,
                                 task_id)
VALUES (187, '2010-04-21 22:07:07.148', TRUE, '2013-04-07 16:12:39.484', NULL, TRUE, 28, 57);
INSERT INTO t_group_task_access (pk, created, deleted, last_update, description, recursive, group_id,
                                 task_id)
VALUES (192, '2013-04-07 16:03:20.644', FALSE, '2013-04-07 16:03:20.644', NULL, TRUE, 40, 63);
INSERT INTO t_group_task_access (pk, created, deleted, last_update, description, recursive, group_id,
                                 task_id)
VALUES (197, '2013-04-07 16:07:49.822', FALSE, '2013-04-07 16:07:49.822', NULL, TRUE, 42, 57);
INSERT INTO t_group_task_access (pk, created, deleted, last_update, description, recursive, group_id,
                                 task_id)
VALUES (202, '2013-04-07 16:07:49.837', FALSE, '2013-04-07 16:07:49.837', NULL, TRUE, 41, 57);
INSERT INTO t_group_task_access (pk, created, deleted, last_update, description, recursive, group_id,
                                 task_id)
VALUES (207, '2013-04-07 16:09:52.019', FALSE, '2013-04-07 16:09:52.019', NULL, TRUE, 27, 49);
INSERT INTO t_group_task_access (pk, created, deleted, last_update, description, recursive, group_id,
                                 task_id)
VALUES (212, '2013-04-07 16:09:52.034', FALSE, '2013-04-07 16:09:52.034', NULL, FALSE, 27, 48);
INSERT INTO t_group_task_access (pk, created, deleted, last_update, description, recursive, group_id,
                                 task_id)
VALUES (217, '2013-04-07 16:09:52.051', FALSE, '2013-04-07 16:09:52.051', NULL, TRUE, 43, 49);
INSERT INTO t_group_task_access (pk, created, deleted, last_update, description, recursive, group_id,
                                 task_id)
VALUES (222, '2013-04-07 16:09:52.069', FALSE, '2013-04-07 16:09:52.069', NULL, FALSE, 43, 48);
INSERT INTO t_group_task_access (pk, created, deleted, last_update, description, recursive, group_id,
                                 task_id)
VALUES (227, '2013-04-07 16:11:00.72', FALSE, '2013-04-07 16:11:00.72', NULL, TRUE, 45, 67);
INSERT INTO t_group_task_access (pk, created, deleted, last_update, description, recursive, group_id,
                                 task_id)
VALUES (232, '2013-04-07 16:11:00.77', FALSE, '2013-04-07 16:11:00.77', NULL, FALSE, 45, 48);
INSERT INTO t_group_task_access (pk, created, deleted, last_update, description, recursive, group_id,
                                 task_id)
VALUES (237, '2013-04-07 16:11:00.784', FALSE, '2013-04-07 16:11:00.784', NULL, TRUE, 44, 67);
INSERT INTO t_group_task_access (pk, created, deleted, last_update, description, recursive, group_id,
                                 task_id)
VALUES (242, '2013-04-07 16:11:00.798', FALSE, '2013-04-07 16:11:00.798', NULL, FALSE, 44, 48);

INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (173, TRUE, TRUE, TRUE, 'TASKS', TRUE, 172);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (174, TRUE, TRUE, TRUE, 'OWN_TIMESHEETS', TRUE, 172);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (175, FALSE, FALSE, TRUE, 'TASK_ACCESS_MANAGEMENT', FALSE, 172);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (176, FALSE, FALSE, TRUE, 'TIMESHEETS', FALSE, 172);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (178, TRUE, TRUE, TRUE, 'TIMESHEETS', TRUE, 177);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (179, TRUE, TRUE, TRUE, 'TASKS', TRUE, 177);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (180, TRUE, TRUE, TRUE, 'OWN_TIMESHEETS', TRUE, 177);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (181, FALSE, FALSE, TRUE, 'TASK_ACCESS_MANAGEMENT', FALSE, 177);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (183, FALSE, FALSE, TRUE, 'TASK_ACCESS_MANAGEMENT', FALSE, 182);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (184, FALSE, FALSE, TRUE, 'TIMESHEETS', FALSE, 182);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (185, TRUE, TRUE, TRUE, 'TASKS', TRUE, 182);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (186, TRUE, TRUE, TRUE, 'OWN_TIMESHEETS', TRUE, 182);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (188, TRUE, TRUE, TRUE, 'TASKS', TRUE, 187);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (189, FALSE, FALSE, TRUE, 'TASK_ACCESS_MANAGEMENT', FALSE, 187);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (190, TRUE, TRUE, TRUE, 'TIMESHEETS', TRUE, 187);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (191, TRUE, TRUE, TRUE, 'OWN_TIMESHEETS', TRUE, 187);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (193, TRUE, TRUE, TRUE, 'TASKS', TRUE, 192);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (194, TRUE, TRUE, TRUE, 'OWN_TIMESHEETS', TRUE, 192);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (195, FALSE, FALSE, TRUE, 'TASK_ACCESS_MANAGEMENT', FALSE, 192);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (196, FALSE, FALSE, TRUE, 'TIMESHEETS', FALSE, 192);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (198, TRUE, TRUE, TRUE, 'TIMESHEETS', TRUE, 197);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (199, TRUE, TRUE, TRUE, 'OWN_TIMESHEETS', TRUE, 197);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (200, TRUE, TRUE, TRUE, 'TASKS', TRUE, 197);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (201, FALSE, FALSE, TRUE, 'TASK_ACCESS_MANAGEMENT', FALSE, 197);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (203, FALSE, FALSE, TRUE, 'TASK_ACCESS_MANAGEMENT', FALSE, 202);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (204, FALSE, FALSE, TRUE, 'TIMESHEETS', FALSE, 202);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (205, TRUE, TRUE, TRUE, 'TASKS', TRUE, 202);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (206, TRUE, TRUE, TRUE, 'OWN_TIMESHEETS', TRUE, 202);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (208, TRUE, TRUE, TRUE, 'TASKS', TRUE, 207);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (209, FALSE, FALSE, TRUE, 'TASK_ACCESS_MANAGEMENT', FALSE, 207);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (210, TRUE, TRUE, TRUE, 'TIMESHEETS', TRUE, 207);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (211, TRUE, TRUE, TRUE, 'OWN_TIMESHEETS', TRUE, 207);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (213, FALSE, FALSE, TRUE, 'TASKS', FALSE, 212);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (214, FALSE, FALSE, FALSE, 'OWN_TIMESHEETS', FALSE, 212);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (215, FALSE, FALSE, FALSE, 'TASK_ACCESS_MANAGEMENT', FALSE, 212);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (216, FALSE, FALSE, FALSE, 'TIMESHEETS', FALSE, 212);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (218, FALSE, FALSE, TRUE, 'TIMESHEETS', FALSE, 217);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (219, TRUE, TRUE, TRUE, 'TASKS', TRUE, 217);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (220, TRUE, TRUE, TRUE, 'OWN_TIMESHEETS', TRUE, 217);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (221, FALSE, FALSE, TRUE, 'TASK_ACCESS_MANAGEMENT', FALSE, 217);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (223, FALSE, FALSE, FALSE, 'TASK_ACCESS_MANAGEMENT', FALSE, 222);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (224, FALSE, FALSE, FALSE, 'TIMESHEETS', FALSE, 222);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (225, FALSE, FALSE, TRUE, 'TASKS', FALSE, 222);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (226, FALSE, FALSE, FALSE, 'OWN_TIMESHEETS', FALSE, 222);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (228, TRUE, TRUE, TRUE, 'TASKS', TRUE, 227);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (229, FALSE, FALSE, TRUE, 'TASK_ACCESS_MANAGEMENT', FALSE, 227);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (230, TRUE, TRUE, TRUE, 'TIMESHEETS', TRUE, 227);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (231, TRUE, TRUE, TRUE, 'OWN_TIMESHEETS', TRUE, 227);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (233, FALSE, FALSE, FALSE, 'OWN_TIMESHEETS', FALSE, 232);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (234, FALSE, FALSE, TRUE, 'TASKS', FALSE, 232);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (235, FALSE, FALSE, FALSE, 'TASK_ACCESS_MANAGEMENT', FALSE, 232);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (236, FALSE, FALSE, FALSE, 'TIMESHEETS', FALSE, 232);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (238, FALSE, FALSE, TRUE, 'TIMESHEETS', FALSE, 237);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (239, TRUE, TRUE, TRUE, 'OWN_TIMESHEETS', TRUE, 237);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (240, TRUE, TRUE, TRUE, 'TASKS', TRUE, 237);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (241, FALSE, FALSE, TRUE, 'TASK_ACCESS_MANAGEMENT', FALSE, 237);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (243, FALSE, FALSE, FALSE, 'TIMESHEETS', FALSE, 242);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (244, FALSE, FALSE, FALSE, 'OWN_TIMESHEETS', FALSE, 242);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (245, FALSE, FALSE, TRUE, 'TASKS', FALSE, 242);
INSERT INTO t_group_task_access_entry (pk, access_delete, access_insert, access_select, access_type, access_update,
                                       group_task_access_fk)
VALUES (246, FALSE, FALSE, FALSE, 'TASK_ACCESS_MANAGEMENT', FALSE, 242);

INSERT INTO t_group_user (group_id, user_id)
VALUES (27, 16);
INSERT INTO t_group_user (group_id, user_id)
VALUES (28, 17);
INSERT INTO t_group_user (group_id, user_id)
VALUES (29, 17);
INSERT INTO t_group_user (group_id, user_id)
VALUES (30, 17);
INSERT INTO t_group_user (group_id, user_id)
VALUES (30, 19);
INSERT INTO t_group_user (group_id, user_id)
VALUES (30, 18);
INSERT INTO t_group_user (group_id, user_id)
VALUES (31, 17);
INSERT INTO t_group_user (group_id, user_id)
VALUES (31, 19);
INSERT INTO t_group_user (group_id, user_id)
VALUES (31, 18);
INSERT INTO t_group_user (group_id, user_id)
VALUES (32, 19);
INSERT INTO t_group_user (group_id, user_id)
VALUES (33, 19);
INSERT INTO t_group_user (group_id, user_id)
VALUES (34, 19);
INSERT INTO t_group_user (group_id, user_id)
VALUES (35, 16);
INSERT INTO t_group_user (group_id, user_id)
VALUES (35, 20);
INSERT INTO t_group_user (group_id, user_id)
VALUES (35, 19);
INSERT INTO t_group_user (group_id, user_id)
VALUES (38, 17);
INSERT INTO t_group_user (group_id, user_id)
VALUES (38, 18);
INSERT INTO t_group_user (group_id, user_id)
VALUES (39, 17);
INSERT INTO t_group_user (group_id, user_id)
VALUES (40, 16);
INSERT INTO t_group_user (group_id, user_id)
VALUES (40, 24);
INSERT INTO t_group_user (group_id, user_id)
VALUES (40, 21);
INSERT INTO t_group_user (group_id, user_id)
VALUES (40, 17);
INSERT INTO t_group_user (group_id, user_id)
VALUES (40, 20);
INSERT INTO t_group_user (group_id, user_id)
VALUES (40, 26);
INSERT INTO t_group_user (group_id, user_id)
VALUES (40, 22);
INSERT INTO t_group_user (group_id, user_id)
VALUES (40, 23);
INSERT INTO t_group_user (group_id, user_id)
VALUES (40, 25);
INSERT INTO t_group_user (group_id, user_id)
VALUES (40, 18);
INSERT INTO t_group_user (group_id, user_id)
VALUES (41, 21);
INSERT INTO t_group_user (group_id, user_id)
VALUES (41, 17);
INSERT INTO t_group_user (group_id, user_id)
VALUES (41, 22);
INSERT INTO t_group_user (group_id, user_id)
VALUES (42, 21);
INSERT INTO t_group_user (group_id, user_id)
VALUES (42, 17);
INSERT INTO t_group_user (group_id, user_id)
VALUES (43, 16);
INSERT INTO t_group_user (group_id, user_id)
VALUES (43, 21);
INSERT INTO t_group_user (group_id, user_id)
VALUES (43, 17);
INSERT INTO t_group_user (group_id, user_id)
VALUES (43, 22);
INSERT INTO t_group_user (group_id, user_id)
VALUES (43, 23);
INSERT INTO t_group_user (group_id, user_id)
VALUES (44, 16);
INSERT INTO t_group_user (group_id, user_id)
VALUES (44, 24);
INSERT INTO t_group_user (group_id, user_id)
VALUES (44, 20);
INSERT INTO t_group_user (group_id, user_id)
VALUES (45, 16);

INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (382, '2017-11-23 12:56:34.621', 'anon', '2017-11-23 12:56:34.621', 'anon', 0, 27,
        'org.projectforge.framework.persistence.user.entities.GroupDO', 'Update',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (386, '2017-11-23 12:56:34.741', 'anon', '2017-11-23 12:56:34.741', 'anon', 0, 28,
        'org.projectforge.framework.persistence.user.entities.GroupDO', 'Update',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (390, '2017-11-23 12:56:34.828', 'anon', '2017-11-23 12:56:34.828', 'anon', 0, 29,
        'org.projectforge.framework.persistence.user.entities.GroupDO', 'Update',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (394, '2017-11-23 12:56:34.932', 'anon', '2017-11-23 12:56:34.932', 'anon', 0, 30,
        'org.projectforge.framework.persistence.user.entities.GroupDO', 'Update',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (398, '2017-11-23 12:56:35.02', 'anon', '2017-11-23 12:56:35.02', 'anon', 0, 31,
        'org.projectforge.framework.persistence.user.entities.GroupDO', 'Update',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (402, '2017-11-23 12:56:35.102', 'anon', '2017-11-23 12:56:35.102', 'anon', 0, 32,
        'org.projectforge.framework.persistence.user.entities.GroupDO', 'Update',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (406, '2017-11-23 12:56:35.185', 'anon', '2017-11-23 12:56:35.185', 'anon', 0, 33,
        'org.projectforge.framework.persistence.user.entities.GroupDO', 'Update',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (410, '2017-11-23 12:56:35.263', 'anon', '2017-11-23 12:56:35.263', 'anon', 0, 34,
        'org.projectforge.framework.persistence.user.entities.GroupDO', 'Update',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (414, '2017-11-23 12:56:35.341', 'anon', '2017-11-23 12:56:35.341', 'anon', 0, 35,
        'org.projectforge.framework.persistence.user.entities.GroupDO', 'Update',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (418, '2017-11-23 12:56:35.446', 'anon', '2017-11-23 12:56:35.446', 'anon', 0, 38,
        'org.projectforge.framework.persistence.user.entities.GroupDO', 'Update',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (422, '2017-11-23 12:56:35.52', 'anon', '2017-11-23 12:56:35.52', 'anon', 0, 39,
        'org.projectforge.framework.persistence.user.entities.GroupDO', 'Update',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (426, '2017-11-23 12:56:35.602', 'anon', '2017-11-23 12:56:35.602', 'anon', 0, 40,
        'org.projectforge.framework.persistence.user.entities.GroupDO', 'Update',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (430, '2017-11-23 12:56:35.707', 'anon', '2017-11-23 12:56:35.707', 'anon', 0, 41,
        'org.projectforge.framework.persistence.user.entities.GroupDO', 'Update',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (434, '2017-11-23 12:56:35.782', 'anon', '2017-11-23 12:56:35.782', 'anon', 0, 42,
        'org.projectforge.framework.persistence.user.entities.GroupDO', 'Update',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (438, '2017-11-23 12:56:35.875', 'anon', '2017-11-23 12:56:35.875', 'anon', 0, 43,
        'org.projectforge.framework.persistence.user.entities.GroupDO', 'Update',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (442, '2017-11-23 12:56:35.955', 'anon', '2017-11-23 12:56:35.955', 'anon', 0, 44,
        'org.projectforge.framework.persistence.user.entities.GroupDO', 'Update',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (446, '2017-11-23 12:56:36.032', 'anon', '2017-11-23 12:56:36.032', 'anon', 0, 45,
        'org.projectforge.framework.persistence.user.entities.GroupDO', 'Update',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (450, '2017-11-23 12:56:36.109', 'anon', '2017-11-23 12:56:36.109', 'anon', 0, 157,
        'org.projectforge.business.teamcal.admin.model.TeamCalDO', 'Update', NULL,
        NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (454, '2017-11-23 12:56:36.185', 'anon', '2017-11-23 12:56:36.185', 'anon', 0, 158,
        'org.projectforge.business.teamcal.admin.model.TeamCalDO', 'Update', NULL,
        NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (461, '2017-11-23 12:56:36.252', 'anon', '2017-11-23 12:56:36.252', 'anon', 0, 159,
        'org.projectforge.business.teamcal.admin.model.TeamCalDO', 'Update', NULL,
        NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (465, '2017-11-23 12:56:36.309', 'anon', '2017-11-23 12:56:36.309', 'anon', 0, 160,
        'org.projectforge.business.teamcal.admin.model.TeamCalDO', 'Update', NULL,
        NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (472, '2017-11-23 12:56:36.355', 'anon', '2017-11-23 12:56:36.355', 'anon', 0, 6,
        'org.projectforge.framework.configuration.entities.ConfigurationDO',
        'Update', NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (476, '2017-11-23 12:56:36.4', 'anon', '2017-11-23 12:56:36.4', 'anon', 0, 128,
        'org.projectforge.business.address.AddressDO', 'Update', NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (480, '2017-11-23 12:56:36.441', 'anon', '2017-11-23 12:56:36.441', 'anon', 0, 129,
        'org.projectforge.business.address.AddressDO', 'Update', NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (486, '2017-11-23 12:56:36.639', '19', '2017-11-23 12:56:36.639', '19', 0, 126,
        'org.projectforge.business.fibu.EmployeeDO', 'Update', NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (493, '2017-11-23 12:56:36.716', '19', '2017-11-23 12:56:36.716', '19', 0, 127,
        'org.projectforge.business.fibu.EmployeeDO', 'Update', NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (499, '2017-11-23 12:56:36.772', '19', '2017-11-23 12:56:36.772', '19', 0, 498,
        'org.projectforge.framework.configuration.entities.ConfigurationDO', 'Insert',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (515, '2017-11-23 12:56:36.821', '19', '2017-11-23 12:56:36.821', '19', 0, 514,
        'org.projectforge.framework.configuration.entities.ConfigurationDO', 'Insert',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (529, '2017-11-23 12:56:36.863', '19', '2017-11-23 12:56:36.863', '19', 0, 528,
        'org.projectforge.framework.configuration.entities.ConfigurationDO', 'Insert',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (543, '2017-11-23 12:56:36.905', '19', '2017-11-23 12:56:36.905', '19', 0, 542,
        'org.projectforge.framework.configuration.entities.ConfigurationDO', 'Insert',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (559, '2017-11-23 12:56:36.944', '19', '2017-11-23 12:56:36.944', '19', 0, 558,
        'org.projectforge.framework.configuration.entities.ConfigurationDO', 'Insert',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (575, '2017-11-23 12:56:36.995', '19', '2017-11-23 12:56:36.995', '19', 0, 574,
        'org.projectforge.framework.configuration.entities.ConfigurationDO', 'Insert',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (591, '2017-11-23 12:56:37.045', '19', '2017-11-23 12:56:37.045', '19', 0, 590,
        'org.projectforge.framework.configuration.entities.ConfigurationDO', 'Insert',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (606, '2017-11-23 12:56:37.071', '19', '2017-11-23 12:56:37.071', '19', 0, 5,
        'org.projectforge.framework.configuration.entities.ConfigurationDO', 'Update',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (610, '2017-11-23 12:56:37.122', '19', '2017-11-23 12:56:37.122', '19', 0, 9,
        'org.projectforge.framework.configuration.entities.ConfigurationDO', 'Update',
        NULL, NULL);
INSERT INTO t_pf_history (pk, createdat, createdby, modifiedat, modifiedby, updatecounter, entity_id, entity_name,
                          entity_optype, transaction_id, user_comment)
VALUES (614, '2017-11-23 12:56:37.175', '19', '2017-11-23 12:56:37.175', '19', 0, 14,
        'org.projectforge.framework.configuration.entities.ConfigurationDO', 'Update',
        NULL, NULL);

INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 383, '2017-11-23 12:56:34.621', 'anon', '2017-11-23 12:56:34.621', 'anon', 0, '16', 'assignedUsers:nv',
        'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 382);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 384, '2017-11-23 12:56:34.621', 'anon', '2017-11-23 12:56:34.621', 'anon', 0, 'Update', 'assignedUsers:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 382);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 385, '2017-11-23 12:56:34.621', 'anon', '2017-11-23 12:56:34.621', 'anon', 0, '', 'assignedUsers:ov', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 382);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 387, '2017-11-23 12:56:34.741', 'anon', '2017-11-23 12:56:34.741', 'anon', 0, '17', 'assignedUsers:nv',
        'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 386);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 388, '2017-11-23 12:56:34.741', 'anon', '2017-11-23 12:56:34.741', 'anon', 0, 'Update', 'assignedUsers:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 386);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 389, '2017-11-23 12:56:34.741', 'anon', '2017-11-23 12:56:34.741', 'anon', 0, '', 'assignedUsers:ov', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 386);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 391, '2017-11-23 12:56:34.828', 'anon', '2017-11-23 12:56:34.828', 'anon', 0, '17', 'assignedUsers:nv',
        'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 390);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 392, '2017-11-23 12:56:34.828', 'anon', '2017-11-23 12:56:34.828', 'anon', 0, 'Update', 'assignedUsers:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 390);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 393, '2017-11-23 12:56:34.828', 'anon', '2017-11-23 12:56:34.828', 'anon', 0, '', 'assignedUsers:ov', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 390);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 395, '2017-11-23 12:56:34.932', 'anon', '2017-11-23 12:56:34.932', 'anon', 0, '17,19,18',
        'assignedUsers:nv', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 394);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 396, '2017-11-23 12:56:34.932', 'anon', '2017-11-23 12:56:34.932', 'anon', 0, 'Update', 'assignedUsers:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 394);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 397, '2017-11-23 12:56:34.932', 'anon', '2017-11-23 12:56:34.932', 'anon', 0, '', 'assignedUsers:ov', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 394);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 399, '2017-11-23 12:56:35.02', 'anon', '2017-11-23 12:56:35.02', 'anon', 0, '17,19,18', 'assignedUsers:nv',
        'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 398);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 400, '2017-11-23 12:56:35.02', 'anon', '2017-11-23 12:56:35.02', 'anon', 0, 'Update', 'assignedUsers:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 398);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 401, '2017-11-23 12:56:35.02', 'anon', '2017-11-23 12:56:35.02', 'anon', 0, '', 'assignedUsers:ov', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 398);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 403, '2017-11-23 12:56:35.102', 'anon', '2017-11-23 12:56:35.102', 'anon', 0, '19', 'assignedUsers:nv',
        'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 402);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 404, '2017-11-23 12:56:35.102', 'anon', '2017-11-23 12:56:35.102', 'anon', 0, 'Update', 'assignedUsers:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 402);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 405, '2017-11-23 12:56:35.102', 'anon', '2017-11-23 12:56:35.102', 'anon', 0, '', 'assignedUsers:ov', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 402);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 407, '2017-11-23 12:56:35.185', 'anon', '2017-11-23 12:56:35.185', 'anon', 0, '19', 'assignedUsers:nv',
        'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 406);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 408, '2017-11-23 12:56:35.185', 'anon', '2017-11-23 12:56:35.185', 'anon', 0, 'Update', 'assignedUsers:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 406);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 409, '2017-11-23 12:56:35.185', 'anon', '2017-11-23 12:56:35.185', 'anon', 0, '', 'assignedUsers:ov', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 406);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 411, '2017-11-23 12:56:35.263', 'anon', '2017-11-23 12:56:35.263', 'anon', 0, '19', 'assignedUsers:nv',
        'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 410);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 412, '2017-11-23 12:56:35.263', 'anon', '2017-11-23 12:56:35.263', 'anon', 0, 'Update', 'assignedUsers:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 410);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 413, '2017-11-23 12:56:35.263', 'anon', '2017-11-23 12:56:35.263', 'anon', 0, '', 'assignedUsers:ov', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 410);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 415, '2017-11-23 12:56:35.341', 'anon', '2017-11-23 12:56:35.341', 'anon', 0, '16,20,19',
        'assignedUsers:nv', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 414);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 416, '2017-11-23 12:56:35.341', 'anon', '2017-11-23 12:56:35.341', 'anon', 0, 'Update', 'assignedUsers:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 414);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 417, '2017-11-23 12:56:35.341', 'anon', '2017-11-23 12:56:35.341', 'anon', 0, '', 'assignedUsers:ov', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 414);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 419, '2017-11-23 12:56:35.446', 'anon', '2017-11-23 12:56:35.446', 'anon', 0, '17,18', 'assignedUsers:nv',
        'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 418);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 420, '2017-11-23 12:56:35.446', 'anon', '2017-11-23 12:56:35.446', 'anon', 0, 'Update', 'assignedUsers:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 418);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 421, '2017-11-23 12:56:35.446', 'anon', '2017-11-23 12:56:35.446', 'anon', 0, '', 'assignedUsers:ov', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 418);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 423, '2017-11-23 12:56:35.52', 'anon', '2017-11-23 12:56:35.52', 'anon', 0, '17', 'assignedUsers:nv', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 422);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 424, '2017-11-23 12:56:35.52', 'anon', '2017-11-23 12:56:35.52', 'anon', 0, 'Update', 'assignedUsers:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 422);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 425, '2017-11-23 12:56:35.52', 'anon', '2017-11-23 12:56:35.52', 'anon', 0, '', 'assignedUsers:ov', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 422);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 427, '2017-11-23 12:56:35.602', 'anon', '2017-11-23 12:56:35.602', 'anon', 0,
        '16,24,21,17,20,26,22,23,25,18', 'assignedUsers:nv', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 426);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 428, '2017-11-23 12:56:35.602', 'anon', '2017-11-23 12:56:35.602', 'anon', 0, 'Update', 'assignedUsers:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 426);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 429, '2017-11-23 12:56:35.602', 'anon', '2017-11-23 12:56:35.602', 'anon', 0, '', 'assignedUsers:ov', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 426);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 431, '2017-11-23 12:56:35.707', 'anon', '2017-11-23 12:56:35.707', 'anon', 0, '21,17,22',
        'assignedUsers:nv', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 430);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 432, '2017-11-23 12:56:35.707', 'anon', '2017-11-23 12:56:35.707', 'anon', 0, 'Update', 'assignedUsers:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 430);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 433, '2017-11-23 12:56:35.707', 'anon', '2017-11-23 12:56:35.707', 'anon', 0, '', 'assignedUsers:ov', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 430);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 435, '2017-11-23 12:56:35.782', 'anon', '2017-11-23 12:56:35.782', 'anon', 0, '21,17', 'assignedUsers:nv',
        'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 434);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 436, '2017-11-23 12:56:35.782', 'anon', '2017-11-23 12:56:35.782', 'anon', 0, 'Update', 'assignedUsers:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 434);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 437, '2017-11-23 12:56:35.782', 'anon', '2017-11-23 12:56:35.782', 'anon', 0, '', 'assignedUsers:ov', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 434);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 439, '2017-11-23 12:56:35.875', 'anon', '2017-11-23 12:56:35.875', 'anon', 0, '16,21,17,22,23',
        'assignedUsers:nv', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 438);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 440, '2017-11-23 12:56:35.875', 'anon', '2017-11-23 12:56:35.875', 'anon', 0, 'Update', 'assignedUsers:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 438);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 441, '2017-11-23 12:56:35.875', 'anon', '2017-11-23 12:56:35.875', 'anon', 0, '', 'assignedUsers:ov', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 438);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 443, '2017-11-23 12:56:35.955', 'anon', '2017-11-23 12:56:35.955', 'anon', 0, '16,24,20',
        'assignedUsers:nv', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 442);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 444, '2017-11-23 12:56:35.955', 'anon', '2017-11-23 12:56:35.955', 'anon', 0, 'Update', 'assignedUsers:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 442);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 445, '2017-11-23 12:56:35.955', 'anon', '2017-11-23 12:56:35.955', 'anon', 0, '', 'assignedUsers:ov', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 442);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 447, '2017-11-23 12:56:36.032', 'anon', '2017-11-23 12:56:36.032', 'anon', 0, '16', 'assignedUsers:nv',
        'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 446);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 448, '2017-11-23 12:56:36.032', 'anon', '2017-11-23 12:56:36.032', 'anon', 0, 'Update', 'assignedUsers:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 446);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 449, '2017-11-23 12:56:36.032', 'anon', '2017-11-23 12:56:36.032', 'anon', 0, '', 'assignedUsers:ov', 'V',
        'org.projectforge.framework.persistence.user.entities.PFUserDO', 446);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 451, '2017-11-23 12:56:36.109', 'anon', '2017-11-23 12:56:36.109', 'anon', 0, '40',
        'readonlyAccessGroupIds:nv', 'V', 'java.lang.String', 450);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 452, '2017-11-23 12:56:36.109', 'anon', '2017-11-23 12:56:36.109', 'anon', 0, 'Update',
        'readonlyAccessGroupIds:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 450);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 453, '2017-11-23 12:56:36.109', 'anon', '2017-11-23 12:56:36.109', 'anon', 0, '11',
        'readonlyAccessGroupIds:ov', 'V', 'java.lang.String', 450);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 455, '2017-11-23 12:56:36.185', 'anon', '2017-11-23 12:56:36.185', 'anon', 0, '43',
        'fullAccessGroupIds:nv', 'V', 'java.lang.String', 454);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 456, '2017-11-23 12:56:36.185', 'anon', '2017-11-23 12:56:36.185', 'anon', 0, 'Update',
        'fullAccessGroupIds:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 454);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 457, '2017-11-23 12:56:36.185', 'anon', '2017-11-23 12:56:36.185', 'anon', 0, '14',
        'fullAccessGroupIds:ov', 'V', 'java.lang.String', 454);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 458, '2017-11-23 12:56:36.185', 'anon', '2017-11-23 12:56:36.185', 'anon', 0, '40',
        'readonlyAccessGroupIds:nv', 'V', 'java.lang.String', 454);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 459, '2017-11-23 12:56:36.185', 'anon', '2017-11-23 12:56:36.185', 'anon', 0, 'Update',
        'readonlyAccessGroupIds:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 454);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 460, '2017-11-23 12:56:36.185', 'anon', '2017-11-23 12:56:36.185', 'anon', 0, '11',
        'readonlyAccessGroupIds:ov', 'V', 'java.lang.String', 454);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 462, '2017-11-23 12:56:36.252', 'anon', '2017-11-23 12:56:36.252', 'anon', 0, '40',
        'minimalAccessGroupIds:nv', 'V', 'java.lang.String', 461);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 463, '2017-11-23 12:56:36.252', 'anon', '2017-11-23 12:56:36.252', 'anon', 0, 'Update',
        'minimalAccessGroupIds:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 461);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 464, '2017-11-23 12:56:36.252', 'anon', '2017-11-23 12:56:36.252', 'anon', 0, '11',
        'minimalAccessGroupIds:ov', 'V', 'java.lang.String', 461);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 466, '2017-11-23 12:56:36.309', 'anon', '2017-11-23 12:56:36.309', 'anon', 0, '35',
        'fullAccessGroupIds:nv', 'V', 'java.lang.String', 465);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 467, '2017-11-23 12:56:36.309', 'anon', '2017-11-23 12:56:36.309', 'anon', 0, 'Update',
        'fullAccessGroupIds:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 465);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 468, '2017-11-23 12:56:36.309', 'anon', '2017-11-23 12:56:36.309', 'anon', 0, '8', 'fullAccessGroupIds:ov',
        'V', 'java.lang.String', 465);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 469, '2017-11-23 12:56:36.309', 'anon', '2017-11-23 12:56:36.309', 'anon', 0, '41,40',
        'readonlyAccessGroupIds:nv', 'V', 'java.lang.String', 465);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 470, '2017-11-23 12:56:36.309', 'anon', '2017-11-23 12:56:36.309', 'anon', 0, 'Update',
        'readonlyAccessGroupIds:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 465);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 471, '2017-11-23 12:56:36.309', 'anon', '2017-11-23 12:56:36.309', 'anon', 0, '12,11',
        'readonlyAccessGroupIds:ov', 'V', 'java.lang.String', 465);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 473, '2017-11-23 12:56:36.355', 'anon', '2017-11-23 12:56:36.355', 'anon', 0, '47', 'intValue:nv', 'V',
        'java.lang.Integer', 472);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 474, '2017-11-23 12:56:36.355', 'anon', '2017-11-23 12:56:36.355', 'anon', 0, 'Update', 'intValue:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 472);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 475, '2017-11-23 12:56:36.355', 'anon', '2017-11-23 12:56:36.355', 'anon', 0, '239', 'intValue:ov', 'V',
        'java.lang.Integer', 472);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 477, '2017-11-23 12:56:36.4', 'anon', '2017-11-23 12:56:36.4', 'anon', 0, '1', 'addressbookList:nv', 'V',
        'org.projectforge.business.address.AddressbookDO', 476);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 478, '2017-11-23 12:56:36.4', 'anon', '2017-11-23 12:56:36.4', 'anon', 0, 'Update', 'addressbookList:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 476);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 479, '2017-11-23 12:56:36.4', 'anon', '2017-11-23 12:56:36.4', 'anon', 0, '', 'addressbookList:ov', 'V',
        'org.projectforge.business.address.AddressbookDO', 476);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 481, '2017-11-23 12:56:36.441', 'anon', '2017-11-23 12:56:36.441', 'anon', 0, '1', 'addressbookList:nv',
        'V',
        'org.projectforge.business.address.AddressbookDO', 480);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 482, '2017-11-23 12:56:36.441', 'anon', '2017-11-23 12:56:36.441', 'anon', 0, 'Update',
        'addressbookList:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 480);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 483, '2017-11-23 12:56:36.441', 'anon', '2017-11-23 12:56:36.441', 'anon', 0, '', 'addressbookList:ov',
        'V',
        'org.projectforge.business.address.AddressbookDO', 480);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 487, '2017-11-23 12:56:36.639', '19', '2017-11-23 12:56:36.639', '19', 0, '2010-01-01 00:00:00:000',
        'timeableAttributes.employeestatus.2010-01-01 00:00:00:000.startTime:nv', 'V', 'java.util.Date', 486);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 488, '2017-11-23 12:56:36.639', '19', '2017-11-23 12:56:36.639', '19', 0, 'Insert',
        'timeableAttributes.employeestatus.2010-01-01 00:00:00:000.startTime:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 486);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 489, '2017-11-23 12:56:36.639', '19', '2017-11-23 12:56:36.639', '19', 0,
        'fibu.employee.status.festAngestellter',
        'timeableAttributes.employeestatus.2010-01-01 00:00:00:000.status:nv', 'V', 'java.lang.String', 486);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 490, '2017-11-23 12:56:36.639', '19', '2017-11-23 12:56:36.639', '19', 0, 'Insert',
        'timeableAttributes.employeestatus.2010-01-01 00:00:00:000.status:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 486);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 494, '2017-11-23 12:56:36.716', '19', '2017-11-23 12:56:36.716', '19', 0, '2010-01-01 00:00:00:000',
        'timeableAttributes.employeestatus.2010-01-01 00:00:00:000.startTime:nv', 'V', 'java.util.Date', 493);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 495, '2017-11-23 12:56:36.716', '19', '2017-11-23 12:56:36.716', '19', 0, 'Insert',
        'timeableAttributes.employeestatus.2010-01-01 00:00:00:000.startTime:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 493);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 496, '2017-11-23 12:56:36.716', '19', '2017-11-23 12:56:36.716', '19', 0,
        'fibu.employee.status.festAngestellter',
        'timeableAttributes.employeestatus.2010-01-01 00:00:00:000.status:nv', 'V', 'java.lang.String', 493);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 497, '2017-11-23 12:56:36.716', '19', '2017-11-23 12:56:36.716', '19', 0, 'Insert',
        'timeableAttributes.employeestatus.2010-01-01 00:00:00:000.status:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 493);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 500, '2017-11-23 12:56:36.772', '19', '2017-11-23 12:56:36.772', '19', 0, 'BOOLEAN',
        'configurationType:nv', 'V',
        'org.projectforge.framework.configuration.ConfigurationType', 499);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 501, '2017-11-23 12:56:36.772', '19', '2017-11-23 12:56:36.772', '19', 0, 'Insert', 'configurationType:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 499);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 502, '2017-11-23 12:56:36.772', '19', '2017-11-23 12:56:36.772', '19', 0, 'false', 'deleted:nv', 'V',
        'boolean', 499);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 503, '2017-11-23 12:56:36.772', '19', '2017-11-23 12:56:36.772', '19', 0, 'Insert', 'deleted:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 499);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 504, '2017-11-23 12:56:36.772', '19', '2017-11-23 12:56:36.772', '19', 0, 'true', 'global:nv', 'V',
        'java.lang.Boolean', 499);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 505, '2017-11-23 12:56:36.772', '19', '2017-11-23 12:56:36.772', '19', 0, 'Insert', 'global:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 499);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 506, '2017-11-23 12:56:36.772', '19', '2017-11-23 12:56:36.772', '19', 0, '498', 'id:nv', 'V',
        'java.lang.Integer', 499);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 507, '2017-11-23 12:56:36.772', '19', '2017-11-23 12:56:36.772', '19', 0, 'Insert', 'id:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 499);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 509, '2017-11-23 12:56:36.772', '19', '2017-11-23 12:56:36.772', '19', 0, 'Insert', 'parameter:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 499);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 510, '2017-11-23 12:56:36.772', '19', '2017-11-23 12:56:36.772', '19', 0, 'false', 'stringValue:nv', 'V',
        'java.lang.String', 499);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 511, '2017-11-23 12:56:36.772', '19', '2017-11-23 12:56:36.772', '19', 0, 'Insert', 'stringValue:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 499);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 516, '2017-11-23 12:56:36.821', '19', '2017-11-23 12:56:36.821', '19', 0, 'STRING', 'configurationType:nv',
        'V',
        'org.projectforge.framework.configuration.ConfigurationType', 515);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 517, '2017-11-23 12:56:36.821', '19', '2017-11-23 12:56:36.821', '19', 0, 'Insert', 'configurationType:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 515);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 518, '2017-11-23 12:56:36.821', '19', '2017-11-23 12:56:36.821', '19', 0, 'false', 'deleted:nv', 'V',
        'boolean', 515);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 519, '2017-11-23 12:56:36.821', '19', '2017-11-23 12:56:36.821', '19', 0, 'Insert', 'deleted:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 515);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 520, '2017-11-23 12:56:36.821', '19', '2017-11-23 12:56:36.821', '19', 0, 'true', 'global:nv', 'V',
        'java.lang.Boolean', 515);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 521, '2017-11-23 12:56:36.821', '19', '2017-11-23 12:56:36.821', '19', 0, 'Insert', 'global:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 515);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 522, '2017-11-23 12:56:36.821', '19', '2017-11-23 12:56:36.821', '19', 0, '514', 'id:nv', 'V',
        'java.lang.Integer', 515);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 523, '2017-11-23 12:56:36.821', '19', '2017-11-23 12:56:36.821', '19', 0, 'Insert', 'id:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 515);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 524, '2017-11-23 12:56:36.821', '19', '2017-11-23 12:56:36.821', '19', 0, 'pluginsActivated',
        'parameter:nv', 'V', 'java.lang.String', 515);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 525, '2017-11-23 12:56:36.821', '19', '2017-11-23 12:56:36.821', '19', 0, 'Insert', 'parameter:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 515);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 526, '2017-11-23 12:56:36.821', '19', '2017-11-23 12:56:36.821', '19', 0, '1', 'tenant:nv', 'V',
        'org.projectforge.framework.persistence.user.entities.TenantDO_$$_jvstf15_1e', 515);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 527, '2017-11-23 12:56:36.821', '19', '2017-11-23 12:56:36.821', '19', 0, 'Insert', 'tenant:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 515);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 530, '2017-11-23 12:56:36.863', '19', '2017-11-23 12:56:36.863', '19', 0, 'CALENDAR',
        'configurationType:nv', 'V',
        'org.projectforge.framework.configuration.ConfigurationType', 529);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 531, '2017-11-23 12:56:36.863', '19', '2017-11-23 12:56:36.863', '19', 0, 'Insert', 'configurationType:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 529);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 532, '2017-11-23 12:56:36.863', '19', '2017-11-23 12:56:36.863', '19', 0, 'false', 'deleted:nv', 'V',
        'boolean', 529);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 533, '2017-11-23 12:56:36.863', '19', '2017-11-23 12:56:36.863', '19', 0, 'Insert', 'deleted:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 529);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 534, '2017-11-23 12:56:36.863', '19', '2017-11-23 12:56:36.863', '19', 0, 'true', 'global:nv', 'V',
        'java.lang.Boolean', 529);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 535, '2017-11-23 12:56:36.863', '19', '2017-11-23 12:56:36.863', '19', 0, 'Insert', 'global:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 529);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 536, '2017-11-23 12:56:36.863', '19', '2017-11-23 12:56:36.863', '19', 0, '528', 'id:nv', 'V',
        'java.lang.Integer', 529);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 537, '2017-11-23 12:56:36.863', '19', '2017-11-23 12:56:36.863', '19', 0, 'Insert', 'id:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 529);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 538, '2017-11-23 12:56:36.863', '19', '2017-11-23 12:56:36.863', '19', 0, 'vacation.cal.id',
        'parameter:nv', 'V', 'java.lang.String', 529);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 539, '2017-11-23 12:56:36.863', '19', '2017-11-23 12:56:36.863', '19', 0, 'Insert', 'parameter:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 529);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 540, '2017-11-23 12:56:36.863', '19', '2017-11-23 12:56:36.863', '19', 0, '1', 'tenant:nv', 'V',
        'org.projectforge.framework.persistence.user.entities.TenantDO_$$_jvstf15_1e', 529);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 541, '2017-11-23 12:56:36.863', '19', '2017-11-23 12:56:36.863', '19', 0, 'Insert', 'tenant:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 529);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 544, '2017-11-23 12:56:36.905', '19', '2017-11-23 12:56:36.905', '19', 0, 'STRING', 'configurationType:nv',
        'V',
        'org.projectforge.framework.configuration.ConfigurationType', 543);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 545, '2017-11-23 12:56:36.905', '19', '2017-11-23 12:56:36.905', '19', 0, 'Insert', 'configurationType:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 543);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 546, '2017-11-23 12:56:36.905', '19', '2017-11-23 12:56:36.905', '19', 0, 'false', 'deleted:nv', 'V',
        'boolean', 543);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 547, '2017-11-23 12:56:36.905', '19', '2017-11-23 12:56:36.905', '19', 0, 'Insert', 'deleted:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 543);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 548, '2017-11-23 12:56:36.905', '19', '2017-11-23 12:56:36.905', '19', 0, 'true', 'global:nv', 'V',
        'java.lang.Boolean', 543);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 549, '2017-11-23 12:56:36.905', '19', '2017-11-23 12:56:36.905', '19', 0, 'Insert', 'global:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 543);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 550, '2017-11-23 12:56:36.905', '19', '2017-11-23 12:56:36.905', '19', 0, '542', 'id:nv', 'V',
        'java.lang.Integer', 543);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 551, '2017-11-23 12:56:36.905', '19', '2017-11-23 12:56:36.905', '19', 0, 'Insert', 'id:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 543);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 552, '2017-11-23 12:56:36.905', '19', '2017-11-23 12:56:36.905', '19', 0, 'vacation.lastyear.enddate',
        'parameter:nv', 'V', 'java.lang.String', 543);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 553, '2017-11-23 12:56:36.905', '19', '2017-11-23 12:56:36.905', '19', 0, 'Insert', 'parameter:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 543);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 554, '2017-11-23 12:56:36.905', '19', '2017-11-23 12:56:36.905', '19', 0, '31.03.', 'stringValue:nv', 'V',
        'java.lang.String', 543);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 555, '2017-11-23 12:56:36.905', '19', '2017-11-23 12:56:36.905', '19', 0, 'Insert', 'stringValue:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 543);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 556, '2017-11-23 12:56:36.905', '19', '2017-11-23 12:56:36.905', '19', 0, '1', 'tenant:nv', 'V',
        'org.projectforge.framework.persistence.user.entities.TenantDO_$$_jvstf15_1e', 543);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 557, '2017-11-23 12:56:36.905', '19', '2017-11-23 12:56:36.905', '19', 0, 'Insert', 'tenant:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 543);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 560, '2017-11-23 12:56:36.944', '19', '2017-11-23 12:56:36.944', '19', 0, 'INTEGER',
        'configurationType:nv', 'V',
        'org.projectforge.framework.configuration.ConfigurationType', 559);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 561, '2017-11-23 12:56:36.944', '19', '2017-11-23 12:56:36.944', '19', 0, 'Insert', 'configurationType:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 559);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 562, '2017-11-23 12:56:36.944', '19', '2017-11-23 12:56:36.944', '19', 0, 'false', 'deleted:nv', 'V',
        'boolean', 559);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 563, '2017-11-23 12:56:36.944', '19', '2017-11-23 12:56:36.944', '19', 0, 'Insert', 'deleted:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 559);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 564, '2017-11-23 12:56:36.944', '19', '2017-11-23 12:56:36.944', '19', 0, 'true', 'global:nv', 'V',
        'java.lang.Boolean', 559);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 565, '2017-11-23 12:56:36.944', '19', '2017-11-23 12:56:36.944', '19', 0, 'Insert', 'global:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 559);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 566, '2017-11-23 12:56:36.944', '19', '2017-11-23 12:56:36.944', '19', 0, '558', 'id:nv', 'V',
        'java.lang.Integer', 559);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 567, '2017-11-23 12:56:36.944', '19', '2017-11-23 12:56:36.944', '19', 0, 'Insert', 'id:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 559);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 568, '2017-11-23 12:56:36.944', '19', '2017-11-23 12:56:36.944', '19', 0, '10', 'intValue:nv', 'V',
        'java.lang.Integer', 559);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 569, '2017-11-23 12:56:36.944', '19', '2017-11-23 12:56:36.944', '19', 0, 'Insert', 'intValue:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 559);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 570, '2017-11-23 12:56:36.944', '19', '2017-11-23 12:56:36.944', '19', 0, 'minPasswordLength',
        'parameter:nv', 'V', 'java.lang.String', 559);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 571, '2017-11-23 12:56:36.944', '19', '2017-11-23 12:56:36.944', '19', 0, 'Insert', 'parameter:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 559);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 572, '2017-11-23 12:56:36.944', '19', '2017-11-23 12:56:36.944', '19', 0, '1', 'tenant:nv', 'V',
        'org.projectforge.framework.persistence.user.entities.TenantDO_$$_jvstf15_1e', 559);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 573, '2017-11-23 12:56:36.944', '19', '2017-11-23 12:56:36.944', '19', 0, 'Insert', 'tenant:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 559);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 576, '2017-11-23 12:56:36.995', '19', '2017-11-23 12:56:36.995', '19', 0, 'BOOLEAN',
        'configurationType:nv', 'V',
        'org.projectforge.framework.configuration.ConfigurationType', 575);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 577, '2017-11-23 12:56:36.995', '19', '2017-11-23 12:56:36.995', '19', 0, 'Insert', 'configurationType:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 575);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 578, '2017-11-23 12:56:36.995', '19', '2017-11-23 12:56:36.995', '19', 0, 'false', 'deleted:nv', 'V',
        'boolean', 575);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 579, '2017-11-23 12:56:36.995', '19', '2017-11-23 12:56:36.995', '19', 0, 'Insert', 'deleted:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 575);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 580, '2017-11-23 12:56:36.995', '19', '2017-11-23 12:56:36.995', '19', 0, 'true', 'global:nv', 'V',
        'java.lang.Boolean', 575);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 581, '2017-11-23 12:56:36.995', '19', '2017-11-23 12:56:36.995', '19', 0, 'Insert', 'global:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 575);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 582, '2017-11-23 12:56:36.995', '19', '2017-11-23 12:56:36.995', '19', 0, '574', 'id:nv', 'V',
        'java.lang.Integer', 575);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 583, '2017-11-23 12:56:36.995', '19', '2017-11-23 12:56:36.995', '19', 0, 'Insert', 'id:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 575);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 584, '2017-11-23 12:56:36.995', '19', '2017-11-23 12:56:36.995', '19', 0, 'password.flag.checkChange',
        'parameter:nv', 'V', 'java.lang.String', 575);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 585, '2017-11-23 12:56:36.995', '19', '2017-11-23 12:56:36.995', '19', 0, 'Insert', 'parameter:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 575);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 586, '2017-11-23 12:56:36.995', '19', '2017-11-23 12:56:36.995', '19', 0, 'true', 'stringValue:nv', 'V',
        'java.lang.String', 575);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 587, '2017-11-23 12:56:36.995', '19', '2017-11-23 12:56:36.995', '19', 0, 'Insert', 'stringValue:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 575);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 588, '2017-11-23 12:56:36.995', '19', '2017-11-23 12:56:36.995', '19', 0, '1', 'tenant:nv', 'V',
        'org.projectforge.framework.persistence.user.entities.TenantDO_$$_jvstf15_1e', 575);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 589, '2017-11-23 12:56:36.995', '19', '2017-11-23 12:56:36.995', '19', 0, 'Insert', 'tenant:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 575);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 592, '2017-11-23 12:56:37.045', '19', '2017-11-23 12:56:37.045', '19', 0, 'BOOLEAN',
        'configurationType:nv', 'V',
        'org.projectforge.framework.configuration.ConfigurationType', 591);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 593, '2017-11-23 12:56:37.045', '19', '2017-11-23 12:56:37.045', '19', 0, 'Insert', 'configurationType:op',
        'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 591);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 594, '2017-11-23 12:56:37.045', '19', '2017-11-23 12:56:37.045', '19', 0, 'false', 'deleted:nv', 'V',
        'boolean', 591);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 595, '2017-11-23 12:56:37.045', '19', '2017-11-23 12:56:37.045', '19', 0, 'Insert', 'deleted:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 591);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 596, '2017-11-23 12:56:37.045', '19', '2017-11-23 12:56:37.045', '19', 0, 'true', 'global:nv', 'V',
        'java.lang.Boolean', 591);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 597, '2017-11-23 12:56:37.045', '19', '2017-11-23 12:56:37.045', '19', 0, 'Insert', 'global:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 591);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 598, '2017-11-23 12:56:37.045', '19', '2017-11-23 12:56:37.045', '19', 0, '590', 'id:nv', 'V',
        'java.lang.Integer', 591);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 599, '2017-11-23 12:56:37.045', '19', '2017-11-23 12:56:37.045', '19', 0, 'Insert', 'id:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 591);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 600, '2017-11-23 12:56:37.045', '19', '2017-11-23 12:56:37.045', '19', 0, 'snoweffect.enabled',
        'parameter:nv', 'V', 'java.lang.String', 591);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 601, '2017-11-23 12:56:37.045', '19', '2017-11-23 12:56:37.045', '19', 0, 'Insert', 'parameter:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 591);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 602, '2017-11-23 12:56:37.045', '19', '2017-11-23 12:56:37.045', '19', 0, 'false', 'stringValue:nv', 'V',
        'java.lang.String', 591);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 603, '2017-11-23 12:56:37.045', '19', '2017-11-23 12:56:37.045', '19', 0, 'Insert', 'stringValue:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 591);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 604, '2017-11-23 12:56:37.045', '19', '2017-11-23 12:56:37.045', '19', 0, '1', 'tenant:nv', 'V',
        'org.projectforge.framework.persistence.user.entities.TenantDO_$$_jvstf15_1e', 591);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 605, '2017-11-23 12:56:37.045', '19', '2017-11-23 12:56:37.045', '19', 0, 'Insert', 'tenant:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 591);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 607, '2017-11-23 12:56:37.071', '19', '2017-11-23 12:56:37.071', '19', 0, 'true', 'deleted:nv', 'V',
        'boolean', 606);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 608, '2017-11-23 12:56:37.071', '19', '2017-11-23 12:56:37.071', '19', 0, 'Update', 'deleted:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 606);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 609, '2017-11-23 12:56:37.071', '19', '2017-11-23 12:56:37.071', '19', 0, 'false', 'deleted:ov', 'V',
        'boolean', 606);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 611, '2017-11-23 12:56:37.122', '19', '2017-11-23 12:56:37.122', '19', 0, 'UTC', 'stringValue:nv', 'V',
        'java.lang.String', 610);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 612, '2017-11-23 12:56:37.122', '19', '2017-11-23 12:56:37.122', '19', 0, 'Update', 'stringValue:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 610);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 613, '2017-11-23 12:56:37.122', '19', '2017-11-23 12:56:37.122', '19', 0, 'Europe/Berlin',
        'stringValue:ov', 'V', 'java.lang.String', 610);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 615, '2017-11-23 12:56:37.175', '19', '2017-11-23 12:56:37.175', '19', 0, 'test', 'stringValue:nv', 'V',
        'java.lang.String', 614);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 616, '2017-11-23 12:56:37.175', '19', '2017-11-23 12:56:37.175', '19', 0, 'Update', 'stringValue:op', 'V',
        'de.micromata.genome.db.jpa.history.entities.PropertyOpType', 614);
INSERT INTO t_pf_history_attr (withdata, pk, createdat, createdby, modifiedat, modifiedby, updatecounter, value,
                               propertyname, type, property_type_class, master_fk)
VALUES ('0', 617, '2017-11-23 12:56:37.175', '19', '2017-11-23 12:56:37.175', '19', 0, 'pf', 'stringValue:ov', 'V',
        'java.lang.String', 614);

INSERT INTO t_timesheet (pk, created, deleted, last_update, description, location, start_time, stop_time, time_zone,
                         kost2_id, task_id, user_id)
VALUES (83, '2010-04-21 22:23:31.601', FALSE, '2010-04-22 06:56:24.638', NULL, 'Micromata', '2010-03-29 06:00:00',
        '2010-03-29 11:15:00', NULL, 70, 52, 17);
INSERT INTO t_timesheet (pk, created, deleted, last_update, description, location, start_time, stop_time, time_zone,
                         kost2_id, task_id, user_id)
VALUES (84, '2010-04-22 06:57:31.102', FALSE, '2010-04-22 06:57:31.102', 'Vorwort / Foreword', 'Micromata',
        '2010-03-29 11:45:00', '2010-03-29 20:15:00', NULL,
        70, 52, 17);
INSERT INTO t_timesheet (pk, created, deleted, last_update, description, location, start_time, stop_time, time_zone,
                         kost2_id, task_id, user_id)
VALUES (85, '2010-04-22 06:57:59.514', FALSE, '2010-04-22 06:57:59.514', NULL, 'Micromata', '2010-03-30 06:00:00',
        '2010-03-30 10:00:00', NULL, 70, 52, 17);
INSERT INTO t_timesheet (pk, created, deleted, last_update, description, location, start_time, stop_time, time_zone,
                         kost2_id, task_id, user_id)
VALUES (86, '2010-04-22 06:58:08.418', FALSE, '2010-04-22 06:58:08.418', NULL, 'Micromata', '2010-03-30 11:00:00',
        '2010-03-30 18:00:00', NULL, 70, 52, 17);
INSERT INTO t_timesheet (pk, created, deleted, last_update, description, location, start_time, stop_time, time_zone,
                         kost2_id, task_id, user_id)
VALUES (87, '2010-04-22 06:58:16.254', FALSE, '2010-04-22 07:55:06.403', 'Working on JiRA-Issues: ACME_WEB-117',
        'Micromata', '2010-03-31 06:00:00',
        '2010-03-31 10:00:00', NULL, 70, 52, 17);
INSERT INTO t_timesheet (pk, created, deleted, last_update, description, location, start_time, stop_time, time_zone,
                         kost2_id, task_id, user_id)
VALUES (88, '2010-04-22 06:58:24.663', FALSE, '2010-04-22 07:54:38.758',
        'Working on JiRA-Issues: ACME_WEB-137, ACME_WEB-139', 'Micromata', '2010-03-31 11:00:00',
        '2010-03-31 18:00:00', NULL, 70, 52, 17);
INSERT INTO t_timesheet (pk, created, deleted, last_update, description, location, start_time, stop_time, time_zone,
                         kost2_id, task_id, user_id)
VALUES (89, '2010-04-22 07:51:39.207', FALSE, '2010-04-22 07:51:39.207', 'Presentation', 'ACME Hamburg',
        '2010-03-23 07:00:00', '2010-03-23 15:00:00', NULL, 71,
        51, 17);
INSERT INTO t_timesheet (pk, created, deleted, last_update, description, location, start_time, stop_time, time_zone,
                         kost2_id, task_id, user_id)
VALUES (90, '2010-04-22 07:52:02.187', FALSE, '2010-04-22 07:52:02.187', 'Preparation of presentation', 'Micromata',
        '2010-03-22 07:00:00', '2010-03-22 15:00:00',
        NULL, 71, 51, 17);
INSERT INTO t_timesheet (pk, created, deleted, last_update, description, location, start_time, stop_time, time_zone,
                         kost2_id, task_id, user_id)
VALUES (91, '2010-04-22 07:52:49.03', FALSE, '2010-04-22 07:52:49.03', 'Discussions with team, project plan...',
        'Micromata', '2010-03-19 07:00:00',
        '2010-03-19 15:00:00', NULL, 71, 51, 17);
INSERT INTO t_timesheet (pk, created, deleted, last_update, description, location, start_time, stop_time, time_zone,
                         kost2_id, task_id, user_id)
VALUES (92, '2010-04-22 07:53:18.897', FALSE, '2010-04-22 07:53:18.897', 'Reading the specifications of the customer',
        'zu Hause / at home', '2010-03-18 07:00:00',
        '2010-03-18 15:00:00', NULL, 71, 51, 17);
INSERT INTO t_timesheet (pk, created, deleted, last_update, description, location, start_time, stop_time, time_zone,
                         kost2_id, task_id, user_id)
VALUES (93, '2010-04-22 08:00:05.496', FALSE, '2010-04-22 08:00:05.496', NULL, 'Hamburg -> Kassel',
        '2010-03-23 15:00:00', '2010-03-23 17:00:00', NULL, 72, 51,
        17);
INSERT INTO t_timesheet (pk, created, deleted, last_update, description, location, start_time, stop_time, time_zone,
                         kost2_id, task_id, user_id)
VALUES (94, '2010-04-22 08:01:00.709', FALSE, '2010-04-22 08:01:00.709', 'Zug / Train', 'Kassel -> Hamburg',
        '2010-03-23 05:00:00', '2010-03-23 07:00:00', NULL,
        72, 51, 17);
INSERT INTO t_timesheet (pk, created, deleted, last_update, description, location, start_time, stop_time, time_zone,
                         kost2_id, task_id, user_id)
VALUES (95, '2010-04-22 08:51:30.868', FALSE, '2010-04-22 08:51:30.868', NULL, 'Micromata', '2010-04-01 06:00:00',
        '2010-04-01 14:00:00', NULL, 70, 52, 17);
INSERT INTO t_timesheet (pk, created, deleted, last_update, description, location, start_time, stop_time, time_zone,
                         kost2_id, task_id, user_id)
VALUES (96, '2010-04-22 21:40:43.083', FALSE, '2010-04-22 21:40:43.083', 'Meeting with Dave', 'Hamburg',
        '2010-03-30 08:00:00', '2010-03-30 10:00:00', NULL, 71,
        51, 18);
INSERT INTO t_timesheet (pk, created, deleted, last_update, description, location, start_time, stop_time, time_zone,
                         kost2_id, task_id, user_id)
VALUES (97, '2010-04-22 21:41:09.572', FALSE, '2010-04-22 21:41:43.326', 'Train', 'Kassel -> Hamburg',
        '2010-03-30 06:00:00', '2010-03-30 08:00:00', NULL, 72, 51,
        18);
INSERT INTO t_timesheet (pk, created, deleted, last_update, description, location, start_time, stop_time, time_zone,
                         kost2_id, task_id, user_id)
VALUES (98, '2010-04-22 21:41:36.638', FALSE, '2010-04-22 21:41:36.638', 'Train', 'Hamburg -> Kassel',
        '2010-03-30 10:00:00', '2010-03-30 12:00:00', NULL, 72, 51,
        18);
INSERT INTO t_timesheet (pk, created, deleted, last_update, description, location, start_time, stop_time, time_zone,
                         kost2_id, task_id, user_id)
VALUES (99, '2010-04-22 21:43:38.229', FALSE, '2010-04-22 21:43:38.229', 'JiRA-Issues: ACME_WEB-146, ACME_WEB-147',
        'Micromata', '2010-04-13 06:00:00',
        '2010-04-13 10:15:00', NULL, 70, 52, 18);
INSERT INTO t_timesheet (pk, created, deleted, last_update, description, location, start_time, stop_time, time_zone,
                         kost2_id, task_id, user_id)
VALUES (100, '2010-04-22 21:45:07.205', FALSE, '2010-04-22 21:45:07.205', 'Großartige Java-Klassen geschrieben',
        'At home', '2010-04-13 12:00:00',
        '2010-04-13 19:00:00', NULL, 73, 62, 18);
INSERT INTO t_timesheet (pk, created, deleted, last_update, description, location, start_time, stop_time, time_zone,
                         kost2_id, task_id, user_id)
VALUES (101, '2010-05-03 23:20:05.018', FALSE, '2010-05-03 23:20:05.018',
        'Requirement-Engineering: ACME_WEB-186, ACME_WEB-1876', 'Head quarter',
        '2010-05-03 06:00:00', '2010-05-03 12:00:00', NULL, 70, 52, 18);
INSERT INTO t_timesheet (pk, created, deleted, last_update, description, location, start_time, stop_time, time_zone,
                         kost2_id, task_id, user_id)
VALUES (102, '2010-05-03 23:20:48.097', FALSE, '2010-05-03 23:20:48.097', NULL, NULL, '2010-05-04 06:00:00',
        '2010-05-04 14:00:00', NULL, 74, 65, 18);
INSERT INTO t_timesheet (pk, created, deleted, last_update, description, location, start_time, stop_time, time_zone,
                         kost2_id, task_id, user_id)
VALUES (103, '2010-05-20 01:42:23.631', FALSE, '2010-05-20 01:42:23.631',
        'Development of famous Hibernate Classes: PF-123, PF-675', 'Head quarter',
        '2010-05-03 13:00:00', '2010-05-03 16:00:00', NULL, 73, 62, 18);

INSERT INTO t_user_pref (pk, created, deleted, last_update, area, name, user_fk)
VALUES (131, '2010-04-21 22:23:28.951', FALSE, '2010-04-21 22:23:28.951', 'TASK_FAVORITE', 'ACME Webportal R 1.0 Spec',
        17);
INSERT INTO t_user_pref (pk, created, deleted, last_update, area, name, user_fk)
VALUES (133, '2010-04-21 22:23:35.769', FALSE, '2010-04-21 22:23:35.769', 'TIMESHEET_TEMPLATE',
        'ACME Webportal R 1.0 Spec', 17);
INSERT INTO t_user_pref (pk, created, deleted, last_update, area, name, user_fk)
VALUES (139, '2010-04-22 07:56:08.902', FALSE, '2010-04-22 07:56:08.902', 'USER_FAVORITE', 'Demo user', 17);
INSERT INTO t_user_pref (pk, created, deleted, last_update, area, name, user_fk)
VALUES (141, '2010-04-22 07:56:30.261', FALSE, '2010-04-22 07:56:30.261', 'TASK_FAVORITE', 'ACME Webportal', 17);
INSERT INTO t_user_pref (pk, created, deleted, last_update, area, name, user_fk)
VALUES (143, '2010-04-22 07:56:43.791', FALSE, '2010-04-22 07:56:43.791', 'USER_FAVORITE', 'Administrator', 17);
INSERT INTO t_user_pref (pk, created, deleted, last_update, area, name, user_fk)
VALUES (145, '2010-04-22 21:42:05.141', FALSE, '2013-04-27 15:17:10.97', 'TASK_FAVORITE',
        'Yellow Logistics Webportal Akquise', 18);
INSERT INTO t_user_pref (pk, created, deleted, last_update, area, name, user_fk)
VALUES (147, '2010-04-22 21:42:20.454', FALSE, '2013-04-27 15:17:43.705', 'TASK_FAVORITE', 'Yellow Webportal Spec',
        18);
INSERT INTO t_user_pref (pk, created, deleted, last_update, area, name, user_fk)
VALUES (149, '2010-04-22 21:43:57.937', FALSE, '2013-04-27 15:19:21.243', 'TIMESHEET_TEMPLATE',
        'Yellow Webportal Realization', 18);
INSERT INTO t_user_pref (pk, created, deleted, last_update, area, name, user_fk)
VALUES (155, '2010-04-22 21:44:33.477', FALSE, '2013-04-27 15:17:29.215', 'TASK_FAVORITE', 'ProjectForge Dev Java',
        18);

INSERT INTO t_user_pref_entry (pk, parameter, s_value, user_pref_fk)
VALUES (132, 'task', '57', 131);
INSERT INTO t_user_pref_entry (pk, parameter, s_value, user_pref_fk)
VALUES (134, 'task', '57', 133);
INSERT INTO t_user_pref_entry (pk, parameter, s_value, user_pref_fk)
VALUES (135, 'description', NULL, 133);
INSERT INTO t_user_pref_entry (pk, parameter, s_value, user_pref_fk)
VALUES (136, 'kost2', '72', 133);
INSERT INTO t_user_pref_entry (pk, parameter, s_value, user_pref_fk)
VALUES (137, 'user', '18', 133);
INSERT INTO t_user_pref_entry (pk, parameter, s_value, user_pref_fk)
VALUES (138, 'location', 'Micromata', 133);
INSERT INTO t_user_pref_entry (pk, parameter, s_value, user_pref_fk)
VALUES (140, 'user', '17', 139);
INSERT INTO t_user_pref_entry (pk, parameter, s_value, user_pref_fk)
VALUES (142, 'task', '55', 141);
INSERT INTO t_user_pref_entry (pk, parameter, s_value, user_pref_fk)
VALUES (144, 'user', '16', 143);
INSERT INTO t_user_pref_entry (pk, parameter, s_value, user_pref_fk)
VALUES (146, 'task', '51', 145);
INSERT INTO t_user_pref_entry (pk, parameter, s_value, user_pref_fk)
VALUES (148, 'task', '52', 147);
INSERT INTO t_user_pref_entry (pk, parameter, s_value, user_pref_fk)
VALUES (150, 'description', 'Wrote nice Java classes...', 149);
INSERT INTO t_user_pref_entry (pk, parameter, s_value, user_pref_fk)
VALUES (151, 'user', '17', 149);
INSERT INTO t_user_pref_entry (pk, parameter, s_value, user_pref_fk)
VALUES (152, 'kost2', 'null', 149);
INSERT INTO t_user_pref_entry (pk, parameter, s_value, user_pref_fk)
VALUES (153, 'location', 'Micromata', 149);
INSERT INTO t_user_pref_entry (pk, parameter, s_value, user_pref_fk)
VALUES (154, 'task', '53', 149);
INSERT INTO t_user_pref_entry (pk, parameter, s_value, user_pref_fk)
VALUES (156, 'task', '62', 155);

INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (350, '2010-05-20 01:39:20.297', FALSE, '2010-05-20 01:39:20.297', 'FIBU_AUSGANGSRECHNUNGEN', 'READWRITE',
        17);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (351, '2010-05-20 01:39:20.303', FALSE, '2010-05-20 01:39:20.303', 'FIBU_DATEV_IMPORT', 'TRUE', 17);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (352, '2010-05-20 01:39:20.307', FALSE, '2010-05-20 01:39:20.307', 'ORGA_OUTGOING_MAIL', 'READWRITE', 17);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (353, '2010-05-20 01:39:20.303', FALSE, '2010-05-20 01:39:20.303', 'FIBU_EINGANGSRECHNUNGEN', 'READWRITE',
        17);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (354, '2010-05-20 01:39:20.306', FALSE, '2010-05-20 01:39:20.306', 'ORGA_INCOMING_MAIL', 'READWRITE', 17);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (355, '2010-09-14 00:44:12.893', FALSE, '2010-09-14 00:44:12.893', 'PM_HR_PLANNING', 'READONLY', 18);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (356, '2010-07-13 17:38:47.509', FALSE, '2010-07-13 17:38:47.509', 'ORGA_CONTRACTS', 'READONLY', 18);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (357, '2010-05-19 23:54:43.09', FALSE, '2010-05-19 23:54:43.09', 'FIBU_DATEV_IMPORT', 'TRUE', 18);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (358, '2010-05-19 23:54:43.092', FALSE, '2010-05-19 23:54:43.092', 'MISC_MEB', 'TRUE', 18);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (359, '2010-05-19 23:54:43.091', FALSE, '2010-05-19 23:54:43.091', 'ORGA_OUTGOING_MAIL', 'READONLY', 18);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (360, '2010-09-14 00:44:12.861', FALSE, '2010-09-14 00:44:12.861', 'FIBU_COST_UNIT', 'READONLY', 18);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (361, '2010-05-19 23:54:43.09', FALSE, '2010-05-19 23:54:43.09', 'ORGA_INCOMING_MAIL', 'READONLY', 18);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (362, '2010-09-14 00:44:12.892', FALSE, '2010-09-14 00:44:12.892', 'PM_ORDER_BOOK', 'READONLY', 18);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (363, '2010-07-13 17:38:47.499', FALSE, '2010-07-13 17:38:47.499', 'HR_EMPLOYEE_SALARY', 'READONLY', 18);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (364, '2010-05-19 23:54:43.089', FALSE, '2010-05-19 23:54:43.089', 'FIBU_EINGANGSRECHNUNGEN', 'READONLY', 18);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (365, '2010-05-19 23:54:43.083', FALSE, '2010-05-19 23:54:43.083', 'FIBU_AUSGANGSRECHNUNGEN', 'READONLY', 18);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (366, '2010-09-14 00:44:12.892', FALSE, '2010-09-14 00:44:12.892', 'PM_PROJECT', 'READONLY', 18);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (367, '2011-02-07 21:16:29.943', FALSE, '2011-02-07 21:16:29.943', 'HR_EMPLOYEE_SALARY', 'READWRITE', 19);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (368, '2011-02-07 21:16:29.997', FALSE, '2011-02-07 21:16:29.997', 'FIBU_COST_UNIT', 'READWRITE', 19);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (369, '2011-02-07 21:16:29.995', FALSE, '2011-02-07 21:16:29.995', 'FIBU_EINGANGSRECHNUNGEN', 'READWRITE',
        19);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (370, '2011-02-07 21:16:30', FALSE, '2011-02-07 21:16:30', 'PM_ORDER_BOOK', 'READWRITE', 19);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (371, '2011-02-07 21:16:29.998', FALSE, '2011-02-07 21:16:29.998', 'ORGA_INCOMING_MAIL', 'READWRITE', 19);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (372, '2014-09-02 12:26:05.062', FALSE, '2014-09-02 12:26:05.062', 'FIBU_ACCOUNTS', 'READWRITE', 19);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (373, '2011-02-07 21:16:29.999', FALSE, '2011-02-07 21:16:29.999', 'ORGA_OUTGOING_MAIL', 'READWRITE', 19);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (374, '2011-02-07 21:16:29.998', FALSE, '2011-02-07 21:16:29.998', 'ORGA_CONTRACTS', 'READWRITE', 19);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (375, '2011-02-07 21:16:30', FALSE, '2011-02-07 21:16:30', 'PM_PROJECT', 'READWRITE', 19);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (376, '2011-02-07 21:16:29.961', FALSE, '2011-02-07 21:16:29.961', 'FIBU_AUSGANGSRECHNUNGEN', 'READWRITE',
        19);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (377, '2014-09-02 12:26:05.02', FALSE, '2014-09-02 12:26:05.02', 'HR_EMPLOYEE', 'READWRITE', 19);
INSERT INTO t_user_right (pk, created, deleted, last_update, right_id, value, user_fk)
VALUES (378, '2011-02-07 21:16:29.996', FALSE, '2011-02-07 21:16:29.996', 'FIBU_DATEV_IMPORT', 'TRUE', 19);

INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (247, '2010-04-21 00:13:19.651', 'org.projectforge.web.admin.ConfigurationListForm:Filter',
        '2013-04-07 18:14:25.515', '<org.projectforge.framework.persistence.api.BaseSearchFilter>
      <searchString></searchString>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      </org.projectforge.framework.persistence.api.BaseSearchFilter>
    ', 1, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (248, '2010-04-21 00:13:19.653', 'CalendarPage.userPrefs', '2013-04-07 18:14:25.506', '<org.projectforge.business.teamcal.filter.TeamCalCalendarFilter>
  <startDate>2019-06-23</startDate>
  <firstHour>8</firstHour>
  <viewType>AGENDA_WEEK</viewType>
  <templateEntries>
    <org.projectforge.business.teamcal.filter.TemplateEntry>
      <calendarProperties class="sorted-set">
        <org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
          <calId>157</calId>
          <colorCode>#008000</colorCode>
          <visible>true</visible>
          <millisOfLastChange>1562227222945</millisOfLastChange>
        </org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
        <org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
          <calId>158</calId>
          <colorCode>#ff0</colorCode>
          <visible>true</visible>
          <millisOfLastChange>1562227225273</millisOfLastChange>
        </org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
        <org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
          <calId>159</calId>
          <colorCode>#FAAF26</colorCode>
          <visible>true</visible>
          <millisOfLastChange>1562227214916</millisOfLastChange>
        </org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
        <org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
          <calId>160</calId>
          <colorCode>#f0f</colorCode>
          <visible>true</visible>
          <millisOfLastChange>1562227233067</millisOfLastChange>
        </org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
      </calendarProperties>
      <visibleCalendarIds>
        <int>160</int>
        <int>157</int>
        <int>158</int>
        <int>159</int>
      </visibleCalendarIds>
      <name>Default</name>
      <defaultCalendarId>-1</defaultCalendarId>
      <showBirthdays>true</showBirthdays>
      <showStatistics>true</showStatistics>
      <timesheetUserId>19</timesheetUserId>
      <selectedCalendar>timesheet</selectedCalendar>
      <showBreaks>true</showBreaks>
      <showPlanning>true</showPlanning>
    </org.projectforge.business.teamcal.filter.TemplateEntry>
  </templateEntries>
  <activeTemplateEntryIndex>0</activeTemplateEntryIndex>
      </org.projectforge.business.teamcal.filter.TeamCalCalendarFilter>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (249, '2010-04-20 22:24:43.644', 'org.projectforge.web.fibu.KundeListAction:Filter', '2013-04-07 18:14:25.493', '<org.projectforge.framework.persistence.api.BaseSearchFilter>
      <searchString></searchString>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      </org.projectforge.framework.persistence.api.BaseSearchFilter>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (250, '2010-04-20 22:24:43.647', 'org.projectforge.web.task.TaskListForm:Filter', '2013-04-07 18:14:25.502', '<TaskFilter notOpened="true" opened="true" closed="false" deleted="false">
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      </TaskFilter>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (251, '2010-04-20 22:24:43.648', 'openTasks', '2013-04-07 18:14:25.521', '<set>
  <int>49</int>
  <int>53</int>
  <int>54</int>
  <int>55</int>
  <int>56</int>
  <int>57</int>
  <int>58</int>
  <int>59</int>
  <int>60</int>
  <int>61</int>
</set>', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (252, '2010-04-20 22:24:43.649', 'menu.openedNodes', '2013-04-07 18:14:25.526', '<set>
      <short>3</short>
      <short>38</short>
      <short>20</short>
      <short>29</short>
      <short>28</short>
      </set>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (253, '2010-04-20 22:24:43.651', 'org.projectforge.web.fibu.ProjektListForm:Filter', '2013-04-07 18:14:25.528', '<org.projectforge.web.fibu.ProjektListFilter>
      <searchString></searchString>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      <listType>notEnded</listType>
      </org.projectforge.web.fibu.ProjektListFilter>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (254, '2010-04-20 22:30:01.107', 'org.projectforge.web.user.UserListAction:Filter', '2013-04-07 18:14:25.503', '<org.projectforge.framework.persistence.api.BaseSearchFilter>
      <searchString></searchString>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      </org.projectforge.framework.persistence.api.BaseSearchFilter>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (264, '2010-04-21 22:11:35.585', 'org.projectforge.web.address.AddressListAction:pageSize',
        '2017-11-23 12:56:37.393', '<int>50</int>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (261, '2010-04-21 22:11:35.581', 'org.projectforge.web.address.AddressListAction:Filter',
        '2017-11-23 12:56:37.464', '<null/>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (259, '2010-04-21 22:11:35.577', 'org.projectforge.web.book.BookListAction:pageSize', '2017-11-23 12:56:37.555',
        '<int>50</int>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (256, '2010-04-21 22:11:35.575', 'org.projectforge.web.fibu.KundeListAction:Filter', '2017-11-23 12:56:37.627', '<org.projectforge.framework.persistence.api.BaseSearchFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
</org.projectforge.framework.persistence.api.BaseSearchFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (258, '2010-04-21 22:11:35.577', 'org.projectforge.web.book.BookListAction:Filter', '2017-11-23 12:56:37.648', '<org.projectforge.web.book.BookListFilter>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
  <present>true</present>
  <missed>false</missed>
  <disposed>false</disposed>
</org.projectforge.web.book.BookListFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (255, '2010-04-21 22:11:35.574', 'org.projectforge.web.fibu.RechnungListForm:Filter', '2017-11-23 12:56:37.67',
        '<null/>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (257, '2010-04-21 22:11:35.576', 'org.projectforge.web.access.AccessListAction:Filter',
        '2017-11-23 12:56:37.678', '<org.projectforge.framework.persistence.api.BaseSearchFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
</org.projectforge.framework.persistence.api.BaseSearchFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (260, '2010-04-21 22:11:35.578', 'org.projectforge.web.admin.ConfigurationListForm:Filter',
        '2017-11-23 12:56:37.779', '<org.projectforge.framework.persistence.api.BaseSearchFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
</org.projectforge.framework.persistence.api.BaseSearchFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (262, '2010-04-21 22:11:35.583', 'org.projectforge.web.timesheet.TimesheetListForm:Filter',
        '2017-11-23 12:56:37.792', '<org.projectforge.web.timesheet.TimesheetListFilter>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
  <timePeriod>
    <fromDate>2009-12-31 23:00:00.0 UTC</fromDate>
    <marker>false</marker>
  </timePeriod>
  <marked>false</marked>
  <longFormat>false</longFormat>
  <recursive>true</recursive>
  <orderType>DESC</orderType>
</org.projectforge.web.timesheet.TimesheetListFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (268, '2010-04-21 22:11:35.591', 'org.projectforge.web.task.TaskTreeAction:Filter', '2017-11-23 12:56:37.347', '<TaskFilter notOpened="true" opened="true" closed="false" deleted="false">
  <deleted defined-in="org.projectforge.framework.persistence.api.BaseSearchFilter">false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
</TaskFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (279, '2010-04-21 23:11:22.37', 'org.projectforge.web.timesheet.TimesheetEditPage', '2017-11-23 12:56:37.391',
        '<null/>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (275, '2010-04-21 22:22:26.385', 'org.projectforge.web.fibu.Kost2ArtListForm:Filter', '2017-11-23 12:56:37.406', '<org.projectforge.web.fibu.Kost2ArtListFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
</org.projectforge.web.fibu.Kost2ArtListFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (269, '2010-04-21 22:11:35.593', 'org.projectforge.web.fibu.AuftragListForm:Filter', '2017-11-23 12:56:37.429',
        '<null/>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (274, '2010-04-21 22:22:26.383', 'org.projectforge.web.user.GroupListForm:Filter', '2017-11-23 12:56:37.442', '<org.projectforge.business.user.GroupFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
</org.projectforge.business.user.GroupFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (280, '2010-04-22 07:58:05.949', 'org.projectforge.web.fibu.EmployeeListAction:Filter',
        '2017-11-23 12:56:37.456', '<org.projectforge.business.fibu.EmployeeFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
  <showOnlyActiveEntries>false</showOnlyActiveEntries>
</org.projectforge.business.fibu.EmployeeFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (271, '2010-04-21 22:11:35.595', 'org.projectforge.web.fibu.ProjektListForm:Filter', '2017-11-23 12:56:37.493', '<org.projectforge.web.fibu.ProjektListFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
  <listType>notEnded</listType>
</org.projectforge.web.fibu.ProjektListFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (267, '2010-04-21 22:11:35.59', 'org.projectforge.web.address.AddressListAction:recentSearchTerms',
        '2017-11-23 12:56:37.525', '<org.projectforge.framework.utils.RecentQueue>
  <maxSize>25</maxSize>
</org.projectforge.framework.utils.RecentQueue>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (276, '2010-04-21 23:11:22.362', 'org.projectforge.web.user.UserListAction:Filter', '2017-11-23 12:56:37.547', '<org.projectforge.framework.persistence.api.BaseSearchFilter>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
</org.projectforge.framework.persistence.api.BaseSearchFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (272, '2010-04-21 22:11:35.596', 'CalendarPage.userPrefs', '2017-11-23 12:56:37.575', '<org.projectforge.business.teamcal.filter.TeamCalCalendarFilter>
  <startDate>2019-06-23</startDate>
  <firstHour>8</firstHour>
  <viewType>AGENDA_WEEK</viewType>
  <templateEntries>
    <org.projectforge.business.teamcal.filter.TemplateEntry>
      <calendarProperties class="sorted-set">
        <org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
          <calId>157</calId>
          <colorCode>#008000</colorCode>
          <visible>true</visible>
          <millisOfLastChange>1562227222945</millisOfLastChange>
        </org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
        <org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
          <calId>158</calId>
          <colorCode>#ff0</colorCode>
          <visible>true</visible>
          <millisOfLastChange>1562227225273</millisOfLastChange>
        </org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
        <org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
          <calId>159</calId>
          <colorCode>#FAAF26</colorCode>
          <visible>true</visible>
          <millisOfLastChange>1562227214916</millisOfLastChange>
        </org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
        <org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
          <calId>160</calId>
          <colorCode>#f0f</colorCode>
          <visible>true</visible>
          <millisOfLastChange>1562227233067</millisOfLastChange>
        </org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
      </calendarProperties>
      <visibleCalendarIds>
        <int>160</int>
        <int>157</int>
        <int>158</int>
        <int>159</int>
      </visibleCalendarIds>
      <name>Default</name>
      <defaultCalendarId>-1</defaultCalendarId>
      <showBirthdays>true</showBirthdays>
      <showStatistics>true</showStatistics>
      <timesheetUserId>19</timesheetUserId>
      <selectedCalendar>timesheet</selectedCalendar>
      <showBreaks>true</showBreaks>
      <showPlanning>true</showPlanning>
    </org.projectforge.business.teamcal.filter.TemplateEntry>
  </templateEntries>
  <activeTemplateEntryIndex>0</activeTemplateEntryIndex>
</org.projectforge.business.teamcal.filter.TeamCalCalendarFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (277, '2010-04-21 23:11:22.364', 'org.projectforge.web.humanresources.HRListForm:Filter',
        '2017-11-23 12:56:37.597', '<org.projectforge.business.humanresources.HRFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
  <timePeriod>
    <fromDate>2010-04-18 22:00:00.0 UTC</fromDate>
    <toDate>2010-04-24 22:00:00.0 UTC</toDate>
    <marker>false</marker>
  </timePeriod>
  <onlyMyProjects>false</onlyMyProjects>
  <otherProjectsGroupedByCustomer>false</otherProjectsGroupedByCustomer>
  <allProjectsGroupedByCustomer>false</allProjectsGroupedByCustomer>
  <showPlanning>true</showPlanning>
  <showBookedTimesheets>false</showBookedTimesheets>
</org.projectforge.business.humanresources.HRFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (273, '2010-04-21 22:22:26.378', 'org.projectforge.web.fibu.KundeListForm:Filter', '2017-11-23 12:56:37.619', '<org.projectforge.framework.persistence.api.BaseSearchFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
</org.projectforge.framework.persistence.api.BaseSearchFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (278, '2010-04-21 23:11:22.366', 'org.projectforge.web.user.UserListForm:Filter', '2017-11-23 12:56:37.7', '<org.projectforge.business.user.PFUserFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
</org.projectforge.business.user.PFUserFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (265, '2010-04-21 22:11:35.586', 'openTasks', '2017-11-23 12:56:37.721', '<set>
  <int>48</int>
  <int>49</int>
  <int>50</int>
  <int>53</int>
  <int>54</int>
  <int>55</int>
  <int>59</int>
  <int>63</int>
  <int>47</int>
</set>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (281, '2010-04-22 08:01:38.344', 'org.projectforge.web.fibu.Kost1ListAction:Filter', '2017-11-23 12:56:37.743', '<org.projectforge.web.fibu.Kost1ListFilter>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
  <listType>notEnded</listType>
</org.projectforge.web.fibu.Kost1ListFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (270, '2010-04-21 22:11:35.593', 'menu.openedNodes', '2017-11-23 12:56:37.75', '<set>
  <short>38</short>
  <short>6</short>
  <short>26</short>
  <short>29</short>
  <short>15</short>
</set>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (283, '2010-04-22 21:45:34.786', 'org.projectforge.web.fibu.RechnungListForm:Filter', '2013-04-12 03:48:24.571', '<org.projectforge.business.fibu.RechnungListFilter>
      <searchString></searchString>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      <year>-1</year>
      <month>-1</month>
      <listType>all</listType>
      <showKostZuweisungStatus>false</showKostZuweisungStatus>
      </org.projectforge.business.fibu.RechnungListFilter>
    ', 4, 18);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (284, '2010-04-22 21:45:34.788', 'org.projectforge.web.timesheet.TimesheetListForm:Filter',
        '2013-04-12 03:48:24.587', '<org.projectforge.web.timesheet.TimesheetListFilter>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      <timePeriod>
      <fromDate>2009-12-31 23:00:00.0 UTC</fromDate>
      <toDate>2010-04-25 21:59:59.999 UTC</toDate>
      <marker>false</marker>
      </timePeriod>
      <marked>false</marked>
      <longFormat>false</longFormat>
      <recursive>true</recursive>
      <orderType>DESC</orderType>
      </org.projectforge.web.timesheet.TimesheetListFilter>
    ', 4, 18);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (285, '2010-04-22 21:45:34.789', 'org.projectforge.web.task.TaskListForm:Filter', '2013-04-12 03:48:24.589', '<TaskFilter notOpened="true" opened="true" closed="false" deleted="false">
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      </TaskFilter>
    ', 4, 18);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (286, '2010-04-22 21:45:34.79', 'org.projectforge.web.address.AddressListAction:pageSize',
        '2013-04-12 03:48:24.591', '<int>50</int>', 4, 18);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (287, '2010-04-22 21:45:34.792', 'org.projectforge.web.book.BookListAction:Filter', '2013-04-12 03:48:24.578', '<org.projectforge.web.book.BookListFilter>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      <present>true</present>
      <missed>false</missed>
      <disposed>false</disposed>
      </org.projectforge.web.book.BookListFilter>
    ', 4, 18);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (288, '2010-04-22 21:45:34.792', 'openTasks', '2013-04-12 03:48:24.592', '<set>
  <int>48</int>
  <int>49</int>
  <int>50</int>
  <int>53</int>
  <int>54</int>
  <int>55</int>
</set>', 4, 18);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (289, '2010-04-22 21:45:34.793', 'org.projectforge.web.user.UserListForm:Filter', '2013-04-12 03:48:24.58', '<org.projectforge.framework.persistence.api.BaseSearchFilter>
      <searchString></searchString>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      </org.projectforge.framework.persistence.api.BaseSearchFilter>
    ', 4, 18);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (290, '2010-04-22 21:45:34.794', 'org.projectforge.web.address.AddressListAction:recentSearchTerms',
        '2013-04-12 03:48:24.594', '<org.projectforge.framework.utils.RecentQueue>
      <maxSize>25</maxSize>
      </org.projectforge.framework.utils.RecentQueue>
    ', 4, 18);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (291, '2010-04-22 21:45:34.796', 'org.projectforge.web.address.AddressListAction:Filter',
        '2013-04-12 03:48:24.585', '<org.projectforge.web.address.AddressListFilter>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>50</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      <uptodate>true</uptodate>
      <outdated>false</outdated>
      <leaved>false</leaved>
      <active>true</active>
      <nonActive>false</nonActive>
      <uninteresting>false</uninteresting>
      <personaIngrata>false</personaIngrata>
      <departed>false</departed>
      <listType>newest</listType>
      </org.projectforge.web.address.AddressListFilter>
    ', 4, 18);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (292, '2010-04-22 21:45:34.797', 'menu.openedNodes', '2013-04-12 03:48:24.595', '<set/>', 4, 18);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (293, '2010-04-22 21:45:34.799', 'org.projectforge.web.timesheet.TimesheetEditPage', '2013-04-12 03:48:24.596', '
      !rO0ABXVyAAJbQqzzF/gGCFTgAgAAeHAAAAGhH4sIAAAAAAAAALWU207CQBCG732KSS+8oqmFUhItJAgoB0mIknhpljK0G9ou7m7x8DI+jC9mawtpmyVC1KudU/b/ZtpZR9IQhY8oZxxXnTMAh6OLkRSpnXgheX2g79hpXjjGzs4ypbrEL900iCR/A0nEerRsa7YGsUCemnUN1kzIemq3NAiYSyRlUVsbIlnCc0y4RK7BEoXL6SZL9XGLAduEiRywFaxIyGIBQ7pAHhGJ0AuIECguYXajm/VGLT3tVlMz/oqtK8FnIVawbjn7/Eh4qYcwJluiT745IvCSKp9TXGB0HINpqiEaRYgpdTkLiSQVjDG97+ojIeJ0At3edPD0OLjWTcuuFb3WkSSWmsQqkkzSNgPQzz15BUMSLmLuVaDmnNBjmz9GMlfJNTOAf5ZUdvmr601FR5UmpsmFNPLghUof+mSLO0XHKOxb2cnseSJ++tLSSHZsx0iPYsg0FTGrEFPi7BFymbu82dOxhOTJFDrFN8Ex8mClJt/NQ+n91hwqUH3mQ7Wqv/CH2nJaObbCmByj/B5/AUD89kmfBQAA
    ', 4, 18);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (294, '2010-04-22 21:45:34.801', 'CalendarPage.userPrefs', '2013-04-12 03:48:24.601', '<org.projectforge.business.teamcal.filter.TeamCalCalendarFilter>
  <startDate>2019-06-23</startDate>
  <firstHour>8</firstHour>
  <viewType>AGENDA_WEEK</viewType>
  <templateEntries>
    <org.projectforge.business.teamcal.filter.TemplateEntry>
      <calendarProperties class="sorted-set">
        <org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
          <calId>157</calId>
          <colorCode>#008000</colorCode>
          <visible>true</visible>
          <millisOfLastChange>1562227222945</millisOfLastChange>
        </org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
        <org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
          <calId>158</calId>
          <colorCode>#ff0</colorCode>
          <visible>true</visible>
          <millisOfLastChange>1562227225273</millisOfLastChange>
        </org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
        <org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
          <calId>159</calId>
          <colorCode>#FAAF26</colorCode>
          <visible>true</visible>
          <millisOfLastChange>1562227214916</millisOfLastChange>
        </org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
        <org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
          <calId>160</calId>
          <colorCode>#f0f</colorCode>
          <visible>true</visible>
          <millisOfLastChange>1562227233067</millisOfLastChange>
        </org.projectforge.business.teamcal.filter.TemplateCalendarProperties>
      </calendarProperties>
      <visibleCalendarIds>
        <int>160</int>
        <int>157</int>
        <int>158</int>
        <int>159</int>
      </visibleCalendarIds>
      <name>Default</name>
      <defaultCalendarId>-1</defaultCalendarId>
      <showBirthdays>true</showBirthdays>
      <showStatistics>true</showStatistics>
      <timesheetUserId>19</timesheetUserId>
      <selectedCalendar>timesheet</selectedCalendar>
      <showBreaks>true</showBreaks>
      <showPlanning>true</showPlanning>
    </org.projectforge.business.teamcal.filter.TemplateEntry>
  </templateEntries>
  <activeTemplateEntryIndex>0</activeTemplateEntryIndex>
      </org.projectforge.business.teamcal.filter.TeamCalCalendarFilter>
    ', 4, 18);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (296, '2010-04-23 21:13:12.76', 'org.projectforge.web.fibu.Kost2ListForm:Filter', '2017-11-23 12:56:37.368', '<org.projectforge.web.fibu.Kost2ListFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
  <listType>notEnded</listType>
</org.projectforge.web.fibu.Kost2ListFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (282, '2010-04-22 08:01:38.349', 'org.projectforge.web.fibu.Kost1ListForm:Filter', '2017-11-23 12:56:37.692', '<org.projectforge.web.fibu.Kost1ListFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
  <listType>notEnded</listType>
</org.projectforge.web.fibu.Kost1ListFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (295, '2010-04-23 21:13:12.756', 'org.projectforge.web.fibu.EingangsrechnungListForm:Filter',
        '2017-11-23 12:56:37.758', '<null/>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (302, '2010-05-03 22:54:06.283', 'org.projectforge.web.address.AddressListAction:pageSize',
        '2013-04-07 18:14:25.504', '<int>50</int>', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (303, '2010-05-03 22:54:06.286', 'org.projectforge.web.address.AddressListAction:recentSearchTerms',
        '2013-04-07 18:14:25.523', '<org.projectforge.framework.utils.RecentQueue>
      <maxSize>25</maxSize>
      </org.projectforge.framework.utils.RecentQueue>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (304, '2010-05-03 22:54:06.288', 'org.projectforge.web.address.AddressListAction:Filter',
        '2013-04-07 18:14:25.501', '<org.projectforge.web.address.AddressListFilter>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>50</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      <uptodate>true</uptodate>
      <outdated>false</outdated>
      <leaved>false</leaved>
      <active>true</active>
      <nonActive>false</nonActive>
      <uninteresting>false</uninteresting>
      <personaIngrata>false</personaIngrata>
      <departed>false</departed>
      <listType>newest</listType>
      </org.projectforge.web.address.AddressListFilter>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (305, '2010-05-03 23:04:47.656', 'org.projectforge.web.scripting.ScriptExecutePage', '2013-04-07 18:14:25.507',
        '<null/>', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (306, '2010-05-03 23:04:47.657', 'org.projectforge.web.scripting.ScriptListForm:Filter',
        '2013-04-07 18:14:25.518', '<org.projectforge.framework.persistence.api.BaseSearchFilter>
      <searchString></searchString>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      </org.projectforge.framework.persistence.api.BaseSearchFilter>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (307, '2010-05-03 23:26:30.573', 'org.projectforge.web.fibu.RechnungListForm:Filter', '2013-04-07 18:14:25.491', '<org.projectforge.business.fibu.RechnungListFilter>
      <searchString></searchString>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      <year>-1</year>
      <month>-1</month>
      <listType>all</listType>
      <showKostZuweisungStatus>false</showKostZuweisungStatus>
      </org.projectforge.business.fibu.RechnungListFilter>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (308, '2010-05-03 23:26:30.579', 'org.projectforge.web.user.UserListForm:Filter', '2013-04-07 18:14:25.514', '<org.projectforge.framework.persistence.api.BaseSearchFilter>
      <searchString></searchString>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      </org.projectforge.framework.persistence.api.BaseSearchFilter>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (309, '2010-05-03 23:26:30.58', 'org.projectforge.web.fibu.EingangsrechnungListForm:Filter',
        '2013-04-07 18:14:25.499', '<org.projectforge.business.fibu.EingangsrechnungListFilter>
      <searchString></searchString>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      <year>-1</year>
      <month>-1</month>
      <listType>all</listType>
      <showKostZuweisungStatus defined-in="org.projectforge.business.fibu.RechnungFilter">false</showKostZuweisungStatus>
      <showKostZuweisungStatus>false</showKostZuweisungStatus>
      </org.projectforge.business.fibu.EingangsrechnungListFilter>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (310, '2010-05-03 23:26:30.587', 'org.projectforge.web.timesheet.TimesheetEditPage', '2013-04-07 18:14:25.527', '<timesheetPref>
      <recents>
      <maxSize>50</maxSize>
      <recents>
      <timesheetPrefEntry taskId="17" userId="2" kost2Id="10"/>
      <timesheetPrefEntry taskId="11" userId="2" kost2Id="3" location="Head quarter" description="Requirement-Engineering:
      ACME_WEB-186, ACME_WEB-1876"/>
      </recents>
      </recents>
      <recentTasks>
      <maxSize>50</maxSize>
      <recents>
      <int>17</int>
      <int>11</int>
      </recents>
      </recentTasks>
      <recentLocations>
      <maxSize>50</maxSize>
      <recents>
      <string>Head quarter</string>
      </recents>
      </recentLocations>
      </timesheetPref>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (311, '2010-05-20 01:06:08.982', 'org.projectforge.web.access.AccessListAction:Filter',
        '2013-04-07 18:14:25.513', '<org.projectforge.framework.persistence.api.BaseSearchFilter>
      <searchString></searchString>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      </org.projectforge.framework.persistence.api.BaseSearchFilter>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (312, '2010-05-20 01:43:32.851', 'org.projectforge.web.book.BookListAction:pageSize', '2013-04-12 03:48:24.583',
        '<int>50</int>', 4, 18);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (313, '2010-07-13 17:37:52.71', 'org.projectforge.web.fibu.EmployeeListAction:Filter', '2013-04-07 18:14:25.509', '<org.projectforge.business.fibu.EmployeeFilter>
      <searchString></searchString>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      <showOnlyActiveEntries>false</showOnlyActiveEntries>
      </org.projectforge.business.fibu.EmployeeFilter>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (300, '2010-04-24 06:01:19.472', 'org.projectforge.web.fibu.EmployeeSalaryListAction:Filter',
        '2017-11-23 12:56:37.451', '<org.projectforge.web.fibu.EmployeeSalaryListFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
  <year>-1</year>
  <month>-1</month>
</org.projectforge.web.fibu.EmployeeSalaryListFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (298, '2010-04-24 06:01:19.47', 'org.projectforge.web.fibu.KontoListAction:Filter', '2017-11-23 12:56:37.64', '<org.projectforge.framework.persistence.api.BaseSearchFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
</org.projectforge.framework.persistence.api.BaseSearchFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (347, '2013-04-27 15:10:41.173', 'org.projectforge.web.fibu.EmployeeSalaryListForm:Filter',
        '2017-11-23 12:56:37.708', '<org.projectforge.business.fibu.EmployeeSalaryFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
  <year>0</year>
  <month>0</month>
</org.projectforge.business.fibu.EmployeeSalaryFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (314, '2010-07-13 17:37:52.72', 'org.projectforge.web.timesheet.TimesheetListForm:Filter',
        '2013-04-07 18:14:25.519', '<org.projectforge.web.timesheet.TimesheetListFilter>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      <timePeriod>
      <fromDate>2008-12-31 23:00:00.0 UTC</fromDate>
      <marker>false</marker>
      </timePeriod>
      <marked>false</marked>
      <longFormat>false</longFormat>
      <recursive>true</recursive>
      <orderType>DESC</orderType>
      </org.projectforge.web.timesheet.TimesheetListFilter>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (315, '2010-07-13 17:42:36.212', 'org.projectforge.web.book.BookListAction:Filter', '2013-04-07 18:14:25.495', '<org.projectforge.web.book.BookListFilter>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      <present>true</present>
      <missed>false</missed>
      <disposed>false</disposed>
      </org.projectforge.web.book.BookListFilter>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (316, '2010-07-13 17:42:36.217', 'org.projectforge.web.user.UserPrefListForm:Filter', '2013-04-07 18:14:25.522', '<org.projectforge.web.user.UserPrefListFilter>
      <searchString></searchString>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      </org.projectforge.web.user.UserPrefListFilter>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (317, '2010-08-27 07:52:17.632', 'org.projectforge.web.gantt.GanttChartListForm:Filter',
        '2013-04-07 18:14:25.52', '<org.projectforge.web.gantt.GanttChartListFilter>
      <searchString></searchString>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      <year>0</year>
      </org.projectforge.web.gantt.GanttChartListFilter>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (318, '2010-08-27 08:50:59.913', 'org.projectforge.web.book.BookListAction:pageSize', '2013-04-07 18:14:25.496',
        '<int>50</int>', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (319, '2010-08-27 08:50:59.915', 'org.projectforge.web.orga.ContractListForm:Filter', '2013-04-07 18:14:25.504',
        '<null/>', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (320, '2010-09-15 15:26:58.667', 'org.projectforge.web.orga.PosteingangListForm:Filter',
        '2013-04-07 18:14:25.517', '<org.projectforge.web.orga.PosteingangListFilter>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      <year>2010</year>
      <month>-1</month>
      </org.projectforge.web.orga.PosteingangListFilter>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (321, '2010-11-08 21:47:31.455', 'org.projectforge.web.gantt.GanttChartEditForm:exportFormat',
        '2013-04-07 18:14:25.505', '<string>PNG</string>', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (322, '2011-01-23 17:22:16.591', 'UserSelectPanel:recentUsers', '2013-04-07 18:16:46.491', '<org.projectforge.framework.utils.RecentQueue>
      <maxSize>25</maxSize>
      </org.projectforge.framework.utils.RecentQueue>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (323, '2011-01-23 17:38:06.594', 'org.projectforge.web.book.BookListForm:Filter', '2013-04-07 18:14:25.494', '<org.projectforge.web.book.BookListFilter>
      <searchString></searchString>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      <present>true</present>
      <missed>false</missed>
      <disposed>false</disposed>
      </org.projectforge.web.book.BookListFilter>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (324, '2011-01-23 17:38:06.597', 'bookSearchTerms', '2013-04-07 18:14:25.508', '<org.projectforge.framework.utils.RecentQueue>
      <maxSize>25</maxSize>
      </org.projectforge.framework.utils.RecentQueue>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (329, '2011-03-18 06:20:05.994', 'org.projectforge.web.address.AddressListForm:Filter',
        '2017-11-23 12:56:37.506', '<null/>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (328, '2011-03-18 06:20:05.989', 'addressSearchTerms', '2017-11-23 12:56:37.513', '<org.projectforge.framework.utils.RecentQueue>
  <maxSize>25</maxSize>
</org.projectforge.framework.utils.RecentQueue>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (332, '2012-05-02 22:45:47.314', 'org.projectforge.plugins.todo.ToDoDO', '2017-11-23 12:56:37.534', '<null/>', 4,
        19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (331, '2011-03-18 06:20:06.01', 'bookSearchTerms', '2017-11-23 12:56:37.605', '<org.projectforge.framework.utils.RecentQueue>
  <maxSize>25</maxSize>
</org.projectforge.framework.utils.RecentQueue>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (333, '2012-05-02 22:45:47.331', 'org.projectforge.plugins.todo.ToDoListForm:Filter', '2017-11-23 12:56:37.657', '<org.projectforge.plugins.todo.ToDoFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
  <opened>false</opened>
  <reopened>false</reopened>
  <inprogress>false</inprogress>
  <closed>false</closed>
  <postponed>false</postponed>
  <onlyRecent>false</onlyRecent>
</org.projectforge.plugins.todo.ToDoFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (325, '2011-03-18 06:20:05.98', 'PhoneCall:recentCalls', '2017-11-23 12:56:37.733', '<org.projectforge.framework.utils.RecentQueue>
  <maxSize>25</maxSize>
</org.projectforge.framework.utils.RecentQueue>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (330, '2011-03-18 06:20:06.003', 'org.projectforge.web.orga.ContractListForm:Filter', '2017-11-23 12:56:37.735',
        '<null/>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (327, '2011-03-18 06:20:05.984', 'UserSelectPanel:recentUsers', '2017-11-23 12:56:37.801', '<org.projectforge.framework.utils.RecentQueue>
  <maxSize>25</maxSize>
  <recents>
    <string>kai (Kai Reinhard, devnull@devnull.com)</string>
    <string>ann (Ann Ville, a.ville@my-company.com)</string>
    <string>admin (Admin ProjectForge Administrator, null)</string>
    <string>demo (Demo User, devnull@devnull.com)</string>
  </recents>
</org.projectforge.framework.utils.RecentQueue>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (326, '2011-03-18 06:20:05.982', 'org.projectforge.web.book.BookListForm:Filter', '2017-11-23 12:56:37.808', '<org.projectforge.web.book.BookListFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
  <present>true</present>
  <missed>false</missed>
  <disposed>false</disposed>
</org.projectforge.web.book.BookListFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (337, '2013-04-07 18:14:25.492', 'org.projectforge.plugins.teamcal.admin.TeamCalListForm:Filter',
        '2013-04-07 18:14:25.492', '<org.projectforge.plugins.teamcal.admin.TeamCalFilter>
      <searchString></searchString>
      <deleted>false</deleted>
      <ignoreDeleted>false</ignoreDeleted>
      <maxRows>-1</maxRows>
      <useModificationFilter>false</useModificationFilter>
      <searchHistory>false</searchHistory>
      <fullAccess>true</fullAccess>
      <readonlyAccess>true</readonlyAccess>
      <minimalAccess>true</minimalAccess>
      <calOwner>ALL</calOwner>
      </org.projectforge.plugins.teamcal.admin.TeamCalFilter>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (338, '2013-04-07 18:14:25.512', 'TeamCalendarPage.userPrefs', '2013-04-07 18:16:46.494', '
      !rO0ABXVyAAJbQqzzF/gGCFTgAgAAeHAAAAIMH4sIAAAAAAAAAN2WXW/aMBSG7/kVqLsmnxBAyiIxChvatFVap11WXnISvDl2ZB/o+PezC/kqaVm30otKkRK/583xOY+j2KGQmVVI8RNiTPUzWAXbZJQrC4HkMWEW5QiZJEgFt661NidMX8ATIpeUIcio1++HConES4IQeY7rD5zhwHFDu1aNJ6VS4QexkdEktOuBCW0p3F7vCohm7xefL2c33xeLj6FdqcaCkBdMp1pwlBSU0bT6xPLrFLt9Ap0iPnRzJUUBEnXufsyIUm8vlJAIyUABXpTuf51yfjRJnXFfwyqJNLH9QyskmJBzkUD0JnUc7ajGTdeWKvqDQYRyAwbcftR05JQxqr6kn4jC+ZrwDCLXD0b+aOJMxoEbhHaHo+7afva2zwjSOwUyPQ/IqTsdjV8TSP8EyOVstvSCs7Ac++7UeU0shydYOk56no9y5Dn+C4O8a/Wh2KH+8u1V0uSv5zH/QXNri16X6HeJw5ZY8eqYL+Qk13sOY/2yXBXad1ppUMA0EkjKl03yI60yr8XtOwnklzqsVEPo/Qfnxn71Yhue/chy2W1+X5GYQHIfnWmeSlwnZNcCUmlNp06CVCGNm9aGWHqR5qDWAPhNgTR7ZqAPE/fFv16Qg+eKEc4pzxquSnqWhdMlHh9fQhIj3ULLvOIJ/I70f+/BWO/JtXSd2f4AepuLf/gJAAA=
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (339, '2013-04-07 18:14:25.525', 'TimesheetEditForm.userPrefs', '2013-04-07 18:14:25.525', '<org.projectforge.web.timesheet.TimesheetEditFilter>
      <ignoredLocations class="linked-list"/>
      </org.projectforge.web.timesheet.TimesheetEditFilter>
    ', 4, 17);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (340, '2013-04-07 20:03:38.985', 'TeamCalendarPage.userPrefs', '2013-04-12 03:48:24.573', '
      !rO0ABXVyAAJbQqzzF/gGCFTgAgAAeHAAAAJCH4sIAAAAAAAAAO1XXW/aMBR951egTtobJJAPiJZFYhQ21GmrtE57nLzkBryZOLIvtPz72YUEp6RjHdBJbBIS8fHJ/Tg3OVZCLqbtXPDvEGOqrqGds8WUZrKNQOYxYW2aIUwFQcqz9o3ChoSpH2QJEWPKEETUaDZDiUTgJUGIunbHadluy+6E1hbVnJQKie/4QkT90Nou9NaSwu3NKodo8Hb04XLw9ctodBVaJaopCPOcqVCjDAUFqTGFPrH8bYjVOoAKEW+6uRY8B4EqdjNmRMrXF5ILhKQlAS8K9p+mHO4k2UZc1zBJIqXY+qKyxRkXQ55A9CK1bcUo1yZrSSX9xiBCsQAt3HplMuaUMSo/pu+JxOGMZFOIOo7vOb4b9Dq+1w2tGsa2a+vobZ9QyO4+IdPTCOk7jueek5DOHiFtu2+f6KH0A8/1z0lLd4+W48Fg3PVPoqXXs4PgmbW87/axvU39xd2TxByByqOtUP9VwW4d6NSBbgUs9arJF2Zkro4dxppFuTK07rGCIIEpSSApbta17WAlecZv3wggP2SUEibVqAykcYDQxpn1/9A7/J3wlG05ffuc/OUvenXQ94Pn9urD/OUornFF6EuSc/lK/r55uP+KeVi/GIZV1fETEr2RPNRON08FzhKykpsHs4qZTBUEqUQam1QDLLhI5yBnAPhZgtD+01MjeQjWTMTIbwxkw7lmJMtoNjVYJXSUwakSd79/QhIjXUKFPMkSuIvUa/7oXuPJtdR99P0EjkMR3zkOAAA=
    ', 4, 18);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (341, '2013-04-07 20:03:38.987', 'UserSelectPanel:recentUsers', '2013-04-12 03:48:24.582', '<org.projectforge.framework.utils.RecentQueue>
      <maxSize>25</maxSize>
      </org.projectforge.framework.utils.RecentQueue>
    ', 4, 18);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (334, '2013-04-07 15:58:58.943', 'TeamCalendarPage.userPrefs', '2017-11-23 12:56:37.386', '<null/>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (344, '2013-04-27 15:10:41.152', 'org.projectforge.web.scripting.ScriptExecutePage', '2017-11-23 12:56:37.395', '<RecentScriptCalls>
  <recentQueue>
    <maxSize>25</maxSize>
    <recents>
      <ScriptCall scriptName="JFreeChart"/>
      <ScriptCall scriptName="Excel export of all user&apos;s"/>
      <ScriptCall scriptName="Booked person days per day">
        <parameter type="TASK" parameterName="task" intValue="2"/>
        <parameter type="TIME_PERIOD" parameterName="timeperiod">
          <timePeriodValue>
            <fromDate>2000-08-31 22:00:00.0 UTC</fromDate>
            <toDate>2015-09-01 22:00:00.0 UTC</toDate>
            <marker>false</marker>
          </timePeriodValue>
        </parameter>
      </ScriptCall>
    </recents>
  </recentQueue>
</RecentScriptCalls>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (345, '2013-04-27 15:10:41.17', 'org.projectforge.web.scripting.ScriptListForm:Filter',
        '2017-11-23 12:56:37.472', '<org.projectforge.framework.persistence.api.BaseSearchFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
</org.projectforge.framework.persistence.api.BaseSearchFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (336, '2013-04-07 16:23:09.864', 'org.projectforge.plugins.teamcal.admin.TeamCalListForm:Filter',
        '2017-11-23 12:56:37.485', '<org.projectforge.plugins.teamcal.admin.TeamCalFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
  <fullAccess>true</fullAccess>
  <readonlyAccess>true</readonlyAccess>
  <minimalAccess>true</minimalAccess>
  <adminAccess>false</adminAccess>
  <calOwner>ALL</calOwner>
</org.projectforge.plugins.teamcal.admin.TeamCalFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (346, '2013-04-27 15:10:41.172', 'org.projectforge.web.fibu.KontoListForm:Filter', '2017-11-23 12:56:37.568', '<org.projectforge.framework.persistence.api.BaseSearchFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
</org.projectforge.framework.persistence.api.BaseSearchFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (343, '2013-04-27 15:10:41.149', 'org.projectforge.web.fibu.CustomerListForm:Filter', '2017-11-23 12:56:37.589', '<org.projectforge.framework.persistence.api.BaseSearchFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
</org.projectforge.framework.persistence.api.BaseSearchFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (335, '2013-04-07 16:09:02.762', 'org.projectforge.web.access.AccessListForm:Filter', '2017-11-23 12:56:37.729', '<org.projectforge.framework.access.AccessFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
  <includeAncestorTasks>false</includeAncestorTasks>
  <includeDescendentTasks>false</includeDescendentTasks>
  <inherit>false</inherit>
</org.projectforge.framework.access.AccessFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (342, '2013-04-27 15:10:41.144', 'org.projectforge.web.fibu.EmployeeListForm:Filter', '2017-11-23 12:56:37.731', '<org.projectforge.business.fibu.EmployeeFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
  <showOnlyActiveEntries>false</showOnlyActiveEntries>
</org.projectforge.business.fibu.EmployeeFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (266, '2010-04-21 22:11:35.589', 'org.projectforge.web.user.GroupListAction:Filter', '2017-11-23 12:56:37.332', '<org.projectforge.framework.persistence.api.BaseSearchFilter>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
</org.projectforge.framework.persistence.api.BaseSearchFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (263, '2010-04-21 22:11:35.584', 'org.projectforge.web.task.TaskListForm:Filter', '2017-11-23 12:56:37.355', '<TaskFilter notOpened="true" opened="true" closed="false" deleted="false">
  <deleted defined-in="org.projectforge.framework.persistence.api.BaseSearchFilter">false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
</TaskFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (348, '2013-04-27 15:10:41.175', 'org.projectforge.web.fibu.AccountingRecordListForm:Filter',
        '2017-11-23 12:56:37.389', '<org.projectforge.web.fibu.AccountingRecordListFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
  <fromYear>0</fromYear>
  <toYear>0</toYear>
  <fromMonth>0</fromMonth>
  <toMonth>0</toMonth>
</org.projectforge.web.fibu.AccountingRecordListFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (301, '2010-04-25 09:57:02.439', 'org.projectforge.web.address.SendSmsForm:recentSearchTerms',
        '2017-11-23 12:56:37.399', '<org.projectforge.framework.utils.RecentQueue>
  <maxSize>25</maxSize>
</org.projectforge.framework.utils.RecentQueue>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (299, '2010-04-24 06:01:19.471', 'org.projectforge.web.orga.PosteingangListForm:Filter',
        '2017-11-23 12:56:37.42', '<org.projectforge.web.orga.PosteingangListFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
  <year>-1</year>
  <month>-1</month>
</org.projectforge.web.orga.PosteingangListFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (349, '2013-04-27 15:10:41.177', 'ReportObjectivesForm:filter', '2017-11-23 12:56:37.454', '<org.projectforge.web.fibu.ReportObjectivesFilter>
  <from>2012-12-31 23:00:00.0 UTC</from>
  <to>2013-12-31 22:59:59.999 UTC</to>
</org.projectforge.web.fibu.ReportObjectivesFilter>', 4, 19);
INSERT INTO t_user_xml_prefs (pk, created, key, last_update, serializedsettings, version, user_id)
VALUES (297, '2010-04-24 06:01:19.467', 'org.projectforge.web.orga.PostausgangListForm:Filter',
        '2017-11-23 12:56:37.771', '<org.projectforge.web.orga.PostausgangListFilter>
  <searchString></searchString>
  <deleted>false</deleted>
  <ignoreDeleted>false</ignoreDeleted>
  <maxRows>-1</maxRows>
  <useModificationFilter>false</useModificationFilter>
  <searchHistory>false</searchHistory>
  <year>-1</year>
  <month>-1</month>
</org.projectforge.web.orga.PostausgangListFilter>', 4, 19);
