-- Remove old tables:
DROP TABLE IF EXISTS t_plugin_poll_result;
DROP TABLE IF EXISTS t_plugin_poll_attendee;
DROP TABLE IF EXISTS t_plugin_skill_rating;
DROP TABLE IF EXISTS t_plugin_skill_training_attendee;
DROP TABLE IF EXISTS t_plugin_skill_training;
DROP TABLE IF EXISTS t_plugin_skill;
DROP TABLE IF EXISTS t_plugin_financialfairplay_accounting;
DROP TABLE IF EXISTS t_plugin_financialfairplay_debt;
DROP TABLE IF EXISTS t_plugin_financialfairplay_event_attendee;
DROP TABLE IF EXISTS t_plugin_financialfairplay_event;
DROP TABLE IF EXISTS t_plugin_poll_event;
DROP TABLE IF EXISTS t_plugin_poll;
DROP TABLE IF EXISTS t_imported_meb_entry;
DROP TABLE IF EXISTS t_meb_entry;
DROP TABLE IF EXISTS t_plugin_employee_configuration_attrdata;
DROP TABLE IF EXISTS t_plugin_employee_configuration_attr;
DROP TABLE IF EXISTS t_plugin_employee_configuration_timedattrdata;
DROP TABLE IF EXISTS t_plugin_employee_configuration_timedattr;
DROP TABLE IF EXISTS t_plugin_employee_configuration_timed;
DROP TABLE IF EXISTS t_plugin_employee_configuration;
DROP TABLE IF EXISTS tb_base_ghistory_attr_data;
DROP TABLE IF EXISTS tb_base_ghistory_attr;
DROP TABLE IF EXISTS tb_base_ghistory;
DROP TABLE IF EXISTS t_personal_contact;
DROP TABLE IF EXISTS t_contactentry;
DROP TABLE IF EXISTS t_contact;
DROP TABLE IF EXISTS t_employee_vacation_calendar;
DROP TABLE IF EXISTS t_plugin_bank_account_balance;
DROP TABLE IF EXISTS t_plugin_bank_account_record;
DROP TABLE IF EXISTS t_plugin_bank_account;
DROP TABLE IF EXISTS t_plugin_plugintemplate;
DROP TABLE IF EXISTS t_tenant_user;
-- Must remove this table later (is used by plugins) DROP TABLE IF EXISTS t_tenant;

-- t_tenant is removed and not in use anymore, drop it:
-- Don't drop it now, all constraints must be handled first (e.g. t_address.unique(tenant_id, uid)):
-- ALTER TABLE t_address                                 DROP COLUMN tenant_id;
-- ALTER TABLE t_addressbook                             DROP COLUMN tenant_id;
-- ALTER TABLE t_book                                    DROP COLUMN tenant_id;
-- ALTER TABLE t_calendar                                DROP COLUMN tenant_id;
-- ALTER TABLE t_calendar_event                          DROP COLUMN tenant_id;
-- ALTER TABLE t_configuration                           DROP COLUMN tenant_id;
-- ALTER TABLE t_contract                                DROP COLUMN tenant_id;
-- ALTER TABLE t_employee_leave_account_entry            DROP COLUMN tenant_id;
-- ALTER TABLE t_employee_remaining_leave                DROP COLUMN tenant_id;
-- ALTER TABLE t_employee_vacation                       DROP COLUMN tenant_id;
-- ALTER TABLE t_fibu_auftrag                            DROP COLUMN tenant_id;
-- ALTER TABLE t_fibu_auftrag_position                   DROP COLUMN tenant_id;
-- ALTER TABLE t_fibu_buchungssatz                       DROP COLUMN tenant_id;
-- ALTER TABLE t_fibu_eingangsrechnung                   DROP COLUMN tenant_id;
-- ALTER TABLE t_fibu_eingangsrechnung_position          DROP COLUMN tenant_id;
-- ALTER TABLE t_fibu_employee                           DROP COLUMN tenant_id;
-- ALTER TABLE t_fibu_employee_salary                    DROP COLUMN tenant_id;
-- ALTER TABLE t_fibu_konto                              DROP COLUMN tenant_id;
-- ALTER TABLE t_fibu_kost1                              DROP COLUMN tenant_id;
-- ALTER TABLE t_fibu_kost2                              DROP COLUMN tenant_id;
-- ALTER TABLE t_fibu_kost2art                           DROP COLUMN tenant_id;
-- ALTER TABLE t_fibu_kost_zuweisung                     DROP COLUMN tenant_id;
-- ALTER TABLE t_fibu_kunde                              DROP COLUMN tenant_id;
-- ALTER TABLE t_fibu_payment_schedule                   DROP COLUMN tenant_id;
-- ALTER TABLE t_fibu_projekt                            DROP COLUMN tenant_id;
-- ALTER TABLE t_fibu_rechnung                           DROP COLUMN tenant_id;
-- ALTER TABLE t_fibu_rechnung_position                  DROP COLUMN tenant_id;
-- ALTER TABLE t_gantt_chart                             DROP COLUMN tenant_id;
-- ALTER TABLE t_group                                   DROP COLUMN tenant_id;
-- ALTER TABLE t_group_task_access                       DROP COLUMN tenant_id;
-- ALTER TABLE t_group_task_access_entry                 DROP COLUMN tenant_id;
-- ALTER TABLE t_hr_planning                             DROP COLUMN tenant_id;
-- ALTER TABLE t_hr_planning_entry                       DROP COLUMN tenant_id;
-- ALTER TABLE t_orga_postausgang                        DROP COLUMN tenant_id;
-- ALTER TABLE t_orga_posteingang                        DROP COLUMN tenant_id;
-- ALTER TABLE t_orga_visitorbook                        DROP COLUMN tenant_id;
-- ALTER TABLE t_personal_address                        DROP COLUMN tenant_id;
-- ALTER TABLE t_pf_user                                 DROP COLUMN tenant_id;
-- ALTER TABLE t_plugin_calendar_event                   DROP COLUMN tenant_id;
-- ALTER TABLE t_plugin_calendar_event_attachment        DROP COLUMN tenant_id;
-- ALTER TABLE t_plugin_calendar_event_attendee          DROP COLUMN tenant_id;
-- ALTER TABLE t_plugin_liqui_entry                      DROP COLUMN tenant_id;
-- ALTER TABLE t_plugin_lm_license                       DROP COLUMN tenant_id;
-- ALTER TABLE t_plugin_marketing_address_campaign       DROP COLUMN tenant_id;
-- ALTER TABLE t_plugin_marketing_address_campaign_value DROP COLUMN tenant_id;
-- ALTER TABLE t_plugin_memo                             DROP COLUMN tenant_id;
-- ALTER TABLE t_plugin_todo                             DROP COLUMN tenant_id;
-- ALTER TABLE t_script                                  DROP COLUMN tenant_id;
-- ALTER TABLE t_task                                    DROP COLUMN tenant_id;
-- ALTER TABLE t_timesheet                               DROP COLUMN tenant_id;
-- ALTER TABLE t_user_pref                               DROP COLUMN tenant_id;
-- ALTER TABLE t_user_pref_entry                         DROP COLUMN tenant_id;
-- ALTER TABLE t_user_right                              DROP COLUMN tenant_id;
-- ALTER TABLE t_user_xml_prefs                          DROP COLUMN tenant_id;

-- Get all tables with foreign keys to t_tenant:
-- SELECT
--     con.conname AS foreign_key_name,
--     src_ns.nspname AS source_schema,
--     src_table.relname AS source_table,
--     src_col.attname AS source_column,
--     tgt_ns.nspname AS target_schema,
--     tgt_table.relname AS target_table,
--     tgt_col.attname AS target_column
-- FROM
--     pg_constraint con
--         INNER JOIN pg_class src_table
--                    ON con.conrelid = src_table.oid
--         INNER JOIN pg_namespace src_ns
--                    ON src_table.relnamespace = src_ns.oid
--         INNER JOIN pg_attribute src_col
--                    ON src_col.attnum = ANY (con.conkey) AND src_col.attrelid = con.conrelid
--         INNER JOIN pg_class tgt_table
--                    ON con.confrelid = tgt_table.oid
--         INNER JOIN pg_namespace tgt_ns
--                    ON tgt_table.relnamespace = tgt_ns.oid
--         INNER JOIN pg_attribute tgt_col
--                    ON tgt_col.attnum = ANY (con.confkey) AND tgt_col.attrelid = con.confrelid
-- WHERE
--     con.contype = 'f' -- Nur Foreign Keys
-- --  AND tgt_ns.nspname = 'your_schema' -- Zielschema der Tabelle
--   AND tgt_table.relname = 't_tenant' -- Zieltabelle
-- ORDER BY
--     source_schema, source_table, foreign_key_name;
