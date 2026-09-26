"use client";

import { useTranslations } from "next-intl";
import { EntityAutocompleteField } from "@/components/shared/form/entity-autocomplete-field";
import { cn } from "@/lib/utils";

/**
 * The group and the structure element (task) an access entry grants rights on — the pair that is the
 * entity's unique key (`GroupTaskAccessDO`), so both are picked before the matrix means anything.
 *
 * Custom rather than declared: the DO serializes group and task id-only (`IdOnlySerializer`), so the
 * `GroupTaskAccess` DTO carries them as references an autocomplete binds to, not as a data type a
 * metadata-driven field could render. Group has no generated metadata at all (`metadataLess`); task
 * does (a `TASK` field), so its `required` rule still comes from the entity.
 */
export function GroupTaskFields({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <div
      className={cn(
        "grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-2",
        className
      )}
    >
      <EntityAutocompleteField
        name="group"
        label={t("group")}
        entity="group"
        metadataLess
      />
      <EntityAutocompleteField name="task" label={t("task")} entity="task" />
    </div>
  );
}
