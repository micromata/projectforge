"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Activity01Icon, Download04Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import {
  dataTransferDownloadAllUrl,
  type DataTransferView,
} from "@/lib/rs/datatransfer";
import { DataTransferAuditDialog } from "./datatransfer-audit-dialog";

/**
 * The head row's actions of an area's page: all files as one ZIP (up to a gigabyte, the backend says
 * whether) and the activities. The admin form is a tab of the page (see DataTransferAreaPage).
 */
export function DataTransferFilesActions({ view }: { view: DataTransferView }) {
  const t = useTranslations();
  const [auditOpen, setAuditOpen] = useState(false);
  const id = view.area.id!;

  return (
    <>
      {view.downloadAllAvailable && (
        <HintTooltip text={t("plugins.datatransfer.button.downloadAll.info")}>
          <Button asChild variant="ghost" size="sm" className="gap-1.5">
            {/* A plain anchor: the ZIP is a download the browser handles, not a route. */}
            <a href={dataTransferDownloadAllUrl(id)} download>
              <HugeiconsIcon icon={Download04Icon} size={14} aria-hidden />
              {t("plugins.datatransfer.button.downloadAll._")}
            </a>
          </Button>
        </HintTooltip>
      )}
      <Button
        type="button"
        variant="ghost"
        size="sm"
        className="gap-1.5"
        onClick={() => setAuditOpen(true)}
      >
        <HugeiconsIcon icon={Activity01Icon} size={14} aria-hidden />
        {t("plugins.datatransfer.audit.display")}
      </Button>
      {auditOpen && (
        <DataTransferAuditDialog id={id} onClose={() => setAuditOpen(false)} />
      )}
    </>
  );
}
