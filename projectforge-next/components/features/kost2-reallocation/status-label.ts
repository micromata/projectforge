import type { Kost2ReallocationStatus } from "@/lib/rs/kost2-reallocation";

/** The text of a run's status. The keys are spelled out: the i18n generator exports literal keys only. */
export function statusLabel(
  t: (key: string) => string,
  status: Kost2ReallocationStatus
): string {
  switch (status) {
    case "DRAFT":
      return t("fibu.kost2Reallocation.status.DRAFT");
    case "APPLIED":
      return t("fibu.kost2Reallocation.status.APPLIED");
    case "REVERTED":
      return t("fibu.kost2Reallocation.status.REVERTED");
  }
}
