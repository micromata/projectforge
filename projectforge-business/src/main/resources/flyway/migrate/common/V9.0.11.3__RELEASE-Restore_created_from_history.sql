-- Saving an entry overwrote its creation date with the one posted, which the forms of projectforge-next don't send:
-- the date got lost (BaseDOPersistenceService ignores created and lastUpdate on update since). The creation date isn't
-- part of the history, but the time of the history entry of the insert is the same moment: it is restored from there.
-- Only empty creation dates are filled, and only where an insert entry exists; all others stay empty.

UPDATE t_addressbook
SET created = (SELECT MIN(h.modifiedat)
               FROM t_pf_history h
               WHERE h.entity_id = t_addressbook.pk
                 AND h.entity_name = 'org.projectforge.business.address.AddressbookDO'
                 AND h.entity_optype = 'Insert')
WHERE created IS NULL;

UPDATE t_fibu_buchungssatz
SET created = (SELECT MIN(h.modifiedat)
               FROM t_pf_history h
               WHERE h.entity_id = t_fibu_buchungssatz.pk
                 AND h.entity_name = 'org.projectforge.business.fibu.kost.BuchungssatzDO'
                 AND h.entity_optype = 'Insert')
WHERE created IS NULL;

UPDATE t_fibu_employee_valid_since_attr
SET created = (SELECT MIN(h.modifiedat)
               FROM t_pf_history h
               WHERE h.entity_id = t_fibu_employee_valid_since_attr.pk
                 AND h.entity_name = 'org.projectforge.business.fibu.EmployeeValidSinceAttrDO'
                 AND h.entity_optype = 'Insert')
WHERE created IS NULL;

UPDATE t_notification_rule
SET created = (SELECT MIN(h.modifiedat)
               FROM t_pf_history h
               WHERE h.entity_id = t_notification_rule.pk
                 AND h.entity_name = 'org.projectforge.business.notification.NotificationRuleDO'
                 AND h.entity_optype = 'Insert')
WHERE created IS NULL;

UPDATE t_orga_visitorbook_entry
SET created = (SELECT MIN(h.modifiedat)
               FROM t_pf_history h
               WHERE h.entity_id = t_orga_visitorbook_entry.pk
                 AND h.entity_name = 'org.projectforge.business.orga.VisitorbookEntryDO'
                 AND h.entity_optype = 'Insert')
WHERE created IS NULL;

UPDATE t_employee_leave_account_entry
SET created = (SELECT MIN(h.modifiedat)
               FROM t_pf_history h
               WHERE h.entity_id = t_employee_leave_account_entry.pk
                 AND h.entity_name = 'org.projectforge.business.vacation.model.LeaveAccountEntryDO'
                 AND h.entity_optype = 'Insert')
WHERE created IS NULL;

UPDATE t_employee_vacation
SET created = (SELECT MIN(h.modifiedat)
               FROM t_pf_history h
               WHERE h.entity_id = t_employee_vacation.pk
                 AND h.entity_name = 'org.projectforge.business.vacation.model.VacationDO'
                 AND h.entity_optype = 'Insert')
WHERE created IS NULL;
