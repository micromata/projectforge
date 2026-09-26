"use client";

import { useTranslations } from "next-intl";
import { EntityAutocompleteField } from "@/components/shared/form/entity-autocomplete-field";
import { TaskSelectField } from "@/components/shared/tasks/task-select-field";
import { leafKeyOf } from "@/lib/leaf-key";
import { cn } from "@/lib/utils";

/**
 * The group and the structure element (task) an access entry grants rights on — the pair that is the
 * entity's unique key (`GroupTaskAccessDO`), so both are picked before the matrix means anything.
 *
 * Custom rather than declared: the DO serializes group and task id-only (`IdOnlySerializer`), so the
 * `GroupTaskAccess` DTO carries them as references a field binds to, not as a data type a
 * metadata-driven field could render. Group has no generated metadata at all (`metadataLess`) and picks
 * through the plain autocomplete; the structure element uses the app-wide [TaskSelectField] (breadcrumb
 * path + tree dialog) and sits on its own row, its `required` rule still coming from the entity.
 */
export function GroupTaskFields({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <div className={cn("flex flex-col gap-y-4", className)}>
      <EntityAutocompleteField
        name="group"
        label={t(leafKeyOf("group", t.has))}
        entity="group"
        metadataLess
      />
      <TaskSelectField name="task" label={t(leafKeyOf("task", t.has))} />
    </div>
  );
}
