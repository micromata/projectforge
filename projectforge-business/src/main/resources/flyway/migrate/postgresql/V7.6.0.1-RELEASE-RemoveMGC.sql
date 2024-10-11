ALTER TABLE t_pf_history ALTER COLUMN createdat DROP NOT NULL; -- modifiedat is used instead.
ALTER TABLE t_pf_history ALTER COLUMN createdby DROP NOT NULL; -- modifiedby is used instead.
ALTER TABLE t_pf_history ALTER COLUMN updatecounter DROP NOT NULL;

ALTER TABLE t_pf_history_attr ALTER COLUMN value TYPE varchar(50000);
ALTER TABLE t_pf_history_attr ALTER COLUMN createdat DROP NOT NULL; -- parent.modifiedat is used instead.
ALTER TABLE t_pf_history_attr ALTER COLUMN createdby DROP NOT NULL; -- parent.modifieby is used instead.
ALTER TABLE t_pf_history_attr ALTER COLUMN modifiedat DROP NOT NULL; -- parent.modifiedat is used instead.
ALTER TABLE t_pf_history_attr ALTER COLUMN modifiedby DROP NOT NULL; -- parent.modifieby is used instead.
ALTER TABLE t_pf_history_attr ALTER COLUMN updatecounter DROP NOT NULL;
ALTER TABLE t_pf_history_attr ALTER COLUMN type DROP NOT NULL;

DROP VIEW IF EXISTS v_t_pf_user; -- Very old view, not used anymore.
DROP TABLE IF EXISTS t_address_attrdata; -- Unused empty table.
DROP TABLE IF EXISTS t_address_attr; -- Unused empty table.
DROP TABLE IF EXISTS t_personal_contact; -- Unused empty table.
DROP TABLE IF EXISTS t_contactentry; -- Unused empty table.
DROP TABLE IF EXISTS t_contact; -- Unused empty table.

DROP TABLE IF EXISTS t_employee_vacation_calendar; -- Unused table (entries not younger than 2020-01-07).

ALTER TABLE t_configuration ALTER COLUMN intvalue TYPE bigint;
ALTER TABLE t_configuration RENAME COLUMN intvalue TO longvalue;

-- Plugins without own flyway scripts:
ALTER TABLE t_plugin_calendar_event ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_plugin_calendar_event ALTER COLUMN calendar_fk TYPE bigint;
ALTER TABLE t_plugin_calendar_event ALTER COLUMN team_event_fk_creator TYPE bigint;
ALTER TABLE t_plugin_calendar_event_attachment ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_plugin_calendar_event_attendee ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_plugin_calendar_event_attendee ALTER COLUMN address_id TYPE bigint;
ALTER TABLE t_plugin_calendar_event_attendee ALTER COLUMN user_id TYPE bigint;
ALTER TABLE t_plugin_liqui_entry ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_plugin_lm_license ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_plugin_marketing_address_campaign ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_plugin_marketing_address_campaign_value ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_plugin_marketing_address_campaign_value ALTER COLUMN address_fk TYPE bigint;
ALTER TABLE t_plugin_marketing_address_campaign_value ALTER COLUMN address_campaign_fk TYPE bigint;
ALTER TABLE t_plugin_memo ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_plugin_memo ALTER COLUMN owner_fk TYPE bigint;
ALTER TABLE t_plugin_todo ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_plugin_todo ALTER COLUMN reporter_fk TYPE bigint;
ALTER TABLE t_plugin_todo ALTER COLUMN assignee_fk TYPE bigint;
ALTER TABLE t_plugin_todo ALTER COLUMN task_id TYPE bigint;
ALTER TABLE t_plugin_todo ALTER COLUMN group_id TYPE bigint;

-- Created by Kotlin-code:
ALTER TABLE t_address ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_address_image ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_address_image ALTER COLUMN address_fk TYPE bigint;
ALTER TABLE t_addressbook ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_addressbook ALTER COLUMN owner_fk TYPE bigint;
ALTER TABLE t_book ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_book ALTER COLUMN lend_out_by TYPE bigint;
ALTER TABLE t_calendar ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_calendar ALTER COLUMN owner_fk TYPE bigint;
ALTER TABLE t_calendar_event ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_calendar_event ALTER COLUMN calendar_fk TYPE bigint;
ALTER TABLE t_configuration ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_contract ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_employee_leave_account_entry ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_employee_leave_account_entry ALTER COLUMN employee_id TYPE bigint;
ALTER TABLE t_employee_remaining_leave ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_employee_remaining_leave ALTER COLUMN employee_id TYPE bigint;
ALTER TABLE t_employee_vacation ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_employee_vacation ALTER COLUMN employee_id TYPE bigint;
ALTER TABLE t_employee_vacation ALTER COLUMN replacement_id TYPE bigint;
ALTER TABLE t_employee_vacation ALTER COLUMN manager_id TYPE bigint;
ALTER TABLE t_fibu_auftrag ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_fibu_auftrag ALTER COLUMN contact_person_fk TYPE bigint;
ALTER TABLE t_fibu_auftrag ALTER COLUMN kunde_fk TYPE bigint;
ALTER TABLE t_fibu_auftrag ALTER COLUMN projekt_fk TYPE bigint;
ALTER TABLE t_fibu_auftrag ALTER COLUMN projectmanager_fk TYPE bigint;
ALTER TABLE t_fibu_auftrag ALTER COLUMN headofbusinessmanager_fk TYPE bigint;
ALTER TABLE t_fibu_auftrag ALTER COLUMN salesmanager_fk TYPE bigint;
ALTER TABLE t_fibu_auftrag_position ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_fibu_auftrag_position ALTER COLUMN auftrag_fk TYPE bigint;
ALTER TABLE t_fibu_auftrag_position ALTER COLUMN task_fk TYPE bigint;
ALTER TABLE t_fibu_buchungssatz ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_fibu_buchungssatz ALTER COLUMN konto_id TYPE bigint;
ALTER TABLE t_fibu_buchungssatz ALTER COLUMN gegenkonto_id TYPE bigint;
ALTER TABLE t_fibu_buchungssatz ALTER COLUMN kost1_id TYPE bigint;
ALTER TABLE t_fibu_buchungssatz ALTER COLUMN kost2_id TYPE bigint;
ALTER TABLE t_fibu_eingangsrechnung ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_fibu_eingangsrechnung ALTER COLUMN konto_id TYPE bigint;
ALTER TABLE t_fibu_eingangsrechnung_position ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_fibu_eingangsrechnung_position ALTER COLUMN eingangsrechnung_fk TYPE bigint;
ALTER TABLE t_fibu_employee ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_fibu_employee ALTER COLUMN user_id TYPE bigint;
ALTER TABLE t_fibu_employee ALTER COLUMN kost1_id TYPE bigint;
ALTER TABLE t_fibu_employee_salary ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_fibu_employee_salary ALTER COLUMN employee_id TYPE bigint;
ALTER TABLE t_fibu_employee_validity_period_attr ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_fibu_employee_validity_period_attr ALTER COLUMN employee_fk TYPE bigint;
ALTER TABLE t_fibu_konto ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_fibu_kost1 ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_fibu_kost2 ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_fibu_kost2 ALTER COLUMN kost2_art_id TYPE bigint;
ALTER TABLE t_fibu_kost2 ALTER COLUMN projekt_id TYPE bigint;
ALTER TABLE t_fibu_kost2art ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_fibu_kost_zuweisung ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_fibu_kost_zuweisung ALTER COLUMN kost1_fk TYPE bigint;
ALTER TABLE t_fibu_kost_zuweisung ALTER COLUMN kost2_fk TYPE bigint;
ALTER TABLE t_fibu_kost_zuweisung ALTER COLUMN rechnungs_pos_fk TYPE bigint;
ALTER TABLE t_fibu_kost_zuweisung ALTER COLUMN eingangsrechnungs_pos_fk TYPE bigint;
ALTER TABLE t_fibu_kost_zuweisung ALTER COLUMN employee_salary_fk TYPE bigint;
ALTER TABLE t_fibu_kunde ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_fibu_kunde ALTER COLUMN konto_id TYPE bigint;
ALTER TABLE t_fibu_payment_schedule ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_fibu_payment_schedule ALTER COLUMN auftrag_id TYPE bigint;
ALTER TABLE t_fibu_projekt ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_fibu_projekt ALTER COLUMN kunde_id TYPE bigint;
ALTER TABLE t_fibu_projekt ALTER COLUMN projektmanager_group_fk TYPE bigint;
ALTER TABLE t_fibu_projekt ALTER COLUMN projectmanager_fk TYPE bigint;
ALTER TABLE t_fibu_projekt ALTER COLUMN headofbusinessmanager_fk TYPE bigint;
ALTER TABLE t_fibu_projekt ALTER COLUMN salesmanager_fk TYPE bigint;
ALTER TABLE t_fibu_projekt ALTER COLUMN task_fk TYPE bigint;
ALTER TABLE t_fibu_projekt ALTER COLUMN konto_id TYPE bigint;
ALTER TABLE t_fibu_rechnung ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_fibu_rechnung ALTER COLUMN kunde_id TYPE bigint;
ALTER TABLE t_fibu_rechnung ALTER COLUMN projekt_id TYPE bigint;
ALTER TABLE t_fibu_rechnung ALTER COLUMN konto_id TYPE bigint;
ALTER TABLE t_fibu_rechnung_position ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_fibu_rechnung_position ALTER COLUMN rechnung_fk TYPE bigint;
ALTER TABLE t_fibu_rechnung_position ALTER COLUMN auftrags_position_fk TYPE bigint;
ALTER TABLE t_gantt_chart ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_gantt_chart ALTER COLUMN task_fk TYPE bigint;
ALTER TABLE t_gantt_chart ALTER COLUMN owner_fk TYPE bigint;
ALTER TABLE t_group ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_group ALTER COLUMN group_owner_fk TYPE bigint;
ALTER TABLE t_group_task_access ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_group_task_access ALTER COLUMN group_id TYPE bigint;
ALTER TABLE t_group_task_access ALTER COLUMN task_id TYPE bigint;
ALTER TABLE t_group_task_access_entry ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_hr_planning ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_hr_planning ALTER COLUMN user_fk TYPE bigint;
ALTER TABLE t_hr_planning_entry ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_hr_planning_entry ALTER COLUMN planning_fk TYPE bigint;
ALTER TABLE t_hr_planning_entry ALTER COLUMN projekt_fk TYPE bigint;
ALTER TABLE t_orga_postausgang ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_orga_posteingang ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_orga_visitorbook ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_orga_visitorbook_entry ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_orga_visitorbook_entry ALTER COLUMN visitorbook_fk TYPE bigint;
ALTER TABLE t_personal_address ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_personal_address ALTER COLUMN address_id TYPE bigint;
ALTER TABLE t_personal_address ALTER COLUMN owner_id TYPE bigint;
ALTER TABLE t_pf_user ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_pf_user_authentications ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_pf_user_authentications ALTER COLUMN user_id TYPE bigint;
ALTER TABLE t_pf_user_password ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_pf_user_password ALTER COLUMN user_id TYPE bigint;
ALTER TABLE t_poll ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_poll ALTER COLUMN owner_fk TYPE bigint;
ALTER TABLE t_poll_response ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_poll_response ALTER COLUMN poll_fk TYPE bigint;
ALTER TABLE t_poll_response ALTER COLUMN owner_fk TYPE bigint;
ALTER TABLE t_script ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_script ALTER COLUMN execute_as_user_id TYPE bigint;
ALTER TABLE t_sipgate_contact_sync ALTER COLUMN address_id TYPE bigint;
ALTER TABLE t_task ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_task ALTER COLUMN parent_task_id TYPE bigint;
ALTER TABLE t_task ALTER COLUMN responsible_user_id TYPE bigint;
ALTER TABLE t_task ALTER COLUMN gantt_predecessor_fk TYPE bigint;
ALTER TABLE t_timesheet ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_timesheet ALTER COLUMN task_id TYPE bigint;
ALTER TABLE t_timesheet ALTER COLUMN user_id TYPE bigint;
ALTER TABLE t_timesheet ALTER COLUMN kost2_id TYPE bigint;
ALTER TABLE t_user_pref ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_user_pref ALTER COLUMN user_fk TYPE bigint;
ALTER TABLE t_user_pref_entry ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_user_right ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_user_right ALTER COLUMN user_fk TYPE bigint;
ALTER TABLE t_user_webauthn ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_user_webauthn ALTER COLUMN owner_fk TYPE bigint;
ALTER TABLE t_user_xml_prefs ALTER COLUMN pk TYPE bigint;
ALTER TABLE t_user_xml_prefs ALTER COLUMN user_id TYPE bigint;
