"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { AbsenceKindsSelect } from "@/components/shared/absence-kinds/absence-kinds-select";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";
import {
  useEntityEditForm,
  useFormReadOnly,
} from "@/components/shared/form/form-context";
import type { TeamCalValues } from "./team-cal-schema";

/**
 * The users and groups with access to a calendar, and those whose leave days it shows — each a multi select
 * bound to the DTO's list of references.
 *
 * Custom fields because none of them is a field of TeamCalDO's metadata: the DO stores csv lists of ids
 * (`fullAccessUserIds`, …), which is what a declared name is checked against.
 */
function RefsField({
  name,
  entity,
  label,
  hint,
  className,
}: {
  name: string;
  entity: "user" | "group";
  label: string;
  hint?: string;
  className?: string;
}) {
  return (
    <EntityMultiAutocompleteField
      name={name}
      label={label}
      hint={hint}
      entity={entity}
      metadataLess
      className={className}
    />
  );
}

type FieldProps = { className?: string };

export function TeamCalFullAccessUsersField({ className }: FieldProps) {
  const t = useTranslations();
  return (
    <RefsField
      name="fullAccessUsers"
      entity="user"
      label={`${t("plugins.teamcal.fullAccess")} (${t("plugins.teamcal.access.users")})`}
      className={className}
    />
  );
}

export function TeamCalFullAccessGroupsField({ className }: FieldProps) {
  const t = useTranslations();
  return (
    <RefsField
      name="fullAccessGroups"
      entity="group"
      label={`${t("plugins.teamcal.fullAccess")} (${t("plugins.teamcal.access.groups")})`}
      className={className}
    />
  );
}

export function TeamCalReadonlyAccessUsersField({ className }: FieldProps) {
  const t = useTranslations();
  return (
    <RefsField
      name="readonlyAccessUsers"
      entity="user"
      label={`${t("plugins.teamcal.readonlyAccess")} (${t("plugins.teamcal.access.users")})`}
      className={className}
    />
  );
}

export function TeamCalReadonlyAccessGroupsField({ className }: FieldProps) {
  const t = useTranslations();
  return (
    <RefsField
      name="readonlyAccessGroups"
      entity="group"
      label={`${t("plugins.teamcal.readonlyAccess")} (${t("plugins.teamcal.access.groups")})`}
      className={className}
    />
  );
}

/** Sees the events as busy blocks only, without any further information. */
export function TeamCalMinimalAccessUsersField({ className }: FieldProps) {
  const t = useTranslations();
  return (
    <RefsField
      name="minimalAccessUsers"
      entity="user"
      label={`${t("plugins.teamcal.minimalAccess._")} (${t("plugins.teamcal.access.users")})`}
      hint={t("plugins.teamcal.minimalAccess.users.hint")}
      className={className}
    />
  );
}

export function TeamCalMinimalAccessGroupsField({ className }: FieldProps) {
  const t = useTranslations();
  return (
    <RefsField
      name="minimalAccessGroups"
      entity="group"
      label={`${t("plugins.teamcal.minimalAccess._")} (${t("plugins.teamcal.access.groups")})`}
      hint={t("plugins.teamcal.minimalAccess.groups.hint")}
      className={className}
    />
  );
}

/** The users whose leave days the calendar shows. */
export function TeamCalLeaveDaysUsersField({ className }: FieldProps) {
  const t = useTranslations();
  return (
    <RefsField
      name="includeLeaveDaysForUsers"
      entity="user"
      label={t("calendar.filter.vacation.users._")}
      hint={t("calendar.filter.vacation.users.tooltip")}
      className={className}
    />
  );
}

/** The groups whose members' leave days the calendar shows. */
export function TeamCalLeaveDaysGroupsField({ className }: FieldProps) {
  const t = useTranslations();
  return (
    <RefsField
      name="includeLeaveDaysForGroups"
      entity="group"
      label={t("calendar.filter.vacation.groups._")}
      hint={t("calendar.filter.vacation.groups.tooltip")}
      className={className}
    />
  );
}

/**
 * Which absences of the people above the calendar includes (vacations, availability entries by their status): in
 * the calendar view and in the calendar's subscription alike.
 */
export function TeamCalAbsenceKindsField({ className }: FieldProps) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const readOnly = useFormReadOnly();
  const kinds = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => (state.values as TeamCalValues).absenceKinds
  );
  return (
    <AbsenceKindsSelect
      value={kinds}
      onChange={(next) => form.setFieldValue("absenceKinds", next)}
      label={t("plugins.teamcal.absenceKinds._")}
      hint={t("plugins.teamcal.absenceKinds.tooltip")}
      disabled={readOnly}
      className={className}
    />
  );
}
