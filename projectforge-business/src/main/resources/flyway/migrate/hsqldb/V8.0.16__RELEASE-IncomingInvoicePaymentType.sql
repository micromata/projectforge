-- Enlarge the payment_type column in the t_fibu_eingangsrechnung table to accommodate longer values
ALTER TABLE t_fibu_eingangsrechnung ALTER COLUMN payment_type varchar(30);
