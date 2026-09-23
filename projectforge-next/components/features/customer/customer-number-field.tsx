"use client";

import { useTranslations } from "next-intl";
import { NumberField } from "@/components/shared/form/number-field";
import { useEntityData } from "@/components/shared/form/form-context";
import type { CustomerDetail } from "./types";

/**
 * The customer number — the entity's user-assigned id (`Customer.copyFrom` sets `id = src.nummer`).
 *
 * A custom field because it is editable only while adding: once assigned, the number identifies the
 * customer and must not change, so it turns read-only on an existing entry — exactly as Wicket and
 * `CustomerPagesRest.createEditLayout` do. `page-def`'s `readOnly` is static and cannot flip per
 * entry, hence the field reads the loaded DTO to tell new from saved (a new one has no id yet).
 */
export function CustomerNumberField({ className }: { className?: string }) {
  const t = useTranslations();
  const data = useEntityData<CustomerDetail>();
  const isNew = data?.id == null;
  return (
    <NumberField
      name="nummer"
      label={t("fibu.kunde.nummer")}
      disabled={!isNew}
      // Three digits: the number runs 0..999 (KundeDO.MAX_ID).
      maxDigits={3}
      className={className}
    />
  );
}
