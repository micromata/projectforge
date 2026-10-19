"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowRight01Icon } from "@hugeicons/core-free-icons";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { FormAlert } from "@/components/shared/form-alert";
import type { PropertyGroup } from "@/lib/rs/properties-editor";
import { cn } from "@/lib/utils";
import {
  isEffectiveEdit,
  type PropertyEdit,
  type PropertyEdits,
} from "./properties-editor-model";
import { PropertyDescription } from "./property-description";
import { PropertyRow } from "./property-row";
import { PropertyStatusPills } from "./property-status-pills";
import {
  UndocumentedGroupInfo,
  useGroupTitle,
} from "./undocumented-group-info";

/**
 * One group (topic) of parameters, collapsed until opened; opened while searching or filtering, so
 * every hit is visible. Its blocks share a description shown once above their parameters.
 */
export function PropertyGroupSection({
  group,
  forceOpen,
  edits,
  onEdit,
  query,
  readOnly,
}: {
  group: PropertyGroup;
  forceOpen: boolean;
  edits: PropertyEdits;
  onEdit: (key: string, edit: PropertyEdit | undefined) => void;
  query: string;
  readOnly: boolean;
}) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  const params = group.blocks.flatMap((block) => block.params);
  const inFile = params.filter((param) => param.inFile).length;
  const modified = params.filter((param) =>
    isEffectiveEdit(param, edits[param.key])
  ).length;
  const expanded = open || forceOpen;
  const title = useGroupTitle(group);

  return (
    <Collapsible
      open={expanded}
      onOpenChange={setOpen}
      className="rounded-md border"
    >
      <CollapsibleTrigger className="flex w-full items-center gap-2 px-3 py-2 text-left hover:bg-muted/40">
        <HugeiconsIcon
          icon={ArrowRight01Icon}
          strokeWidth={2}
          className={cn(
            "size-4 shrink-0 transition-transform",
            expanded && "rotate-90"
          )}
        />
        <span className="font-medium">{title}</span>
        <PropertyStatusPills params={params} edits={edits} />
        <span className="text-xs text-muted-foreground">
          {inFile > 0 &&
            t("system.propertiesEditor.inFileCount", { arg0: inFile })}
        </span>
        {modified > 0 && (
          <span className="text-xs font-medium text-primary">
            {t("system.propertiesEditor.changeCount", { arg0: modified })}
          </span>
        )}
      </CollapsibleTrigger>
      <CollapsibleContent className="flex flex-col gap-3 border-t px-3 py-2">
        {group.expert && (
          <FormAlert tone="error">
            {t("system.propertiesEditor.expertWarning")}
          </FormAlert>
        )}
        <UndocumentedGroupInfo
          group={group}
          edits={edits}
          onEdit={onEdit}
          readOnly={readOnly}
        />
        {group.blocks.map((block) => (
          <div key={block.params[0].key} className="flex flex-col gap-1">
            {block.description && (
              <PropertyDescription text={block.description} query={query} />
            )}
            <div className="flex flex-col divide-y">
              {block.params.map((param) => (
                <PropertyRow
                  key={param.key}
                  param={param}
                  edit={edits[param.key]}
                  onEdit={(edit) => onEdit(param.key, edit)}
                  query={query}
                  readOnly={readOnly}
                />
              ))}
            </div>
          </div>
        ))}
      </CollapsibleContent>
    </Collapsible>
  );
}
