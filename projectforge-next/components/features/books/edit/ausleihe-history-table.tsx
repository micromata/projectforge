"use client";

import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import type { BookDetail } from "../types";

interface Row {
  who: string;
  from: string;
  to: string | null;
  active: boolean;
}

// Synthesizes a small loan history from the book's current loan + the
// hard-coded mock entries the design shows. Once the real backend exposes a
// /rs/book/{id}/loanHistory endpoint, swap this for a useQuery hook.
function buildRows(book: BookDetail): Row[] {
  const rows: Row[] = [];
  if (book.lendOutBy && book.lendOutDate) {
    rows.push({
      who: book.lendOutBy.displayName,
      from: book.lendOutDate,
      to: null,
      active: true,
    });
  }
  rows.push(
    {
      who: "Weber, Max",
      from: "2024-01-05",
      to: "2024-03-03",
      active: false,
    },
    {
      who: "Weber, Max",
      from: "2023-09-12",
      to: "2023-12-02",
      active: false,
    }
  );
  return rows;
}

interface Props {
  book: BookDetail;
}

export function AusleiheHistoryTable({ book }: Props) {
  const t = useTranslations("books.edit");
  const rows = buildRows(book);

  if (rows.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">{t("loanHistory.empty")}</p>
    );
  }

  const headers = [
    t("loanHistory.from"),
    t("loanHistory.since"),
    t("loanHistory.until"),
    t("loanHistory.status"),
  ];

  return (
    <div className="overflow-hidden rounded-md border border-border">
      <div className="grid grid-cols-[2fr_1fr_1fr_1fr] bg-muted/40">
        {headers.map((h) => (
          <div
            key={h}
            className="px-3 py-1.5 text-[10.5px] font-bold uppercase tracking-wide text-foreground/70"
          >
            {h}
          </div>
        ))}
      </div>
      {rows.map((r, i) => (
        <div
          key={i}
          className={cn(
            "grid grid-cols-[2fr_1fr_1fr_1fr] border-t border-border/60 transition-colors hover:bg-primary/5",
            i % 2 === 1 && "bg-muted/20"
          )}
        >
          <div className="px-3 py-1.5 text-sm">{r.who}</div>
          <div className="px-3 py-1.5 text-xs text-foreground/70">{r.from}</div>
          <div className="px-3 py-1.5 text-xs text-foreground/70">
            {r.to ?? "—"}
          </div>
          <div className="px-3 py-1.5">
            <span
              className="inline-flex items-center rounded-full px-2 py-0.5 text-[11px] font-semibold"
              style={
                r.active
                  ? {
                      background: "var(--status-loaned-bg)",
                      color: "var(--status-loaned)",
                    }
                  : {
                      background: "var(--status-available-bg)",
                      color: "var(--status-available)",
                    }
              }
            >
              {r.active ? t("status.active") : t("status.returned")}
            </span>
          </div>
        </div>
      ))}
    </div>
  );
}
