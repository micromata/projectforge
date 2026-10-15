"use client";

import { useTranslations } from "next-intl";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";

/**
 * The members of the group — the legacy form's user multi select (`UISelect` with `multi = true`),
 * bound to `Group.assignedUsers`.
 *
 * A custom field rather than a plain declaration for one reason: `assignedUsers` is a collection and
 * therefore no field of GroupDO's metadata, which is what a declared name is checked against. The
 * picker itself is the shared one — nothing about it is the group's.
 */
export function AssignedUsersField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <EntityMultiAutocompleteField
      name="assignedUsers"
      label={t("group.assignedUsers")}
      entity="user"
      metadataLess
      className={className}
    />
  );
}
