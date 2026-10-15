"use client";

import { useTranslations } from "next-intl";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";

/**
 * The users and groups of an area, each a multi select bound to the DTO's list of references.
 *
 * Custom fields because none of them is a field of DataTransferAreaDO's metadata: the DO stores csv lists
 * of ids (`adminIds`, …), which is what a declared name is checked against.
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

/** Full access to the area and its settings. */
export function DataTransferAdminsField({ className }: { className?: string }) {
  return (
    <RefsField
      name="admins"
      entity="user"
      labelKey="plugins.datatransfer.admins._"
      hintKey="plugins.datatransfer.admins.info"
      className={className}
    />
  );
}

/** Notified by mail of uploads and external downloads; each one needs access of their own. */
export function DataTransferObserversField({
  className,
}: {
  className?: string;
}) {
  return (
    <RefsField
      name="observers"
      entity="user"
      labelKey="plugins.datatransfer.observers._"
      hintKey="plugins.datatransfer.observers.info"
      className={className}
    />
  );
}

export function DataTransferAccessUsersField({
  className,
}: {
  className?: string;
}) {
  return (
    <RefsField
      name="accessUsers"
      entity="user"
      labelKey="plugins.datatransfer.accessUsers._"
      hintKey="plugins.datatransfer.accessUsers.info"
      className={className}
    />
  );
}

export function DataTransferAccessGroupsField({
  className,
}: {
  className?: string;
}) {
  return (
    <RefsField
      name="accessGroups"
      entity="group"
      labelKey="plugins.datatransfer.accessGroups._"
      hintKey="plugins.datatransfer.accessGroups.info"
      className={className}
    />
  );
}
