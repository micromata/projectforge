"use client";

import { useTranslations } from "next-intl";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";

/**
 * Who may execute the script without seeing its code, by group and by user — each a multi select bound
 * to the DTO's list of references.
 *
 * Custom fields because ScriptDO stores csv lists of ids (`executableByGroupIds`), which is what a
 * declared name is checked against; label and hint are that column's.
 */
export function ScriptExecutableByGroupsField({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  return (
    <EntityMultiAutocompleteField
      name="executableByGroups"
      label={t("scripting.script.executableByGroups._")}
      hint={t("scripting.script.executableByGroups.info")}
      entity="group"
      metadataLess
      className={className}
    />
  );
}

export function ScriptExecutableByUsersField({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  return (
    <EntityMultiAutocompleteField
      name="executableByUsers"
      label={t("scripting.script.executableByUsers._")}
      hint={t("scripting.script.executableByUsers.info")}
      entity="user"
      metadataLess
      className={className}
    />
  );
}
