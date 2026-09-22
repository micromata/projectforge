"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { FieldHint } from "@/components/shared/form/field-hint";
import { leafKeyOf } from "@/lib/leaf-key";
import type {
  MassUpdateFieldMeta,
  MassUpdateParameter,
} from "@/lib/rs/multi-select";
import { MassUpdateValueControl } from "./mass-update-value-control";
import { MassUpdateReplaceInput } from "./mass-update-replace-input";
import {
  availableModes,
  inferMode,
  paramForMode,
  type MassUpdateMode,
} from "./mass-update-mode";

const MODE_LABEL: Record<MassUpdateMode, string> = {
  set: "massUpdate.mode.set",
  append: "massUpdate.mode.append",
  replace: "massUpdate.mode.replace",
  delete: "massUpdate.mode.delete",
};

/**
 * One field of the mass update: an action picked from a dropdown, plus the input(s) that action
 * needs. The action is mutually exclusive by construction, so the illegal combinations the backend
 * would reject (`massUpdate.error.invalidOptionMix`) can't be expressed.
 *
 * No form library and no Zod schema, for the same reason a `UILayout` page has none: the field set
 * only exists at runtime (the backend answers it) and the rules are the backend's. The state is one
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
   * An option key can be a text *and* the parent of its `.info` hint, so it travels as the
   * generator's leaf — see [leafKeyOf]. The key is spelled out at each call rather than composed,
   * because `NextI18nKeyScanner` finds what a source file literally contains.
   */
  const option = (key: string) => t(leafKeyOf(key, t.has));
  const label = meta.label ?? meta.field;
  const modes = availableModes(meta);
  const [mode, setMode] = useState<MassUpdateMode>(() => inferMode(param));
  const patch = (values: Partial<MassUpdateParameter>) =>
    onChange({ ...param, ...values });

  return (
    <div className="grid gap-2 border-b py-3 last:border-b-0 md:grid-cols-[minmax(0,1fr)_auto]">
      <div className="space-y-1.5">
        <span className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
          {label}
        </span>
        {mode !== "delete" && (
          <MassUpdateValueControl
            meta={meta}
            param={param}
            disabled={false}
            label={label}
            onChange={patch}
          />
        )}
        {mode === "replace" && (
          <MassUpdateReplaceInput
            value={param.replaceText}
            rows={meta.rows}
            onChange={(replaceText) => patch({ replaceText })}
          />
        )}
        {mode === "delete" && meta.replaceOption && (
          <div className="flex items-center gap-1">
            <MassUpdateValueControl
              meta={meta}
              param={param}
              disabled={false}
              label={label}
              onChange={patch}
            />
            <FieldHint
              hint={t("massUpdate.mode.delete.searchHint")}
              label={option("massUpdate.mode.delete")}
            />
          </div>
        )}
      </div>
      {modes.length > 1 && (
        <div className="space-y-1 md:pt-5">
          <span className="block text-[11px] uppercase tracking-wide text-muted-foreground">
            {t("massUpdate.mode.label")}
          </span>
          <Select
            value={mode}
            onValueChange={(next) => {
              const chosen = next as MassUpdateMode;
              setMode(chosen);
              onChange(paramForMode(chosen, param, meta));
            }}
          >
            <SelectTrigger
              className="w-52"
              aria-label={t("massUpdate.mode.label")}
            >
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {modes.map((value) => (
                <SelectItem key={value} value={value} className="pr-8">
                  {option(MODE_LABEL[value])}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      )}
    </div>
  );
}
