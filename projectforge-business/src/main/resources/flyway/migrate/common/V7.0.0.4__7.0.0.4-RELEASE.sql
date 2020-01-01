-- Since 7.0 X-max snapshot, handling of month values were changed to 1-based.
-- This scripts update the databases.
-- In Java calendar months were handled 0-based (from 0-January to 11-December).
-- In java.time months are handled 1-based (from 1-January to 12-December).

-- Increment month column (12 statements for preventing constraint violations)
-- UPDATE  t_fibu_buchungssatz SET month=month+1; does't work.
UPDATE t_fibu_buchungssatz SET month=12 where month=11;
UPDATE t_fibu_buchungssatz SET month=11 where month=10;
UPDATE t_fibu_buchungssatz SET month=10 where month=9;
UPDATE t_fibu_buchungssatz SET month=9 where month=8;
UPDATE t_fibu_buchungssatz SET month=8 where month=7;
UPDATE t_fibu_buchungssatz SET month=7 where month=6;
UPDATE t_fibu_buchungssatz SET month=6 where month=5;
UPDATE t_fibu_buchungssatz SET month=5 where month=4;
UPDATE t_fibu_buchungssatz SET month=4 where month=3;
UPDATE t_fibu_buchungssatz SET month=3 where month=2;
UPDATE t_fibu_buchungssatz SET month=2 where month=1;
UPDATE t_fibu_buchungssatz SET month=1 where month=0;

UPDATE T_FIBU_EMPLOYEE_SALARY SET month=12 where month=11;
UPDATE T_FIBU_EMPLOYEE_SALARY SET month=11 where month=10;
UPDATE T_FIBU_EMPLOYEE_SALARY SET month=10 where month=9;
UPDATE T_FIBU_EMPLOYEE_SALARY SET month=9 where month=8;
UPDATE T_FIBU_EMPLOYEE_SALARY SET month=8 where month=7;
UPDATE T_FIBU_EMPLOYEE_SALARY SET month=7 where month=6;
UPDATE T_FIBU_EMPLOYEE_SALARY SET month=6 where month=5;
UPDATE T_FIBU_EMPLOYEE_SALARY SET month=5 where month=4;
UPDATE T_FIBU_EMPLOYEE_SALARY SET month=4 where month=3;
UPDATE T_FIBU_EMPLOYEE_SALARY SET month=3 where month=2;
UPDATE T_FIBU_EMPLOYEE_SALARY SET month=2 where month=1;
UPDATE T_FIBU_EMPLOYEE_SALARY SET month=1 where month=0;
