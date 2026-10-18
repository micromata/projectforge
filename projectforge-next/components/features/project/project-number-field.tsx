"use client";

import { useEffect, useMemo } from "react";
import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { NumberField } from "@/components/shared/form/number-field";
import { SegmentedNumberField } from "@/components/shared/form/segmented-number-field";
import type { NumberSegment } from "@/lib/form/number-segments";
import {
  formatCustomerNumber,
  formatProjectNumber,
  maxLocalNumber,
  projectNumber,
  rangeForCustomer,
  rangeForProjectNumber,
  rangeWithoutCustomers,
} from "@/lib/number-ranges";
import {
  useNumberRanges,
  type NumberRange,
  type NumberRangesInfo,
} from "@/lib/rs/number-ranges";
import type { ProjectValues } from "./project-schema";

/**
 * The project number, shaped by the configured number ranges (`/rs/numberRanges`, see lib/number-ranges.ts):
 *
 * - a customer of a range: the customer's part is fixed ("5.123.", "1.0007."), only the local number is the
 *   user's to type, with the digits of the range (2 for range 5, 1 for range 1);
 * - no customer and a range without customers (internal projects): its prefix ("4.") and the local number;
 * - otherwise (a customer of no range, or no range at all): a free project number; empty is the next free one.
 *
 * `.##` stands for the cost 2 type, which each cost 2 unit of the project adds (only with cost units).
 * The backend computes the project number again on save (`ProjektDao.assignProjektnummer`); the form keeps
 * `projektnummer` in line so that the number of an internal project is posted with it.
 *
 * Fixed once the project has cost 2 units, which carry a copy of the number (`numberLocked`, guarded
 * by `ProjektDao.onUpdate`); so is ProjectCustomerField.
 */
export function ProjectNumberField({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const locked = useNumberLocked();
  const { info, range, customerId } = useProjectNumberRange();
  const nummer = useStore(form.store, (s) => values(s).nummer);
  const projektnummer = useStore(form.store, (s) => values(s).projektnummer);
  const computed =
    range != null
      ? projectNumber(range, customerId != null ? customerId : null, nummer)
      : null;

  // Keep the posted values in line with what the field shows (not for a locked number: it can't change).
  useEffect(() => {
    if (info == null || locked) return;
    if (range != null) {
      if (computed !== projektnummer)
        form.setFieldValue("projektnummer", computed);
      return;
    }
    // Free number: one of a range (left over from a customer of a range) doesn't fit any more.
    if (rangeForProjectNumber(info, projektnummer) != null) {
      form.setFieldValue("projektnummer", null);
      return;
    }
    // A free project has no local number of its own (ProjektDao sets it to the project number).
    const local = projektnummer ?? 0;
    if (nummer !== local) form.setFieldValue("nummer", local);
  }, [info, range, computed, projektnummer, nummer, locked, form]);

  const segments = useMemo<NumberSegment[]>(
    () =>
      range == null
        ? []
        : [
            {
              name: "nummer",
              label: t("fibu.projekt.nummer"),
              min: 0,
              max: maxLocalNumber(range),
              digits: range.projectDigits,
            },
          ],
    [range, t]
  );

  if (info == null) return null;
  const suffix = info.costConfigured ? ".##" : undefined;
  if (range == null) {
    return (
      <NumberField
        name="projektnummer"
        label={t("fibu.projekt.projektnummer")}
        hint={t("fibu.projekt.freeProjektnummer.info")}
        disabled={locked}
        maxDigits={9}
        className={className}
      />
    );
  }
  const prefix =
    customerId != null
      ? `${formatCustomerNumber(info, customerId)}.`
      : `${range.prefix}.`;
  const formatted =
    computed != null ? formatProjectNumber(info, computed) : undefined;
  return (
    <SegmentedNumberField
      label={t("fibu.projekt.nummer")}
      segments={segments}
      separator="."
      prefix={prefix}
      suffix={suffix}
      disabled={locked}
      hint={
        locked
          ? t("fibu.projekt.validation.numberLocked")
          : formatted != null && range.projectDigits > 2
            ? `${t("fibu.projekt.projektnummer")}: ${formatted}`
            : undefined
      }
      className={className}
    />
  );
}

/**
 * The number range the project belongs to by its customer (or, without customer, the range without
 * customers), undefined for a free project number or while the ranges load.
 */
export function useProjectNumberRange(): {
  info: NumberRangesInfo | undefined;
  range: NumberRange | undefined;
  customerId: number | null;
} {
  const form = useEntityEditForm();
  const info = useNumberRanges();
  // The id of a customer is its number (Customer.copyFrom).
  const customerId = useStore(
    form.store,
    (s) => values(s).customer?.id ?? null
  );
  const range =
    customerId != null
      ? rangeForCustomer(info, customerId)
      : rangeWithoutCustomers(info);
  return { info, range, customerId };
}

/** Whether number and customer are fixed (see ProjectNumberField); shared with ProjectCustomerField. */
export function useNumberLocked(): boolean {
  const form = useEntityEditForm();
  return useStore(form.store, (s) => values(s).numberLocked);
}

/** The form values; the context is deliberately untyped (form-context). */
function values(state: unknown): ProjectValues {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return (state as any).values as ProjectValues;
}
