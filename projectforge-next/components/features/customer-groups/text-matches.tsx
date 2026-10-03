"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { fetchCustomerMatches } from "@/lib/rs/customer-groups";
import { isValidTextMember } from "./values";

/**
 * What a set's customer names and patterns catch right now — customer entities by name and the free
 * texts of orders and invoices — so a pattern catching more than meant (`dh*`) is seen before saving.
 * The server evaluates the unsaved texts (`POST /rs/customerGroups/matches`).
 */
export function TextMatches({ texts }: { texts: string[] }) {
  const t = useTranslations();
  const valid = texts.filter(isValidTextMember);
  const { data } = useQuery({
    queryKey: ["customerGroups", "matches", valid],
    queryFn: ({ signal }) => fetchCustomerMatches(valid, signal),
    enabled: valid.length > 0,
    placeholderData: keepPreviousData,
  });
  if (valid.length === 0 || !data) return null;
  const customers = data.customers ?? [];
  const freeTexts = data.freeTexts ?? [];
  const count = customers.length + freeTexts.length;
  if (count === 0) {
    return (
      <p className="text-xs text-muted-foreground">
        {t("fibu.customerGroups.matchesNone")}
      </p>
    );
  }
  return (
    <p className="text-xs text-muted-foreground">
      {t("fibu.customerGroups.matches", { arg0: count })}:{" "}
      {customers.join(", ")}
      {customers.length > 0 && freeTexts.length > 0 && ", "}
      {/* Italic, as the free-text customers are set apart in the checklists too. */}
      <span className="italic">{freeTexts.join(", ")}</span>
    </p>
  );
}
