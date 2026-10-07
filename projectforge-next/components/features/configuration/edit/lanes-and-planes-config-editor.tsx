"use client";

import { useTranslations } from "next-intl";
import { FieldError } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { NumberBox } from "@/components/shared/form/number-box";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import {
  EMPTY_ADDITIONAL_USER,
  parseLanesAndPlanesConfig,
  type LanesAndPlanesAdditionalUser,
  type LanesAndPlanesConfig,
} from "../lanes-and-planes-config";
import type { JsonEditorProps } from "./json-editors";
import { KostNumbersPicker } from "./kost-numbers-picker";
import { LanesAndPlanesRows } from "./lanes-and-planes-rows";
import { ListOrTextSetting } from "./list-or-text-setting";

/**
 * The editor of the parameter `lanesAndPlanes`: its JSON object as one form value (`stringValue`), edited
 * through an input per setting. Every change writes the whole object back, so the form posts exactly what
 * the backend validates (`LanesAndPlanesSettings.validate`); its errors arrive on `stringValue`, each naming
 * the setting it is about, and are listed above the inputs.
 */
export function LanesAndPlanesConfigEditor({
  label,
  hint,
  className,
}: JsonEditorProps) {
  const t = useTranslations("lanesAndPlanes.config");
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  return (
    <form.Field name={"stringValue" as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const invalid = !meta.isValid;
        const errors = fieldErrors(meta, label);
        // Each backend message starts with the label of its setting ("Allgemeine Kost2 3: …"), so only
        // the settings named by one are marked, not the whole editor.
        const hasError = (settingLabel: string) =>
          invalid &&
          errors.some(
            (error) =>
              error.includes(`${settingLabel} `) ||
              error.includes(`${settingLabel}:`)
          );
        const { config, invalid: invalidJson } = parseLanesAndPlanesConfig(
          field.state.value as string | null
        );
        const update = (patch: Partial<LanesAndPlanesConfig>) =>
          field.handleChange(JSON.stringify({ ...config, ...patch }));
        return (
          <FieldShell
            name="stringValue"
            label={label}
            hint={hint}
            invalid={false}
            errors={[]}
            className={className}
            ids={ids}
          >
            <div
              id={ids.controlId}
              role="group"
              aria-labelledby={ids.labelId}
              className="grid gap-4 rounded-md border p-3"
            >
              {invalid && errors.length > 0 && (
                <div className="grid gap-1">
                  {errors.map((error) => (
                    <FieldError key={error}>{error}</FieldError>
                  ))}
                </div>
              )}
              {invalidJson && (
                <p className="text-xs text-warning">{t("invalidJson")}</p>
              )}
              <div className="grid gap-4 md:max-w-xs">
                <LanesAndPlanesRows<number | null>
                  label={t("accountingInvoiceProfileIds._")}
                  hint={t("accountingInvoiceProfileIds.tooltip")}
                  invalid={hasError(t("accountingInvoiceProfileIds._"))}
                  ariaLabel={t("accountingInvoiceProfileIds._")}
                  rows={config.accountingInvoiceProfileIds}
                  empty={null}
                  onChange={(accountingInvoiceProfileIds) =>
                    update({ accountingInvoiceProfileIds })
                  }
                  renderInput={(value, set, ariaLabel) => (
                    <NumberBox
                      value={value}
                      fractionDigits={0}
                      grouped={false}
                      aria-label={ariaLabel}
                      onChange={set}
                      onBlur={field.handleBlur}
                    />
                  )}
                />
              </div>
              <ListOrTextSetting
                label={t("generalKost1._")}
                hint={t("generalKost1.tooltip")}
                invalid={hasError(t("generalKost1._"))}
                entries={config.generalKost1}
                placeholder="1.005.01.00"
                onChange={(generalKost1) => update({ generalKost1 })}
                onBlur={field.handleBlur}
                list={
                  <KostNumbersPicker
                    kind="cost1"
                    value={config.generalKost1}
                    ariaLabel={t("generalKost1._")}
                    onChange={(generalKost1) => {
                      update({ generalKost1 });
                      field.handleBlur();
                    }}
                  />
                }
              />
              <ListOrTextSetting
                label={t("generalKost2._")}
                hint={t("generalKost2.tooltip")}
                invalid={hasError(t("generalKost2._"))}
                entries={config.generalKost2}
                placeholder="5.999.10.09"
                onChange={(generalKost2) => update({ generalKost2 })}
                onBlur={field.handleBlur}
                list={
                  <KostNumbersPicker
                    kind="cost2"
                    value={config.generalKost2}
                    ariaLabel={t("generalKost2._")}
                    onChange={(generalKost2) => {
                      update({ generalKost2 });
                      field.handleBlur();
                    }}
                  />
                }
              />
              <ListOrTextSetting
                label={t("kost2Patterns._")}
                hint={t("kost2Patterns.tooltip")}
                invalid={hasError(t("kost2Patterns._"))}
                entries={config.kost2Patterns}
                placeholder="5.*.02"
                onChange={(kost2Patterns) => update({ kost2Patterns })}
                onBlur={field.handleBlur}
                list={
                  <LanesAndPlanesRows<string>
                    rows={config.kost2Patterns}
                    empty=""
                    onChange={(kost2Patterns) => update({ kost2Patterns })}
                    ariaLabel={t("kost2Patterns._")}
                    renderInput={(value, set, ariaLabel) => (
                      <Input
                        value={value}
                        placeholder="5.*.02"
                        aria-label={ariaLabel}
                        onChange={(e) => set(e.target.value)}
                        onBlur={field.handleBlur}
                      />
                    )}
                  />
                }
              />
              <LanesAndPlanesRows<LanesAndPlanesAdditionalUser>
                label={t("additionalUsers._")}
                hint={t("additionalUsers.tooltip")}
                invalid={hasError(t("additionalUsers._"))}
                ariaLabel={t("additionalUsers._")}
                rows={config.additionalUsers}
                empty={EMPTY_ADDITIONAL_USER}
                onChange={(additionalUsers) => update({ additionalUsers })}
                renderInput={(value, set, ariaLabel) => (
                  <div className="grid gap-2 md:grid-cols-[2fr_1fr_1fr]">
                    {(
                      [
                        ["email", t("additionalUsers.email")],
                        ["firstName", t("additionalUsers.firstName")],
                        ["lastName", t("additionalUsers.lastName")],
                      ] as const
                    ).map(([key, placeholder]) => (
                      <Input
                        key={key}
                        value={value[key]}
                        type={key === "email" ? "email" : "text"}
                        placeholder={placeholder}
                        aria-label={`${ariaLabel}: ${placeholder}`}
                        onChange={(e) =>
                          set({ ...value, [key]: e.target.value })
                        }
                        onBlur={field.handleBlur}
                      />
                    ))}
                  </div>
                )}
              />
            </div>
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
