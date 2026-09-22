"use client";

import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { FieldHint } from "@/components/shared/form/field-hint";
import { leafKeyOf } from "@/lib/leaf-key";

/**
 * The "replace by" input of the search-&-replace action: the text every occurrence of the field's
 * value is replaced with (backend `MassUpdateParameter.replaceText`). Multi-line when the field is.
 */
export function MassUpdateReplaceInput({
  value,
  rows,
  onChange,
}: {
  value: string | undefined;
  rows: number | undefined;
  onChange: (value: string | undefined) => void;
}) {
  const t = useTranslations();
  const label = t(leafKeyOf("massUpdate.field.replace", t.has));
  const shared = {
    value: value ?? "",
    "aria-label": label,
    onChange: (event: { target: { value: string } }) =>
      onChange(event.target.value || undefined),
  };
  return (
    <div className="space-y-1">
      <div className="flex items-center gap-1">
        <span className="text-[11px] uppercase tracking-wide text-muted-foreground">
          {label}
        </span>
        <FieldHint hint={t("massUpdate.field.replace.info")} label={label} />
      </div>
      {rows ? <Textarea rows={rows} {...shared} /> : <Input {...shared} />}
    </div>
  );
}
