"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react";
import {
  Delete02Icon,
  RotateLeft01Icon,
  Undo02Icon,
} from "@hugeicons/core-free-icons";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { FieldHint } from "@/components/shared/form/field-hint";
import { HighlightedText } from "@/components/shared/highlighted-text";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import type { PropertyParam } from "@/lib/rs/properties-editor";
import {
  isEffectiveEdit,
  isUndocumented,
  type PropertyEdit,
} from "./properties-editor-model";
import { hasPropertyInfo, PropertyInfo } from "./property-info";
import { PropertyValueInput } from "./property-value-input";

/**
 * One parameter on one line: key with its explanation behind an info icon (see [PropertyInfo]), the
 * input, undo / reset to default (an undocumented key has no default, it is deleted). Below it only
 * what needs attention: the running system uses another value than the file (restart pending, or
 * overridden by the environment).
 */
export function PropertyRow({
  param,
  edit,
  onEdit,
  query,
  readOnly,
}: {
  param: PropertyParam;
  edit: PropertyEdit | undefined;
  onEdit: (edit: PropertyEdit | undefined) => void;
  query: string;
  readOnly: boolean;
}) {
  const t = useTranslations();
  const removed = edit?.kind === "remove";
  const modified = isEffectiveEdit(param, edit);
  const canReset = param.inFile && !removed && !readOnly;
  const undocumented = isUndocumented(param);

  return (
    <div className="grid gap-1 py-1.5 sm:grid-cols-[minmax(0,2fr)_minmax(0,3fr)_4.5rem] sm:items-center sm:gap-3">
      <div className="flex min-w-0 items-center gap-1.5">
        <span className="font-mono text-xs break-all">
          <HighlightedText text={param.key} query={query} />
        </span>
        {hasPropertyInfo(param) && (
          <FieldHint
            hint={<PropertyInfo param={param} query={query} />}
            label={param.key}
          />
        )}
        {modified && (
          <Badge variant="secondary" className="shrink-0">
            {removed
              ? t("system.propertiesEditor.removed")
              : t("system.propertiesEditor.filter.modified")}
          </Badge>
        )}
      </div>
      <PropertyValueInput
        param={param}
        edit={edit}
        onEdit={onEdit}
        readOnly={readOnly}
      />
      <div className="flex gap-1 sm:justify-end">
        {edit && (
          <RowAction
            icon={Undo02Icon}
            label={t("system.propertiesEditor.undo")}
            onClick={() => onEdit(undefined)}
          />
        )}
        {canReset && (
          <RowAction
            icon={undocumented ? Delete02Icon : RotateLeft01Icon}
            label={
              undocumented
                ? t("system.propertiesEditor.delete")
                : t("system.propertiesEditor.reset._")
            }
            hint={
              undocumented
                ? t("system.propertiesEditor.deleteInfo")
                : t("system.propertiesEditor.reset.tooltip")
            }
            onClick={() => onEdit({ kind: "remove" })}
          />
        )}
      </div>
      {param.activeDiffers && (
        <p className="text-xs font-medium break-all text-destructive sm:col-span-3">
          {t("system.propertiesEditor.activeDiffers")}
          {!param.secret &&
            ` ${t("system.propertiesEditor.activeValue")}: ${param.activeValue ?? "-"}`}
          {param.activeSource &&
            ` (${t("system.propertiesEditor.activeSource", { arg0: param.activeSource })})`}
        </p>
      )}
    </div>
  );
}

function RowAction({
  icon,
  label,
  hint,
  onClick,
}: {
  icon: IconSvgElement;
  label: string;
  hint?: string;
  onClick: () => void;
}) {
  return (
    <HintTooltip text={hint ? `${label}: ${hint}` : label}>
      <Button
        size="icon-sm"
        variant="ghost"
        aria-label={label}
        onClick={onClick}
      >
        <HugeiconsIcon icon={icon} size={14} />
      </Button>
    </HintTooltip>
  );
}
