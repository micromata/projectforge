"use client";

import { useTranslations } from "next-intl";
import Link from "next/link";
import { useEntityData } from "@/components/shared/form/form-context";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import type { AccountingRecordDetail } from "./types";

/**
 * The kost2 shares of a reallocated record (Kost2 reallocation), read-only: where the amount counts instead of the
 * record's own cost unit, and the run it comes from. Nothing for a record that isn't reallocated.
 */
export function Kost2SharesField() {
  const t = useTranslations();
  const format = useFormatContext();
  const shares = useEntityData<AccountingRecordDetail>()?.shares;
  if (!shares || shares.length === 0) return null;
  return (
    <div className="col-span-full flex flex-col gap-1">
      <h3 className="text-sm font-semibold">
        {t("fibu.kost2Reallocation.shares")}
      </h3>
      <ul className="flex flex-col gap-0.5 text-sm">
        {shares.map((share, i) => (
          <li key={i} className="flex gap-3">
            <span className="w-64 truncate">{share.kost2?.displayName}</span>
            <span className="w-28 text-right tabular-nums">
              {formatCurrency(share.betrag, format)}
            </span>
            {share.runId != null && (
              <Link
                href={`/kost2-reallocation/${share.runId}`}
                className="text-primary hover:underline"
              >
                {share.runDescription}
              </Link>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
