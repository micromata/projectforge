"use client";

import { useTranslations } from "next-intl";
import { parseLanesAndPlanesConfig } from "../lanes-and-planes-config";

/** The settings of the Lanes & Planes push in one line, for the value column of the list. */
export function LanesAndPlanesConfigSummary({
  json,
}: {
  json: string | null | undefined;
}) {
  const t = useTranslations("lanesAndPlanes.config");
  const { config, invalid } = parseLanesAndPlanesConfig(json);
  if (invalid) {
    return <span className="text-warning">{t("invalidJson")}</span>;
  }
  const count = (rows: unknown[]) =>
    rows.filter((row) => row != null && `${row}`.trim() !== "").length;
  return (
    <span className="tabular-nums">
      {t("summary", {
        arg0: count(config.generalKost1),
        arg1: count(config.generalKost2),
        arg2: count(config.kost2Patterns),
        arg3: config.additionalUsers.filter((user) => user.email.trim() !== "")
          .length,
      })}
    </span>
  );
}
