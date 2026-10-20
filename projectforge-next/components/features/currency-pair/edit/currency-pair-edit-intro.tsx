"use client";

import { useTranslations } from "next-intl";
import { FormAlert } from "@/components/shared/form-alert";
import { useEntityData } from "@/components/shared/form/form-context";
import type { CurrencyPairDetail } from "../types";

/**
 * For a new currency pair only: the conversion rates can be entered once it is stored — the form stays open
 * after the first save for this (see the route's outcome).
 */
export function CurrencyPairEditIntro() {
  const t = useTranslations();
  const data = useEntityData<CurrencyPairDetail>();
  if (!data || data.id != null) return null;
  return (
    <FormAlert tone="info">{t("fibu.currencyPair.insert.hint")}</FormAlert>
  );
}
