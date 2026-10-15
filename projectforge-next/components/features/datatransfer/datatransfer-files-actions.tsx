"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Download04Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import {
  dataTransferDownloadAllUrl,
  type DataTransferView,
} from "@/lib/rs/datatransfer";

/**
 * The head row's actions of an area's page: all files as one ZIP (up to a gigabyte, the backend says
 * whether). The activities and the admin form are tabs of the page (see DataTransferAreaPage).
 */
export function DataTransferFilesActions({ view }: { view: DataTransferView }) {
  const t = useTranslations();
  const id = view.area.id!;

  if (!view.downloadAllAvailable) return null;
  return (
    <HintTooltip text={t("plugins.datatransfer.button.downloadAll.info")}>
      <Button asChild variant="ghost" size="sm" className="gap-1.5">
        {/* A plain anchor: the ZIP is a download the browser handles, not a route. */}
        <a href={dataTransferDownloadAllUrl(id)} download>
          <HugeiconsIcon icon={Download04Icon} size={14} aria-hidden />
          {t("plugins.datatransfer.button.downloadAll._")}
        </a>
      </Button>
    </HintTooltip>
  );
}
