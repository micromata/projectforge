"use client";

import type { DynamicComponentProps } from "../dynamic-renderer";
import { useDynamicLayout } from "../dynamic-context";
import { fieldDomId } from "./dynamic-field";
import { Label } from "@/components/ui/label";
import { FieldHint } from "@/components/shared/form/field-hint";
import { getByPath } from "@/lib/dynamic/path";

/**
 * One radio button of a group (org.projectforge.ui.UIRadioButton).
 *
 * The protocol has no element for the group itself: several RADIOBUTTONs share the same `id`
 * (the property) and differ by `value`, so each renders as a standalone input bound to that
 * property. That rules out the shadcn RadioGroup, which owns the whole group.
 */
export function DynamicRadioButton({ node }: DynamicComponentProps) {
  const { ui, data, setData, translate } = useDynamicLayout();

  const id = node.id as string;
  const value = node.value as string;
  const label = node.label as string | undefined;
  // The name groups the browser's radios; it defaults to the property (UIRadioButton.name).
  const name = (node.name as string | undefined) ?? id;
  const domId = `${fieldDomId(ui.uid, id)}-${value}`;
  const tooltip = node.tooltip as string | undefined;
  const labelText = label ? translate(label) : undefined;

  return (
    <div className="flex items-center gap-2">
      <input
        type="radio"
        id={domId}
        name={fieldDomId(ui.uid, name)}
        value={value}
        checked={getByPath(data, id) === value}
        className="size-4 accent-primary"
        onChange={(e) => {
          if (e.target.checked) setData({ [id]: value });
        }}
      />
      {label && (
        <Label htmlFor={domId} className="text-sm font-normal">
          {translate(label)}
        </Label>
      )}
      {/* The tooltip as a tap-openable ⓘ, not the whole row wrapped in a [HintTooltip]: a tooltip over
          the radio would swallow the tap that should select it on a touch device. */}
      {tooltip && labelText && <FieldHint hint={tooltip} label={labelText} />}
    </div>
  );
}
