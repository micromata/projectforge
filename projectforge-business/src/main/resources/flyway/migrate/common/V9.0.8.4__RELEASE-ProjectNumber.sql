-- System-wide unique project number (customers and projects independent of Kost2, number ranges configured in
-- application.properties: projectforge.fibu.number-ranges.*).
-- The existing projects get the number of the formerly hard-coded number ranges their Kost2 were built of:
-- customer projects 5.<customer 0-999>.<nummer 0-99> = 500000 + customer * 100 + nummer,
-- internal projects 4.<intern_kost2_4 0-999>.<nummer 0-99> = 400000 + intern_kost2_4 * 100 + nummer.
-- Projects not fitting these ranges keep NULL and get a free number on their next save.
ALTER TABLE t_fibu_projekt
    ADD COLUMN projektnummer INTEGER;

UPDATE t_fibu_projekt
SET projektnummer = 500000 + kunde_id * 100 + nummer
WHERE kunde_id BETWEEN 0 AND 999
  AND nummer BETWEEN 0 AND 99;

UPDATE t_fibu_projekt
SET projektnummer = 400000 + intern_kost2_4 * 100 + nummer
WHERE kunde_id IS NULL
  AND intern_kost2_4 BETWEEN 0 AND 999
  AND nummer BETWEEN 0 AND 99;

-- The local number of a project without customer covers all digits after the prefix (intern_kost2_4 is legacy):
UPDATE t_fibu_projekt
SET nummer = projektnummer - 400000
WHERE kunde_id IS NULL
  AND projektnummer IS NOT NULL;

ALTER TABLE t_fibu_projekt
    ADD CONSTRAINT unique_t_fibu_projekt_projektnummer UNIQUE (projektnummer);

-- Rollback (restores the former local number 0-99 and intern_kost2_4 of the internal projects, drops the project number):
-- UPDATE t_fibu_projekt SET intern_kost2_4 = (projektnummer - 400000) / 100, nummer = MOD(projektnummer - 400000, 100)
--   WHERE kunde_id IS NULL AND projektnummer BETWEEN 400000 AND 499999;
-- Customer projects saved with a free number store it as local number, too (range 5 restores digits 5-6, only if
-- digits 2-4 are the project's own customer; otherwise the local number could collide within the customer):
-- UPDATE t_fibu_projekt SET nummer = MOD(projektnummer, 100)
--   WHERE kunde_id IS NOT NULL AND nummer = projektnummer AND projektnummer BETWEEN 500000 AND 599999
--     AND (projektnummer - 500000) / 100 = kunde_id;
-- Other free numbers have no counterpart in the former ranges and keep their local number: after migrating again,
-- these projects have no project number and get a free one on their next save.
-- ALTER TABLE t_fibu_projekt DROP CONSTRAINT unique_t_fibu_projekt_projektnummer;
-- ALTER TABLE t_fibu_projekt DROP COLUMN projektnummer;
-- DELETE FROM t_flyway_schema_version WHERE version = '9.0.8.4';
