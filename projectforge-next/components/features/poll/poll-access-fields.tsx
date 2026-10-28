"use client";

import { useTranslations } from "next-intl";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";

/**
 * The attendees and the users with full access to a poll, users and groups — each a multi select bound to the
 * DTO's list of references.
 *
 * Custom fields because none of them is a field of PollDO's metadata: the DO stores csv lists of ids
 * (`attendeeUserIds`, …), which is what a declared name is checked against.
 */
function RefsField({
  name,
  entity,
  labelKey,
  hintKey,
  className,
}: {
  name: string;
  entity: "user" | "group";
  labelKey: string;
  hintKey: string;
  className?: string;
}) {
  const t = useTranslations();
  return (
    <EntityMultiAutocompleteField
      name={name}
      label={t(labelKey)}
      hint={t(hintKey)}
      entity={entity}
      metadataLess
      className={className}
    />
  );
}

type FieldProps = { className?: string };

export function PollAttendeeUsersField({ className }: FieldProps) {
  return (
    <RefsField
      name="attendeeUsers"
      entity="user"
      labelKey="poll.attendees._"
      hintKey="poll.attendees.tooltip"
      className={className}
    />
  );
}

export function PollAttendeeGroupsField({ className }: FieldProps) {
  return (
    <RefsField
      name="attendeeGroups"
      entity="group"
      labelKey="poll.attendeeGroups"
      hintKey="poll.attendees.tooltip"
      className={className}
    />
  );
}

export function PollFullAccessUsersField({ className }: FieldProps) {
  return (
    <RefsField
      name="fullAccessUsers"
      entity="user"
      labelKey="poll.fullAccessUsers"
      hintKey="poll.fullAccess.tooltip"
      className={className}
    />
  );
}

export function PollFullAccessGroupsField({ className }: FieldProps) {
  return (
    <RefsField
      name="fullAccessGroups"
      entity="group"
      labelKey="poll.fullAccessGroups"
      hintKey="poll.fullAccess.tooltip"
      className={className}
    />
  );
}
