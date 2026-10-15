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
  parseContributionMarginConfig,
  type ContributionMarginConfig,
} from "../contribution-margin-config";
import { ContributionMarginBands } from "./contribution-margin-bands";
import { ContributionMarginKost2Rows } from "./contribution-margin-kost2-rows";
import { ContributionMarginRemark } from "./contribution-margin-remark";
import type { JsonEditorProps } from "./json-editors";

/**
 * The editor of the parameter `fibu.contributionMargin`: its JSON object as one form value
 * (`stringValue`), edited through an input per setting. Every change writes the whole object back, so
 * the form posts exactly what the backend validates (`ContributionMarginConfig.validate`); its errors
 * arrive on `stringValue`, each naming the setting it is about, and are listed above the inputs.
 */
export function ContributionMarginConfigEditor({
  label,
  hint,
  className,
}: JsonEditorProps) {
  const t = useTranslations("fibu.auftrag.contributionMargin.config");
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const revenueIds = useFieldIds();
  const rateIds = useFieldIds();
  const targetIds = useFieldIds();
  const redIds = useFieldIds();
  return (
    <form.Field name={"stringValue" as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const invalid = !meta.isValid;
        const errors = fieldErrors(meta, label);
        const { config, invalid: invalidJson } = parseContributionMarginConfig(
          field.state.value as string | null
        );
        const update = (patch: Partial<ContributionMarginConfig>) =>
          field.handleChange(JSON.stringify({ ...config, ...patch }));
        return (
          <FieldShell
            name="stringValue"
            label={label}
            hint={hint}
            invalid={invalid}
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
              <div className="grid gap-4 sm:grid-cols-2">
                <FieldShell
                  label={t("revenueAccounts._")}
                  hint={t("revenueAccounts.tooltip")}
                  invalid={false}
                  errors={[]}
                  ids={revenueIds}
                >
                  <Input
                    id={revenueIds.controlId}
                    value={config.revenueAccounts ?? ""}
                    onChange={(e) =>
                      update({ revenueAccounts: e.target.value || null })
                    }
                    onBlur={field.handleBlur}
                  />
                </FieldShell>
                <FieldShell
                  label={t("hourlyRate._")}
                  hint={t("hourlyRate.tooltip")}
                  invalid={false}
                  errors={[]}
                  ids={rateIds}
                >
                  <NumberBox
                    id={rateIds.controlId}
                    value={config.hourlyRate}
                    fractionDigits={2}
                    onChange={(next) => update({ hourlyRate: next })}
                    onBlur={field.handleBlur}
                  />
                </FieldShell>
                <FieldShell
                  label={t("targetPercentage")}
                  invalid={false}
                  errors={[]}
                  ids={targetIds}
                >
                  <NumberBox
                    id={targetIds.controlId}
                    value={config.targetPercentage}
                    fractionDigits={0}
                    suffix="%"
                    onChange={(next) => update({ targetPercentage: next ?? 0 })}
                    onBlur={field.handleBlur}
                  />
                </FieldShell>
                <FieldShell
                  label={t("redThreshold")}
                  invalid={false}
                  errors={[]}
                  ids={redIds}
                >
                  <NumberBox
                    id={redIds.controlId}
                    value={config.redThreshold}
                    fractionDigits={0}
                    suffix="%"
                    onChange={(next) => update({ redThreshold: next ?? 0 })}
                    onBlur={field.handleBlur}
                  />
                </FieldShell>
              </div>
              <ContributionMarginRemark
                value={config.remark}
                onChange={(remark) => update({ remark })}
                onBlur={field.handleBlur}
              />
              <ContributionMarginBands limits={config} />
              <ContributionMarginKost2Rows
                rows={config.kost2Assignments}
                onChange={(kost2Assignments) => update({ kost2Assignments })}
                onBlur={field.handleBlur}
              />
            </div>
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
