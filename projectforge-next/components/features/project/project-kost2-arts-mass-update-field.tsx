"use client";

import { useEffect, useId, useState } from "react";
import { useTranslations } from "next-intl";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { ValueCombobox } from "@/components/shared/value-combobox";
import type {
  MassUpdateFieldMeta,
  MassUpdateParameter,
} from "@/lib/rs/multi-select";

type Kost2ArtsAction = "activate" | "deactivate";

/**
 * The cost 2 types (Kost2-Arten) of the project mass update: pick types, then create/activate their cost 2
 * units on every selected project, or deactivate them — a project without an active unit of a type is left
 * alone. Neither touches an ended project (`ProjectMultiSelectedPageRest.proceedMassUpdate`).
 *
 * A custom field because the generic renderer offers one value per field, not several. The options are the
 * field's `values` ("04: Name"); the picks travel as comma-separated ids in `textValue`, the action as
 * `append` (create/activate) or `delete` (deactivate), so the confirmation reads "add" or "remove". With
 * nothing picked the field posts nothing.
 */
export function ProjectKost2ArtsMassUpdateField({
  meta,
  setParam,
}: {
  meta: MassUpdateFieldMeta;
  setParam: (name: string, param: MassUpdateParameter | undefined) => void;
}) {
  const t = useTranslations();
  const id = useId();
  const [artIds, setArtIds] = useState<string[]>([]);
  const [action, setAction] = useState<Kost2ArtsAction>("activate");
  const label = meta.label ?? meta.field;
  const options = (meta.values ?? []).map((value) => ({
    value: String(value.id),
    label: value.displayName,
  }));

  useEffect(() => {
    setParam(
      meta.field,
      artIds.length === 0
        ? undefined
        : action === "activate"
          ? { textValue: artIds.join(","), append: true }
          : { textValue: artIds.join(","), delete: true }
    );
    // `setParam` is stable (a useCallback in MassUpdateForm); the picks and the action drive the param.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [artIds, action, meta.field]);

  return (
    <div className="grid gap-2 md:grid-cols-[minmax(0,1fr)_auto]">
      <div className="space-y-1.5">
        <span className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
          {label}
        </span>
        <ValueCombobox
          options={options}
          selected={artIds}
          multi
          onChange={setArtIds}
          clearable
          aria-label={label}
        />
      </div>
      <RadioGroup
        value={action}
        onValueChange={(value) => setAction(value as Kost2ArtsAction)}
        aria-label={label}
        className="gap-2 md:w-52 md:pt-5"
      >
        <div className="flex items-center gap-2">
          <RadioGroupItem id={`${id}-activate`} value="activate" />
          <Label htmlFor={`${id}-activate`} className="text-sm font-normal">
            {t("fibu.projekt.massUpdate.kost2Arts.activate")}
          </Label>
        </div>
        <div className="flex items-center gap-2">
          <RadioGroupItem id={`${id}-deactivate`} value="deactivate" />
          <Label htmlFor={`${id}-deactivate`} className="text-sm font-normal">
            {t("fibu.projekt.massUpdate.kost2Arts.deactivate")}
          </Label>
        </div>
      </RadioGroup>
    </div>
  );
}
