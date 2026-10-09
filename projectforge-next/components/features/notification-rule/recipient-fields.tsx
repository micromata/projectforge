"use client";

import { useTranslations } from "next-intl";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { CheckboxListField } from "@/components/shared/form/checkbox-list-field";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";
import { EMPLOYEE_METADATA } from "@/lib/metadata/employee.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * The recipients of a rule (NotificationRecipients): groups, single users and employees by status,
 * the union of all, minus the excluded statuses. None of them has metadata: they are kept as JSON in
 * NotificationRuleDO.recipients.
 */

const employee = fromMetadata(EMPLOYEE_METADATA);

type FieldProps = { className?: string };

export function RecipientGroupsField({ className }: FieldProps) {
  const t = useTranslations("notification.recipients");
  return (
    <EntityMultiAutocompleteField
      name="recipientGroups"
      label={t("groups")}
      entity="group"
      metadataLess
      className={className}
    />
  );
}

export function RecipientUsersField({ className }: FieldProps) {
  const t = useTranslations("notification.recipients");
  return (
    <EntityMultiAutocompleteField
      name="recipientUsers"
      label={t("users")}
      entity="user"
      metadataLess
      className={className}
    />
  );
}

export function AllEmployeesField({ className }: FieldProps) {
  const t = useTranslations("notification.recipients");
  return (
    <CheckboxField
      name="allEmployees"
      label={t("allEmployees")}
      className={className}
    />
  );
}

export function OnlyAffectedField({ className }: FieldProps) {
  const t = useTranslations("notification.recipients");
  return (
    <CheckboxField
      name="onlyAffected"
      label={t("onlyAffected")}
      hint={t("onlyAffectedInfo")}
      className={className}
    />
  );
}

export function EmployeeStatusField({ className }: FieldProps) {
  const t = useTranslations();
  return (
    <CheckboxListField
      name="employeeStatus"
      label={t("notification.recipients.employeeStatus")}
      options={employee.enumOptions("status", t)}
      className={className}
    />
  );
}

export function ExcludedEmployeeStatusField({ className }: FieldProps) {
  const t = useTranslations();
  return (
    <CheckboxListField
      name="excludedEmployeeStatus"
      label={t("notification.recipients.excludedEmployeeStatus")}
      options={employee.enumOptions("status", t)}
      className={className}
    />
  );
}

export function EditableByGroupsField({ className }: FieldProps) {
  const t = useTranslations("notification.rule");
  return (
    <EntityMultiAutocompleteField
      name="editableByGroups"
      label={t("editableByGroups")}
      hint={t("editableByGroupsInfo")}
      entity="group"
      metadataLess
      className={className}
    />
  );
}
