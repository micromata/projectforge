"use client";

import { useTranslations } from "next-intl";
import { parseContractTypesConfig } from "../contract-types-config";

/** The contract types in one line (their values), for the value column of the list. */
export function ContractTypesConfigSummary({ json }: { json: string | null | undefined }) {
  const t = useTranslations();
  const { entries, invalid } = parseContractTypesConfig(json);
  if (invalid) {
    return (
      <span className="text-warning">
        {t("administration.configuration.contractTypes.invalidJson")}
      </span>
    );
  }
  return (
    <span className="line-clamp-2">
      {entries
        .map((entry) => entry.value.trim() || entry.label.trim())
        .filter((text) => text.length > 0)
        .join(", ")}
    </span>
  );
}
