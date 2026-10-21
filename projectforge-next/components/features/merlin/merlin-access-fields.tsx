"use client";

import { useTranslations } from "next-intl";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";
import { ReadOnlyValue } from "@/components/shared/form/read-only-value";
import { useEntityData } from "@/components/shared/form/form-context";
import type { MerlinDetail } from "./types";

/**
 * Who administers the template (full access) and who may execute it, by user and by group — each a multi
 * select bound to the DTO's list of references.
 *
 * Custom fields because MerlinTemplateDO stores csv lists of ids (`adminIds`), which is what a declared
 * name is checked against.
 */
export function MerlinAdminsField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <EntityMultiAutocompleteField
      name="admins"
      label={t("plugins.merlin.admins._")}
      hint={t("plugins.merlin.admins.info")}
      entity="user"
      metadataLess
      className={className}
    />
  );
}

export function MerlinAccessUsersField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <EntityMultiAutocompleteField
      name="accessUsers"
      label={t("plugins.merlin.accessUsers._")}
      hint={t("plugins.merlin.accessUsers.info")}
      entity="user"
      metadataLess
      className={className}
    />
  );
}

export function MerlinAccessGroupsField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <EntityMultiAutocompleteField
      name="accessGroups"
      label={t("plugins.merlin.accessGroups._")}
      hint={t("plugins.merlin.accessGroups.info")}
      entity="group"
      metadataLess
      className={className}
    />
  );
}

/** The members of the access groups, as stored: who may execute the template through a group. */
export function MerlinAccessGroupMembers({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  const members = useEntityData<MerlinDetail>()?.accessGroupsAsString;
  if (!members || members === "-") return null;
  return (
    <ReadOnlyValue
      label={t("plugins.merlin.accessGroups._")}
      className={className}
    >
      {members}
    </ReadOnlyValue>
  );
}
