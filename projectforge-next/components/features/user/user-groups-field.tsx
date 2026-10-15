"use client";

import { useTranslations } from "next-intl";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";

/**
 * The groups of the user — the legacy form's group multi select, bound to `User.assignedGroups`. A
 * custom field because a collection is no field of PFUserDO's metadata; the rights below depend on it
 * (see UserRightsField).
 */
export function UserGroupsField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <EntityMultiAutocompleteField
      name="assignedGroups"
      label={t("user.assignedGroups")}
      entity="group"
      metadataLess
      className={className}
    />
  );
}
