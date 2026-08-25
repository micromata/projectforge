"use client";

import { useTranslations } from "next-intl";
import { FormAlert } from "@/components/shared/form-alert";

/**
 * The invoices whose amount could not be converted into the system currency, so their own amount entered
 * the sums above them (`AbstractRechnungsStatistik.convertToSystemCurrency`).
 *
 * Above the statistics rather than beside them, as the Wicket list has it: the warning says that the sums
 * next to it are not comparable, so it has to be read before them, not after.
 *
 * Absent for every installation with a single currency — which is most of them, and why this costs no
 * space there.
 */
export function CurrencyConversionWarnings({
  warnings,
}: {
  /** Each entry is a sentence the backend composed and translated (`fibu.rechnung.currencyConversionFailed`). */
  warnings?: string[] | null;
}) {
  const t = useTranslations();
  if (!warnings?.length) return null;

  return (
    <FormAlert tone="error" className="rounded-none border-x-0 border-t-0">
      <p className="font-semibold">
        {t("fibu.rechnung.currencyConversion.warnings.header")}
      </p>
      <ul className="mt-0.5 list-disc pl-4">
        {warnings.map((warning) => (
          <li key={warning}>{warning}</li>
        ))}
      </ul>
    </FormAlert>
  );
}
