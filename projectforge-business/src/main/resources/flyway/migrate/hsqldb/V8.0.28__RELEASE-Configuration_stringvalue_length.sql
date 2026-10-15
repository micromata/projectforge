-- Room for the customer groups and business units (JSON, see ConfigurationParam.CUSTOMER_GROUPS).
ALTER TABLE T_CONFIGURATION ALTER COLUMN stringvalue VARCHAR(100000);
