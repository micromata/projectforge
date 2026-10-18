"use client";

import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { NumberField } from "@/components/shared/form/number-field";
import {
  useEntityData,
  useEntityEditForm,
} from "@/components/shared/form/form-context";
import { hasCustomers } from "@/lib/number-ranges";
import { useNumberRanges } from "@/lib/rs/number-ranges";
import type { CustomerDetail } from "./types";

/**
 * The customer number — the entity's user-assigned id (`Customer.copyFrom` sets `id = src.nummer`).
 *
 * A custom field because it is editable only while adding: once assigned, the number identifies the
 * customer and must not change, so it turns read-only on an existing entry — exactly as the former Wicket
 * form did. `page-def`'s `readOnly` is static and cannot flip per entry, hence the field reads the loaded
 * DTO to tell new from saved (a new one has no id yet).
 *
 * The configured number ranges (`/rs/numberRanges`) bound the number: the hint names them, and a new
 * customer is offered the next free number of each range. Whether a number outside the ranges is allowed
 * is the backend's check (`KundeDao.onInsert`, `fibu.numberRange.error.customerNotInRange`).
 */
export function CustomerNumberField({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const data = useEntityData<CustomerDetail>();
  // Fresh: the next free numbers of the ranges change with every customer saved.
  const info = useNumberRanges({ fresh: true });
  const isNew = data?.id == null;
  const ranges = (info?.ranges ?? []).filter(hasCustomers);
  const rangesText = ranges
    .map((r) => `${r.prefix} (${r.customerMin}–${r.customerMax})`)
    .join(", ");
  const hint =
    ranges.length === 0
      ? undefined
      : t(
          info?.allowFreeNumbers
            ? "fibu.kunde.numberRanges.info"
            : "fibu.kunde.numberRanges.infoNoFree",
          { arg0: rangesText }
        );
  const maxDigits = info?.allowFreeNumbers
    ? 9
    : Math.max(3, ...ranges.map((r) => String(r.customerMax).length));
  return (
    <div className={className}>
      <NumberField
        name="nummer"
        label={t("fibu.kunde.nummer")}
        hint={hint}
        disabled={!isNew}
        maxDigits={maxDigits}
      />
      {isNew &&
        ranges
          .filter((r) => r.nextFreeCustomerNumber != null)
          .map((r) => (
            <Button
              key={r.prefix}
              type="button"
              variant="link"
              size="sm"
              className="h-auto px-0 text-xs"
              onClick={() =>
                form.setFieldValue("nummer", r.nextFreeCustomerNumber)
              }
            >
              {t("fibu.kunde.nextFreeNumber", {
                arg0: r.name ?? String(r.prefix),
                arg1: String(r.nextFreeCustomerNumber),
              })}
            </Button>
          ))}
    </div>
  );
}
