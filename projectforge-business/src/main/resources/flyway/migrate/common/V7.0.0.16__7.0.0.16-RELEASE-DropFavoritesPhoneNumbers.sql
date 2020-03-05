-- Favorite phone numbers are not supported anymore, so drop the columns.

ALTER TABLE t_personal_address DROP COLUMN business_phone;
ALTER TABLE t_personal_address DROP COLUMN fax;
ALTER TABLE t_personal_address DROP COLUMN mobile_phone;
ALTER TABLE t_personal_address DROP COLUMN private_phone;
ALTER TABLE t_personal_address DROP COLUMN private_mobile_phone;
