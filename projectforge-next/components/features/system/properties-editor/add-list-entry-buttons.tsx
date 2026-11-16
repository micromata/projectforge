"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Add01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import type { PropertyParam } from "@/lib/rs/properties-editor";
import { listName, listsOf } from "./list-entries";

/** One button per indexed list of a block (e.g. bank accounts), adding an empty entry below it. */
export function AddListEntryButtons({
  params,
  onAdd,
}: {
  params: PropertyParam[];
  onAdd: (list: string) => void;
}) {
  const t = useTranslations();
  const lists = listsOf(params);
  if (lists.length === 0) return null;
  return (
    <div className="flex flex-wrap gap-2 pt-1">
      {lists.map((list) => (
        <Button
          key={list}
          size="sm"
          variant="outline"
          onClick={() => onAdd(list)}
        >
          <HugeiconsIcon icon={Add01Icon} size={14} />
          {t("system.propertiesEditor.addListEntry", { arg0: listName(list) })}
        </Button>
      ))}
    </div>
  );
}
