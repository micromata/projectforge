"use client";

import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { useFormatContext } from "@/hooks/use-format";
import { formatTimestampMinutes } from "@/lib/format";
import {
  RING_LOG_EVENT,
  clearRingLog,
  readRingLog,
  type RingLogEntry,
} from "@/lib/ring-log";

/** The last signals of this browser, newest first: whether it rang, when, and why (or why not). */
export function RingLogView() {
  const t = useTranslations();
  const ctx = useFormatContext();
  const [entries, setEntries] = useState<RingLogEntry[]>([]);
  useEffect(() => {
    const load = () => setEntries(readRingLog().reverse());
    load();
    window.addEventListener(RING_LOG_EVENT, load);
    window.addEventListener("storage", load);
    return () => {
      window.removeEventListener(RING_LOG_EVENT, load);
      window.removeEventListener("storage", load);
    };
  }, []);
  return (
    <section className="flex flex-col gap-1.5" aria-labelledby="ringLogHeading">
      <div className="flex items-center justify-between gap-2">
        <h3 id="ringLogHeading" className="text-xs font-semibold">
          {t("chat.secret.alert.log")}
        </h3>
        {entries.length > 0 && (
          <Button
            type="button"
            size="sm"
            variant="ghost"
            onClick={clearRingLog}
          >
            {t("chat.secret.alert.logClear")}
          </Button>
        )}
      </div>
      {entries.length === 0 ? (
        <p className="text-muted-foreground">
          {t("chat.secret.alert.logEmpty")}
        </p>
      ) : (
        <ul className="flex max-h-56 flex-col gap-1 overflow-y-auto">
          {entries.map((entry) => (
            <li key={entry.at} className="flex flex-col">
              <span>
                <span className="font-medium">
                  {entry.rang
                    ? t("chat.secret.alert.logRang")
                    : entry.silent === "muted"
                      ? t("chat.secret.alert.logMuted")
                      : t("chat.secret.alert.logBlocked")}
                </span>
                {" · "}
                {formatTimestampMinutes(entry.at, ctx)}
              </span>
              <span className="text-muted-foreground">
                {t("chat.secret.alert.logSecretOffer", {
                  arg0: entry.arg ?? "",
                })}
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
