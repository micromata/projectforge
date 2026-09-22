"use client";

import { useDynamicLayout } from "../dynamic-context";
import { fieldDomId } from "../components/dynamic-field";
import { getByPath } from "@/lib/dynamic/path";
import { ColorPicker } from "@/components/shared/color-picker";
import { Label } from "@/components/ui/label";
import type { CustomizedComponentProps } from "./dynamic-customized";

/**
 * The `COLOR_CHOOSER` customised field (`UICustomized.TYPE.COLOR_CHOOSER`, id `"color-chooser"`), used
 * by the calendar settings page for its four event colours. The field name, label and default colour
 * ride the element's `values` map; the picker writes a validated hex back through the form context, so
 * the server's `checkColorCode` (HTTP 406) is what ultimately governs — this only avoids the obvious
 * mistake early. The default colour is offered by seeding the field with it when it is still empty.
 */
export function ColorChooser({ values }: CustomizedComponentProps) {
  const { ui, data, setData, translate, validationErrors } = useDynamicLayout();

  const fieldId = String(values.id ?? "");
  const label = values.label ? String(values.label) : undefined;
  const value = (getByPath(data, fieldId) as string | undefined) ?? "";
  const error = validationErrors.find((e) => e.fieldId === fieldId);
  const domId = fieldDomId(ui.uid, fieldId);

  return (
    <div className="flex flex-col gap-1.5">
      {label && (
        <Label htmlFor={domId} className="text-sm">
          {translate(label)}
        </Label>
      )}
      <ColorPicker
        id={domId}
        value={value}
        onChange={(next) => setData({ [fieldId]: next })}
        invalid={error != null}
        aria-label={label ? translate(label) : fieldId}
      />
      {error && <p className="text-xs text-destructive">{error.message}</p>}
    </div>
  );
}
