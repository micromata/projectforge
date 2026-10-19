"use client";

import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import type { PropertyGroup } from "@/lib/rs/properties-editor";
import {
  OBSOLETE_GROUP_ID,
  OTHER_GROUP_ID,
  type PropertyEdit,
  type PropertyEdits,
} from "./properties-editor-model";

/** The title of a group; the backend's groups of undocumented keys are translated here. */
export function useGroupTitle(group: PropertyGroup): string {
  const t = useTranslations();
  if (group.id === OBSOLETE_GROUP_ID)
    return t("system.propertiesEditor.group.obsolete");
  if (group.id === OTHER_GROUP_ID)
    return t("system.propertiesEditor.group.other");
  return group.title;
}

/**
 * What the groups of undocumented keys hold, and for the keys no longer used by ProjectForge a way to
 * remove them all at once (deleted on save, the backup keeps them).
 */
export function UndocumentedGroupInfo({
  group,
  edits,
  onEdit,
  readOnly,
}: {
  group: PropertyGroup;
  edits: PropertyEdits;
  onEdit: (key: string, edit: PropertyEdit | undefined) => void;
  readOnly: boolean;
}) {
  const t = useTranslations();
  if (group.id === OTHER_GROUP_ID) {
    return (
      <p className="text-xs text-muted-foreground">
        {t("system.propertiesEditor.otherInfo")}
      </p>
    );
  }
  if (group.id !== OBSOLETE_GROUP_ID) return null;
  const removable = group.blocks
    .flatMap((block) => block.params)
    .filter((param) => param.inFile && edits[param.key]?.kind !== "remove");
  return (
    <div className="flex flex-col items-start gap-2">
      <p className="text-xs text-muted-foreground">
        {t("system.propertiesEditor.obsoleteInfo")}
      </p>
      {!readOnly && removable.length > 0 && (
        <Button
          size="sm"
          variant="outline"
          onClick={() =>
            removable.forEach((param) => onEdit(param.key, { kind: "remove" }))
          }
        >
          {t("system.propertiesEditor.deleteAll", { arg0: removable.length })}
        </Button>
      )}
    </div>
  );
}
