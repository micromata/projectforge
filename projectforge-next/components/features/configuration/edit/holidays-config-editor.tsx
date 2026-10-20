"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Delete02Icon, PlusSignIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { FieldError } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { NumberBox } from "@/components/shared/form/number-box";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import {
  EMPTY_HOLIDAY,
  HOLIDAY_DEFINITIONS,
  parseHolidaysConfig,
  serializeHolidaysConfig,
  type HolidayEntry,
  type HolidayId,
} from "../holidays-config";
import { ColumnHeader } from "./column-header";
import type { JsonEditorProps } from "./json-editors";

/** Radix forbids an empty item value: the own holiday is "OWN" in the select. */
const OWN = "OWN";

// Fixed widths besides holiday and label, also of the delete button (1.75rem): the header row has none, so
// an auto column would shift its headers against the inputs.
const COLUMNS =
  "grid grid-cols-[minmax(12rem,1.2fr)_minmax(10rem,1.5fr)_4.5rem_5rem_4.5rem_7.5rem_7.5rem_7rem_1.75rem] items-center gap-2";

/**
 * The editor of the parameter `holidays`: one row per entry, either a change of a pre-defined holiday
 * (ignore it, rename it, make it a partial working day) or an own holiday (label, day, month, optional year,
 * working day). Every change writes the whole array back; the backend's errors arrive on `stringValue`
 * and are listed above the rows.
 */
export function HolidaysConfigEditor({ label, hint, className }: JsonEditorProps) {
  const t = useTranslations();
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
        const { entries, invalid: invalidJson } = parseHolidaysConfig(
          field.state.value as string | null
        );
        const write = (next: HolidayEntry[]) =>
          field.handleChange(serializeHolidaysConfig(next));
        const set = (index: number, patch: Partial<HolidayEntry>) =>
          write(entries.map((entry, i) => (i === index ? { ...entry, ...patch } : entry)));
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
              className="grid gap-3 rounded-md border p-3"
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
                  {t("administration.configuration.holidays.invalidJson")}
                </p>
              )}
              {/* The header row sticks to the top of the page's scrolled column. A sticky element sticks to
                  its nearest scroll container, so from lg on (wide enough for the rows) this one doesn't scroll
                  itself; below, it scrolls horizontally and the header stays at its top. */}
              <div className="overflow-x-auto lg:overflow-visible">
                <div className="grid min-w-[68rem] gap-2">
                  <div
                    className={`${COLUMNS} sticky top-0 z-10 border-b bg-muted py-1 text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground`}
                  >
                    <ColumnHeader label={t("administration.configuration.holidays.holiday")} hint={t("administration.configuration.holidays.help.holiday")} />
                    <ColumnHeader label={t("administration.configuration.holidays.label")} hint={t("administration.configuration.holidays.help.label")} />
                    <ColumnHeader label={t("administration.configuration.holidays.dayOfMonth")} hint={t("administration.configuration.holidays.help.dayOfMonth")} />
                    <ColumnHeader label={t("administration.configuration.holidays.month")} hint={t("administration.configuration.holidays.help.month")} />
                    <ColumnHeader label={t("administration.configuration.holidays.year")} hint={t("administration.configuration.holidays.help.year")} />
                    <ColumnHeader label={t("administration.configuration.holidays.workingDay")} hint={t("administration.configuration.holidays.help.workingDay")} />
                    <ColumnHeader label={t("administration.configuration.holidays.workFraction")} hint={t("administration.configuration.holidays.help.workFraction")} />
                    <ColumnHeader label={t("administration.configuration.holidays.ignore")} hint={t("administration.configuration.holidays.help.ignore")} />
                    <span />
                  </div>
                  {entries.map((entry, index) => {
                    const definition = HOLIDAY_DEFINITIONS.find((def) => def.id === entry.id);
                    const rowLabel = definition
                      ? t(definition.i18nKey)
                      : entry.label || `${index + 1}`;
                    const own = !definition;
                    return (
                      <div
                        // The rows have no identity of their own; the index is what the JSON array has.
                        key={index}
                        className={COLUMNS}
                      >
                        <Select
                          value={entry.id ?? OWN}
                          onValueChange={(value) => {
                            if (value === "") return;
                            set(index, { id: value === OWN ? null : (value as HolidayId) });
                          }}
                        >
                          <SelectTrigger
                            aria-label={`${t("administration.configuration.holidays.holiday")} ${index + 1}`}
                          >
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value={OWN}>
                              {t("administration.configuration.holidays.own")}
                            </SelectItem>
                            {HOLIDAY_DEFINITIONS.map((def) => (
                              <SelectItem key={def.id} value={def.id}>
                                {t(def.i18nKey)}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        <Input
                          value={entry.label}
                          placeholder={definition ? t(definition.i18nKey) : ""}
                          aria-label={`${t("administration.configuration.holidays.label")}: ${rowLabel}`}
                          onChange={(e) => set(index, { label: e.target.value })}
                          onBlur={field.handleBlur}
                        />
                        {own ? (
                          <>
                            <NumberBox
                              value={entry.dayOfMonth}
                              fractionDigits={0}
                              grouped={false}
                              maxDigits={2}
                              aria-label={`${t("administration.configuration.holidays.dayOfMonth")}: ${rowLabel}`}
                              onChange={(dayOfMonth) => set(index, { dayOfMonth })}
                              onBlur={field.handleBlur}
                            />
                            <NumberBox
                              value={entry.month}
                              fractionDigits={0}
                              grouped={false}
                              maxDigits={2}
                              aria-label={`${t("administration.configuration.holidays.month")}: ${rowLabel}`}
                              onChange={(month) => set(index, { month })}
                              onBlur={field.handleBlur}
                            />
                            <NumberBox
                              value={entry.year}
                              fractionDigits={0}
                              grouped={false}
                              maxDigits={4}
                              aria-label={`${t("administration.configuration.holidays.year")}: ${rowLabel}`}
                              onChange={(year) => set(index, { year })}
                              onBlur={field.handleBlur}
                            />
                            <Checkbox
                              checked={entry.workingDay}
                              aria-label={`${t("administration.configuration.holidays.workingDay")}: ${rowLabel}`}
                              onCheckedChange={(checked) =>
                                set(index, {
                                  workingDay: checked === true,
                                  workFraction: checked === true ? entry.workFraction : null,
                                })
                              }
                            />
                          </>
                        ) : (
                          // Date and working day are the pre-defined ones.
                          <>
                            <span className="col-span-4 text-xs text-muted-foreground">
                              {t("administration.configuration.holidays.predefined")}
                            </span>
                          </>
                        )}
                        <NumberBox
                          value={entry.workFraction}
                          fractionDigits={2}
                          disabled={entry.ignore || (own && !entry.workingDay)}
                          aria-label={`${t("administration.configuration.holidays.workFraction")}: ${rowLabel}`}
                          onChange={(workFraction) => set(index, { workFraction })}
                          onBlur={field.handleBlur}
                        />
                        {own ? (
                          <span />
                        ) : (
                          <Checkbox
                            checked={entry.ignore}
                            aria-label={`${t("administration.configuration.holidays.ignore")}: ${rowLabel}`}
                            onCheckedChange={(checked) => set(index, { ignore: checked === true })}
                          />
                        )}
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          aria-label={t("administration.configuration.holidays.remove", { arg0: rowLabel })}
                          onClick={() => write(entries.filter((_, i) => i !== index))}
                        >
                          <HugeiconsIcon icon={Delete02Icon} className="h-4 w-4" />
                        </Button>
                      </div>
                    );
                  })}
                </div>
              </div>
              <ul className="grid list-disc gap-1 pl-5 text-xs text-muted-foreground">
                <li>{t("administration.configuration.holidays.help.predefinedHolidays")}</li>
                <li>{t("administration.configuration.holidays.help.own")}</li>
              </ul>
              <div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => write([...entries, EMPTY_HOLIDAY])}
                >
                  <HugeiconsIcon icon={PlusSignIcon} className="h-4 w-4" />
                  {t("administration.configuration.holidays.add")}
                </Button>
              </div>
            </div>
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
