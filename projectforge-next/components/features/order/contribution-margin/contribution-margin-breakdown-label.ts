"use client";

import { useCallback } from "react";
import { useTranslations } from "next-intl";
import type { ContributionMarginBreakdownRow } from "@/lib/rs/order";

type Row = ContributionMarginBreakdownRow;

/** The label of a row: customer and project, order or task, or "not assignable" for the residual. */
export function useBreakdownRowLabel() {
  const t = useTranslations("fibu.auftrag.contributionMargin");
  return useCallback(
    (row: Row): string => {
      if (row.type === "RESIDUAL") return t("breakdown.residual._");
      if (row.type === "PROJECT") {
        return [row.customer, row.label ?? String(row.projectId)]
          .filter(Boolean)
          .join(" – ");
      }
      return row.label ?? row.id;
    },
    [t]
  );
}
