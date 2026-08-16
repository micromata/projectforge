"use client";

import { useTranslations } from "next-intl";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { FieldHint } from "@/components/shared/form/field-hint";
import { leafKeyOf } from "@/lib/leaf-key";
import type {
  MassUpdateFieldMeta,
  MassUpdateParameter,
} from "@/lib/rs/multi-select";
import { MassUpdateValueControl } from "./mass-update-value-control";

/**
 * One field of the mass update: the value to set, and which of the three extra actions to take with it.
 *
 * No form library and no Zod schema, for the same reason a `UILayout` page has none: the field set only
 * exists at runtime (the backend answers it, see `MultiSelectMetaData`) and the rules are the
 * backend's — a schema built per response would validate nothing it doesn't. The state is one
 * [MassUpdateParameter] per field, owned by the page.
 */
export function MassUpdateField({
  meta,
  param,
  onChange,
}: {
  meta: MassUpdateFieldMeta;
  param: MassUpdateParameter;
  onChange: (param: MassUpdateParameter) => void;
}) {
  const t = useTranslations();
  /**
   * Every option key is a text *and* the parent of its `.info` hint, so it travels as the generator's
   * leaf — see [leafKeyOf]. Without this the checkboxes read "massUpdate.field.checkbox4deletion".
   *
   * The key is spelled out at each call rather than built here from its last segment: `NextI18nKeyScanner`
   * finds what a source file literally contains, so a composed key lands in no catalog at all.
   */
  const option = (key: string) => t(leafKeyOf(key, t.has));
  const label = meta.label ?? meta.field;
  const patch = (values: Partial<MassUpdateParameter>) =>
    onChange({ ...param, ...values });
  // Clearing a value on every entry leaves nothing to type in, and the backend rejects the two
  // together (`massUpdate.error.invalidOptionMix`) — so the inputs say so before it does.
  const disabled = param.delete === true;

  return (
    <div className="grid gap-2 border-b py-3 last:border-b-0 md:grid-cols-[minmax(0,1fr)_auto]">
      <div className="space-y-1.5">
        <span className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
          {label}
        </span>
        <MassUpdateValueControl
          meta={meta}
          param={param}
          disabled={disabled}
          label={label}
          onChange={patch}
        />
      </div>
      <div className="flex flex-wrap items-start gap-4 md:pt-5">
        {meta.deleteOption && (
          <Option
            label={option("massUpdate.field.checkbox4deletion")}
            hint={t("massUpdate.field.checkbox4deletion.info")}
            checked={param.delete === true}
            onChange={(checked) => patch({ delete: checked || undefined })}
          />
        )}
        {meta.appendOption && (
          <Option
            label={option("massUpdate.field.checkbox4appending")}
            hint={t("massUpdate.field.checkbox4appending.info")}
            checked={param.append === true}
            disabled={disabled}
            onChange={(checked) => patch({ append: checked || undefined })}
          />
        )}
        {meta.replaceOption && (
          <div className="w-40 space-y-1">
            <div className="flex items-center gap-1">
              <span className="text-[11px] uppercase tracking-wide text-muted-foreground">
                {option("massUpdate.field.replace")}
              </span>
              <FieldHint
                hint={t("massUpdate.field.replace.info")}
                label={option("massUpdate.field.replace")}
              />
            </div>
            <Input
              value={param.replaceText ?? ""}
              disabled={disabled}
              onChange={(event) =>
                patch({ replaceText: event.target.value || undefined })
              }
            />
          </div>
        )}
      </div>
    </div>
  );
}

function Option({
  label,
  hint,
  checked,
  disabled,
  onChange,
}: {
  label: string;
  hint: string;
  checked: boolean;
  disabled?: boolean;
  onChange: (checked: boolean) => void;
}) {
  return (
    <label className="flex items-center gap-1.5 text-xs md:pt-1.5">
      <Checkbox
        checked={checked}
        disabled={disabled}
        onCheckedChange={(value) => onChange(value === true)}
      />
      {label}
      <FieldHint hint={hint} label={label} />
    </label>
  );
}
