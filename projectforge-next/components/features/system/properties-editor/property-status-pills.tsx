"use client";

import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import type { PropertyParam } from "@/lib/rs/properties-editor";
import type { PropertyEdits } from "./properties-editor-model";

/**
 * Pills in a group's header (visible while collapsed) counting the keys ProjectForge doesn't know: red
 * for those no longer used (its own namespace), outlined for unknown ones. Keys already marked for
 * removal aren't counted.
 */
export function PropertyStatusPills({
  params,
  edits,
}: {
  params: PropertyParam[];
  edits: PropertyEdits;
}) {
  const t = useTranslations();
  const count = (status: PropertyParam["status"]) =>
    params.filter(
      (param) =>
        param.status === status &&
        param.inFile &&
        edits[param.key]?.kind !== "remove"
    ).length;
  const obsolete = count("OBSOLETE");
  const unknown = count("UNKNOWN");
  return (
    <>
      {obsolete > 0 && (
        <HintTooltip text={t("system.propertiesEditor.obsoleteInfo")}>
          <Badge variant="destructive" className="shrink-0">
            {t("system.propertiesEditor.status.obsolete")}: {obsolete}
          </Badge>
        </HintTooltip>
      )}
      {unknown > 0 && (
        <HintTooltip text={t("system.propertiesEditor.otherInfo")}>
          <Badge variant="outline" className="shrink-0">
            {t("system.propertiesEditor.status.unknown")}: {unknown}
          </Badge>
        </HintTooltip>
      )}
    </>
  );
}
