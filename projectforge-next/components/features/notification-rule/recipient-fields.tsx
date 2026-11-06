"use client";

import { useTranslations } from "next-intl";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { CheckboxListField } from "@/components/shared/form/checkbox-list-field";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";
import { EMPLOYEE_METADATA } from "@/lib/metadata/employee.generated";
import { cn } from "@/lib/utils";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { useChosenRuleType } from "./use-rule-types";

/**
 * The recipients of a rule (NotificationRecipients): groups, single users and employees by status,
 * the union of all, minus the excluded statuses. None of them has metadata: they are kept as JSON in
 * NotificationRuleDO.recipients.
 */

const employee = fromMetadata(EMPLOYEE_METADATA);

type FieldProps = { className?: string };

/**
 * A rule type notifying only those concerned (e.g. the members who haven't answered a team check) doesn't use the
 * recipients given, so they aren't offered (the excluded statuses still apply).
 */
function useRecipientsUnused() {
  return useChosenRuleType()?.recipientMode === "AFFECTED_ONLY";
}

export function RecipientGroupsField({ className }: FieldProps) {
  const t = useTranslations("notification.recipients");
  if (useRecipientsUnused()) return null;
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
  if (useRecipientsUnused()) return null;
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
  if (useRecipientsUnused()) return null;
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
  const ruleType = useChosenRuleType();
  // The handler finds those concerned itself (the recipients given in copy, or not used at all): its hint
  // instead of the checkbox.
  if (ruleType && ruleType.recipientMode !== "RECIPIENTS") {
    return (
      <p className={cn("text-xs text-muted-foreground", className)}>
        {ruleType.recipientsHint}
      </p>
    );
  }
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
  if (useRecipientsUnused()) return null;
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
