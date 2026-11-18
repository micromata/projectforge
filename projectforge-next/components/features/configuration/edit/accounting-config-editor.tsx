"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowDown01Icon,
  ArrowUp01Icon,
  Delete02Icon,
  PlusSignIcon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { FieldError } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import { FieldHint } from "@/components/shared/form/field-hint";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { NumberBox } from "@/components/shared/form/number-box";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import {
  BWA_PRIORITIES,
  EMPTY_BWA_ROW,
  parseAccountingConfig,
  serializeAccountingConfig,
  type AccountingConfig,
  type BwaPriority,
  type BwaRow,
} from "../accounting-config";
import { ColumnHeader } from "./column-header";
import type { JsonEditorProps } from "./json-editors";

/** Radix forbids an empty item value: "not given" is "NONE" in the selects. */
const NONE = "NONE";

// Fixed widths besides the title, also of the actions (3 icon buttons of 1.75rem): the header row has no
// buttons, so an auto column would shift its headers against the inputs.
const ROW_COLUMNS =
  "grid grid-cols-[5rem_10rem_minmax(10rem,1fr)_10rem_8rem_5.5rem_5.5rem_5.5rem_5.25rem] items-start gap-2";

const HEADING = "text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground";

/**
 * The editor of the parameter `fibu.accounting`: the account number ranges of debitors and creditors, the
 * heading and key rows of the business assessment (BWA) and its rows in their order (a record is assigned
 * to the first matching row, so they can be moved). Every change writes the whole object back; the
 * backend's errors arrive on `stringValue` and are listed above the inputs.
 */
export function AccountingConfigEditor({ label, hint, className }: JsonEditorProps) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const debitorsIds = useFieldIds();
  const creditorsIds = useFieldIds();
  const headingIds = useFieldIds();
  const overallPerformanceIds = useFieldIds();
  const merchandisePurchaseIds = useFieldIds();
  const preliminaryResultIds = useFieldIds();
  return (
    <form.Field name={"stringValue" as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const invalid = !meta.isValid;
        const errors = fieldErrors(meta, label);
        const { config, invalid: invalidJson } = parseAccountingConfig(
          field.state.value as string | null
        );
        const update = (patch: Partial<AccountingConfig>) =>
          field.handleChange(serializeAccountingConfig({ ...config, ...patch }));
        const rows = config.rows;
        const setRow = (index: number, patch: Partial<BwaRow>) =>
          update({ rows: rows.map((row, i) => (i === index ? { ...row, ...patch } : row)) });
        const move = (index: number, delta: number) => {
          const target = index + delta;
          if (target < 0 || target >= rows.length) return;
          const next = [...rows];
          [next[index], next[target]] = [next[target], next[index]];
          update({ rows: next });
        };
        const rowIds = rows.map((row) => row.id.trim()).filter((id) => id.length > 0);
        const textSetting = (
          fieldIds: ReturnType<typeof useFieldIds>,
          labelKey: string,
          hintKey: string,
          value: string,
          placeholder: string,
          onChange: (value: string) => void
        ) => (
          <FieldShell label={t(labelKey)} hint={t(hintKey)} invalid={false} errors={[]} ids={fieldIds}>
            <Input
              id={fieldIds.controlId}
              value={value}
              placeholder={placeholder}
              onChange={(e) => onChange(e.target.value)}
              onBlur={field.handleBlur}
            />
          </FieldShell>
        );
        const rowSetting = (
          fieldIds: ReturnType<typeof useFieldIds>,
          labelKey: string,
          value: string,
          onChange: (value: string) => void
        ) => (
          <FieldShell
            label={t(labelKey)}
            hint={t("administration.configuration.accounting.help.keyRow")}
            invalid={false}
            errors={[]}
            ids={fieldIds}
          >
            <Select
              value={value.trim() || NONE}
              onValueChange={(next) => {
                if (next === "") return;
                onChange(next === NONE ? "" : next);
              }}
            >
              <SelectTrigger id={fieldIds.controlId} aria-labelledby={fieldIds.labelId}>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NONE}>
                  {t("administration.configuration.accounting.notGiven")}
                </SelectItem>
                {/* A configured id not (yet) among the rows stays selectable, so it isn't lost. */}
                {[...new Set([...rowIds, ...(value.trim() ? [value.trim()] : [])])].map((id) => (
                  <SelectItem key={id} value={id}>
                    {id}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FieldShell>
        );
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
                <p className="text-xs text-warning">
                  {t("administration.configuration.accounting.invalidJson")}
                </p>
              )}
              <div className="grid gap-4 md:grid-cols-2">
                {textSetting(
                  debitorsIds,
                  "administration.configuration.accounting.debitorsAccountNumberRanges",
                  "administration.configuration.accounting.help.debitors",
                  config.debitorsAccountNumberRanges,
                  "10000-12999",
                  (debitorsAccountNumberRanges) => update({ debitorsAccountNumberRanges })
                )}
                {textSetting(
                  creditorsIds,
                  "administration.configuration.accounting.creditorsAccountNumberRanges",
                  "administration.configuration.accounting.help.creditors",
                  config.creditorsAccountNumberRanges,
                  "70000-79999",
                  (creditorsAccountNumberRanges) => update({ creditorsAccountNumberRanges })
                )}
              </div>
              <div className="grid gap-4 md:grid-cols-4">
                {textSetting(
                  headingIds,
                  "administration.configuration.accounting.heading",
                  "administration.configuration.accounting.help.heading",
                  config.heading,
                  "BWA",
                  (heading) => update({ heading })
                )}
                {rowSetting(
                  overallPerformanceIds,
                  "administration.configuration.accounting.overallPerformance",
                  config.overallPerformance,
                  (overallPerformance) => update({ overallPerformance })
                )}
                {rowSetting(
                  merchandisePurchaseIds,
                  "administration.configuration.accounting.merchandisePurchase",
                  config.merchandisePurchase,
                  (merchandisePurchase) => update({ merchandisePurchase })
                )}
                {rowSetting(
                  preliminaryResultIds,
                  "administration.configuration.accounting.preliminaryResult",
                  config.preliminaryResult,
                  (preliminaryResult) => update({ preliminaryResult })
                )}
              </div>
              <div className="grid gap-2">
                <span className={`${HEADING} flex items-center gap-1`}>
                  {t("administration.configuration.accounting.rows")}
                  <FieldHint
                    hint={t("administration.configuration.accounting.help.rows")}
                    label={t("administration.configuration.accounting.rows")}
                  />
                </span>
                {/* The header row sticks to the top of the page's scrolled column. A sticky element sticks
                    to its nearest scroll container, so from lg on (wide enough for the rows) this one doesn't
                    scroll itself; below, it scrolls horizontally and the header stays at its top. */}
                <div className="overflow-x-auto lg:overflow-visible">
                  <div className="grid min-w-[72rem] gap-2">
                    <div className={`${ROW_COLUMNS} ${HEADING} sticky top-0 z-10 border-b bg-muted py-1`}>
                      <ColumnHeader
                        label={t("administration.configuration.accounting.row.no")}
                        hint={t("administration.configuration.accounting.help.no")}
                      />
                      <ColumnHeader
                        label={t("administration.configuration.accounting.row.id")}
                        hint={t("administration.configuration.accounting.help.id")}
                      />
                      <ColumnHeader
                        label={t("administration.configuration.accounting.row.title")}
                        hint={t("administration.configuration.accounting.help.title")}
                      />
                      <ColumnHeader
                        label={t("administration.configuration.accounting.row.accountRange")}
                        hint={t("administration.configuration.accounting.help.accountRange")}
                      />
                      <ColumnHeader
                        label={t("administration.configuration.accounting.row.priority")}
                        hint={t("administration.configuration.accounting.help.priority")}
                      />
                      <ColumnHeader
                        label={t("administration.configuration.accounting.row.indent")}
                        hint={t("administration.configuration.accounting.help.indent")}
                      />
                      <ColumnHeader
                        label={t("administration.configuration.accounting.row.scale")}
                        hint={t("administration.configuration.accounting.help.scale")}
                      />
                      <ColumnHeader
                        label={t("administration.configuration.accounting.row.unit")}
                        hint={t("administration.configuration.accounting.help.unit")}
                      />
                      <span />
                    </div>
                    {rows.map((row, index) => {
                      const rowLabel = row.title || row.id || row.no || `${index + 1}`;
                      const aria = (key: string) => `${t(key)}: ${rowLabel}`;
                      return (
                        <div
                          // The rows have no identity of their own; the index is what the JSON array has.
                          key={index}
                          className="grid gap-1 border-b pb-2 last:border-b-0"
                        >
                          <div className={ROW_COLUMNS}>
                            <Input
                              value={row.no}
                              placeholder="1051"
                              aria-label={aria("administration.configuration.accounting.row.no")}
                              onChange={(e) => setRow(index, { no: e.target.value })}
                              onBlur={field.handleBlur}
                            />
                            <Input
                              value={row.id}
                              placeholder="gesamtleistung"
                              aria-label={aria("administration.configuration.accounting.row.id")}
                              onChange={(e) => setRow(index, { id: e.target.value })}
                              onBlur={field.handleBlur}
                            />
                            <Input
                              value={row.title}
                              aria-label={aria("administration.configuration.accounting.row.title")}
                              onChange={(e) => setRow(index, { title: e.target.value })}
                              onBlur={field.handleBlur}
                            />
                            <Input
                              value={row.accountRange}
                              placeholder="4000-4799"
                              aria-label={aria("administration.configuration.accounting.row.accountRange")}
                              onChange={(e) => setRow(index, { accountRange: e.target.value })}
                              onBlur={field.handleBlur}
                            />
                            <Select
                              value={row.priority ?? NONE}
                              onValueChange={(value) => {
                                if (value === "") return;
                                setRow(index, {
                                  priority: value === NONE ? null : (value as BwaPriority),
                                });
                              }}
                            >
                              <SelectTrigger
                                aria-label={aria("administration.configuration.accounting.row.priority")}
                              >
                                <SelectValue />
                              </SelectTrigger>
                              <SelectContent>
                                <SelectItem value={NONE}>
                                  {t("administration.configuration.accounting.notGiven")}
                                </SelectItem>
                                {BWA_PRIORITIES.map((priority) => (
                                  <SelectItem key={priority.value} value={priority.value}>
                                    {t(priority.i18nKey)}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                            <NumberBox
                              value={row.indent}
                              fractionDigits={0}
                              grouped={false}
                              maxDigits={1}
                              aria-label={aria("administration.configuration.accounting.row.indent")}
                              onChange={(indent) => setRow(index, { indent })}
                              onBlur={field.handleBlur}
                            />
                            <NumberBox
                              value={row.scale}
                              fractionDigits={0}
                              grouped={false}
                              maxDigits={1}
                              aria-label={aria("administration.configuration.accounting.row.scale")}
                              onChange={(scale) => setRow(index, { scale })}
                              onBlur={field.handleBlur}
                            />
                            <Input
                              value={row.unit}
                              placeholder="%"
                              aria-label={aria("administration.configuration.accounting.row.unit")}
                              onChange={(e) => setRow(index, { unit: e.target.value })}
                              onBlur={field.handleBlur}
                            />
                            <div className="flex">
                              <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                disabled={index === 0}
                                aria-label={t("administration.configuration.accounting.moveUp", { arg0: rowLabel })}
                                onClick={() => move(index, -1)}
                              >
                                <HugeiconsIcon icon={ArrowUp01Icon} className="h-4 w-4" />
                              </Button>
                              <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                disabled={index === rows.length - 1}
                                aria-label={t("administration.configuration.accounting.moveDown", { arg0: rowLabel })}
                                onClick={() => move(index, 1)}
                              >
                                <HugeiconsIcon icon={ArrowDown01Icon} className="h-4 w-4" />
                              </Button>
                              <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                aria-label={t("administration.configuration.accounting.remove", { arg0: rowLabel })}
                                onClick={() => update({ rows: rows.filter((_, i) => i !== index) })}
                              >
                                <HugeiconsIcon icon={Delete02Icon} className="h-4 w-4" />
                              </Button>
                            </div>
                          </div>
                          <Textarea
                            value={row.value}
                            placeholder={t("administration.configuration.accounting.row.value.placeholder")}
                            aria-label={aria("administration.configuration.accounting.row.value._")}
                            className="min-h-8 font-mono"
                            spellCheck={false}
                            onChange={(e) => setRow(index, { value: e.target.value })}
                            onBlur={field.handleBlur}
                          />
                        </div>
                      );
                    })}
                  </div>
                </div>
                <ul className="grid list-disc gap-1 pl-5 text-xs text-muted-foreground">
                  <li>{t("administration.configuration.accounting.help.value")}</li>
                  <li>{t("administration.configuration.accounting.help.order")}</li>
                  <li>{t("administration.configuration.accounting.help.priority")}</li>
                </ul>
                <div>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => update({ rows: [...rows, EMPTY_BWA_ROW] })}
                  >
                    <HugeiconsIcon icon={PlusSignIcon} className="h-4 w-4" />
                    {t("administration.configuration.accounting.add")}
                  </Button>
                </div>
              </div>
            </div>
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
